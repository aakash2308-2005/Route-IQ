"""FastAPI ETA prediction service (stretch component).

Exposes POST /predict, consumed by the backend's EtaMlAdapter. The backend circuit-breaks this call
and falls back to its own statistical baseline, so this service being down never breaks routing
(docs/08-ai-ml-design.md section 2.6).

Run:
    python generate_data.py && python train.py     # once, to produce models/eta_model.pkl
    uvicorn app:app --host 0.0.0.0 --port 8000
"""
from __future__ import annotations

import json
import os

import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field

MODEL_PATH = os.path.join("models", "eta_model.pkl")
CARD_PATH = os.path.join("models", "model_card.json")

app = FastAPI(title="Smart Commute ETA Service", version="0.1.0")

_model = None
_residual_std = 90.0
_model_version = "eta-0.1.0"


def _load() -> None:
    global _model, _residual_std, _model_version
    if os.path.exists(MODEL_PATH):
        _model = joblib.load(MODEL_PATH)
    if os.path.exists(CARD_PATH):
        with open(CARD_PATH, encoding="utf-8") as f:
            card = json.load(f)
            _residual_std = float(card.get("residual_std_seconds", 90.0))
            _model_version = card.get("model_version", _model_version)


@app.on_event("startup")
def startup() -> None:
    _load()


class PredictRequest(BaseModel):
    segmentId: int = Field(..., ge=0)
    mode: str
    hour: int = Field(12, ge=0, le=23)
    dayOfWeek: int = Field(1, ge=0, le=6)
    baseSeconds: int = Field(..., ge=0)


class PredictResponse(BaseModel):
    etaSeconds: int
    low: int
    high: int
    confidence: float
    modelVersion: str


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "modelLoaded": _model is not None, "modelVersion": _model_version}


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest) -> PredictResponse:
    if _model is None:
        # No trained model available -> echo the base time so the caller still gets a sane value.
        eta = req.baseSeconds
        spread = int(0.25 * eta)
        return PredictResponse(etaSeconds=eta, low=max(0, eta - spread), high=eta + spread,
                               confidence=0.4, modelVersion="untrained")

    features = pd.DataFrame([{
        "mode": req.mode,
        "hour": req.hour,
        "day_of_week": req.dayOfWeek,
        "is_weekend": 1 if req.dayOfWeek >= 5 else 0,
        "base_seconds": req.baseSeconds,
        "segment_id": req.segmentId,
    }])
    eta = int(round(float(_model.predict(features)[0])))
    band = int(round(1.28 * _residual_std))  # ~80% band from residual std
    return PredictResponse(
        etaSeconds=eta,
        low=max(0, eta - band),
        high=eta + band,
        confidence=0.8,
        modelVersion=_model_version,
    )
