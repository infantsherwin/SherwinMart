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
    pub