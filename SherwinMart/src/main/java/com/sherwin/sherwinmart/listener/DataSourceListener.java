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

/**
 * Singleton owner of the HikariCP connection pool (Section 2 rule 5, Section 12 Singleton
 * pattern). No DriverManager.getConnection() call exists anywhere outside this class.
 * On startup, schema.sql and seed.sql are applied so the app runs immediately.
 */
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
        LOG.info("HikariCP connection pool initialized against {}", config.getJdbcUrl());

        runStartupScripts();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dataSource != null) {
            dataSource.close();
            LOG.info("HikariCP connection pool closed");
        }
    }

    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not initialized — is the ServletContextListener registered?");
        }
        return dataSource;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOG.warn("config.properties not found on classpath; falling back to in-memory H2 defaults");
                props.setProperty("db.jdbc.url", "jdbc:h2:mem:sherwinmart;DB_CLOSE_DELAY=-1");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
        return props;
    }

    private void runStartupScripts() {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            String schema = readClasspathResource("schema.sql");
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql);
                }
            }
            String seed = readClasspathResource("seed.sql");
            for (String sql : seed.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql);
                }
            }
            LOG.info("schema.sql and seed.sql applied successfully");
        } catch (Exception e) {
            LOG.error("Failed to run startup schema/seed scripts", e);
        }
    }

    private String readClasspathResource(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Resource not found: " + name);
            }
            return new String(in.readAllBytes());
        }
    }
}
