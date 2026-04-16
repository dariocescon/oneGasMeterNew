package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.config.UdpServerConfig;
import com.aton.proj.oneGasMeter.cosem.CompactFrameData;
import com.aton.proj.oneGasMeter.cosem.CompactFrameParser;
import com.aton.proj.oneGasMeter.dlms.UdpReassemblyContext;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.Command;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server UDP che riceve DATA-NOTIFICATION dai contatori gas via DLMS/COSEM
 * WRAPPER.
 *
 * <h3>Segmentazione GBT (General Block Transfer)</h3> Un contatore può
 * suddividere una DATA-NOTIFICATION in più datagram UDP usando GBT. La chiave
 * di riassemblaggio è "ip:porta" del mittente: Gurux
 * ({@link gurux.dlms.GXDLMSClient}) accumula i blocchi nella stessa istanza di
 * {@link GXReplyData} fino al completamento. Dopo ogni blocco intermedio il
 * server invia un ACK GBT; quando il messaggio è completo lo processa.
 *
 * <h3>Flusso per messaggio singolo (no GBT)</h3>
 * 
 * <pre>
 *   Meter → [WRAPPER | DATA-NOTIFICATION] → Server
 *   Server: getData() → CompactFrameParser → TelemetryService.save()
 * </pre>
 *
 * <h3>Flusso GBT (messaggio segmentato)</h3>
 * 
 * <pre>
 *   Meter  → [WRAPPER | GBT block-1] → Server
 *   Server → [WRAPPER | GBT ACK-1  ] → Meter
 *   Meter  → [WRAPPER | GBT block-2] → Server  (last-block flag)
 *   Server: riassembla → CompactFrameParser → TelemetryService.save()
 * </pre>
 *
 * <h3>Identificativo dispositivo</h3> Via UDP non esiste handshake AARQ/AARE:
 * il serial number non è noto a priori. Il campo {@code serial_number} in
 * {@code telemetry_data} è valorizzato con l'IP sorgente del contatore.
 */
@Component
@Order(2)
public class UdpServer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(UdpServer.class);

	private final UdpServerConfig config;
	private final DlmsSessionConfig dlmsConfig;
	private final TelemetryService telemetryService;

	/** Un contesto per ogni peer attivo (ip:porta). Accesso thread-safe. */
	private final ConcurrentHashMap<String, UdpReassemblyContext> sessions = new ConcurrentHashMap<>();

	private DatagramSocket socket;
	private ExecutorService executor;
	private ScheduledExecutorService cleaner;
	private volatile boolean running = false;

	public UdpServer(UdpServerConfig config, DlmsSessionConfig dlmsConfig, TelemetryService telemetryService) {
		this.config = config;
		this.dlmsConfig = dlmsConfig;
		this.telemetryService = telemetryService;
	}

	@Override
	public void run(String... args) throws Exception {
		if (config.getPort() == 0) {
			log.info("Server UDP disabilitato (porta=0)");
			return;
		}

		socket = new DatagramSocket(config.getPort());
		executor = Executors.newVirtualThreadPerTaskExecutor();
		running = true;

		// Pulizia periodica delle sessioni GBT stantie (ogni 60 secondi)
		cleaner = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("udp-cleaner").factory());
		cleaner.scheduleAtFixedRate(this::cleanStaleSessions, 60, 60, TimeUnit.SECONDS);

		log.info("Server UDP in ascolto sulla porta {} (max pacchetto: {} byte, retry: {}x{}ms, sessionTimeout: {}ms)",
				config.getPort(), config.getMaxPacketSize(), config.getRetryCount(), config.getRetryDelayMs(),
				config.getSessionTimeoutMs());

		// Loop di ricezione pacchetti
		while (running) {
			try {
				byte[] buffer = new byte[config.getMaxPacketSize()];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				// Copia i dati ricevuti prima di riusare il buffer
				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
				InetAddress senderAddress = packet.getAddress();
				int senderPort = packet.getPort();

				log.debug("Pacchetto UDP ricevuto da {}:{} ({} byte)", senderAddress.getHostAddress(), senderPort,
						data.length);

				executor.submit(() -> handlePacket(data, senderAddress, senderPort));

			} catch (IOException e) {
				if (running) {
					log.error("Errore ricezione pacchetto UDP: {}", e.getMessage());
				}
			}
		}
	}

	/**
	 * Gestisce un singolo pacchetto UDP ricevuto.
	 *
	 * <p>
	 * Delega a Gurux il parsing del WRAPPER e la gestione del GBT. Se il messaggio
	 * è frammentato ({@code isMoreData()}), invia l'ACK GBT e attende il blocco
	 * successivo. Quando il messaggio è completo lo processa.
	 */
	private void handlePacket(byte[] data, InetAddress senderAddress, int senderPort) {
		String senderIp = senderAddress.getHostAddress();
		String peerKey = senderIp + ":" + senderPort;

		// GBT è sequenziale per protocollo: il contatore attende l'ACK prima di inviare
		// il blocco successivo, quindi non arriveranno mai due pacchetti dallo stesso
		// peer
		// contemporaneamente. Non serve sincronizzazione esplicita.
		UdpReassemblyContext ctx = sessions.computeIfAbsent(peerKey, k -> new UdpReassemblyContext(dlmsConfig));
		ctx.lastActivity = Instant.now();

		try {
			// Gurux parsa l'header WRAPPER (8 byte) e accumula i dati nel reply.
			// Se il frame è GBT, aggiorna reply.blockNumber e reply.moreData internamente.
			ctx.client.getData(data, ctx.reply);

		} catch (Exception e) {
			log.error("Errore parsing WRAPPER da {}:{}: {}", senderIp, senderPort, e.getMessage());
			sessions.remove(peerKey);
			return;
		}

		if (ctx.reply.isMoreData()) {
			// Messaggio GBT incompleto: invia ACK per sbloccare il prossimo blocco.
			// receiverReady() genera il frame WRAPPER con il GBT-acknowledgment.
			log.debug("GBT blocco {} ricevuto da {}, invio ACK", ctx.reply.getBlockNumber(), peerKey);
			try {
				byte[] ack = ctx.client.receiverReady(ctx.reply);
				if (ack != null && ack.length > 0) {
					sendWithRetry(ack, senderAddress, senderPort);
				}
			} catch (Exception e) {
				log.error("Errore generazione ACK GBT per {}: {}", peerKey, e.getMessage());
				sessions.remove(peerKey);
			}
			return; // aspetta il blocco successivo
		}

		// Messaggio completo: rimuovi il contesto e processa
		sessions.remove(peerKey);

		int command = ctx.reply.getCommand();
		if (command == Command.DATA_NOTIFICATION) {
			processNotification(ctx.reply, senderIp, peerKey);
		} else {
			log.warn("Comando UDP non gestito (0x{}) da {}", Integer.toHexString(command), peerKey);
		}
	}

	/**
	 * Processa una DATA-NOTIFICATION completa (eventualmente riassemblata da GBT).
	 *
	 * <p>
	 * Il valore contenuto nella notifica è atteso come {@code byte[]}
	 * rappresentante una compact frame DLMS (Class 62). Viene parsata da
	 * {@link CompactFrameParser} e ogni campo salvato in {@code telemetry_data}.
	 *
	 * @param reply    reply Gurux con il messaggio completo
	 * @param senderIp IP del contatore (usato come identificativo in assenza di
	 *                 serial number)
	 * @param peerKey  "ip:porta" per i log
	 */
	private void processNotification(GXReplyData reply, String senderIp, String peerKey) {
		Object value = reply.getValue();

		if (!(value instanceof byte[] rawBytes)) {
			log.warn("DATA-NOTIFICATION da {} contiene valore non byte[] ({}): ignorato", peerKey,
					value == null ? "null" : value.getClass().getSimpleName());
			return;
		}

		CompactFrameData cfData;
		try {
			cfData = CompactFrameParser.parse(rawBytes);
		} catch (Exception e) {
			log.error("Errore parsing compact frame da {}: {}", peerKey, e.getMessage());
			return;
		}

		if (cfData == null) {
			log.warn("Compact frame non riconosciuta da {} ({} byte raw)", peerKey, rawBytes.length);
			return;
		}

		Instant timestamp = cfData.getTimestamp() != null ? cfData.getTimestamp() : Instant.now();
		String sessionId = "udp-" + peerKey;
		int savedCount = 0;

		for (var entry : cfData.getValues().entrySet()) {
			// Salta payload binari grezzi (profili, snapshot): troppo grandi per raw_value
			if (entry.getValue() instanceof byte[]) {
				continue;
			}
			telemetryService.save(senderIp, senderIp, sessionId, entry.getKey(), 62, entry.getValue(), 0, null,
					timestamp);
			savedCount++;
		}

		log.info("DATA-NOTIFICATION da {} processata: CF{} → {} campi salvati", peerKey, cfData.getTemplateId(),
				savedCount);
	}

	/**
	 * Rimuove dalla map i contesti di riassemblaggio stantii. Viene invocato
	 * periodicamente dal cleaner ogni 60 secondi. Tutela contro contatori che si
	 * interrompono nel mezzo di un trasferimento GBT.
	 */
	private void cleanStaleSessions() {
		Instant cutoff = Instant.now().minusMillis(config.getSessionTimeoutMs());
		int before = sessions.size();
		sessions.entrySet().removeIf(e -> e.getValue().lastActivity.isBefore(cutoff));
		int removed = before - sessions.size();
		if (removed > 0) {
			log.info("Rimosse {} sessioni GBT stantie (timeout {}ms)", removed, config.getSessionTimeoutMs());
		}
	}

	/**
	 * Invia una risposta UDP con meccanismo di retry.
	 *
	 * @param data          dati da inviare
	 * @param targetAddress indirizzo destinatario
	 * @param targetPort    porta destinatario
	 * @return true se l'invio ha avuto successo
	 */
	public boolean sendWithRetry(byte[] data, InetAddress targetAddress, int targetPort) {
		for (int attempt = 1; attempt <= config.getRetryCount(); attempt++) {
			try {
				DatagramPacket response = new DatagramPacket(data, data.length, targetAddress, targetPort);
				socket.send(response);
				log.debug("Risposta UDP inviata a {}:{} (tentativo {})", targetAddress.getHostAddress(), targetPort,
						attempt);
				return true;
			} catch (IOException e) {
				log.warn("Invio risposta UDP fallito (tentativo {}/{}): {}", attempt, config.getRetryCount(),
						e.getMessage());
				if (attempt < config.getRetryCount()) {
					try {
						Thread.sleep(config.getRetryDelayMs());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						return false;
					}
				}
			}
		}
		log.error("Invio risposta UDP fallito dopo {} tentativi verso {}:{}", config.getRetryCount(),
				targetAddress.getHostAddress(), targetPort);
		return false;
	}

	@PreDestroy
	public void shutdown() {
		running = false;
		log.info("Arresto server UDP...");

		if (cleaner != null) {
			cleaner.shutdownNow();
		}
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
		if (executor != null) {
			executor.close();
		}
		sessions.clear();
		log.info("Server UDP arrestato");
	}

	public boolean isRunning() {
		return running;
	}
}
