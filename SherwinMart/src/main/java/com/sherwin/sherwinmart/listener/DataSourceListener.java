package com.sherwin.sherwinmart.listener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class DataSourceListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceListener.class);
    private static volatile HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Properties props = loadProperties();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.jdbc.url"));
        config.setUsername(props.getProperty("db.user", "sa"));
        config.setPassword(props.getProperty("db.password", ""));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
        config.setDriverClassName("org.h2.Driver");

        dataSource = new HikariDataSource(config);
        LOG.info("HikariCP pool initialized against {}", config.getJdbcUrl());

        runStartupScripts();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOG.info("HikariCP pool closed");
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                LOG.warn("config.properties not found, using defaults");
            }
        } catch (IOException e) {
            LOG.error("Failed to load config.properties", e);
        }
        return props;
    }

    private void runStartupScripts() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("schema.sql");
            InputStream seedStream = getClass().getClassLoader().getResourceAsStream("seed.sql");

            if (schemaStream != null) {
                String schema = new String(schemaStream.readAllBytes());
                for (String sql : schema.split(";")) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                LOG.info("Schema loaded");
            }

            if (seedStream != null) {
                String seed = new String(seedStream.readAllBytes());
                for (String sql : seed.split(";")) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                LOG.info("Seed data loaded");
            }
        } catch (Exception e) {
            LOG.error("Failed to run startup scripts", e);
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}