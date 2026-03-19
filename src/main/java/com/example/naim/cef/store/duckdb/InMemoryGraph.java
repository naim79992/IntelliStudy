package com.example.naim.cef.store.duckdb;

import com.example.naim.cef.model.Node;
import com.example.naim.cef.store.GraphStore;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryGraph {
    private final GraphStore graphStore;
    private final Map<String, Graph<String, DefaultWeightedEdge>> graphs = new ConcurrentHashMap<>();

    public InMemoryGraph(GraphStore graphStore) {
        this.graphStore = graphStore;
    }

    public void refresh(String docId) {
        Graph<String, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

        List<Node> nodes = graphStore.findNodesByDocId(docId);
        for (Node n : nodes) {
            graph.addVertex(n.getLabel().toLowerCase());
        }

        graphStore.findEdgesByDocId(docId).forEach(edge -> {
            String from = extractLabel(edge.getFromId(), docId);
            String to   = extractLabel(edge.getToId(), docId);
            if (graph.containsVertex(from) && graph.containsVertex(to)) {
                DefaultWeightedEdge e = graph.addEdge(from, to);
                if (e != null) graph.setEdgeWeight(e, edge.getWeight());
            }
        });
        
        graphs.put(docId, graph);
    }

    public Set<String> findRelated(String docId, Collection<String> seedTerms, int depth) {
        Graph<String, DefaultWeightedEdge> graph = graphs.get(docId);
        if (graph == null) return Collections.emptySet();

        Set<String> result = new LinkedHashSet<>();
        for (String term : seedTerms) {
            String lc = term.toLowerCase();
            if (!graph.containsVertex(lc)) continue;

            BreadthFirstIterator<String, DefaultWeightedEdge> bfs = new BreadthFirstIterator<>(graph, lc);
            while (bfs.hasNext()) {
                String v = bfs.next();
                if (bfs.getDepth(v) > depth) break;
                result.add(v);
            }
        }
        return result;
    }

    public void evict(String docId) {
        graphs.remove(docId);
    }

    private String extractLabel(String nodeId, String docId) {
        String prefix = docId + "_";
        return nodeId.startsWith(prefix) ? nodeId.substring(prefix.length()) : nodeId;
    }
}
