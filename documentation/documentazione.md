# OneGasMeter - Documentazione

## Panoramica

OneGasMeter e' un modulo software Java 21 che espone un server TCP per la raccolta di teleletture
dai contatori gas italiani tramite protocollo DLMS/COSEM. Il sistema riceve le connessioni in ingresso
dai dispositivi, legge i dati di misura e di stato, li salva in database SQL Server, e invia eventuali
comandi pendenti prima di chiudere la comunicazione.

## Stack Tecnologico

| Componente | Tecnologia | Versione |
|------------|-----------|----------|
| Linguaggio | Java | 21 |
| Framework | Spring Boot | 4.0.3 |
| Protocollo | DLMS/COSEM (gurux.dlms) | 4.0.78 |
| Database | SQL Server | - |
| ORM | Hibernate (Spring Data JPA) | - |
| Test DB | H2 (modalita' MSSQLServer) | - |
| Logging | SLF4J + Logback | - |
| Build | Maven | 3.9+ |

## Architettura

### Struttura Package

```
com.aton.proj.oneGasMeter/
  OneGasMeterApplication.java        -- Punto di ingresso Spring Boot
  config/
    TcpServerConfig.java             -- Configurazione server TCP
    DlmsSessionConfig.java           -- Configurazione sessione DLMS
  cosem/
    CosemObject.java                 -- Catalogo oggetti COSEM (ENUM)
  entity/
    TelemetryData.java               -- Entita' dati telemetria
    DeviceCommand.java               -- Entita' comandi dispositivo
    CommandStatus.java               -- Stato comandi (ENUM)
    CommandType.java                  -- Tipo comandi (ENUM)
  repository/
    TelemetryDataRepository.java     -- Repository telemetria
    DeviceCommandRepository.java     -- Repository comandi
  service/
    TelemetryService.java            -- Logica salvataggio letture
    CommandService.java              -- Logica gestione comandi
  dlms/
    DlmsTransport.java               -- Interfaccia trasporto
    IncomingTcpTransport.java        -- Trasporto TCP WRAPPER
    DlmsMeterClient.java            -- Client DLMS semplificato
  server/
    TcpServer.java                   -- Server TCP con virtual threads
    MeterSessionHandler.java         -- Gestore sessione per connessione
  exception/
    DlmsCommunicationException.java  -- Eccezione comunicazione
```

### Flusso di Comunicazione

```
Contatore Gas --> TCP connect --> TcpServer
                                     |
                                     v
                              MeterSessionHandler (virtual thread)
                                     |
                              1. Handshake DLMS (AARQ/AARE)
                              2. Lettura numero seriale
                              3. Lettura orologio contatore
                              4. Lettura oggetti COSEM (Data, Register, ecc.)
                              5. Salvataggio in DB (TelemetryService)
                              6. Esecuzione comandi pendenti (CommandService)
                              7. Disconnessione DLMS
                              8. Chiusura socket
```

### Gestione Connessioni

- Ogni connessione in ingresso genera un **virtual thread** (Java 21)
- Il numero massimo di connessioni contemporanee e' controllato da un **semaforo**
- Se il limite viene raggiunto, la connessione viene rifiutata con un log di warning
- Ogni sessione ha un **timeout** configurabile

## Configurazione

### application.properties

```properties
# Server TCP
tcp.server.port=60103                    # Porta di ascolto
tcp.server.max-connections=10000         # Max connessioni contemporanee
tcp.server.backlog=1000                  # Coda di connessioni in attesa
tcp.server.session-timeout-ms=30000      # Timeout sessione (ms)

# Sessione DLMS
dlms.client-address=16                   # Indirizzo client DLMS
dlms.server-address=1                    # Indirizzo server DLMS
dlms.authentication=NONE                 # Livello autenticazione
dlms.password=                           # Password (per LOW/HIGH)
dlms.use-logical-name-referencing=true   # LN referencing

# SQL Server
spring.datasource.url=jdbc:sqlserver://localhost;databaseName=OneGasDbDLMS;encrypt=true;trustServerCertificate=true
spring.datasource.username=oneGasDbDLMS_username
spring.datasource.password=oneGasDbDLMS_password

# JPA
spring.jpa.hibernate.ddl-auto=none
```

### Livelli di Autenticazione DLMS

| Valore | Descrizione |
|--------|------------|
| NONE | Nessuna autenticazione |
| LOW | Password in chiaro |
| HIGH | Challenge-response generico |
| HIGH_MD5 | Challenge-response con MD5 |
| HIGH_SHA1 | Challenge-response con SHA-1 |
| HIGH_SHA256 | Challenge-response con SHA-256 |
| HIGH_GMAC | Challenge-response con AES-GCM (HLS5) |

I contatori gas italiani tipicamente usano HIGH_GMAC per la massima sicurezza.

## Database

### Tabella telemetry_data

Contiene le letture di telemetria ricevute dai contatori.

| Colonna | Tipo | Descrizione |
|---------|------|------------|
| id | BIGINT (PK) | Identificativo auto-incrementale |
| serial_number | NVARCHAR(64) | Numero seriale del contatore |
| meter_ip | NVARCHAR(45) | Indirizzo IP del contatore |
| obis_code | NVARCHAR(20) | Codice OBIS dell'oggetto letto |
| class_id | INT | Class ID COSEM |
| raw_value | NVARCHAR(MAX) | Valore grezzo come stringa |
| scaler | FLOAT | Scalatore (esponente base 10) |
| unit | NVARCHAR(20) | Unita' di misura |
| scaled_value | FLOAT | Valore scalato (raw * 10^scaler) |
| meter_timestamp | DATETIME2 | Timestamp dal contatore |
| received_at | DATETIME2 | Timestamp di ricezione |
| session_id | NVARCHAR(36) | UUID della sessione |

### Tabella device_commands

Contiene i comandi da inviare ai contatori.

| Colonna | Tipo | Descrizione |
|---------|------|------------|
| id | BIGINT (PK) | Identificativo auto-incrementale |
| serial_number | NVARCHAR(64) | Seriale del contatore destinatario |
| command_type | NVARCHAR(50) | Tipo di comando (enum) |
| payload | NVARCHAR(MAX) | Parametri in formato JSON |
| status | NVARCHAR(20) | Stato: PENDING, IN_PROGRESS, DONE, FAILED |
| created_at | DATETIME2 | Data creazione |
| executed_at | DATETIME2 | Data esecuzione |
| error_message | NVARCHAR(MAX) | Messaggio errore (se FAILED) |

### Script SQL

- **SQL Server**: `src/main/resources/db/schema-sqlserver.sql` - da eseguire manualmente prima dell'avvio
- **H2 (test)**: lo schema viene generato automaticamente da Hibernate (`ddl-auto=create-drop`)
- **H2 (riferimento)**: `src/test/resources/db/schema-h2.sql` - script di riferimento per la struttura

## Oggetti COSEM

L'enum `CosemObject` contiene il catalogo degli oggetti COSEM supportati. Ogni entry associa:
- **Codice OBIS**: identificativo dell'oggetto (es. "0.0.96.1.0.255")
- **Class ID**: classe COSEM (1=Data, 3=Register, 7=ProfileGeneric, 8=Clock, ecc.)
- **Attribute Index**: indice dell'attributo da leggere (tipicamente 2)
- **Descrizione**: descrizione in italiano

### Classi COSEM Supportate

| Class ID | Nome | Descrizione |
|----------|------|------------|
| 1 | Data | Oggetti dati semplici (seriale, firmware, ecc.) |
| 3 | Register | Registri con valore, scalatore e unita' |
| 7 | ProfileGeneric | Profili di carico e log eventi |
| 8 | Clock | Orologio del dispositivo |
| 15 | AssociationLN | Associazione Logical Name |
| 40 | PushSetup | Configurazione push |
| 64 | SecuritySetup | Configurazione sicurezza |
| 70 | DisconnectControl | Controllo valvola gas |

## Comandi

### Tipi di Comando Supportati

| Tipo | Descrizione | Payload |
|------|------------|---------|
| SYNC_CLOCK | Sincronizza orologio con ora server | - |
| SET_CLOCK | Imposta orologio a orario specifico | ISO timestamp |
| DISCONNECT_VALVE | Chiudi valvola gas | - |
| RECONNECT_VALVE | Riapri valvola gas | - |
| READ_LOAD_PROFILE | Leggi profilo di carico | JSON con date |
| READ_EVENT_LOG | Leggi log eventi | - |
| CHANGE_PUSH_DESTINATION | Cambia destinazione push | JSON con ip/port |

### Come Inserire un Comando

```sql
INSERT INTO device_commands (serial_number, command_type, payload, status)
VALUES ('ABC123', 'SYNC_CLOCK', NULL, 'PENDING');

-- Comando con payload
INSERT INTO device_commands (serial_number, command_type, payload, status)
VALUES ('ABC123', 'SET_CLOCK', '2026-03-27T12:00:00Z', 'PENDING');
```

Il comando verra' eseguito alla prossima connessione del contatore.

### Come Aggiungere un Nuovo Tipo di Comando

1. Aggiungere il valore nell'enum `CommandType.java`
2. Implementare il caso nel metodo `executeCommand` di `MeterSessionHandler.java`
3. Se necessario, aggiungere il metodo corrispondente in `DlmsMeterClient.java`

## Test

### Test Unitari

- `CosemObjectTest` - Validazione enum COSEM
- `TelemetryDataTest`, `DeviceCommandTest` - Validazione entita'
- `IncomingTcpTransportTest` - Test trasporto TCP con mock socket
- `DlmsMeterClientTest` - Test client DLMS con mock transport
- `TelemetryServiceTest`, `CommandServiceTest` - Test servizi con mock repository
- `MeterSessionHandlerTest` - Test gestione sessione
- `DlmsCommunicationExceptionTest` - Test eccezione

### Test di Integrazione

- `TelemetryDataRepositoryTest`, `DeviceCommandRepositoryTest` - Test repository con H2
- `FullSessionIntegrationTest` - Test end-to-end simulazione sessione completa

### Esecuzione Test

```bash
# Tutti i test (richiede JDK 21)
JAVA_HOME=/path/to/jdk-21 ./mvnw test

# Solo test unitari
./mvnw test -Dtest="CosemObjectTest,TelemetryDataTest,DeviceCommandTest"

# Solo test integrazione
./mvnw test -Dtest="FullSessionIntegrationTest"
```

## Deployment

### Prerequisiti

- JDK 21
- SQL Server con database `OneGasDbDLMS` creato
- Script `schema-sqlserver.sql` eseguito sul database

### Build

```bash
JAVA_HOME=/path/to/jdk-21 ./mvnw clean package -DskipTests
```

### Avvio

```bash
java -jar target/oneGasMeter-0.0.1-SNAPSHOT.jar
```

Il server TCP si avviera' in ascolto sulla porta configurata (default: 60103).
