-- ===== BANNERS =====
CREATE TABLE banners (
    id UUID PRIMARY KEY,
    title VARCHAR(200),
    subtitle VARCHAR(500),
    cta_text VARCHAR(100),
    cta_link VARCHAR(500),
    image_url VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_banner_active_order ON banners(active, display_order);
