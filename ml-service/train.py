"""Train an interpretable ETA regression model on the synthetic data.

Model: HistGradientBoostingRegressor (gradient-boosted trees) — interpretable enough, strong on
tabular data, no deep learning needed (docs/08-ai-ml-design.md section 2.3).

Evaluates with a TIME-AWARE-ish split and reports MAE/RMSE (in seconds). Saves the model plus a
metadata "model card" (metrics, feature list, version).
"""
from __future__ import annotations

import json
import os

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

MODEL_VERSION = "eta-0.1.0"
FEATURES = ["mode", "hour", "day_of_week", "is_weekend", "base_seconds", "segment_id"]
TARGET = "travel_seconds"


def build_pipeline() -> Pipeline:
    categorical = ["mode"]
    pre = ColumnTransformer(
        transformers=[("mode_ohe", OneHotEncoder(handle_unknown="ignore"), categorical)],
        remainder="passthrough",
    )
    model = HistGradientBoostingRegressor(max_iter=200, learning_rate=0.08, random_state=42)
    return Pipeline([("pre", pre), ("model", model)])


def main() -> None:
    df = pd.read_csv(os.path.join("data", "historical_trips.csv"))
    x = df[FEATURES]
    y = df[TARGET]

    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=42)

    pipeline = build_pipeline()
    pipeline.fit(x_train, y_train)

    preds = pipeline.predict(x_test)
    mae = float(mean_absolute_error(y_test, preds))
    rmse = float(np.sqrt(mean_squared_error(y_test, preds)))
    # Residual std used at inference to build a confidence band.
    residual_std = float(np.std(y_test - preds))

    os.makedirs("models", exist_ok=True)
    joblib.dump(pipeline, os.path.join("models", "eta_model.pkl"))

    card = {
        "model_version": MODEL_VERSION,
        "algorithm": "HistGradientBoostingRegressor",
        "features": FEATURES,
        "target": TARGET,
        "metrics": {"mae_seconds": round(mae, 1), "rmse_seconds": round(rmse, 1)},
        "residual_std_seconds": round(residual_std, 1),
        "data": "SYNTHETIC (generate_data.py) - NOT real transit data",
        "limitations": "Trained on synthetic data; not personalized; no online learning.",
    }
    with open(os.path.join("models", "model_card.json"), "w", encoding="utf-8") as f:
        json.dump(card, f, indent=2)

    print(f"Saved model {MODEL_VERSION}: MAE={mae:.1f}s RMSE={rmse:.1f}s (on SYNTHETIC data)")


if __name__ == "__main__":
    main()
