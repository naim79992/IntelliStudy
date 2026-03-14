package com.example.naim.enums;

public enum FeatureEnum {
    ASK("ask"),
    RAG("rag"),
    QUIZ("quiz");

    private final String value;

    FeatureEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}