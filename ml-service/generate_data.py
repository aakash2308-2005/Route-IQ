"""Generate SYNTHETIC historical trip data for ETA model training.

This data is NOT real. It models realistic patterns (rush-hour peaks, weekend dips, per-mode
variance) so the ETA model has something meaningful to learn (docs/08-ai-ml-design.md section 2.4).
Output: data/historical_trips.csv
"""
from __future__ import annotations

import os
import numpy as np
import pandas as pd

RNG = np.random.default_rng(42)  # fixed seed -> reproducible dataset

MODES = ["WALK", "BUS", "METRO", "CAB"]
# Base speed characteristics per mode (seconds per "unit" segment) and traffic sensitivity.
BASE_SECONDS = {"WALK": 600, "BUS": 480, "METRO": 360, "CAB": 420}
TRAFFIC_SENSITIVITY = {"WALK": 0.0, "BUS": 0.6, "METRO": 0.05, "CAB": 0.8}


def traffic_factor(hour: int) -> float:
    """Congestion multiplier by hour of day (peaks at 8-10 and 17-20)."""
    if 8 <= hour < 10 or 17 <= hour < 20:
        return 1.5
    if 10 <= hour < 17:
        return 1.15
    return 1.0


def generate(n_rows: int = 20000) -> pd.DataFrame:
    rows = []
    for _ in range(n_rows):
        segment_id = int(RNG.integers(1000, 1010))
        mode = MODES[int(RNG.integers(0, len(MODES)))]
        hour = int(RNG.integers(0, 24))
        day_of_week = int(RNG.integers(0, 7))
        is_weekend = 1 if day_of_week >= 5 else 0

        base = BASE_SECONDS[mode]
        factor = traffic_factor(hour)
        # Weekends are lighter; weekday peaks bite harder for traffic-sensitive modes.
        effective_factor = 1.0 + TRAFFIC_SENSITIVITY[mode] * (factor - 1.0) * (0.6 if is_weekend else 1.0)
        noise = RNG.normal(1.0, 0.08)  # +/- 8% random variation
        travel_seconds = max(30, int(base * effective_factor * noise))

        rows.append({
            "segment_id": segment_id,
            "mode": mode,
            "hour": hour,
            "day_of_week": day_of_week,
            "is_weekend": is_weekend,
            "base_seconds": base,
            "travel_seconds": travel_seconds,
        })
    return pd.DataFrame(rows)


def main() -> None:
    os.makedirs("data", exist_ok=True)
    df = generate()
    out = os.path.join("data", "historical_trips.csv")
    df.to_csv(out, index=False)
    print(f"Wrote {len(df)} SYNTHETIC rows to {out}")


if __name__ == "__main__":
    main()
