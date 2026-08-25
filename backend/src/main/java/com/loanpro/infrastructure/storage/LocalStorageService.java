package com.loanpro.infrastructure.storage;

import com.loanpro.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AppProperties properties) throws IOException {
        this.root = Path.of(properties.storage().localDir());
        Files.createDirectories(root);
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Invalid storage key");
            }
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root) || !Files.exists(target)) {
                throw new IllegalArgumentException("File not found");
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path target = root.resolve(key).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file", e);
        }
    }
}
