package com.example.naim.cef.model;

public class Edge {
    private final String fromId;
    private final String toId;
    private final String docId;
    private final String relation;
    private       int    weight;

    public Edge(String fromId, String toId, String docId, String relation, int weight) {
        this.fromId   = fromId;
        this.toId     = toId;
        this.docId    = docId;
        this.relation = relation;
        this.weight   = weight;
    }
    public String getFromId()   { return fromId; }
    public String getToId()     { return toId; }
    public String getDocId()    { return docId; }
    public String getRelation() { return relation; }
    public int    getWeight()   { return weight; }
    public void   setWeight(int weight) { this.weight = weight; }
}
