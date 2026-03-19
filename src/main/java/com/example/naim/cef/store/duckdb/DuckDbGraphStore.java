package com.example.naim.cef.store.duckdb;

import com.example.naim.cef.model.Edge;
import com.example.naim.cef.model.Node;
import com.example.naim.cef.store.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class DuckDbGraphStore implements GraphStore {
    private static final Logger log = LoggerFactory.getLogger(DuckDbGraphStore.class);
    private final Connection conn;

    public DuckDbGraphStore(@Qualifier("duckDbConnection") Connection conn) {
        this.conn = conn;
    }

    @Override
    public void saveNode(Node node) {
        String sql = "INSERT INTO cef_nodes (id, doc_id, label, type, frequency) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE SET frequency = cef_nodes.frequency + 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, node.getId());
            ps.setString(2, node.getDocId());
            ps.setString(3, node.getLabel());
            ps.setString(4, node.getType());
            ps.setInt(5, node.getFrequency());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Failed to save node", e); }
    }

    @Override
    public List<Node> findNodesByDocId(String docId) {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT id, doc_id, label, type, frequency FROM cef_nodes WHERE doc_id = ? ORDER BY frequency DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, docId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodes.add(new Node(rs.getString("id"), rs.getString("doc_id"), rs.getString("label"), rs.getString("type"), rs.getInt("frequency")));
            }
        } catch (SQLException e) { log.error("Failed to load nodes", e); }
        return nodes;
    }

    @Override
    public List<Node> findAdjacentNodes(String nodeId) {
        List<Node> nodes = new ArrayList<>();
        String sql = "SELECT n.id, n.doc_id, n.label, n.type, n.frequency FROM cef_nodes n WHERE n.id IN (SELECT to_id FROM cef_edges WHERE from_id = ? UNION SELECT from_id FROM cef_edges WHERE to_id = ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId); ps.setString(2, nodeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nodes.add(new Node(rs.getString("id"), rs.getString("doc_id"), rs.getString("label"), rs.getString("type"), rs.getInt("frequency")));
            }
        } catch (SQLException e) { log.error("Failed to load adj nodes", e); }
        return nodes;
    }

    @Override
    public void saveEdge(Edge edge) {
        String sql = "INSERT INTO cef_edges (from_id, to_id, doc_id, relation, weight) VALUES (?, ?, ?, ?, ?) ON CONFLICT (from_id, to_id, doc_id) DO UPDATE SET weight = excluded.weight";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, edge.getFromId());
            ps.setString(2, edge.getToId());
            ps.setString(3, edge.getDocId());
            ps.setString(4, edge.getRelation());
            ps.setInt(5, edge.getWeight());
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Failed to save edge", e); }
    }

    @Override
    public List<Edge> findEdgesByDocId(String docId) {
        List<Edge> edges = new ArrayList<>();
        String sql = "SELECT from_id, to_id, doc_id, relation, weight FROM cef_edges WHERE doc_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, docId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                edges.add(new Edge(rs.getString("from_id"), rs.getString("to_id"), rs.getString("doc_id"), rs.getString("relation"), rs.getInt("weight")));
            }
        } catch (SQLException e) { log.error("Failed to load edges", e); }
        return edges;
    }

    @Override
    public void deleteByDocId(String docId) {
        try {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cef_edges WHERE doc_id = ?")) { ps.setString(1, docId); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cef_nodes WHERE doc_id = ?")) { ps.setString(1, docId); ps.executeUpdate(); }
        } catch (SQLException e) { log.error("Failed delete graph", e); }
    }
}
