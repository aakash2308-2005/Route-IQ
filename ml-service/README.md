# ML ETA Service (stretch)

Optional Python/FastAPI service that predicts segment travel time. The Java backend calls it via
`EtaMlAdapter`, **circuit-broken** with a fallback to a statistical baseline — so this service being
down never breaks route planning.

> All data here is **synthetic** (see `generate_data.py`). It is not real transit data. Any metrics
> in `models/model_card.json` are measured on the synthetic set.

## Run locally

```bash
python -m venv .venv && source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pip install -r requirements.txt
python generate_data.py     # writes data/historical_trips.csv
python train.py             # writes models/eta_model.pkl + model_card.json
uvicorn app:app --host 0.0.0.0 --port 8000
```

## Enable it in the backend

Set `ETA_ML_ENABLED=true` and `ETA_ML_BASE_URL=http://localhost:8000` (env / `.env`). With it
disabled (the default), the backend uses its own statistical baseline.

## Endpoints
- `GET /health` — liveness + whether a model is loaded.
- `POST /predict` — `{segmentId, mode, hour, dayOfWeek, baseSeconds}` → `{etaSeconds, low, high, confidence, modelVersion}`.

## Model
- `HistGradientBoostingRegressor` (gradient-boosted trees). Interpretable, strong on tabular data,
  no deep learning. Features: mode, hour, day_of_week, is_weekend, base_seconds, segment_id.
- Confidence band derived from residual std. See `docs/08-ai-ml-design.md`.
