package com.aton.proj.oneGasMeter.cosem;

/**
 * Stato della valvola gas del contatore, estratto dalla CF8.
 *
 * output_state (boolean): true = aperta, false = chiusa
 * control_state (enum): 0=disconnected, 1=connected, 2=ready_for_reconnection
 * closureCause (unsigned): codice causa chiusura (vedi EventCode per i dettagli)
 */
public class ValveStatus {

    private final boolean outputState;
    private final int controlState;
    private final int closureCause;

    public ValveStatus(boolean outputState, int controlState, int closureCause) {
        this.outputState = outputState;
        this.controlState = controlState;
        this.closureCause = closureCause;
    }

    /**
     * Crea un ValveStatus da una CompactFrameData (CF8).
     *
     * @param cfData risultato del parsing di CF8
     * @return ValveStatus, o null se i dati non sono disponibili
     */
    public static ValveStatus fromCompactFrame(CompactFrameData cfData) {
        if (cfData == null || cfData.getTemplateId() != 8) return null;

        Boolean output = cfData.getBoolean("0.0.96.3.10.255#output");
        Integer control = cfData.getInt("0.0.96.3.10.255#control");
        Integer cause = cfData.getInt("0.0.94.39.7.255");

        if (output == null || control == null) return null;
        return new ValveStatus(output, control, cause != null ? cause : 0);
    }

    /** true = valvola fisicamente aperta, false = chiusa */
    public boolean isOpen() { return outputState; }

    /** true = valvola fisicamente chiusa */
    public boolean isClosed() { return !outputState; }

    /** Stato di controllo: 0=disconnected, 1=connected, 2=ready_for_reconnection */
    public int getControlState() { return controlState; }

    /** Descrizione leggibile dello stato di controllo */
    public String getControlStateDescription() {
        return switch (controlState) {
            case 0 -> "disconnected";
            case 1 -> "connected";
            case 2 -> "ready_for_reconnection";
            default -> "unknown (" + controlState + ")";
        };
    }

    /** Codice causa chiusura (0 = nessuna causa / valvola aperta) */
    public int getClosureCause() { return closureCause; }

    /** Descrizione della causa di chiusura usando EventCode */
    public String getClosureCauseDescription() {
        if (closureCause == 0) return "nessuna";
        return EventCode.describeCode(closureCause);
    }

    @Override
    public String toString() {
        return "ValveStatus{" +
               (outputState ? "APERTA" : "CHIUSA") +
               ", control=" + getControlStateDescription() +
               ", causa=" + getClosureCauseDescription() + "}";
    }
}
