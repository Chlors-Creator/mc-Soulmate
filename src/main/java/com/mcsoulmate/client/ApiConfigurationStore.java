package com.mcsoulmate.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.neoforged.fml.loading.FMLPaths;

public final class ApiConfigurationStore {
    private static final String CONFIG_FILE_NAME = "mcsoulmate-api.properties";
    private static final String PROVIDER_KEY = "provider";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String API_KEY_KEY = "apiKey";
    private static final String MODEL_KEY = "model";

    private ApiConfigurationStore() {
    }

    public static ApiConfiguration load() {
        Path configPath = configurationPath();
        if (Files.notExists(configPath)) {
            return ApiConfiguration.empty();
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load MC Soulmate API configuration", exception);
        }

        return new ApiConfiguration(
                properties.getProperty(PROVIDER_KEY, ""),
                properties.getProperty(BASE_URL_KEY, ""),
                properties.getProperty(API_KEY_KEY, ""),
                properties.getProperty(MODEL_KEY, "")
        );
    }

    public static void save(ApiConfiguration configuration) {
        Properties properties = new Properties();
        properties.setProperty(PROVIDER_KEY, configuration.provider());
        properties.setProperty(BASE_URL_KEY, configuration.baseUrl());
        properties.setProperty(API_KEY_KEY, configuration.apiKey());
        properties.setProperty(MODEL_KEY, configuration.model());

        Path configPath = configurationPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                properties.store(writer, "MC Soulmate API configuration");
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save MC Soulmate API configuration", exception);
        }
    }

    private static Path configurationPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }
}
