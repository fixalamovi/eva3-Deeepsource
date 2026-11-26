package com.deepsource.records.support;
public final class Rules {
    private Rules() {}
    public static String text(String value, int min, int max, String field) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() < min || clean.length() > max) throw new ApiException(400, field + ": usa entre " + min + " y " + max + " caracteres");
        return clean;
    }
    public static String id(String value) { return text(value, 1, 100, "Identificador"); }
}
