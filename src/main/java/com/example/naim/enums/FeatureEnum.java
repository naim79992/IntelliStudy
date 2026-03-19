package com.example.naim.enums;

public enum FeatureEnum {
    ASK("ask"),
    RAG("rag"),
    QUIZ("quiz"),
    SUMMARIZE("summarize"),
    TRANSLATE("translate");

    private final String value;

    FeatureEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FeatureEnum fromValue(String value) {
        for (FeatureEnum f : FeatureEnum.values()) {
            if (f.value.equalsIgnoreCase(value)) return f;
        }
        return ASK; // Default fallback
    }
}