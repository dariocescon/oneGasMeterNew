package com.aton.proj.oneGasMeter.dlms;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.InterfaceType;

import java.time.Instant;

/**
 * Stato di riassemblaggio per un peer UDP identificato da "ip:porta".
 *
 * Quando un contatore invia una DATA-NOTIFICATION troppo grande per un singolo
 * datagram UDP, DLMS la suddivide in blocchi tramite General Block Transfer (GBT).
 * Ogni blocco arriva come datagram indipendente; questo oggetto accumula i blocchi
 * finché il messaggio non è completo.
 *
 * <p>Viene usato {@link GXDLMSClient} (e non {@link gurux.dlms.GXDLMSNotify}) perché
 * è l'unica classe Gurux che espone sia {@code getData()} che {@code receiverReady()},
 * necessario per generare gli ACK GBT inter-blocco.
 *
 * <p>Il client è configurato con {@code Authentication.NONE}: per la ricezione di
 * DATA-NOTIFICATION non è richiesto handshake né autenticazione.
 *
 * <p>La stessa istanza di {@link GXDLMSClient} e {@link GXReplyData} deve essere
 * riutilizzata per tutti i blocchi dello stesso messaggio: Gurux aggiorna
 * internamente lo stato del riassemblaggio ad ogni chiamata a {@code getData()}.
 */
public class UdpReassemblyContext {

    /**
     * Client Gurux usato per il parsing WRAPPER e la generazione degli ACK GBT.
     * Non esegue handshake AARQ/AARE: viene usato solo per getData() e receiverReady().
     */
    public final GXDLMSClient client;

    /** Accumula i dati GBT attraverso i blocchi successivi. */
    public final GXReplyData reply;

    /** Timestamp dell'ultimo pacchetto ricevuto da questo peer. */
    public volatile Instant lastActivity;

    public UdpReassemblyContext(DlmsSessionConfig config) {
        this.client = new GXDLMSClient(
                config.isUseLogicalNameReferencing(),
                config.getClientAddress(),
                config.getServerAddress(),
                Authentication.NONE,   // DATA-NOTIFICATION non richiede autenticazione
                null,
                InterfaceType.WRAPPER
        );
        this.reply = new GXReplyData();
        this.lastActivity = Instant.now();
    }
}
