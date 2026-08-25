package com.loanpro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotenvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotenvLoader.class);

    private DotenvLoader() {
    }

    public static void load() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(cwd.resolve(".env"), cwd.getParent().resolve(".env"));
        for (Path file : candidates) {
            if (Files.isRegularFile(file)) {
                apply(file);
                return;
            }
        }
    }

    private static void apply(Path file) {
        try {
            int loaded = 0;
            for (String raw : Files.readAllLines(file)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int split = line.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                String key = line.substring(0, split).trim();
                String value = unquote(line.substring(split + 1).trim());
                if (key.isEmpty() || System.getenv(key) != null || System.getProperty(key) != null) {
                    continue;
                }
                System.setProperty(key, value);
                loaded++;
            }
            log.info("Loaded {} values from {}", loaded, file.toAbsolutePath());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), ex);
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
