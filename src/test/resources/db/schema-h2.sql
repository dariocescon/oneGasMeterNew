-- Schema OneGasMeter per H2 (compatibilita' SQL Server mode)

CREATE TABLE IF NOT EXISTS telemetry_data (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    serial_number     VARCHAR(64)     NOT NULL,
    meter_ip          VARCHAR(45)     NOT NULL,
    obis_code         VARCHAR(20)     NOT NULL,
    class_id          INT             NOT NULL,
    raw_value         CLOB,
    scaler            DOUBLE          NOT NULL DEFAULT 1.0,
    unit              VARCHAR(20),
    scaled_value      DOUBLE,
    meter_timestamp   TIMESTAMP,
    received_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id        VARCHAR(36)     NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_telemetry_serial ON telemetry_data (serial_number);
CREATE INDEX IF NOT EXISTS ix_telemetry_session ON telemetry_data (session_id);
CREATE INDEX IF NOT EXISTS ix_telemetry_received ON telemetry_data (received_at);

CREATE TABLE IF NOT EXISTS device_commands (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    serial_number     VARCHAR(64)     NOT NULL,
    command_type      VARCHAR(50)     NOT NULL,
    payload           CLOB,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_at       TIMESTAMP,
    error_message     CLOB
);

CREATE INDEX IF NOT EXISTS ix_commands_serial_status ON device_commands (serial_number, status);

-- Anagrafica dispositivi con chiavi crittografiche cifrate (AES-256-GCM)
CREATE TABLE IF NOT EXISTS device_registry (
    serial_number           VARCHAR(64)     PRIMARY KEY,
    logical_device_name     VARCHAR(34),
    device_type             VARCHAR(20),
    metering_point_id       VARCHAR(14),
    encryption_key_enc      VARBINARY(128)  NOT NULL,
    authentication_key_enc  VARBINARY(128)  NOT NULL,
    master_key_enc          VARBINARY(128),
    system_title            VARBINARY(8)    NOT NULL,
    frame_counter_tx        BIGINT          NOT NULL DEFAULT 0,
    frame_counter_rx        BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Log sessioni di comunicazione
CREATE TABLE IF NOT EXISTS session_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id          VARCHAR(36)     NOT NULL,
    serial_number       VARCHAR(64),
    meter_ip            VARCHAR(45)     NOT NULL,
    protocol            VARCHAR(3)      NOT NULL,
    started_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at            TIMESTAMP,
    status              VARCHAR(20)     NOT NULL DEFAULT 'STARTED',
    error_message       CLOB,
    objects_read        INT             DEFAULT 0,
    commands_executed   INT             DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_session_serial ON session_log (serial_number);
CREATE INDEX IF NOT EXISTS ix_session_started ON session_log (started_at);
