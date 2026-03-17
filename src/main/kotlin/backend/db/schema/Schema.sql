CREATE TYPE simulation_status_type AS ENUM (
    'queued',
    'running',
    'completed',
    'failed',
    'aborted'
);

CREATE TYPE log_status_type AS ENUM (
    'not_ready',
    'ready',
    'viewed',
    'expired'
);

CREATE TABLE app_user (
    username      VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    last_access    TIMESTAMP
);

CREATE TABLE simulation_config (
    id            BIGSERIAL PRIMARY KEY,
    n_groups      INTEGER NOT NULL,
    n_stations    INTEGER NOT NULL,
    width         INTEGER NOT NULL,
    height        INTEGER NOT NULL,
    verbosity     INTEGER NOT NULL,
    sim_length    INTEGER NOT NULL,
    packet_rate   INTEGER NOT NULL,
    slot_length   INTEGER NOT NULL
    );

CREATE TABLE simulation (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL,
    config_id     BIGINT NOT NULL,
    status        simulation_status_type NOT NULL,
    seed          INT,
    log_status    log_status_type NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    started_at    TIMESTAMP,
    finished_at   TIMESTAMP,
    error_msg     TEXT,
    w_pe          BOOLEAN NOT NULL,
    w_pp          BOOLEAN NOT NULL,
    w_group_file  BOOLEAN NOT NULL,
    w_mp          BOOLEAN NOT NULL,
    w_metrics     BOOLEAN NOT NULL,
    zipped_output BOOLEAN NOT NULL,

    CONSTRAINT fk_simulation_user
        FOREIGN KEY (username)
            REFERENCES app_user(username),

    CONSTRAINT fk_simulation_config
        FOREIGN KEY (config_id)
            REFERENCES simulation_config(id)
);

CREATE TABLE simulation_log (
    id            BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL,
    log_url       TEXT NOT NULL,
    file_size     BIGINT,

    CONSTRAINT fk_log_simulation
        FOREIGN KEY (simulation_id)
            REFERENCES simulation(id)
            ON DELETE CASCADE
);

CREATE TABLE simulation_metrics (
    simulation_id           BIGINT PRIMARY KEY,
    total_packets           INTEGER NOT NULL,
    packets_aborted         INTEGER NOT NULL,
    packets_reached_medium  INTEGER NOT NULL,
    packets_delivered       INTEGER NOT NULL,
    delivery_rate_total     DOUBLE PRECISION NOT NULL,
    delivery_rate_medium    DOUBLE PRECISION NOT NULL,

    CONSTRAINT fk_metrics_simulation
        FOREIGN KEY (simulation_id)
            REFERENCES simulation(id)
            ON DELETE CASCADE
);