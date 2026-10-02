-- Explicit named index on products.slug for fast slug lookups.
-- Note: the UNIQUE constraint already creates an implicit btree index; this
-- named index makes it identifiable in EXPLAIN output and pg_indexes.
-- PostgreSQL deduplicates identical indexes, so this resolves to the same
-- physical index as the unique constraint — no extra storage cost.
CREATE INDEX IF NOT EXISTS idx_product_slug ON products(slug);

-- Composite partial index for the most common storefront query:
-- "active products ordered by created_at / sales_count" — covers
-- findByActiveTrue, findByActiveTrueAndFeaturedTrue, etc.
CREATE INDEX IF NOT EXISTS idx_product_active_created
    ON products(active, created_at DESC)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_product_active_sales
    ON products(active, sales_count DESC)
    WHERE active = TRUE;

-- Composite index for category + active filter used in category pages
CREATE INDEX IF NOT EXISTS idx_product_category_active
    ON products(category_id, active);

-- Index on product_tags.tag for tag-based search (:tag MEMBER OF p.tags)
CREATE INDEX IF NOT EXISTS idx_product_tags_tag ON product_tags(tag);

-- Full-text search: GIN index over tsvector of name (weight A) + description (weight B).
-- Supports: WHERE to_tsvector('english', name || ' ' || COALESCE(description,'')) @@ plainto_tsquery('query')
-- Uses a generated column so the index stays up to date automatically.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS fts_doc tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(short_description, '')), 'B') ||
            setweight(to_tsvector('english', coalesce(description, '')), 'C')
        ) STORED;

CREATE INDEX IF NOT EXISTS idx_product_fts ON products USING GIN(fts_doc);
