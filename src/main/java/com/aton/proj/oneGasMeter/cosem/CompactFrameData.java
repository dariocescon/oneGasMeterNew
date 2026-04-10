package com.aton.proj.oneGasMeter.cosem;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Risultato del parsing di una Compact Frame (Class 62).
 *
 * Contiene i valori estratti dal compact_buffer, indicizzati per codice OBIS.
 * Ogni valore e' un Object (Long, Boolean, Integer, byte[], etc.)
 * a seconda del tipo DLMS dell'attributo.
 */
public class CompactFrameData {

    /** ID del template (es. 47, 48, 49) */
    private final int templateId;

    /** Valori estratti, chiave = OBIS code */
    private final Map<String, Object> values = new LinkedHashMap<>();

    /** Timestamp UNIX estratto dalla frame (se presente) */
    private Instant timestamp;

    public CompactFrameData(int templateId) {
        this.templateId = templateId;
    }

    public int getTemplateId() {
        return templateId;
    }

    public void put(String obisCode, Object value) {
        values.put(obisCode, value);
    }

    public Object get(String obisCode) {
        return values.get(obisCode);
    }

    /**
     * Restituisce un valore come Long, o null se non presente o non convertibile.
     */
    public Long getLong(String obisCode) {
        Object val = values.get(obisCode);
        if (val instanceof Number n) return n.longValue();
        return null;
    }

    /**
     * Restituisce un valore come Integer, o null se non presente.
     */
    public Integer getInt(String obisCode) {
        Object val = values.get(obisCode);
        if (val instanceof Number n) return n.intValue();
        return null;
    }

    /**
     * Restituisce un valore come Boolean, o null se non presente.
     */
    public Boolean getBoolean(String obisCode) {
        Object val = values.get(obisCode);
        if (val instanceof Boolean b) return b;
        return null;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CompactFrameData{templateId=" + templateId + ", fields=" + values.size() + "}";
    }
}
