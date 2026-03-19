package com.example.naim.cef.store.duckdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Configuration
public class DuckDbConfig {
    private static final Logger log = LoggerFactory.getLogger(DuckDbConfig.class);

    @Value("${cef.duckdb.path:./intellistudy.db}")
    private String dbPath;

    @Bean(name = "duckDbConnection")
    public Connection duckDbConnection() throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath);
        initSchema(conn);
        return conn;
    }

    private void initSchema(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS cef_chunks (id INTEGER, doc_id VARCHAR NOT NULL, text VARCHAR NOT NULL, embedding BLOB, PRIMARY KEY (doc_id, id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS cef_nodes (id VARCHAR PRIMARY KEY, doc_id VARCHAR NOT NULL, label VARCHAR NOT NULL, type VARCHAR NOT NULL, frequency INTEGER DEFAULT 1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS cef_edges (from_id VARCHAR NOT NULL, to_id VARCHAR NOT NULL, doc_id VARCHAR NOT NULL, relation VARCHAR NOT NULL, weight INTEGER DEFAULT 1, PRIMARY KEY (from_id, to_id, doc_id))");
        }
    }
}
