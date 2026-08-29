-- Seed default admin user (password: Admin@123)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, enabled, email_verified, created_at, updated_at, deleted)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin@clothingai.com',
        '$2a$12$LQv3c1yqBwEHxv6jO8q5p.Uh1HfGfQr8OKAYx4GqmM2lzNMWkP4Lq',
        'Admin', 'User', 'ADMIN', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- Seed categories
INSERT INTO categories (id, name, slug, description, display_order, active, created_at, updated_at, deleted) VALUES
('11111111-1111-1111-1111-111111111101', 'Women', 'women', 'Women clothing and apparel', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('11111111-1111-1111-1111-111111111102', 'Men', 'men', 'Men clothing and apparel', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('11111111-1111-1111-1111-111111111103', 'Kids', 'kids', 'Kids clothing', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('11111111-1111-1111-1111-111111111104', 'Accessories', 'accessories', 'Fashion accessories', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('11111111-1111-1111-1111-111111111105', 'Shoes', 'shoes', 'Footwear', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- Seed brands
INSERT INTO brands (id, name, slug, description, active, created_at, updated_at, deleted) VALUES
('22222222-2222-2222-2222-222222222201', 'Atelier Noir', 'atelier-noir', 'Premium minimalist fashion', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('22222222-2222-2222-2222-222222222202', 'Urban Thread', 'urban-thread', 'Streetwear essentials', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('22222222-2222-2222-2222-222222222203', 'Coastal Linen', 'coastal-linen', 'Sustainable linen and cotton', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- Seed sample products
INSERT INTO products (id, name, slug, sku, description, short_description, category_id, brand_id, price, currency, main_image_url, status, active, is_featured, is_new_arrival, created_at, updated_at, deleted) VALUES
('33333333-3333-3333-3333-333333333301', 'Classic Crewneck Tee', 'classic-crewneck-tee-001', 'CL-TEE-001', 'A timeless crewneck t-shirt made from 100% organic cotton. Soft, breathable, and perfect for everyday wear. Pre-shrunk for a consistent fit wash after wash.', 'Soft organic cotton tee for everyday wear', '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222201', 29.99, 'USD', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800', 'ACTIVE', TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('33333333-3333-3333-3333-333333333302', 'Slim Fit Chinos', 'slim-fit-chinos-002', 'CL-CHN-002', 'Tailored slim-fit chinos crafted from stretch cotton twill. A versatile staple that transitions seamlessly from office to weekend.', 'Tailored stretch cotton chinos', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222202', 59.99, 'USD', 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800', 'ACTIVE', TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('33333333-3333-3333-3333-333333333303', 'Linen Summer Dress', 'linen-summer-dress-003', 'CL-DRS-003', 'Flowy midi dress in 100% European linen with a flattering A-line silhouette. Breathable and elegant for warm-weather days.', 'Breathable European linen midi dress', '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222203', 89.99, 'USD', 'https://images.unsplash.com/photo-1572804013427-4d7ca7268217?w=800', 'ACTIVE', TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('33333333-3333-3333-3333-333333333304', 'Merino Wool Sweater', 'merino-wool-sweater-004', 'CL-SWT-004', 'Luxuriously soft merino wool sweater with ribbed cuffs and hem. Temperature-regulating and naturally odor-resistant.', 'Soft merino wool ribbed sweater', '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222201', 119.99, 'USD', 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800', 'ACTIVE', TRUE, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

-- Seed tags
INSERT INTO product_tags (product_id, tag) VALUES
('33333333-3333-3333-3333-333333333301', 'cotton'),('33333333-3333-3333-3333-333333333301', 'casual'),('33333333-3333-3333-3333-333333333301', 'basics'),
('33333333-3333-3333-3333-333333333302', 'chino'),('33333333-3333-3333-3333-333333333302', 'slim-fit'),('33333333-3333-3333-3333-333333333302', 'workwear'),
('33333333-3333-3333-3333-333333333303', 'linen'),('33333333-3333-3333-3333-333333333303', 'summer'),('33333333-3333-3333-3333-333333333303', 'midi'),
('33333333-3333-3333-3333-333333333304', 'wool'),('33333333-3333-3333-3333-333333333304', 'winter'),('33333333-3333-3333-3333-333333333304', 'sweater');

-- Seed variants
INSERT INTO product_variants (id, product_id, sku, size, color, color_hex, material, price, stock_quantity, low_stock_threshold, active, created_at, updated_at, deleted) VALUES
('44444444-4444-4444-4444-444444444401', '33333333-3333-3333-3333-333333333301', 'CL-TEE-001-S-BLK', 'S', 'Black', '#000000', 'Organic Cotton', 29.99, 50, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444402', '33333333-3333-3333-3333-333333333301', 'CL-TEE-001-M-BLK', 'M', 'Black', '#000000', 'Organic Cotton', 29.99, 80, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444403', '33333333-3333-3333-3333-333333333301', 'CL-TEE-001-L-BLK', 'L', 'Black', '#000000', 'Organic Cotton', 29.99, 60, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444404', '33333333-3333-3333-3333-333333333301', 'CL-TEE-001-S-WHT', 'S', 'White', '#FFFFFF', 'Organic Cotton', 29.99, 40, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444405', '33333333-3333-3333-3333-333333333302', 'CL-CHN-002-32', '32', 'Khaki', '#C3B091', 'Stretch Cotton', 59.99, 30, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444406', '33333333-3333-3333-3333-333333333302', 'CL-CHN-002-34', '34', 'Khaki', '#C3B091', 'Stretch Cotton', 59.99, 25, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444407', '33333333-3333-3333-3333-333333333303', 'CL-DRS-003-S-BEI', 'S', 'Beige', '#E8DCC4', 'Linen', 89.99, 20, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444408', '33333333-3333-3333-3333-333333333303', 'CL-DRS-003-M-BEI', 'M', 'Beige', '#E8DCC4', 'Linen', 89.99, 18, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444409', '33333333-3333-3333-3333-333333333304', 'CL-SWT-004-M-CRM', 'M', 'Cream', '#FFFDD0', 'Merino Wool', 119.99, 15, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
('44444444-4444-4444-4444-444444444410', '33333333-3333-3333-3333-333333333304', 'CL-SWT-004-L-CRM', 'L', 'Cream', '#FFFDD0', 'Merino Wool', 119.99, 12, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);
