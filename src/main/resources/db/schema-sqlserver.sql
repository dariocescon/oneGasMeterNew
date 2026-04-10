-- Schema OneGasMeter per SQL Server
-- Database: OneGasDbDLMS

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'telemetry_data')
CREATE TABLE telemetry_data (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    serial_number     NVARCHAR(64)    NOT NULL,
    meter_ip          NVARCHAR(45)    NOT NULL,
    obis_code         NVARCHAR(20)    NOT NULL,
    class_id          INT             NOT NULL,
    raw_value         NVARCHAR(MAX),
    scaler            FLOAT           NOT NULL DEFAULT 1.0,
    unit              NVARCHAR(20),
    scaled_value      FLOAT,
    meter_timestamp   DATETIME2,
    received_at       DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    session_id        NVARCHAR(36)    NOT NULL
);
GO

-- Indici per ricerche frequenti
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_telemetry_serial')
    CREATE INDEX ix_telemetry_serial ON telemetry_data (serial_number);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_telemetry_session')
    CREATE INDEX ix_telemetry_session ON telemetry_data (session_id);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_telemetry_received')
    CREATE INDEX ix_telemetry_received ON telemetry_data (received_at);
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'device_commands')
CREATE TABLE device_commands (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    serial_number     NVARCHAR(64)    NOT NULL,
    command_type      NVARCHAR(50)    NOT NULL,
    payload           NVARCHAR(MAX),
    status            NVARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at        DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    executed_at       DATETIME2,
    error_message     NVARCHAR(MAX)
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_commands_serial_status')
    CREATE INDEX ix_commands_serial_status ON device_commands (serial_number, status);
GO

-- Anagrafica dispositivi con chiavi crittografiche cifrate (AES-256-GCM)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'device_registry')
CREATE TABLE device_registry (
    serial_number           NVARCHAR(64)    PRIMARY KEY,
    logical_device_name     NVARCHAR(34),
    device_type             NVARCHAR(20),
    metering_point_id       NVARCHAR(14),
    encryption_key_enc      VARBINARY(128)  NOT NULL,
    authentication_key_enc  VARBINARY(128)  NOT NULL,
    master_key_enc          VARBINARY(128),
    system_title            VARBINARY(8)    NOT NULL,
    frame_counter_tx        BIGINT          NOT NULL DEFAULT 0,
    frame_counter_rx        BIGINT          NOT NULL DEFAULT 0,
    created_at              DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    updated_at              DATETIME2       NOT NULL DEFAULT GETUTCDATE()
);
GO

-- Log sessioni di comunicazione
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'session_log')
CREATE TABLE session_log (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id          NVARCHAR(36)    NOT NULL,
    serial_number       NVARCHAR(64),
    meter_ip            NVARCHAR(45)    NOT NULL,
    protocol            NVARCHAR(3)     NOT NULL,
    started_at          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    ended_at            DATETIME2,
    status              NVARCHAR(20)    NOT NULL DEFAULT 'STARTED',
    error_message       NVARCHAR(MAX),
    objects_read        INT             DEFAULT 0,
    commands_executed   INT             DEFAULT 0
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_session_serial')
    CREATE INDEX ix_session_serial ON session_log (serial_number);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_session_started')
    CREATE INDEX ix_session_started ON session_log (started_at);
GO
