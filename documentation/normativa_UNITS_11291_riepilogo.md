# Riepilogo Normativa UNI/TS 11291 - Guida per lo Sviluppatore

## Documenti di riferimento
- **UNI/TS 11291-12-2:2020** - Modello dati (COSEM objects, OBIS codes, associazioni, sicurezza)
- **UNI/TS 11291-12-4:2020** - Profilo di comunicazione PP4 (TCP/UDP, push, retry, timeout)
- **UNI/TS 11291-10:2013** - Sicurezza (crittografia, chiavi, accesso)
- **Blue Book Ed.17** - Classi di interfacciamento COSEM (riferimento DLMS UA)
- **Green Book Ed.12** - Architettura e protocolli DLMS/COSEM (riferimento DLMS UA)

## Ambito di applicazione
Contatori gas <G10 (portata massima < 10 m3/h), mass market, comunicazione punto-punto su reti GPRS/UMTS/LTE o NB-IoT.

---

## 1. Associazioni (Client) e Sicurezza

### 1.1 Client definiti dalla normativa

| Client | SAP (Client Address) | OBIS Association | Autenticazione | Cifratura |
|--------|---------------------|-----------------|----------------|-----------|
| **Public (Pub)** | 16 (0x10) | 0-0:40.0.2.255 | NONE | No |
| **Management (MG)** | 1 (0x01) | 0-0:40.0.1.255 | HLS (mechanism 5) | AES-GCM-128 |
| **Installer/Maintainer (I/M)** | 3 (0x03) | 0-0:40.0.3.255 | HLS (mechanism 5) | AES-GCM-128 |
| **Guarantor Authority (GA)** | 48 (0x30) | 0-0:40.0.48.255 | HLS (mechanism 5) | AES-GCM-128 |
| **Broadcasting** | 32 (0x20) | 0-0:40.0.32.255 | HLS (mechanism 5) | AES-GCM-128 |

**Nota:** Il SAC (il nostro server) opera come **Management Client** (SAP=1). L'Installer/Maintainer e' per tecnici sul campo.

### 1.2 Conformance Block (per tutte le associazioni cifrate)
- Multiple reference
- Get / Set / Selective access / Action
- Data Notification
- general-protection (General-glo-ciphering)
- event notification
- MaxPDUSize: **504 byte**
- DLMS version: **6**

### 1.3 Security Suite
- Suite **0**: AES-GCM-128
- Security Policy: **3** (encrypted AND authenticated) - obbligatoria per MG, I/M, GA
- Il Public Client non usa cifratura

### 1.4 System Title di default (configurazione di fabbrica)
- Management Client [SAC]: `{0x53, 0x41, 0x43, 0x53, 0x41, 0x43, 0x53, 0x41}` = "SACSACSA"
- Broadcast: `{0x42, 0x52, 0x4F, 0x4D, 0x45, 0x4C, 0x4D, 0x4F}` = "BROMELMO"
- Installer/Maintainer: `{0x49, 0x4E, 0x53, 0x54, 0x41, 0x4C, 0x4C, 0x45}` = "INSTALLE"
- Guarantor Authority: `{0x47, 0x55, 0x41, 0x4F, 0x52, 0x49, 0x54, 0x41}` = "GUARITA" (con piccole variazioni)

### 1.5 Modello delle chiavi (da UNI/TS 11291-10)
Quattro categorie di chiavi, ciascuna >= 128 bit simmetriche:

| Chiave | Sigla | Custode | Scopo |
|--------|-------|---------|-------|
| **Servizio** | KEYS | Autorita' Garante | Cambio concessione, recovery chiavi |
| **Esercizio** | KEYC | Operatore (SAC) | Operativita' normale (lettura/scrittura) |
| **Utenza** | KEYT | Utente finale | Display utente |
| **Installazione/Manutenzione** | KEM | Tecnico | Manutenzione locale, durata limitata |

- Algoritmi obbligatori: AES128-GCM (cifratura + autenticazione), AES128-GMAC (solo autenticazione)
- **Vietato**: AES-ECB
- Chiavi mai trasmesse in chiaro
- Tutte le modifiche alle chiavi registrate nel log eventi

### 1.6 Frame Counter e Anti-Replay
- Ogni client ha il proprio frame counter
- Soglie globali configurabili (Global Frame Counter Thresholds: 0-0:94.39.33.255)
- Finestra di accettazione: tra `FC_corrente - low_threshold` e `FC_corrente + high_threshold`
- Se FC non valido: risposta negativa max 3 volte, poi APDU scartata silenziosamente

---

## 2. Comunicazione PP4 (da UNI/TS 11291-12-4)

### 2.1 Trasporto
- **DLMS WRAPPER** su TCP o UDP (version 0x0001)
- Header wrapper: 8 byte (version 2B + source wPort 2B + dest wPort 2B + length 2B)
- Cifratura end-to-end: general-glo-ciphering

### 2.2 Modalita' di connessione

| Modalita' | Caratteristiche | Timeout inattivita' | Chiusura |
|-----------|----------------|---------------------|----------|
| **Connection-Oriented** (TCP) | Sequenza garantita | Si | Esplicita (script 22) o timeout |
| **Connection-Less** (UDP) | Nessuna garanzia ordine | No | Solo session_max_duration |

### 2.3 Timeout

**Rete GPRS:**

| Parametro | Default | Massimo |
|-----------|---------|---------|
| session_max_duration | 40s | 60s |
| inactivity_timeout | 20s | - |
| network_attach_timeout | 30s | 120s |

**Rete NB-IoT:**

| Parametro | Default | Massimo |
|-----------|---------|---------|
| session_max_duration | 80s | 80s |
| inactivity_timeout | 20s | - |
| network_attach_timeout | 120s | 120s |

- Tempo massimo di risposta SAC: **1.1 secondi** (Tresp_max)

### 2.4 Strategie di comunicazione
- **S00** - Default
- **S01** - Periodica regolare (push spontaneo)
- **S02** - Orphan mode (dispositivo senza comunicazione per N giorni)

### 2.5 Push Setup (4 istanze, priorita' crescente)

| Push | OBIS Scheduler | OBIS Setup | Scopo default |
|------|---------------|------------|---------------|
| **Push 1** | 0-1:15.0.4.255 | 0-1:25.9.0.255 | Recupero dati di fatturazione |
| **Push 2** | 0-2:15.0.4.255 | 0-2:25.9.0.255 | Recupero dati giornalieri (ogni 3 giorni) |
| **Push 3** | 0-3:15.0.4.255 | 0-3:25.9.0.255 | Dati fatturazione + SLA riapertura valvola |
| **Push 4** | 0-4:15.0.4.255 | 0-4:25.9.0.255 | Gestione orphan mode (settimanale) |

- Priorita': Push 4 > Push 3 > Push 2 > Push 1
- Se due push sovrapposti: solo il piu' prioritario viene eseguito
- I dati push sono trasportati come **Compact Frame** (Class 62)

### 2.6 Retry
- Controllato da `number_of_retries` per ogni Push Setup
- Attivato su: network attach timeout, inactivity timeout (solo connection-oriented)
- **NON** attivato su session_max_duration timeout
- Scheduling retry annullato se altro push in corso

### 2.7 Flusso sessione Connection-Oriented
1. Network attachment (entro network_attach_timeout)
2. Attiva session_max_duration timeout
3. Invia push_object_list (se non vuota)
4. Attende comandi dal SAC con inactivity_timeout
5. Reset inactivity_timeout ad ogni risposta SAC
6. Chiusura: script esplicito (22), inactivity timeout, o session_max_duration

### 2.8 Invoke_Id
- Ogni richiesta client ha un Invoke_Id univoco
- Il server copia l'Invoke_Id nella risposta corrispondente
- Fondamentale in connection-less per il matching richiesta/risposta

---

## 3. Oggetti COSEM - Catalogo completo

### 3.1 Identificazione dispositivo

| Nome | OBIS | IC | Attr | Tipo | Accesso (MG/Pub/IM/GA) |
|------|------|----|------|------|------------------------|
| COSEM Logical Device Name | 0-0:42.0.0.255 | 1 | 2 | octet-string (16 byte) | G/G/G/- |
| Meter Serial Number | 0-0:96.1.0.255 | 1 | 2 | visible-string (max 16) | G/G/G/- |
| Metering Point Id (PDR) | 0-0:96.1.10.255 | 1 | 2 | visible-string (max 14) | G/S / G / G/S / - |
| Device Type Identifier | 0-0:96.1.3.255 | 1 | 2 | visible-string | G/G/G/- |
| Device Type Identifier 2 | 0-0:96.1.4.255 | 1 | 2 | visible-string | G/G/G/- |
| Utility Number (opzionale) | 0-0:96.1.1.255 | 1 | 2 | visible-string (max 8) | G/S / G / G / - |

### 3.2 Orologio e Tempo

| Nome | OBIS | IC | Attr | Tipo | Note |
|------|------|----|------|------|------|
| Clock | 0-0:1.0.0.255 | 8 | vari | date-time | Attr 5=status, 6=shift_time (ACTION per sync) |
| UNIX Time | 0-0:1.1.0.255 | 1 | 2 | double-long-unsigned | Secondi da epoch, formato SET |
| Start of Conventional Gas Day | 7-0:0.9.3.255 | 1 | 2 | time | Ora inizio giorno gas (default 06:00) |
| Synchronization Algorithm | 0-0:94.39.44.255 | 1 | 2 | structure | Parametri sync orologio |

### 3.3 Totalizzatori di volume

| Nome | OBIS | IC | Attr | Tipo | Unita' |
|------|------|----|------|------|--------|
| Current Index of Converted Volume | 7-0:13.2.0.255 | 3 | 2 | double-long-unsigned | Sm3 (scaler=-3, unit=14) |
| Current Index of Converted Vol. Under Alarm | 7-0:12.2.0.255 | 3 | 2 | double-long-unsigned | Sm3 (scaler=-3, unit=14) |
| High Resolution Current Index Conv. Vol. | 7-128:13.2.0.255 | 3 | 2 | double-long-unsigned | Sm3 (scaler=-4, unit=14) |
| Current Index of Conv. Vol. - Fascia F1 | 7-0:13.2.1.255 | 3 | 2 | double-long-unsigned | Sm3 |
| Current Index of Conv. Vol. - Fascia F2 | 7-0:13.2.2.255 | 3 | 2 | double-long-unsigned | Sm3 |
| Current Index of Conv. Vol. - Fascia F3 | 7-0:13.2.3.255 | 3 | 2 | double-long-unsigned | Sm3 |

### 3.4 Misure di portata, pressione, temperatura

| Nome | OBIS | IC | Attr | Tipo | Unita' |
|------|------|----|------|------|--------|
| Conventional Converted Gas Flow Qbc | 7-0:43.45.0.255 | 4 | 2,3,4,5 | extended register | Sm3/h |
| Max Conv. Converted Gas Flow Qbc Max | 7-0:43.45.0.255 | 4 | -- | extended register | Sm3/h (con capture_time) |
| Remaining Battery Capacity_0 | 0-0:96.6.6.255 | 3 | 2 | long-unsigned | minuti |
| Remaining Battery Capacity_1 | 0-1:96.6.6.255 | 3 | 2 | long-unsigned | minuti |
| Battery Use Time Counter_0 | 0-0:96.6.0.255 | 3 | 2 | double-long-unsigned | minuti |
| Battery Use Time Counter_1 | 0-1:96.6.0.255 | 3 | 2 | double-long-unsigned | minuti |
| Total Operating Time | 0-0:96.8.0.255 | 3 | 2 | double-long-unsigned | minuti |

### 3.5 Diagnostica e allarmi

| Nome | OBIS | IC | Attr | Tipo |
|------|------|----|------|------|
| Daily Diagnostic | 7-1:96.5.1.255 | 1 | 2 | long-unsigned (bitmask) |
| Metrological Event Counter | 0-0:96.15.1.255 | 1 | 2 | long-unsigned |
| Event Counter | 0-0:96.15.2.255 | 1 | 2 | long-unsigned |
| Communication Tamper Event Counter | 0-0:96.20.30.255 | 1 | 2 | long-unsigned |
| Monitoring Communication Data | 0-0:94.39.56.255 | 1 | 2 | structure |
| Monitoring SLA Data | 0-0:94.39.59.255 | 1 | 2 | structure |
| Error Register | 0-0:97.97.0.255 | 1 | 2 | long-unsigned |
| PP4 Network Status | 0-1:96.5.4.255 | 1 | 2 | long-unsigned |

### 3.6 Valvola (Disconnect Control)

| Nome | OBIS | IC | Attr/Method | Tipo |
|------|------|----|------------|------|
| Disconnect Control | 0-0:96.3.10.255 | 70 | attr 2: output_state | boolean |
| | | | attr 3: control_state | enum |
| | | | method 1: remote_disconnect | - |
| | | | method 2: remote_reconnect | - |
| Valve Configuration PGV | 0-0:94.39.3.255 | 1 | 2 | long-unsigned |
| Maximum Password Attempts | 0-0:94.39.2.255 | 1 | 2 | unsigned |
| Valve Enable Password | 0-0:94.39.1.255 | 1 | 2 | long-unsigned |
| Valve Closure Cause | 0-0:94.39.7.255 | 1 | 2 | unsigned |
| Opening Command Duration Validity | 0-0:94.39.6.255 | 1 | 2 | long-unsigned |
| Days Without Comms Threshold | 0-0:94.39.5.255 | 21 | 2 | array of 1 long-unsigned |
| Tampering Attempts Threshold | 0-0:94.39.25.255 | 21 | 2 | array of 1 double-long-unsigned |
| Leakage Test Parameters | 0-0:94.39.26.255 | 1 | 2 | structure |
| Disconnect Control Single Action Schedule | 0-0:15.0.1.255 | 22 | 2,4 | script + execution_time |

### 3.7 Profili di carico e log eventi

| Nome | OBIS | IC | Capture Period | Contenuto |
|------|------|----|---------------|-----------|
| Daily Load Profile | 7-0:99.99.3.255 | 7 | Giornaliero | Timestamp, totalizzatori convertiti, diagnostica, contatori eventi |
| Metrological Logbook | 7-0:99.98.1.255 | 7 | Su evento | Timestamp, event_code, parametri aggiuntivi |
| Snapshot Period Data (billing) | 7-0:98.11.0.255 | 7 | Fine periodo fatturazione | Totalizzatori, fasce, portata max, diagnostica |
| Parameter Monitor Logbook | 7-0:99.16.0.255 | 7 | Su modifica parametro | Timestamp, oggetto modificato, valore precedente/nuovo |

### 3.8 Fine fatturazione (EOB - End of Billing)

| Nome | OBIS | IC | Attr | Tipo |
|------|------|----|------|------|
| Billing/Snapshot Period Counter | 7-0:0.1.0.255 | 1 | 2 | unsigned |
| EOB Snapshot Period | 7-0:0.8.23.255 | 1 | 2 | long-unsigned (giorni) |
| EOB Snapshot Starting Date | 0-0:94.39.11.255 | 1 | 2 | date |
| On Demand Snapshot Time | 0-0:94.39.8.255 | 1 | 2 | date-time |

### 3.9 Piano tariffario

| Nome | OBIS | IC | Tipo |
|------|------|----|------|
| Active Tariff Plan | 0-0:94.39.21.255 | 8192 | Classe proprietaria UNI/TS 11291 |
| Passive Tariff Plan | 0-0:94.39.22.255 | 8192 | Classe proprietaria UNI/TS 11291 |

La classe 8192 (proprietaria) contiene: calendar_name, enabled, plan (con fasce F1/F2/F3, stagioni inverno/estate, giorni speciali/festivi italiani), activation_date_time.

### 3.10 Configurazione dispositivo (Global Script)

| Nome | OBIS | IC | Script ID | Azione |
|------|------|----|-----------|--------|
| Global Script | 0-0:10.0.0.255 | 9 | 1 | NON CONFIGURATO -> NORMALE |
| | | | 2 | NON CONFIGURATO -> MANUTENZIONE |
| | | | 3 | NORMALE -> MANUTENZIONE |
| | | | 4 | MANUTENZIONE -> NORMALE |
| | | | 5 | NORMALE -> NON CONFIGURATO |
| | | | 7 | Reset EOB |
| | | | 8 | Esecuzione forzata EOB |
| | | | 22 | Chiusura esplicita connessione PP4 |

### 3.11 Frame Counter

| Nome | OBIS | IC | Tipo |
|------|------|----|------|
| Management FC On-line | 0-0:43.1.1.255 | 1 | double-long-unsigned |
| Management FC Off-line | 0-1:43.1.1.255 | 1 | double-long-unsigned |
| Guarantor Authority FC | 0-0:43.1.48.255 | 1 | double-long-unsigned |
| Installer/Maintainer FC | 0-0:43.1.3.255 | 1 | double-long-unsigned |
| Global FC Thresholds | 0-0:94.39.33.255 | 1 | structure {high, low} |

### 3.12 Security Setup (per client)

| Nome | OBIS | IC | Security Suite | Security Policy |
|------|------|----|---------------|----------------|
| Management Security Setup | 0-0:43.0.1.255 | 64 | 0 (AES-GCM-128) | 0 -> 3 alla messa in servizio |
| I/M Security Setup | 0-0:43.0.3.255 | 64 | 0 | 3 |
| GA Security Setup | 0-0:43.0.48.255 | 64 | 0 | 3 |
| Broadcasting Security Setup | 0-0:43.0.32.255 | 64 | 0 | 3 |

### 3.13 Image Transfer (Firmware Update)

| Nome | OBIS | IC | Attributi chiave |
|------|------|----|-----------------|
| Image Transfer | 0-0:44.0.0.255 | 18 | attr 3: image_transferred_blocks_status, attr 6: image_transfer_status |

### 3.14 Installer/Maintainer Setup

| Nome | OBIS | IC | Attr | Tipo |
|------|------|----|------|------|
| I/M Setup | 0-0:94.39.30.255 | 1 | 2 | structure con permission bitmask, scheduled_time |
| I/M Remaining Time | 0-0:94.39.31.255 | 1 | 2 | long-unsigned (secondi rimanenti) |

### 3.15 Configurazione comunicazione PP4

| Nome | OBIS | IC |
|------|------|----|
| Timeout GPRS | 0-0:94.39.52.255 | 1 |
| Timeout NB-IoT | 0-1:94.39.52.255 | 1 |
| SIM Setup GPRS | 0-0:25.4.0.255 | - |
| SIM Setup NB-IoT | 0-1:25.4.0.255 | - |
| PPP Setup GPRS | 0-0:25.3.0.255 | - |
| PPP Setup NB-IoT | 0-1:25.3.0.255 | - |
| TCP-UDP Setup | 0-0:25.0.0.255 | - |
| NB-IoT Setup | 0-1:15.0.4.255 | - |
| Orphaned Threshold | 0-0:94.39.10.255 | 1 |
| Manual Communication Parameters | 0-0:25.0.0.255 | - |

---

## 4. Compact Frame (Trame Compatte - Class 62)

Le trame compatte ottimizzano il payload eliminando i tag dei tipi. Il SAC deve conoscere i template.

### 4.1 Elenco Compact Frame definite

| CF | OBIS | Direzione | Contenuto |
|----|------|-----------|-----------|
| CF1 | 0-0:66.0.1.255 | GET | Asset Data (dati anagrafici con Public Client) |
| CF2 | 0-0:66.0.2.255 | GET/SET | Clock Behaviour (parametri orologio) |
| CF3 | 0-0:66.0.3.255 | GET | Diagnostics and Alarms |
| CF4 | 0-0:66.0.4.255 | GET/SET | EOB Parameters (parametri fine fatturazione) |
| CF5 | 0-0:66.0.5.255 | GET | Active Tariff Plan |
| CF6 | 0-0:66.0.6.255 | GET/SET | Passive Tariff Plan (programmazione) |
| CF7 | 0-0:66.0.7.255 | GET/SET | Valve Programming (configurazione valvola) |
| CF8 | 0-0:66.0.8.255 | GET | Valve Status |
| CF9 | 0-0:66.0.9.255 | GET/SET | Valve Management (comandi valvola) |
| CF14 | 0-0:66.0.14.255 | GET | Daily Profile Response |
| CF15 | 0-0:66.0.15.255 | GET | Event Response (log metrologico) |
| CF16 | 0-0:66.0.16.255 | GET | Billing Data Response |
| CF22 | 0-0:66.0.22.255 | GET | FW Transfer Status |
| CF41 | 0-0:66.0.41.255 | GET/SET | Communication Setup PP4 (tutti i push) |
| CF47 | 0-0:66.0.47.255 | DATA-NOTIF/GET | Content A PP4 (push base) |
| CF48 | 0-0:66.0.48.255 | DATA-NOTIF/GET | Content B PP4 (push + ultimi 3 daily) |
| CF49 | 0-0:66.0.49.255 | DATA-NOTIF/GET | Content C PP4 (push + daily + billing + tariffe) |
| CF51 | 0-0:66.0.51.255 | GET | Frame Counter Values |

---

## 5. Casi d'uso principali per il SAC

### 5.1 Ricezione push spontaneo (DATA-NOTIFICATION)
Il contatore si connette e invia CF47/CF48/CF49. Il SAC:
1. Riceve e decodifica la compact frame
2. Salva i dati in database
3. Puo' inviare comandi prima della chiusura sessione
4. Chiude esplicitamente la connessione (script 22) per risparmiare batteria

### 5.2 Lettura dati su richiesta
Il SAC invia GET request per leggere specifici oggetti o compact frame:
- CF3 per diagnostica/allarmi
- CF14 per profilo giornaliero (con selective access per intervallo date)
- CF15 per log eventi (con selective access)
- CF16 per dati di fatturazione (con selective access)

### 5.3 Comandi al contatore
Il SAC invia SET/ACTION per:
- Sync orologio: SET su UNIX Time (0-0:1.1.0.255) o ACTION shift_time su Clock
- Apertura/chiusura valvola: SET su CF9
- Programmazione push: SET su CF41
- Programmazione piano tariffario: SET su CF6
- Aggiornamento firmware: Image Transfer (0-0:44.0.0.255)
- Cambio chiavi: ACTION change_HLS_secret sulle associazioni

### 5.4 Chiusura esplicita sessione
Il SAC invoca lo script 22 del Global Script (0-0:10.0.0.255) per terminare la sessione in modo esplicito, segnalando al contatore che il push e' stato completato con successo.

---

## 6. Codici Evento (Appendice D della 11291-12-2)

### 6.1 Eventi principali (181 codici definiti, >192 riservati ai fabbricanti)

**Dispositivo (1-3):** Reset dispositivo, Reset registro eventi metrologici, Reset registro eventi

**Orologio (10-12):** Sync fallita, Impostazione orologio, Sync riuscita

**Misura (20-65):** Errore algoritmo misura, overflow, flusso inverso, fuori range pressione/temperatura, guasti sensori

**Alimentazione (70-77):** Mancanza critica, batteria <10%, mancanza rete CA

**Frode/Manomissione (80-81, 116-121):** Rilevata manomissione, campo interferente, decrittazione errore, autenticazione errore, accesso non autorizzato

**Valvola (30-31, 102-114):** Chiusa per comando, aperta, chiusa per perdite, chiusa per batteria rimossa, chiusa per nessuna comunicazione, password non valida, configurazione PGV modificata

**Fatturazione (90-93):** Chiusura periodica, per modifica piano tariffario, su richiesta locale/remota

**Firmware (96-101):** Nuovo aggiornamento iniziato, verifica OK/fallita, attivazione OK/fallita

**Chiavi (122-133):** Programmazione/attivazione chiavi, aggiornamento Master Key, KEYC, KEYT, KEYS

**Comunicazione (134-149, 171-178):** Power level PM1, channel, active/orphan mode, connessione remota inizio/fine/timeout

**Configurazione (150-170):** Modifiche a push scheduler/setup, parametri orologio, piano tariffario, PDR

---

## 7. Impatto sull'implementazione OneGasMeter

### 7.1 Da implementare lato SAC (il nostro server)

**Comunicazione:**
- [ ] Server TCP **e** UDP sulla stessa porta o porte dedicate
- [ ] Supporto DLMS WRAPPER (header 8 byte)
- [ ] Ricezione DATA-NOTIFICATION (push spontaneo)
- [ ] Invio GET/SET/ACTION (comandi al contatore)
- [ ] Gestione Invoke_Id per matching richiesta/risposta (UDP)
- [ ] Timeout risposta: max 1.1 secondi
- [ ] Chiusura esplicita sessione (script 22)

**Sicurezza:**
- [ ] Associazione Management (SAP=1) con HLS mechanism 5
- [ ] AES-GCM-128 per cifratura e autenticazione
- [ ] Gestione Frame Counter (invio e verifica)
- [ ] System Title del SAC configurabile (default "SACSACSA")
- [ ] Storage sicuro delle chiavi per ogni contatore

**Decodifica dati:**
- [ ] Parser per tutte le Compact Frame (CF1-CF51)
- [ ] Decodifica Daily Load Profile (CF14) con selective access
- [ ] Decodifica Metrological Logbook (CF15)
- [ ] Decodifica Snapshot/Billing data (CF16)
- [ ] Decodifica push content (CF47/48/49)

**Comandi:**
- [ ] Sync orologio (SET UNIX Time o ACTION shift_time)
- [ ] Gestione valvola (SET CF9: apertura, chiusura, programmazione)
- [ ] Programmazione push (SET CF41)
- [ ] Lettura diagnostica (GET CF3)
- [ ] Programmazione piano tariffario (SET CF6)
- [ ] Aggiornamento firmware (Image Transfer class 18)
- [ ] Cambio chiavi (ACTION change_HLS_secret)
- [ ] Script di configurazione (ACTION Global Script)

**Database:**
- [ ] Tabella per tutti gli OBIS codes e relativi valori
- [ ] Tabella chiavi per contatore (KEYC, system_title, frame_counter)
- [ ] Tabella log eventi decodificati (181 codici)
- [ ] Tabella compact frame ricevute (raw + parsed)
- [ ] Tabella comandi pendenti con stato

---

*Documento generato il 2026-04-10 sulla base delle normative UNI/TS 11291-10:2013, 11291-12-2:2020, 11291-12-4:2020*
