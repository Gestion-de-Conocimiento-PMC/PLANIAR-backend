package com.planiarback.planiar.config;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbInfoLogger {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void logInfo() {
        try {
            String db = jdbc.queryForObject("select current_database()", String.class);
            String user = jdbc.queryForObject("select current_user", String.class);
            System.out.println("DB INFO: current_database=" + db + " current_user=" + user);
        } catch (Exception e) {
            System.out.println("DB INFO: could not query DB - " + e.getMessage());
        }
    }
}
