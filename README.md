# Clothing AI E-Commerce Backend

Spring Boot 3 + Spring AI-powered backend for a clothing e-commerce brand. Includes auth, catalog, cart, orders, payments, reviews, recommendations, AI chatbot, virtual try-on, size prediction, and AI product description generation.

## Stack

- Java 21, Spring Boot 3.3.4
- Spring AI 1.0 (OpenAI, Embeddings, PGVector)
- PostgreSQL 16 + pgvector
- Redis (cache/sessions)
- Flyway (migrations)
- JWT auth (jjwt 0.12)
- Stripe SDK (payments)
- OpenAPI/Swagger
- Caffeine cache, Micrometer/Prometheus

## Quick Start

```bash
docker compose up -d postgres redis
export JWT_SECRET=$(openssl rand -hex 32)
export OPENAI_API_KEY=sk-...
./mvnw spring-boot:run
```

Swagger UI: http://localhost:8080/api/swagger-ui.html

## Features

### Core
- JWT + OAuth2-ready auth (access/refresh tokens, role-based access)
- Product catalog with categories, brands, variants (size/color), tags, images
- Cart + wishlist with guest-to-user merging
- Order management with Stripe / COD / PayPal / Apple Pay / Google Pay
- Reviews with ratings, verified purchase, helpfulness
- Notifications (in-app + email via MailHog)
- Audit logging

### AI
- Conversational shopping assistant ("Clothie")
- Personalized recommendations (behavioral + AI-curated)
- Similar-product suggestions
- Semantic vector search via PGVector
- AI product description generation (cached)
- Virtual try-on (image upload + AI style advice)
- AI fit analysis & size prediction

### Admin
- Dashboard summary
- Order/catalog management
- Audit log viewer

## Endpoints (high-level)

- `POST /auth/register|login|refresh|forgot-password|reset-password`
- `GET/POST/PUT/DELETE /products/*` (admin-gated writes)
- `GET /products/{slug}` (public)
- `GET /products/search?q=&categoryId=&minPrice=&maxPrice=&tag=`
- `GET /ai/recommendations/trending|for-you|similar/{id}|search?q=`
- `POST /ai/chatbot/chat`
- `POST /ai/size-predict/predict`
- `POST /ai/virtual-try-on/try/{productId}` (multipart)
- `POST /ai/descriptions/generate/{productId}` (admin)
- `POST /cart/items`, `GET /cart`, `DELETE /cart/items/{id}`
- `POST /orders/checkout`, `GET /orders`, `POST /orders/{number}/cancel`
- `POST /payments/create-intent`, `POST /payments/stripe/webhook`
- `GET/POST /reviews/product/{id}`, `POST /reviews/{id}/helpful`
- `GET /notifications`, `POST /notifications/{id}/read`
- `GET /admin/dashboard/summary`, `GET /admin/orders`, `GET /admin/audit`

## Configuration

All configuration via environment variables or `application-{profile}.yml`. See `application.yml` for the full list.

## License
MIT
