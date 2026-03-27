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
