package com.example.naim.cef.model;

public class Node {
    private final String id;
    private final String docId;
    private final String label;
    private final String type;
    private       int    frequency;

    public Node(String id, String docId, String label, String type, int frequency) {
        this.id        = id;
        this.docId     = docId;
        this.label     = label;
        this.type      = type;
        this.frequency = frequency;
    }
    public String getId()        { return id; }
    public String getDocId()     { return docId; }
    public String getLabel()     { return label; }
    public String getType()      { return type; }
    public int    getFrequency() { return frequency; }
    public void   setFrequency(int frequency) { this.frequency = frequency; }
}
