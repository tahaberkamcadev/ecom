-- Bulk seed for inventorydb.products
-- Does NOT publish Kafka/outbox events. For CQRS read model, use POST /api/products instead.

BEGIN;

INSERT INTO products (id, version, category, name, brand, description, stock, price, active)
VALUES
    (gen_random_uuid(), 0, 'ELECTRONICS', 'Wireless Noise-Cancelling Headphones', 'SoundMax', 'Over-ear Bluetooth headphones with 30-hour battery life.', 25, 149.99, true),
    (gen_random_uuid(), 0, 'ELECTRONICS', '4K Ultra HD Smart TV 55"', 'ViewPro', '55-inch 4K smart TV with HDR10 and built-in streaming apps.', 12, 599.00, true),
    (gen_random_uuid(), 0, 'ELECTRONICS', 'Mechanical Gaming Keyboard', 'KeyForge', 'Hot-swappable RGB mechanical keyboard with tactile switches.', 40, 89.99, true),
    (gen_random_uuid(), 0, 'ELECTRONICS', 'USB-C Laptop Charger 65W', 'ChargeHub', 'Compact GaN charger compatible with most USB-C laptops.', 60, 39.99, true),
    (gen_random_uuid(), 0, 'CLOTHING', 'Organic Cotton T-Shirt', 'UrbanWear', 'Unisex crew-neck tee made from 100% organic cotton.', 100, 24.99, true),
    (gen_random_uuid(), 0, 'CLOTHING', 'Slim Fit Denim Jeans', 'DenimCo', 'Stretch denim jeans with classic five-pocket styling.', 45, 59.99, true),
    (gen_random_uuid(), 0, 'CLOTHING', 'Waterproof Rain Jacket', 'TrailGuard', 'Lightweight shell jacket with sealed seams for rainy days.', 30, 79.99, true),
    (gen_random_uuid(), 0, 'HOME', 'Ceramic Coffee Mug Set (4)', 'HomeNest', 'Set of four 350ml matte ceramic mugs, dishwasher safe.', 55, 29.99, true),
    (gen_random_uuid(), 0, 'HOME', 'Memory Foam Pillow', 'SleepWell', 'Ergonomic pillow with cooling gel layer for neck support.', 35, 44.99, true),
    (gen_random_uuid(), 0, 'HOME', 'LED Desk Lamp', 'BrightDesk', 'Adjustable brightness and color temperature USB desk lamp.', 28, 34.99, true),
    (gen_random_uuid(), 0, 'BOOKS', 'Clean Code', 'Prentice Hall', 'A handbook of agile software craftsmanship by Robert C. Martin.', 20, 42.50, true),
    (gen_random_uuid(), 0, 'BOOKS', 'Designing Data-Intensive Applications', 'O''Reilly', 'Principles for reliable, scalable distributed systems.', 15, 54.99, true),
    (gen_random_uuid(), 0, 'TOYS', 'Building Blocks Set 500pcs', 'BlockWorld', 'Creative construction set for ages 6 and up.', 50, 39.99, true),
    (gen_random_uuid(), 0, 'BEAUTY', 'Hydrating Face Moisturizer', 'GlowLab', 'Daily moisturizer with hyaluronic acid and SPF 15.', 70, 19.99, true),
    (gen_random_uuid(), 0, 'SPORTS', 'Yoga Mat 6mm', 'FlexFit', 'Non-slip TPE yoga mat with carrying strap.', 40, 27.99, true),
    (gen_random_uuid(), 0, 'SPORTS', 'Stainless Steel Water Bottle 1L', 'HydroGo', 'Insulated bottle keeps drinks cold for 24 hours.', 65, 22.99, true),
    (gen_random_uuid(), 0, 'OUTDOORS', 'Camping Tent 2-Person', 'WildPeak', 'Lightweight dome tent with waterproof rainfly.', 10, 129.99, true),
    (gen_random_uuid(), 0, 'AUTOMOTIVE', 'Dash Cam Full HD', 'RoadEye', '1080p front camera with loop recording and G-sensor.', 18, 69.99, true),
    (gen_random_uuid(), 0, 'FOOD', 'Premium Dark Roast Coffee 1kg', 'BeanCraft', 'Single-origin Arabica beans, medium-dark roast.', 80, 18.99, true),
    (gen_random_uuid(), 0, 'FOOD', 'Extra Virgin Olive Oil 750ml', 'Mediterra', 'Cold-pressed olive oil from Mediterranean groves.', 45, 14.99, true);

COMMIT;

SELECT category, count(*) AS product_count
FROM products
GROUP BY category
ORDER BY category;
