-- V1 initial schema. See docs/05-database-design.md for rationale, keys, indexes, and constraints.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE preferences (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    objective          VARCHAR(20) NOT NULL DEFAULT 'BALANCED',
    max_walking_meters INT,
    max_cost_cents     INT,
    max_transfers      INT,
    -- Stored as JSON-array text via a JPA AttributeConverter. TEXT (not JSONB) keeps the
    -- JDBC binding simple; these fields are always read/written as a whole, never queried
    -- element-wise (see docs/05-database-design.md section 4).
    preferred_modes    TEXT NOT NULL DEFAULT '[]',
    avoided_modes      TEXT NOT NULL DEFAULT '[]',
    CONSTRAINT chk_pref_objective CHECK (objective IN
        ('FASTEST','CHEAPEST','LEAST_WALKING','FEWEST_TRANSFERS','MOST_RELIABLE','BALANCED')),
    CONSTRAINT chk_pref_walk  CHECK (max_walking_meters IS NULL OR max_walking_meters >= 0),
    CONSTRAINT chk_pref_cost  CHECK (max_cost_cents IS NULL OR max_cost_cents >= 0),
    CONSTRAINT chk_pref_trans CHECK (max_transfers IS NULL OR max_transfers >= 0)
);

CREATE TABLE saved_places (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    label      VARCHAR(80) NOT NULL,
    lat        DOUBLE PRECISION NOT NULL,
    lon        DOUBLE PRECISION NOT NULL,
    node_id    BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_place_user_label UNIQUE (user_id, label)
);
CREATE INDEX idx_saved_places_user ON saved_places (user_id);

CREATE TABLE trip_history (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source_label      VARCHAR(120) NOT NULL,
    dest_label        VARCHAR(120) NOT NULL,
    objective         VARCHAR(20) NOT NULL,
    total_seconds     INT NOT NULL,
    total_cost_cents  INT NOT NULL,
    total_walk_meters INT NOT NULL,
    transfer_count    INT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_trip_seconds CHECK (total_seconds >= 0),
    CONSTRAINT chk_trip_cost    CHECK (total_cost_cents >= 0),
    CONSTRAINT chk_trip_walk    CHECK (total_walk_meters >= 0),
    CONSTRAINT chk_trip_trans   CHECK (transfer_count >= 0)
);
-- Supports the paginated "recent trips" query (FR-36).
CREATE INDEX idx_trip_history_user_created ON trip_history (user_id, created_at DESC);
