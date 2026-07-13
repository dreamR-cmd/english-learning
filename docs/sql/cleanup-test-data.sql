SET NAMES utf8mb4;

DELETE FROM shop_orders
WHERE UPPER(order_no) LIKE '%TEST%'
   OR UPPER(order_no) LIKE '%STRESS%'
   OR UPPER(order_no) LIKE '%IDEMPOTENCY%'
   OR UPPER(order_no) LIKE '%REDISSON%'
   OR UPPER(order_no) LIKE '%SERVER_TOKEN%'
   OR UPPER(product_name) LIKE '%TEST%'
   OR UPPER(product_name) LIKE '%STRESS%'
   OR UPPER(product_name) LIKE '%IDEMPOTENCY%'
   OR UPPER(product_name) LIKE '%REDISSON%'
   OR UPPER(product_name) LIKE '%SERVER_TOKEN%';

DELETE FROM shop_products
WHERE UPPER(title) LIKE '%TEST%'
   OR UPPER(title) LIKE '%STRESS%'
   OR UPPER(title) LIKE '%IDEMPOTENCY%'
   OR UPPER(title) LIKE '%REDISSON%'
   OR UPPER(title) LIKE '%SERVER_TOKEN%';

DELETE FROM users
WHERE UPPER(username) LIKE '%TEST%'
   OR UPPER(username) LIKE '%STRESS%'
   OR UPPER(username) LIKE '%IDEMPOTENCY%'
   OR UPPER(username) LIKE '%REDISSON%'
   OR UPPER(username) LIKE '%SERVER_TOKEN%'
   OR UPPER(username) LIKE 'TOKEN_USER_%'
   OR UPPER(username) LIKE 'CLAUDE_SMOKE_%';
