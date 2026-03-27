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
