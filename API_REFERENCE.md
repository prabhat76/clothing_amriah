# Clothing AI Backend — Complete API Reference

> **Base URL:** `http://localhost:8080/api`  
> **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`  
> **Content-Type:** `application/json` for all requests unless noted as `multipart/form-data`

---

## Authentication

All protected endpoints require:
```
Authorization: Bearer <accessToken>
```

### Unified Response Envelope

Every endpoint returns this shape:

```json
{
  "success": true,
  "message": "...",
  "data": { },
  "errorCode": null,
  "timestamp": "2026-10-02T08:48:00Z"
}
```

On error: `"success": false`, `"data": null`, `"errorCode": "RESOURCE_NOT_FOUND"`.

### Common Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| `VALIDATION_FAILED` | 400 | Bean validation failed — `message` lists field errors |
| `BAD_REQUEST` | 400 | Business rule violation |
| `RESOURCE_NOT_FOUND` | 404 | Entity not found |
| `CONFLICT` | 409 | Duplicate (email, SKU, review) |
| `AUTH_FAILED` | 401 | Wrong credentials or expired token |
| `UNAUTHORIZED` | 401 | Missing/invalid JWT |
| `ACCESS_DENIED` | 403 | Insufficient role |
| `ACCOUNT_DISABLED` | 403 | Account is banned/disabled |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 1. Authentication — `/auth`

All endpoints are **public** (no token required).

---

### `POST /auth/register`

Create a new customer account. Returns tokens so the user is immediately logged in.

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "min8chars",
  "firstName": "Riya",
  "lastName": "Sharma",
  "phone": "9999999999"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | ✅ | Valid email format |
| `password` | string | ✅ | 8–100 characters |
| `firstName` | string | ✅ | Max 80 chars |
| `lastName` | string | ✅ | Max 80 chars |
| `phone` | string | ❌ | Max 30 chars |

**Response 201:**
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 3600000,
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "firstName": "Riya",
      "lastName": "Sharma",
      "phone": "9999999999",
      "avatarUrl": null,
      "role": "CUSTOMER",
      "heightCm": null,
      "weightKg": null,
      "gender": null
    }
  }
}
```

**Errors:** `409` email already registered · `400` password too short

---

### `POST /auth/login`

**Request body:**
```json
{ "email": "user@example.com", "password": "mypassword" }
```

**Response 200:** Same `TokenResponse` as register.

**Errors:** `401 AUTH_FAILED` wrong credentials · `403 ACCOUNT_DISABLED`

---

### `POST /auth/refresh`

Exchange a refresh token for a new token pair.

**Request body:**
```json
{ "refreshToken": "eyJ..." }
```

**Response 200:** Fresh `TokenResponse`.

**Errors:** `401` token expired or invalid

---

### `POST /auth/forgot-password`

**Request body:**
```json
{ "email": "user@example.com" }
```

**Response 200:** Always succeeds to prevent user enumeration.

---

### `POST /auth/reset-password`

**Request body:**
```json
{ "token": "uuid-from-email-link", "newPassword": "newpass123" }
```

**Errors:** `400` token expired, already used, or invalid

---

## 2. Users — `/users` 🔒

All endpoints require authentication.

---

### `GET /users/me`

Returns the current user's profile.

**Response 200:** `UserResponse`
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "Riya",
  "lastName": "Sharma",
  "phone": "9999999999",
  "avatarUrl": "https://...",
  "role": "CUSTOMER",
  "heightCm": 165,
  "weightKg": 58,
  "gender": "FEMALE"
}
```
`gender` enum: `MALE` · `FEMALE` · `OTHER` · `PREFER_NOT_TO_SAY`

---

### `PATCH /users/me`

Partial update — only included fields are changed. Cannot change `email` or `role`.

**Request body (all optional):**
```json
{
  "firstName": "Riya",
  "lastName": "Sharma",
  "phone": "9999999999",
  "avatarUrl": "https://...",
  "heightCm": 165,
  "weightKg": 58,
  "gender": "FEMALE"
}
```

---

### `POST /users/me/change-password`

**Request body:**
```json
{ "currentPassword": "oldpassword", "newPassword": "newmin8chars" }
```

**Errors:** `400` current password incorrect · `400` new password too short

---

### `GET /users/me/addresses`

**Response 200:** `AddressResponse[]`
```json
[
  {
    "id": "uuid",
    "label": "Home",
    "fullName": "Riya Sharma",
    "phone": "9999999999",
    "line1": "12 MG Road",
    "line2": "Apt 4B",
    "city": "Jaipur",
    "stateProvince": "Rajasthan",
    "postalCode": "302001",
    "country": "India",
    "defaultAddress": true
  }
]
```

---

### `POST /users/me/addresses` → 201

**Request body:**
```json
{
  "label": "Home",
  "fullName": "Riya Sharma",
  "phone": "9999999999",
  "line1": "12 MG Road",
  "line2": "Apt 4B",
  "city": "Jaipur",
  "stateProvince": "Rajasthan",
  "postalCode": "302001",
  "country": "India",
  "defaultAddress": true
}
```

| Field | Required |
|-------|----------|
| `fullName` | ✅ |
| `line1` | ✅ |
| `city` | ✅ |
| `postalCode` | ✅ |
| `country` | ✅ |
| `label`, `phone`, `line2`, `stateProvince`, `defaultAddress` | ❌ |

---

### `PUT /users/me/addresses/{id}`

Full replacement — same body as POST.

---

### `DELETE /users/me/addresses/{id}`

**Response 200.** Errors: `404` not found.

---

## 3. Products — `/products`

Read endpoints are **public**. Write endpoints require `ADMIN` or `STAFF` role.

---

### `GET /products?page=0&size=20&sort=createdAt,desc`

**Response 200:** `PageResponse<ProductSummaryResponse>`
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Classic White Tee",
        "slug": "classic-white-tee",
        "sku": "CWT-001",
        "shortDescription": "Everyday essential in 100% cotton",
        "mainImageUrl": "https://...",
        "price": 29.99,
        "compareAtPrice": 39.99,
        "averageRating": 4.3,
        "reviewCount": 24,
        "featured": true,
        "newArrival": false,
        "categoryId": "uuid",
        "categoryName": "Men's T-Shirts",
        "brandId": "uuid",
        "brandName": "Fabindia",
        "minVariantPrice": 29.99
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

### `GET /products/search`

| Query Param | Type | Description |
|-------------|------|-------------|
| `q` | string | Keyword — searched in name, description, tags |
| `categoryId` | UUID | Filter by category |
| `brandId` | UUID | Filter by brand |
| `minPrice` | decimal | Minimum price (inclusive) |
| `maxPrice` | decimal | Maximum price (inclusive) |
| `tag` | string | Tag slug filter |
| `page` | int | Default `0` |
| `size` | int | Default `20`, max `100` |
| `sort` | string | `price,asc` · `price,desc` · `rating,desc` · `createdAt,desc` |

---

### `GET /products/featured?page=0&size=12`

---

### `GET /products/{slug}`

**Response 200:** `ProductDetailResponse`
```json
{
  "data": {
    "product": { /* ProductSummaryResponse */ },
    "description": "Full HTML/markdown description",
    "aiGeneratedDescription": "AI-written description...",
    "imageUrls": ["https://img1.jpg", "https://img2.jpg"],
    "tags": ["cotton", "basics", "summer"],
    "variants": [
      {
        "id": "uuid",
        "productId": "uuid",
        "sku": "CWT-001-M-WHT",
        "size": "M",
        "color": "White",
        "colorHex": "#ffffff",
        "material": "100% Cotton",
        "price": 29.99,
        "salePrice": null,
        "effectivePrice": 29.99,
        "stockQuantity": 42,
        "imageUrl": "https://...",
        "barcode": null,
        "active": true
      }
    ],
    "availableSizes": ["XS", "S", "M", "L", "XL"],
    "availableColors": ["White", "Navy Blue", "Black"],
    "viewCount": 1240,
    "createdAt": "2026-09-01T10:00:00Z",
    "updatedAt": "2026-10-01T08:00:00Z"
  }
}
```

---

### `GET /products/id/{uuid}`

Same as `GET /products/{slug}` but addressed by internal UUID.

---

### `POST /products/{id}/view`

Fire-and-forget — increments `viewCount`. No auth needed. Always returns 200.

---

### `POST /products` 🔒 [ADMIN/STAFF] → 201

**Request body:**
```json
{
  "name": "Classic White Tee",
  "sku": "CWT-001",
  "description": "Full description...",
  "shortDescription": "Everyday essential",
  "categoryId": "uuid",
  "brandId": "uuid",
  "price": 29.99,
  "compareAtPrice": 39.99,
  "costPrice": 12.00,
  "currency": "USD",
  "mainImageUrl": "https://...",
  "imageUrls": ["https://img1.jpg"],
  "tags": ["cotton", "basics"],
  "featured": false,
  "newArrival": true,
  "weightGrams": 200,
  "variants": [
    {
      "sku": "CWT-001-S-WHT",
      "size": "S",
      "color": "White",
      "colorHex": "#ffffff",
      "material": "Cotton",
      "price": 29.99,
      "salePrice": null,
      "stockQuantity": 50,
      "lowStockThreshold": 5,
      "imageUrl": null,
      "barcode": null
    }
  ]
}
```

**Errors:** `409` SKU already exists · `404` category or brand not found

---

### `PUT /products/{id}` 🔒 [ADMIN/STAFF]

Partial update — `null` fields are ignored.

```json
{
  "active": true,
  "status": "ACTIVE",
  "price": 24.99,
  "featured": true
}
```

`status` enum: `DRAFT` · `ACTIVE` · `OUT_OF_STOCK` · `DISCONTINUED` · `ARCHIVED`

---

### `DELETE /products/{id}` 🔒 [ADMIN]

Soft-archives (sets `active=false`, `status=ARCHIVED`).

---

### `POST /products/{id}/variants` 🔒 [ADMIN/STAFF] → 201

```json
{
  "sku": "CWT-001-XL-WHT",
  "size": "XL",
  "color": "White",
  "colorHex": "#ffffff",
  "material": "Cotton",
  "price": 29.99,
  "salePrice": null,
  "stockQuantity": 30,
  "lowStockThreshold": 5,
  "imageUrl": null,
  "barcode": null
}
```

---

### `POST /products/{id}/images/upload` 🔒 [ADMIN/STAFF]

`multipart/form-data`

| Field | Type | Description |
|-------|------|-------------|
| `file` | File | Image (jpg/png/webp) |
| `setAsMain` | boolean | `true` = set as hero image. Default `false` |

---

## 4. Categories — `/categories`

### `GET /categories` (public)

**Response 200:** `CategoryResponse[]`
```json
[
  {
    "id": "uuid",
    "name": "Men's T-Shirts",
    "slug": "mens-t-shirts",
    "description": "...",
    "imageUrl": "https://...",
    "parentId": "uuid-or-null",
    "displayOrder": 1,
    "active": true
  }
]
```

> Build the tree client-side: items with `parentId: null` are root categories. Nest by matching `parentId` to `id`.

### `POST /categories` 🔒 [ADMIN] → 201
```json
{ "name": "Men's T-Shirts", "description": "...", "imageUrl": "https://...", "parentId": null, "displayOrder": 1 }
```

### `PUT /categories/{id}` 🔒 [ADMIN]

### `DELETE /categories/{id}` 🔒 [ADMIN]

**Errors:** `409` if category has active products attached.

---

## 5. Brands — `/brands`

### `GET /brands` (public)

**Response 200:** `BrandResponse[]`
```json
[{ "id": "uuid", "name": "Fabindia", "slug": "fabindia", "description": "...", "logoUrl": "https://...", "active": true }]
```

### `POST /brands` 🔒 [ADMIN] → 201
```json
{ "name": "Fabindia", "description": "...", "logoUrl": "https://..." }
```

### `PUT /brands/{id}` 🔒 [ADMIN]
### `DELETE /brands/{id}` 🔒 [ADMIN]

---

## 6. Banners — `/banners`

### `GET /banners` (public)

Returns active banners sorted by `displayOrder` ascending.

**Response 200:** `BannerResponse[]`
```json
[
  {
    "id": "uuid",
    "title": "Summer Sale",
    "subtitle": "Up to 50% off",
    "ctaText": "Shop Now",
    "ctaLink": "/sale",
    "imageUrl": "https://res.cloudinary.com/...",
    "displayOrder": 0,
    "active": true,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

### `GET /banners/admin` 🔒 [ADMIN] — all banners including inactive
### `GET /banners/admin/{id}` 🔒 [ADMIN]

### `POST /banners/admin` 🔒 [ADMIN] → 201
```json
{
  "title": "Summer Sale",
  "subtitle": "Up to 50% off",
  "ctaText": "Shop Now",
  "ctaLink": "/sale",
  "imageUrl": "https://...",
  "displayOrder": 0,
  "active": true
}
```

### `PUT /banners/admin/{id}` 🔒 [ADMIN] — partial update, all fields optional

### `POST /banners/admin/upload` 🔒 [ADMIN] → 201

`multipart/form-data`

| Field | Type | Required |
|-------|------|----------|
| `file` | File | ✅ Image (jpg/png/webp) |
| `title` | string | ❌ |
| `subtitle` | string | ❌ |
| `ctaText` | string | ❌ |
| `ctaLink` | string | ❌ |
| `displayOrder` | int | ❌ Default `0` |
| `active` | boolean | ❌ Default `true` |

### `DELETE /banners/admin/{id}` 🔒 [ADMIN] → 204

---

## 7. Cart — `/cart` 🔒

### `GET /cart`

Returns the current cart (auto-creates an empty one if needed).

**Response 200:** `CartResponse`
```json
{
  "id": "uuid",
  "items": [
    {
      "id": "cart-item-uuid",
      "variantId": "uuid",
      "productId": "uuid",
      "productName": "Classic White Tee",
      "size": "M",
      "color": "White",
      "imageUrl": "https://...",
      "sku": "CWT-001-M-WHT",
      "unitPrice": 29.99,
      "quantity": 2,
      "lineTotal": 59.98
    }
  ],
  "subtotal": 59.98,
  "discount": 0.00,
  "shipping": 9.99,
  "tax": 4.80,
  "total": 74.77,
  "itemCount": 2
}
```

> Shipping is free when `subtotal >= 100.00`. Tax rate is 8%.

---

### `POST /cart/items`

```json
{ "variantId": "uuid", "quantity": 1 }
```

> ⚠️ If the variant is **already in the cart**, quantity is **added** to the existing value — not replaced.

**Errors:** `400` insufficient stock · `404` variant not found

---

### `PATCH /cart/items/{itemId}`

```json
{ "quantity": 3 }
```

> Set `quantity: 0` to remove the item.

**Errors:** `400` insufficient stock · `404` item not found

---

### `DELETE /cart/items/{itemId}`

Removes the specific line item.

---

### `DELETE /cart`

Clears all items from the cart. Cart record is preserved.

---

## 8. Wishlist — `/wishlist` 🔒

### `GET /wishlist`

**Response 200:** `WishlistResponse[]`
```json
[
  {
    "id": "wishlist-entry-uuid",
    "productId": "uuid",
    "productName": "Classic White Tee",
    "slug": "classic-white-tee",
    "imageUrl": "https://...",
    "price": 29.99,
    "averageRating": 4.3
  }
]
```

### `POST /wishlist/{productId}` → 201

Idempotent — adding an already-wishlisted product is a no-op.

**Errors:** `404` product not found

### `DELETE /wishlist/{productId}`

Idempotent.

### `GET /wishlist/{productId}/check`

**Response 200:** `{ "data": true }` or `{ "data": false }`

---

## 9. Orders — `/orders` 🔒

### `POST /orders/checkout` → 201

**Request body:**
```json
{
  "shippingAddressId": "uuid",
  "billingAddressId": "uuid",
  "couponCode": "SUMMER20",
  "notes": "Leave at door",
  "paymentMethod": "STRIPE"
}
```

| Field | Required | Values |
|-------|----------|--------|
| `shippingAddressId` | ✅ | UUID from `/users/me/addresses` |
| `billingAddressId` | ❌ | Defaults to `shippingAddressId` |
| `couponCode` | ❌ | — |
| `notes` | ❌ | Max 1000 chars |
| `paymentMethod` | ✅ | `STRIPE` · `PAYPAL` · `COD` |

**Response 201:** `OrderResponse`
```json
{
  "id": "uuid",
  "orderNumber": "CL-20261002143000-1234",
  "status": "PENDING",
  "paymentStatus": "AUTHORIZED",
  "paymentToken": "pi_xxx_secret_yyy",
  "items": [
    {
      "id": "uuid",
      "variantId": "uuid",
      "productId": "uuid",
      "productName": "Classic White Tee",
      "size": "M",
      "color": "White",
      "sku": "CWT-001-M-WHT",
      "imageUrl": "https://...",
      "unitPrice": 29.99,
      "quantity": 2,
      "lineTotal": 59.98
    }
  ],
  "shippingAddress": {
    "fullName": "Riya Sharma",
    "phone": "9999999999",
    "line1": "12 MG Road",
    "line2": "Apt 4B",
    "city": "Jaipur",
    "stateProvince": "Rajasthan",
    "postalCode": "302001",
    "country": "India"
  },
  "subtotal": 59.98,
  "discount": 0.00,
  "shippingCost": 9.99,
  "tax": 4.80,
  "total": 74.77,
  "currency": "USD",
  "trackingNumber": null,
  "shippingCarrier": null,
  "createdAt": "2026-10-02T08:48:00Z",
  "shippedAt": null,
  "deliveredAt": null,
  "cancelledAt": null
}
```

#### `paymentToken` usage by payment method

| `paymentMethod` | `paymentToken` value | Frontend action |
|----------------|----------------------|-----------------|
| `STRIPE` | Stripe `client_secret` e.g. `pi_xxx_secret_yyy` | Call `stripe.confirmPayment({ clientSecret: paymentToken })` |
| `PAYPAL` | PayPal Order ID e.g. `9XJ08458WG9784765` | Pass to PayPal JS SDK `createOrder` callback, then call `POST /payments/paypal/capture/{paymentToken}` |
| `COD` | `"PENDING_CL-..."` | No payment action needed — order confirmed immediately |
| Any (gateway error) | `null` | Show "Order placed — complete payment from Order History" |
| `STRIPE` (dev, no key) | `"SIMULATED_STRIPE_CL-..."` | Skip Stripe.js, treat as confirmed |
| `PAYPAL` (dev, no key) | `"SIMULATED_PAYPAL_CL-..."` | Skip PayPal SDK, treat as confirmed |

**Errors:**
- `400` cart is empty
- `400` insufficient stock for SKU `{sku}` — shows which variant failed
- `404` address not found
- `403` address belongs to another user

---

### `GET /orders?page=0&size=20`

Returns the current user's order history, newest first.

---

### `GET /orders/{orderNumber}`

Full order detail. Returns `403` if the order belongs to another user.

---

### `POST /orders/{orderNumber}/cancel`

Cancels the order and restores stock.

**Errors:**
- `400` order is already SHIPPED or DELIVERED
- `403` order belongs to another user

---

### `GET /orders/{orderNumber}/tracking`

```json
{
  "orderNumber": "CL-...",
  "status": "SHIPPED",
  "trackingNumber": "FX9876543",
  "shippingCarrier": "FedEx",
  "shippedAt": "2026-10-03T10:00:00Z",
  "deliveredAt": null,
  "estimatedDelivery": null
}
```

---

## 10. Payments — `/payments` 🔒

> Most frontend payment flows should use `POST /orders/checkout` which handles payment creation internally and returns `paymentToken`. Use these endpoints for standalone payment creation or status queries.

### `POST /payments/create-intent`

```json
{ "orderId": "uuid", "method": "STRIPE" }
```

**Response 200:**
```json
{
  "transactionId": "pi_xxx",
  "clientSecret": "pi_xxx_secret_yyy",
  "status": "AUTHORIZED",
  "amount": 74.77,
  "currency": "USD"
}
```

### `POST /payments/paypal/capture/{paypalOrderId}`

Call this after `onApprove` fires in the PayPal JS SDK.

```javascript
// Full PayPal frontend integration:
paypal.Buttons({
  createOrder: () => {
    return checkoutResponse.data.paymentToken; // from POST /orders/checkout
  },
  onApprove: async (data) => {
    const res = await fetch(`/api/payments/paypal/capture/${data.orderID}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${accessToken}` }
    });
    const json = await res.json();
    if (json.data.status === 'COMPLETED') {
      // redirect to order success page
    }
  }
}).render('#paypal-button-container');
```

**Response 200:**
```json
{
  "paypalOrderId": "9XJ08458WG9784765",
  "status": "COMPLETED",
  "amount": 74.77,
  "currency": "USD"
}
```

### `GET /payments/order/{orderId}`

Returns the latest payment status for an order.

### `POST /payments/stripe/webhook` (public — Stripe-signed)

Configure this URL in Stripe Dashboard → Developers → Webhooks.  
Handled events: `payment_intent.succeeded` · `payment_intent.payment_failed` · `charge.refunded`

### `POST /payments/paypal/webhook` (public — PayPal-signed)

Configure in PayPal Developer Dashboard → Apps & Credentials → Webhooks.  
Handled events: `PAYMENT.CAPTURE.COMPLETED` · `PAYMENT.CAPTURE.DENIED` · `PAYMENT.CAPTURE.REFUNDED`

---

## 11. Reviews — `/reviews`

### `GET /reviews/product/{productId}?page=0&size=20` (public)

**Response 200:** `PageResponse<ReviewResponse>`
```json
{
  "content": [
    {
      "id": "uuid",
      "productId": "uuid",
      "productName": "Classic White Tee",
      "userId": "uuid",
      "userName": "Riya S.",
      "userAvatar": "https://...",
      "rating": 5,
      "title": "Perfect fit!",
      "comment": "Great quality, exactly as described.",
      "sizeFit": 3,
      "quality": 5,
      "verifiedPurchase": true,
      "helpfulCount": 12,
      "createdAt": "2026-09-15T10:00:00Z"
    }
  ]
}
```

### `POST /reviews` 🔒 → 201

```json
{
  "productId": "uuid",
  "rating": 4,
  "title": "Great fit, runs slightly small",
  "comment": "Bought size M, fits like a small. Quality is excellent.",
  "sizeFit": 2,
  "quality": 5,
  "orderItemId": "uuid"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `productId` | ✅ | — |
| `rating` | ✅ | 1–5 |
| `title` | ❌ | Max 150 chars |
| `comment` | ❌ | — |
| `sizeFit` | ❌ | 1=very small · 3=true to size · 5=very large |
| `quality` | ❌ | 1–5 |
| `orderItemId` | ❌ | Enables "Verified Purchase" badge |

**Errors:** `409` you have already reviewed this product

### `POST /reviews/{id}/helpful` 🔒

Increments helpful count. No body required.

### `DELETE /reviews/{id}` 🔒 [ADMIN]

---

## 12. Notifications — `/notifications` 🔒

### `GET /notifications?page=0&size=20`

**Response 200:** `PageResponse<NotificationResponse>`
```json
{
  "content": [
    {
      "id": "uuid",
      "type": "ORDER",
      "title": "Order Confirmed",
      "body": "Your order CL-20261002143000-1234 has been placed successfully.",
      "payload": "CL-20261002143000-1234",
      "isRead": false,
      "readAt": null,
      "channel": "IN_APP",
      "createdAt": "2026-10-02T09:00:00Z"
    }
  ]
}
```

`type` enum: `ORDER` · `PROMOTION` · `BACK_IN_STOCK` · `PRICE_DROP` · `SYSTEM`

### `GET /notifications/unread-count`

**Response 200:** `{ "data": 3 }`

### `POST /notifications/{id}/read`

Idempotent — marks a single notification as read.

### `POST /notifications/read-all`

Marks all unread notifications as read.

### `DELETE /notifications/{id}`

---

## 13. Admin — `/admin` 🔒 [ADMIN/STAFF]

### `GET /admin/dashboard/summary`

**Response 200:** `Map<String, Object>`
```json
{
  "data": {
    "totalRevenue": 152430.50,
    "revenueToday": 4230.00,
    "ordersTotal": 1840,
    "ordersPending": 23,
    "ordersShipped": 45,
    "totalCustomers": 3210,
    "newCustomersToday": 12,
    "totalProducts": 340,
    "lowStockProducts": 8,
    "averageOrderValue": 82.84
  }
}
```

### `GET /admin/dashboard/revenue-chart?days=30`

### `GET /admin/dashboard/top-products?limit=10&days=30`

### `GET /admin/orders?status=PENDING&page=0&size=50`

`status` enum: `PENDING` · `CONFIRMED` · `PROCESSING` · `SHIPPED` · `DELIVERED` · `CANCELLED` · `RETURNED` · `REFUNDED`

### `GET /admin/orders/{orderNumber}`

Returns any order regardless of customer ownership.

### `PATCH /admin/orders/{orderNumber}/status`

```json
{
  "status": "SHIPPED",
  "trackingNumber": "FX9876543",
  "shippingCarrier": "FedEx"
}
```

Valid `status` values: `CONFIRMED` · `PROCESSING` · `SHIPPED` · `DELIVERED` · `CANCELLED` · `REFUNDED`

---

## 14. Pagination

All paginated endpoints return:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

---

## 15. Edge Cases & Frontend Handling

| Scenario | API behaviour | Frontend action |
|----------|---------------|-----------------|
| Cart empty on checkout | `400 BAD_REQUEST` "Cart is empty" | Redirect to shop |
| Stock runs out between add-to-cart and checkout | `400 BAD_REQUEST` "Insufficient stock for SKU X" | Show which item failed, refresh cart |
| Stripe gateway down | `paymentToken` = `null` | "Order placed — complete payment from Order History" |
| Stripe not configured (dev) | `paymentToken` starts with `SIMULATED_STRIPE_` | Skip Stripe.js, show order confirmation |
| PayPal not configured (dev) | `paymentToken` starts with `SIMULATED_PAYPAL_` | Skip PayPal SDK, show order confirmation |
| PayPal capture fails after `onApprove` | `400 BAD_REQUEST` | Show "Payment capture failed — try again" |
| Duplicate review | `409 CONFLICT` | "You've already reviewed this product" |
| Expired access token | `401 UNAUTHORIZED` | Call `POST /auth/refresh` with refresh token; if that also 401s, redirect to login |
| Expired refresh token | `401 AUTH_FAILED` | Clear tokens from storage, redirect to login |
| Address belongs to another user | `403 FORBIDDEN` | Should not be reachable — only show the current user's own addresses |
| Category delete has products | `409 CONFLICT` | "Move or archive the products in this category first" |
| Product SKU conflict | `409 CONFLICT` | "A product with this SKU already exists" |
| File upload too large | `413 PAYLOAD_TOO_LARGE` | "Image must be under 25 MB" |

---

## 16. Environment Variables

Copy `.env.example` to `.env` and fill in:

```env
# Database (Neon / PostgreSQL)
DATABASE_URL=jdbc:postgresql://host/db?sslmode=require
DATABASE_USERNAME=db_user
DATABASE_PASSWORD=db_password

# JWT
JWT_SECRET=at-least-256-bit-random-string

# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# PayPal
PAYPAL_CLIENT_ID=AXxx...
PAYPAL_CLIENT_SECRET=Exx...
PAYPAL_MODE=sandbox
PAYPAL_WEBHOOK_ID=xxxxx

# OpenAI (AI features)
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini

# Cloudinary (image upload)
CLOUDINARY_CLOUD_NAME=your_cloud
CLOUDINARY_API_KEY=123456789
CLOUDINARY_API_SECRET=xxx

# Server
PUBLIC_URL=http://localhost:8080
CORS_ORIGINS=http://localhost:3000,http://localhost:5173
```

---

*Generated: 2026-10-02 — reflects backend version with PayPal + Stripe integration*
