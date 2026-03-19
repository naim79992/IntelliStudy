package com.example.naim.cef.store;

import com.example.naim.cef.model.Edge;
import com.example.naim.cef.model.Node;
import java.util.List;

public interface GraphStore {
    void saveNode(Node node);
    void saveEdge(Edge edge);
    List<Node> findNodesByDocId(String docId);
    List<Node> findAdjacentNodes(String nodeId);
    List<Edge> findEdgesByDocId(String docId);
    void deleteByDocId(String docId);
}
