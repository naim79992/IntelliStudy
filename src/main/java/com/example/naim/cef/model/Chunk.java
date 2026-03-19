package com.example.naim.cef.model;

public class Chunk {
    private final int    index;
    private final String docId;
    private final String text;
    private       float[] embedding;

    public Chunk(int index, String docId, String text) {
        this.index = index;
        this.docId = docId;
        this.text  = text;
    }
    public int     getIndex()     { return index; }
    public String  getDocId()     { return docId; }
    public String  getText()      { return text; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
}
