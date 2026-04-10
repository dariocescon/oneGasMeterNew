# OneGasMeter - Documentazione Tecnica

## Indice
1. [Panoramica](#1-panoramica)
2. [Architettura](#2-architettura)
3. [Configurazione](#3-configurazione)
4. [Struttura del Progetto](#4-struttura-del-progetto)
5. [Database](#5-database)
6. [Protocollo DLMS/COSEM](#6-protocollo-dlmscosem)
7. [Oggetti COSEM (CosemObject)](#7-oggetti-cosem)
8. [Compact Frame](#8-compact-frame)
9. [Codici Evento](#9-codici-evento)
10. [Comandi Dispositivo](#10-comandi-dispositivo)
11. [Sicurezza](#11-sicurezza)
12. [Flusso Sessione](#12-flusso-sessione)
13. [Test](#13-test)
14. [Conformita' Normativa](#14-conformita-normativa)

---

## 1. Panoramica

OneGasMeter e' un server Java che riceve connessioni TCP e UDP dai contatori gas italiani (Pietro Fiorentini RSE/RSV, HM-ICON, SSM-ICON) tramite protocollo DLMS/COSEM, conforme alla normativa UNI/TS 11291.

Il server:
- Accetta connessioni in ingresso dai contatori (push spontaneo)
- Decodifica i dati delle compact frame
- Salva le letture nel database
- Esegue i comandi pendenti sul contatore
- Chiude la sessione in modo esplicito per risparmiare la batteria del contatore

### Stack tecnologico
- Java 21 (virtual threads)
- Spring Boot 4.0.3
- Gurux DLMS 4.0.78
- Spring Data JPA
- SQL Server (produzione) / H2 (test)

---

## 2. Architettura

```
Contatore Gas (PF)
    |
    | TCP:60103 oppure UDP:60104
    | (DLMS WRAPPER)
    v
+-------------------+
|   TcpServer       |  <-- Virtual thread per ogni connessione
|   UdpServer       |  <-- Virtual thread per ogni pacchetto
+-------------------+
    |
    v
+-------------------+
| MeterSessionHandler|  <-- Flusso sessione lineare
+-------------------+
    |
    v
+-------------------+
|  DlmsMeterClient  |  <-- Handshake DLMS, lettura/scrittura oggetti
+-------------------+
    |
    v
+-------------------+     +-------------------+
| TelemetryService  |     |  CommandService   |
+-------------------+     +-------------------+
    |                         |
    v                         v
+-------------------+     +-------------------+
|  telemetry_data   |     | device_commands   |
|  device_registry  |     | session_log       |
+-------------------+     +-------------------+
      SQL Server / H2
```

---

## 3. Configurazione

### application.properties

```properties
# Server TCP
tcp.server.port=60103
tcp.server.max-connections=10000
tcp.server.backlog=1000
tcp.server.session-timeout-ms=30000

# Server UDP
udp.server.port=60104
udp.server.max-packet-size=2048
udp.server.retry-count=3
udp.server.retry-delay-ms=1000

# Sessione DLMS (UNI/TS 11291-12-2: Management Client SAP=1)
dlms.client-address=1
dlms.server-address=1
dlms.authentication=HIGH_GMAC
dlms.use-logical-name-referencing=true

# Cifratura chiavi in database
security.key-master-password=${ONEGASMETER_KEY_MASTER_PASSWORD:defaultDevKey123456}

# SQL Server
spring.datasource.url=jdbc:sqlserver://localhost;databaseName=OneGasDbDLMS;encrypt=true;trustServerCertificate=true
spring.datasource.username=oneGasDbDLMS_username
spring.datasource.password=oneGasDbDLMS_password
```

### Variabili d'ambiente
| Variabile | Descrizione |
|-----------|-------------|
| `ONEGASMETER_KEY_MASTER_PASSWORD` | Password master per cifrare le chiavi DLMS in database (AES-256-GCM) |

---

## 4. Struttura del Progetto

```
com.aton.proj.oneGasMeter/
  config/
    TcpServerConfig.java        - Configurazione server TCP
    UdpServerConfig.java        - Configurazione server UDP
    DlmsSessionConfig.java      - Parametri sessione DLMS
  server/
    TcpServer.java              - Server TCP porta 60103
    UdpServer.java              - Server UDP porta 60104 con retry
    MeterSessionHandler.java    - Gestione sessione singolo contatore
  dlms/
    DlmsMeterClient.java        - Client DLMS (handshake, lettura, scrittura, comandi)
    DlmsTransport.java          - Interfaccia trasporto astratta
    IncomingTcpTransport.java   - Trasporto TCP WRAPPER
  cosem/
    CosemObject.java            - Enum con 95 oggetti COSEM e relativi OBIS codes
    CompactFrameParser.java     - Parser per 13 tipi di compact frame
    CompactFrameData.java       - Risultato del parsing di una compact frame
    EventCode.java              - 181 codici evento UNI/TS 11291-12-2
    ValveStatus.java            - Modello stato valvola gas
  entity/
    TelemetryData.java          - Dati di telemetria (JPA)
    DeviceCommand.java          - Comandi pendenti (JPA)
    DeviceRegistry.java         - Anagrafica dispositivi con chiavi cifrate (JPA)
    SessionLog.java             - Log sessioni (JPA)
    CommandType.java            - 28 tipi di comando supportati
    CommandStatus.java          - Stati del comando (PENDING/IN_PROGRESS/DONE/FAILED)
  repository/
    TelemetryDataRepository.java
    DeviceCommandRepository.java
    DeviceRegistryRepository.java
    SessionLogRepository.java
  service/
    TelemetryService.java       - Salvataggio e query dati di telemetria
    CommandService.java         - Ciclo di vita dei comandi
    KeyEncryptionService.java   - Cifratura/decifratura chiavi DLMS (AES-256-GCM)
  exception/
    DlmsCommunicationException.java - Eccezione con codici errore DLMS
```

---

## 5. Database

### Tabelle

#### telemetry_data
Ogni riga rappresenta una singola lettura di un oggetto COSEM.

| Colonna | Tipo | Descrizione |
|---------|------|-------------|
| id | BIGINT PK | Auto-increment |
| serial_number | VARCHAR(64) | Numero seriale del contatore |
| meter_ip | VARCHAR(45) | Indirizzo IP del contatore |
| obis_code | VARCHAR(20) | Codice OBIS dell'oggetto letto |
| class_id | INT | Class ID COSEM |
| raw_value | TEXT | Valore grezzo |
| scaler | DOUBLE | Fattore di scala (10^scaler) |
| unit | VARCHAR(20) | Unita' di misura |
| scaled_value | DOUBLE | Valore scalato (raw * 10^scaler) |
| meter_timestamp | TIMESTAMP | Timestamp orologio contatore |
| received_at | TIMESTAMP | Timestamp ricezione server |
| session_id | VARCHAR(36) | UUID sessione |

#### device_commands
Comandi da inviare ai contatori durante la prossima sessione.

| Colonna | Tipo | Descrizione |
|---------|------|-------------|
| id | BIGINT PK | Auto-increment |
| serial_number | VARCHAR(64) | Contatore destinatario |
| command_type | VARCHAR(50) | Tipo comando (vedi CommandType) |
| payload | TEXT | Parametri JSON/Base64 |
| status | VARCHAR(20) | PENDING, IN_PROGRESS, DONE, FAILED |
| created_at | TIMESTAMP | Creazione |
| executed_at | TIMESTAMP | Esecuzione |
| error_message | TEXT | Dettaglio errore |

#### device_registry
Anagrafica dispositivi con chiavi crittografiche cifrate.

| Colonna | Tipo | Descrizione |
|---------|------|-------------|
| serial_number | VARCHAR(64) PK | Numero seriale |
| logical_device_name | VARCHAR(34) | Nome logico DLMS |
| device_type | VARCHAR(20) | RSE, RSV, HM_ICON, SSM_ICON |
| metering_point_id | VARCHAR(14) | Punto di riconsegna PDR |
| encryption_key_enc | VARBINARY(128) | Chiave cifratura DLMS (cifrata AES-256-GCM) |
| authentication_key_enc | VARBINARY(128) | Chiave autenticazione (cifrata) |
| master_key_enc | VARBINARY(128) | Master key (cifrata, opzionale) |
| system_title | VARBINARY(8) | System Title DLMS (8 byte) |
| frame_counter_tx | BIGINT | Frame counter messaggi inviati |
| frame_counter_rx | BIGINT | Frame counter messaggi ricevuti |

#### session_log
Log delle sessioni di comunicazione.

| Colonna | Tipo | Descrizione |
|---------|------|-------------|
| id | BIGINT PK | Auto-increment |
| session_id | VARCHAR(36) | UUID sessione |
| serial_number | VARCHAR(64) | Contatore |
| meter_ip | VARCHAR(45) | IP contatore |
| protocol | VARCHAR(3) | TCP o UDP |
| started_at | TIMESTAMP | Inizio |
| ended_at | TIMESTAMP | Fine |
| status | VARCHAR(20) | STARTED, COMPLETED, FAILED |
| objects_read | INT | Oggetti letti con successo |
| commands_executed | INT | Comandi eseguiti |

---

## 6. Protocollo DLMS/COSEM

### Trasporto
- **DLMS WRAPPER** su TCP (porta 60103) e UDP (porta 60104)
- Header wrapper: 8 byte (version 2B + source wPort 2B + dest wPort 2B + length 2B)
- Version: 0x0001

### Associazione
Il server opera come **Management Client** (SAP = 1) con:
- Autenticazione: HLS mechanism 5 (HIGH_GMAC)
- Cifratura: AES-GCM-128 (Security Suite 0, Policy 3 = encrypted + authenticated)
- System Title default SAC: "SACSACSA" (0x53,0x41,0x43,0x53,0x41,0x43,0x53,0x41)

### Timeout (da UNI/TS 11291-12-4)
| Parametro | GPRS | NB-IoT |
|-----------|------|--------|
| session_max_duration | 40s (max 60s) | 80s |
| inactivity_timeout | 20s | 20s |
| Tempo risposta SAC | max 1.1s | max 1.1s |

---

## 7. Oggetti COSEM

L'enum `CosemObject` contiene 95 oggetti organizzati per categoria:

| Categoria | Oggetti | Class ID |
|-----------|---------|----------|
| Identificazione | 6 | 1 (Data) |
| Orologio e Tempo | 4 | 1, 8 (Clock) |
| Totalizzatori Volume | 6 | 3 (Register) |
| Portata | 1 | 4 (ExtendedRegister) |
| Diagnostica e Batteria | 13 | 1, 3 |
| Valvola | 10 | 70, 1, 21, 22 |
| Fatturazione (EOB) | 4 | 1 |
| Piano Tariffario | 2 | 8192 (proprietaria UNI/TS) |
| Profili e Log | 4 | 7 (ProfileGeneric) |
| Push Setup | 8 | 40, 22 |
| Frame Counter | 5 | 1 |
| Associazioni | 5 | 15 (AssociationLN) |
| Security Setup | 4 | 64 |
| Configurazione | 5 | 9, 1, 3 |
| Firmware | 1 | 18 (ImageTransfer) |
| Comunicazione PP4 | 2 | 1 |
| Compact Frame | 18 | 62 (CompactData) |

Ogni oggetto ha il flag `isAutoReadable()` che indica se viene letto automaticamente durante la sessione (Class 1, 3, 4, 8, 70).

---

## 8. Compact Frame

Il `CompactFrameParser` decodifica 13 tipi di compact frame:

| CF | OBIS | Contenuto | Direzione |
|----|------|-----------|-----------|
| CF3 | 0-0:66.0.3.255 | Diagnostica e allarmi (batterie, tempo operativo) | GET |
| CF4 | 0-0:66.0.4.255 | Parametri fine fatturazione | GET/SET |
| CF5 | 0-0:66.0.5.255 | Piano tariffario attivo | GET |
| CF6 | 0-0:66.0.6.255 | Piano tariffario passivo | GET/SET |
| CF7 | 0-0:66.0.7.255 | Programmazione valvola (PGV, soglie) | GET/SET |
| CF8 | 0-0:66.0.8.255 | Stato valvola | GET |
| CF9 | 0-0:66.0.9.255 | Gestione valvola (password, durata) | GET/SET |
| CF22 | 0-0:66.0.22.255 | Stato trasferimento firmware | GET |
| CF41 | 0-0:66.0.41.255 | Configurazione comunicazione PP4 | GET/SET |
| CF47 | 0-0:66.0.47.255 | Push Content A (dati base) | DATA-NOTIF |
| CF48 | 0-0:66.0.48.255 | Push Content B (+ ultimi 3 daily) | DATA-NOTIF |
| CF49 | 0-0:66.0.49.255 | Push Content C (+ daily + billing + tariffe) | DATA-NOTIF |
| CF51 | 0-0:66.0.51.255 | Valori frame counter | GET |

### Formato dati nelle compact frame
I dati sono serializzati senza type-tag (il SAC conosce il template):
- `unsigned`: 1 byte, senza segno (0..255)
- `long-unsigned`: 2 byte big-endian (0..65535)
- `double-long-unsigned`: 4 byte big-endian (0..4294967295)
- `boolean`: 1 byte (0=false, altrimenti true)
- `enum`: 1 byte
- `octet-string`: prefisso lunghezza + dati

---

## 9. Codici Evento

L'enum `EventCode` contiene 181 codici evento definiti dall'Appendice D della UNI/TS 11291-12-2.

Ogni codice ha:
- `code`: valore numerico (1-181)
- `metrological`: true se deve comparire nel logbook metrologico
- `description`: descrizione in italiano

Categorie principali:
- **1-3**: Reset dispositivo e registri
- **10-12**: Orologio (sync fallita/riuscita)
- **20-65**: Misura (overflow, flusso inverso, pressione, temperatura)
- **70-77**: Alimentazione (batteria, rete)
- **80-81, 116-121**: Sicurezza (manomissione, campo interferente, crittografia)
- **90-93**: Fatturazione (chiusura periodica/su richiesta)
- **96-101**: Firmware (aggiornamento, verifica, attivazione)
- **102-114**: Valvola (chiusura per perdite/batteria/frode/comms)
- **122-133**: Chiavi crittografiche (aggiornamento)
- **134-178**: Comunicazione e configurazione
- **>192**: Riservati ai fabbricanti

---

## 10. Comandi Dispositivo

28 tipi di comando supportati (enum `CommandType`):

### Orologio
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| SYNC_CLOCK | - | Sincronizza con ora server (UNIX Time) |
| SET_CLOCK | ISO timestamp | Imposta orario specifico |

### Valvola
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| DISCONNECT_VALVE | - | Chiudi valvola |
| RECONNECT_VALVE | - | Riapri valvola |
| SET_VALVE_PASSWORD | numero (0-65535) | Imposta password valvola |
| SET_VALVE_OPENING_DURATION | minuti | Durata validita' apertura |

### Push
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| CHANGE_PUSH_DESTINATION | `{"ip":"...","port":...}` | Cambia destinazione push |

### Lettura profili
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| READ_LOAD_PROFILE | `{"profile":"daily","from":"ISO","to":"ISO"}` | Profilo giornaliero |
| READ_EVENT_LOG | `{"from":"ISO","to":"ISO"}` | Log eventi metrologici |
| READ_BILLING_DATA | `{"from":"ISO","to":"ISO"}` | Dati fatturazione |

### Diagnostica
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| READ_DIAGNOSTICS | - | Leggi CF3 |
| READ_VALVE_STATUS | - | Leggi CF8 |

### Fatturazione
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| FORCE_EOB | - | Forza chiusura periodo (script 8) |

### Configurazione remota
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| READ_EOB_PARAMS | - | Leggi CF4 |
| WRITE_EOB_PARAMS | Base64 della CF4 | Scrivi parametri EOB |
| READ_ACTIVE_TARIFF | - | Leggi CF5 |
| WRITE_PASSIVE_TARIFF | Base64 della CF6 | Programma piano tariffario |
| READ_COMM_SETUP | - | Leggi CF41 |
| WRITE_COMM_SETUP | Base64 della CF41 | Configura comunicazione PP4 |

### Firmware
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| FW_TRANSFER_INITIATE | `{"identifier":"...","size":N,"image":"base64"}` | Trasferisci firmware |
| FW_VERIFY | - | Verifica immagine trasferita |
| FW_ACTIVATE | - | Attiva firmware (riavvio contatore) |
| READ_FW_STATUS | - | Leggi CF22 |

### Chiavi crittografiche
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| CHANGE_HLS_SECRET | `{"association":"OBIS","secret":"base64"}` | Cambia chiave HLS |
| GLOBAL_KEY_TRANSFER | `{"securitySetup":"OBIS","wrappedKey":"base64"}` | Trasferisci chiave globale |

### Installer/Maintainer
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| SET_IM_PERMISSIONS | bitmask (intero) | Imposta permessi I/M |
| READ_IM_REMAINING_TIME | - | Leggi tempo residuo sessione |

### Dispositivo
| Comando | Payload | Descrizione |
|---------|---------|-------------|
| EXECUTE_SCRIPT | script ID (intero) | Esegui script Global Script |

---

## 11. Sicurezza

### Cifratura chiavi in database
Le chiavi DLMS (encryption_key, authentication_key, master_key) sono cifrate in database con:
- Algoritmo: **AES-256-GCM** (javax.crypto, nessuna dipendenza esterna)
- Master password: configurata tramite variabile d'ambiente `ONEGASMETER_KEY_MASTER_PASSWORD`
- Formato: `[IV 12 byte] + [ciphertext] + [GCM tag 16 byte]`
- Ogni cifratura usa un IV random diverso
- Classe: `KeyEncryptionService`

### Modello chiavi (UNI/TS 11291-10)
| Chiave | Sigla | Custode | Uso |
|--------|-------|---------|-----|
| Servizio | KEYS | Autorita' Garante | Cambio concessione |
| Esercizio | KEYC | Operatore (SAC) | Operativita' normale |
| Utenza | KEYT | Utente finale | Display utente |
| Manutenzione | KEM | Tecnico | Manutenzione locale |

### Frame Counter e Anti-Replay
- Ogni client ha il proprio frame counter (tx e rx) memorizzato in device_registry
- Soglie globali configurabili (OBIS 0-0:94.39.33.255)
- Se FC non valido: risposta negativa max 3 volte, poi silenzio

---

## 12. Flusso Sessione

```
1. CONNESSIONE
   Contatore si connette su TCP:60103 o invia pacchetto su UDP:60104

2. HANDSHAKE DLMS
   AARQ/AARE con autenticazione HLS (AES-GCM-128)

3. IDENTIFICAZIONE
   Lettura numero seriale (OBIS 0-0:96.1.0.255)

4. LETTURA OROLOGIO
   Lettura Clock (OBIS 0-0:1.0.0.255)

5. LETTURA OGGETTI COSEM
   Lettura automatica di tutti gli oggetti con isAutoReadable()=true
   (Class 1, 3, 4, 8, 70)

6. LETTURA COMPACT FRAME PUSH
   Tentativo CF49 -> CF48 -> CF47 (fallback)
   Decodifica e salvataggio dati estratti

7. ESECUZIONE COMANDI PENDENTI
   Cerca in device_commands per serial_number con status=PENDING
   Esegue ciascun comando e aggiorna lo stato

8. CHIUSURA SESSIONE PP4
   Invocazione script 22 (Global Script) per segnalare al contatore
   che il push e' stato completato (risparmio batteria)

9. DISCONNESSIONE
   Release request DLMS + chiusura socket
```

---

## 13. Test

### Esecuzione
```bash
./mvnw test
```

### Copertura
125 test totali:

| Pacchetto | Test | Tipo |
|-----------|------|------|
| cosem/CosemObjectTest | 11 | Unitario |
| cosem/CompactFrameParserTest | 13 | Unitario |
| cosem/EventCodeTest | 9 | Unitario |
| cosem/ValveStatusTest | 7 | Unitario |
| dlms/DlmsMeterClientTest | 11 | Unitario |
| dlms/IncomingTcpTransportTest | 8 | Unitario |
| entity/DeviceCommandTest | 3 | Unitario |
| entity/TelemetryDataTest | 5 | Unitario |
| server/MeterSessionHandlerTest | 16 | Unitario |
| server/UdpServerTest | 2 | Unitario |
| service/CommandServiceTest | 5 | Unitario |
| service/TelemetryServiceTest | 5 | Unitario |
| service/KeyEncryptionServiceTest | 5 | Unitario |
| repository/DeviceCommandRepositoryTest | 5 | Integrazione (H2) |
| repository/TelemetryDataRepositoryTest | 5 | Integrazione (H2) |
| repository/DeviceRegistryRepositoryTest | 4 | Integrazione (H2) |
| integration/FullSessionIntegrationTest | 2 | Integrazione (H2) |
| OneGasMeterApplicationTests | 1 | Context loading |

### Database di test
- H2 persistito in `src/test/resources/db/testdb`
- Schema: `src/test/resources/db/schema-h2.sql`
- Modalita': create-drop (ricreato ad ogni run)

---

## 14. Conformita' Normativa

### Normative di riferimento
- **UNI/TS 11291-12-2:2020** - Modello dati (oggetti COSEM, OBIS codes)
- **UNI/TS 11291-12-4:2020** - Profilo comunicazione PP4 (TCP/UDP, push, timeout)
- **UNI/TS 11291-10:2013** - Sicurezza (AES-GCM, chiavi, accesso)

### Copertura implementata
- 95 oggetti COSEM con OBIS codes dalla normativa
- 13 parser compact frame (CF3/4/5/6/7/8/9/22/41/47/48/49/51)
- 181 codici evento (Appendice D)
- 28 tipi di comando per tutti i casi d'uso principali
- Classe proprietaria 8192 per piano tariffario UNI/TS 11291
- 4 Push Setup con priorita' (1-4)
- Image Transfer (Class 18) per aggiornamento firmware
- Cambio chiavi HLS e global key transfer
- Gestione Installer/Maintainer (permessi, timeout)
- Chiusura esplicita sessione PP4 (script 22)
- Anti-replay con frame counter

### Documento di dettaglio
Vedi `documentation/normativa_UNITS_11291_riepilogo.md` per il riepilogo completo della normativa con tutti gli OBIS codes, le associazioni, i timeout e i codici evento.

---

*Ultimo aggiornamento: 2026-04-10*
