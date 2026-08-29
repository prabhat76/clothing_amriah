package com.clothing.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Ai ai = new Ai();
    private Upload upload = new Upload();
    private RateLimit rateLimit = new RateLimit();
    private Notifications notifications = new Notifications();
    private Payment payment = new Payment();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpiration;
        private long refreshTokenExpiration;
    }

    @Data
    public static class Cors {
        private String allowedOrigins;
    }

    @Data
    public static class Ai {
        private OpenAi openai = new OpenAi();
        private boolean enabled = true;
        private int cacheTtlMinutes = 60;

        @Data
        public static class OpenAi {
            private String apiKey;
            private String model;
            private String embeddingModel;
        }
    }

    @Data
    public static class Upload {
        private String baseDir;
        private String publicUrl;
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute = 100;
    }

    @Data
    public static class Notifications {
        private Email email = new Email();

        @Data
        public static class Email {
            private String from;
        }
    }

    @Data
    public static class Payment {
        private Stripe stripe = new Stripe();

        @Data
        public static class Stripe {
            private String secretKey;
            private String webhookSecret;
            private String currency;
        }
    }
}
