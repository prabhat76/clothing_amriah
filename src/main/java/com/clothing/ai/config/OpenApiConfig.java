package com.clothing.ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.1 / Swagger UI configuration for Clothing AI E-Commerce API.
 *
 * <p>Accessible at /api/swagger-ui.html (dev) after {@code ./mvnw spring-boot:run}.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    private static final String BEARER_AUTH = "BearerAuth";

    @Bean
    public OpenAPI clothingAiOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub Repository & Developer Docs")
                        .url("https://github.com/your-org/clothing-ai-backend"))
                .servers(List.of(
                        new Server().url(publicUrl + "/api").description("Current server"),
                        new Server().url("http://localhost:8080/api").description("Local development"),
                        new Server().url("https://api.clothingai.com").description("Production")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme())
                        .addParameters("PageParam", pageParam())
                        .addParameters("SizeParam", sizeParam())
                        .addParameters("SortParam", sortParam()))
                .tags(apiTags());
    }

    // ------------------------------------------------------------------ info

    private Info apiInfo() {
        return new Info()
                .title("Clothing AI E-Commerce API")
                .description("""
                        ## Overview
                        AI-powered clothing e-commerce backend built with Spring Boot 3 + Spring AI.

                        ### Authentication
                        All protected endpoints require a JWT Bearer token obtained from
                        `POST /auth/login` or `POST /auth/register`.
                        Include it as `Authorization: Bearer <token>` in every request.

                        ### Rate Limiting
                        Unauthenticated endpoints: **60 req/min**.
                        Authenticated endpoints: **300 req/min**.

                        ### Error Format
                        All errors follow the unified `ApiResponse` envelope:
                        ```json
                        {
                          "success": false,
                          "errorCode": "RESOURCE_NOT_FOUND",
                          "message": "Product not found with id: …",
                          "timestamp": "2024-08-29T10:00:00Z"
                        }
                        ```
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("ClothingAI Engineering")
                        .email("dev@clothingai.com")
                        .url("https://clothingai.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    // ------------------------------------------------------------------ schemes

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT access token. Obtain via POST /auth/login.");
    }

    // ------------------------------------------------------------------ shared params

    private Parameter pageParam() {
        return new Parameter()
                .name("page").in("query")
                .description("Zero-based page number (0-indexed)")
                .required(false)
                .schema(new StringSchema().type("integer").example("0")._default("0"));
    }

    private Parameter sizeParam() {
        return new Parameter()
                .name("size").in("query")
                .description("Number of items per page (1–100)")
                .required(false)
                .schema(new StringSchema().type("integer").example("20")._default("20"));
    }

    private Parameter sortParam() {
        return new Parameter()
                .name("sort").in("query")
                .description("Sort expression: field,direction e.g. `createdAt,desc`")
                .required(false)
                .schema(new StringSchema().example("createdAt,desc"));
    }

    // ------------------------------------------------------------------ tag catalogue

    private List<Tag> apiTags() {
        return List.of(
                tag("Authentication",
                        "Register, login, token refresh, password reset"),
                tag("Users",
                        "User profile, address book, and account management"),
                tag("Products",
                        "Product catalogue: list, search, detail, create/update (admin)"),
                tag("Categories",
                        "Product category tree management"),
                tag("Brands",
                        "Brand management"),
                tag("Banners",
                        "Homepage hero carousel banners: upload, manage, reorder (public GET, admin writes)"),
                tag("Cart",
                        "Shopping cart: add, update, remove, clear items"),
                tag("Wishlist",
                        "Save products for later"),
                tag("Orders",
                        "Checkout, order history, order tracking, cancellation"),
                tag("Payments",
                        "Stripe payment intents, webhook handling, COD/PayPal"),
                tag("Reviews",
                        "Product reviews and ratings: create, list, mark helpful"),
                tag("Notifications",
                        "In-app notification inbox: list, mark read"),
                tag("AI - Recommendations",
                        "Personalised and trending product recommendations, semantic search"),
                tag("AI - Chatbot",
                        "Conversational AI shopping assistant (Clothie)"),
                tag("AI - Size Predict",
                        "AI-powered clothing size prediction based on body measurements"),
                tag("AI - Virtual Try-On",
                        "Upload a photo for AI style advice and fit analysis"),
                tag("AI - Descriptions",
                        "Admin-only: generate AI product descriptions (cached)"),
                tag("Admin - Dashboard",
                        "KPI summary card for the admin dashboard"),
                tag("Admin - Orders",
                        "Admin order management and status transitions"),
                tag("Admin - Audit Logs",
                        "Immutable audit trail of all write operations"));
    }

    private Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
