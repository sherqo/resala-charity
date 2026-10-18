package com.resala.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public class ConfigManager {
    private static ConfigManager instance;
    private final Properties props;

    private ConfigManager() {
        props = new Properties();
        // Loads from src/main/resources via the classpath — bundled into the jar
        try (InputStream in = ConfigManager.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config.properties", e);
        }
    }      // loads config.properties from classpath
    public static synchronized ConfigManager getInstance(){
        if (instance == null){
            instance = new ConfigManager();
        }
        return  instance;
    }
    public String get(String key){
        return  props.getProperty(key);
    }
    public String get(String key, String defaultValue){
        return props.getProperty(key, defaultValue);
    }
}