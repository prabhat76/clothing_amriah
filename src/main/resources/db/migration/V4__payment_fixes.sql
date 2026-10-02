-- V4: Payment table fixes
-- Add index on order_id for fast "latest payment for order" queries
-- (order_id intentionally has no UNIQUE constraint — retries create multiple rows)
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payments(order_id);

-- Ensure the payments method column supports all enum values
-- (PAYPAL was added in code but schema used a plain VARCHAR so no DDL change needed)
-- Backfill any NULL status rows left by interrupted transactions
UPDATE payments SET status = 'FAILED' WHERE status IS NULL;
