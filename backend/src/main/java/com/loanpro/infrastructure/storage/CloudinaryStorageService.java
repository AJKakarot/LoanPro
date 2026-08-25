package com.loanpro.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.loanpro.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
public class CloudinaryStorageService implements StorageService {

    private static final String RESOURCE_TYPE = "raw";
    private static final String DELIVERY_TYPE = "authenticated";

    private final Cloudinary cloudinary;
    private final String folder;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public CloudinaryStorageService(AppProperties properties) {
        var cfg = properties.storage().cloudinary();
        if (cfg == null || isBlank(cfg.cloudName()) || isBlank(cfg.apiKey()) || isBlank(cfg.apiSecret())) {
            throw new IllegalStateException("Cloudinary credentials are required when STORAGE_TYPE=cloudinary");
        }
        this.folder = isBlank(cfg.folder()) ? "loanpro" : cfg.folder().replaceAll("/+$", "");
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cfg.cloudName(),
                "api_key", cfg.apiKey(),
                "api_secret", cfg.apiSecret(),
                "secure", true
        ));
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            cloudinary.uploader().upload(content.readAllBytes(), ObjectUtils.asMap(
                    "public_id", publicId(key),
                    "resource_type", RESOURCE_TYPE,
                    "type", DELIVERY_TYPE,
                    "overwrite", false,
                    "unique_filename", false
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file in Cloudinary", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            String url = cloudinary.url()
                    .resourceType(RESOURCE_TYPE)
                    .type(DELIVERY_TYPE)
                    .signed(true)
                    .secure(true)
                    .generate(publicId(key));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Failed to load object from Cloudinary: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading object from Cloudinary", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load object from storage", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId(key), ObjectUtils.asMap(
                    "resource_type", RESOURCE_TYPE,
                    "type", DELIVERY_TYPE,
                    "invalidate", true
            ));
            Object status = result.get("result");
            if (status != null && !"ok".equals(status) && !"not found".equals(status)) {
                throw new IllegalStateException("Failed to delete object from Cloudinary: " + status);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file from Cloudinary", e);
        }
    }

    private String publicId(String key) {
        String trimmed = key.startsWith("/") ? key.substring(1) : key;
        return folder + "/" + trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
