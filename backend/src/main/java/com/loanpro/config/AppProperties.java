package com.loanpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Storage storage,
        Mail mail,
        Seed seed,
        Security security,
        Ai ai
) {
    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays, String issuer, String audience) {}

    public record Cors(String allowedOrigins) {
        public List<String> origins() {
            return Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        }
    }

    public record Storage(String type, String localDir, S3 s3, Cloudinary cloudinary) {
        public record S3(
                String endpoint,
                String bucket,
                String region,
                String accessKey,
                String secretKey,
                boolean pathStyle
        ) {}

        public record Cloudinary(
                String cloudName,
                String apiKey,
                String apiSecret,
                String folder
        ) {}
    }

    public record Mail(String from) {}

    public record Seed(boolean enabled) {}

    public record Security(int lockoutAttempts, int lockoutMinutes) {}

    public record Ai(String serviceUrl, int timeoutSeconds) {
        public Ai {
            if (serviceUrl == null || serviceUrl.isBlank()) {
                serviceUrl = "http://127.0.0.1:8000";
            } else {
                // Java's HttpClient prefers IPv6 for "localhost"; the local AI service binds IPv4.
                serviceUrl = serviceUrl.replace("://localhost", "://127.0.0.1");
            }
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 90;
            }
        }
    }
}
