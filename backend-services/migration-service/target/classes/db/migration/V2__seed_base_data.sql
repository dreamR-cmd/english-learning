SET NAMES utf8mb4;

INSERT INTO admin_roles (code, name, description)
SELECT 'USER', '普通用户', '访问学习前台'
WHERE NOT EXISTS (SELECT 1 FROM admin_roles WHERE code = 'USER');

INSERT INTO admin_roles (code, name, description)
SELECT 'ADMIN', '系统管理员', '访问后台管理端'
WHERE NOT EXISTS (SELECT 1 FROM admin_roles WHERE code = 'ADMIN');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'ADMIN_DASHBOARD', '后台首页', '后台概览', '/admin', 1
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'ADMIN_DASHBOARD');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'ORDER_MANAGE', '订单管理', '查看和管理全部订单', '/admin/orders', 2
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'ORDER_MANAGE');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'MODULE_MANAGE', '模块管理', '管理考试模块、商城入口和精选读物入口', '/admin/modules', 3
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'MODULE_MANAGE');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'USER_MANAGE', '用户管理', '管理用户和分配角色', '/admin/users', 4
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'USER_MANAGE');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'ROLE_MANAGE', '角色管理', '创建角色并分配权限', '/admin/roles', 5
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'ROLE_MANAGE');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'PERMISSION_MANAGE', '权限管理', '查看后台权限菜单', '/admin/permissions', 6
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'PERMISSION_MANAGE');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'SWAGGER_DOCS', '接口文档', '打开 Swagger 接口测试页面', '/swagger-ui.html', 7
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'SWAGGER_DOCS');

INSERT INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code IN (
  'ADMIN_DASHBOARD',
  'ORDER_MANAGE',
  'MODULE_MANAGE',
  'USER_MANAGE',
  'ROLE_MANAGE',
  'PERMISSION_MANAGE',
  'SWAGGER_DOCS'
)
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'shop', '学习商城', '精选课程、真题资料与备考书籍，配合等级考试模块系统学习。', '🛒', -20, '/shop'
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'shop');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'selected-readings', '精选读物', '独立于阅读理解题库，整理适合英语学习者的分级读物和公版经典。', '📖', -10, '/selected-readings'
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'selected-readings');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'cet4', '大学英语四级', 'CET-4 大学英语四级考试，适合大学生基础英语水平', '📘', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'cet4');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'cet6', '大学英语六级', 'CET-6 大学英语六级考试，适合中高级英语水平', '📗', 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'cet6');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'toefl', '托福', 'TOEFL 托福考试，适合出国留学英语水平', '📕', 3, NULL
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'toefl');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'ielts', '雅思', 'IELTS 雅思考试，适合英联邦国家留学移民', '📙', 4, NULL
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'ielts');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'kaoyan', '考研英语', '全国硕士研究生入学统一考试英语', '📚', 5, NULL
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'kaoyan');

INSERT INTO exam_modules (code, name, description, icon, sort_order, route_path)
SELECT 'gre', 'GRE', 'GRE 美国研究生入学考试', '📓', 6, NULL
WHERE NOT EXISTS (SELECT 1 FROM exam_modules WHERE code = 'gre');

INSERT INTO shop_products (title, category, price, original_price, stock, active, tag, icon, tone, sort_order, description, points, created_at, updated_at)
SELECT 'CET4 精选课程', '大学英语四级', 199.00, 299.00, 120, b'1', '热卖课程', '🎓', 'tone-blue', 1,
       '覆盖核心词汇、阅读技巧、听力突破和写作模板，适合四级系统备考。',
       '30 节精讲课|四级高频词清单|模拟训练计划', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM shop_products WHERE title = 'CET4 精选课程');

INSERT INTO shop_products (title, category, price, original_price, stock, active, tag, icon, tone, sort_order, description, points, created_at, updated_at)
SELECT '考研英语精选课程', '考研英语', 399.00, 599.00, 80, b'1', '系统课', '📚', 'tone-green', 2,
       '面向考研英语一/二，强化长难句、阅读逻辑、翻译与作文提分路径。',
       '长难句专项|阅读题型拆解|作文素材库', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM shop_products WHERE title = '考研英语精选课程');

INSERT INTO shop_products (title, category, price, original_price, stock, active, tag, icon, tone, sort_order, description, points, created_at, updated_at)
SELECT '考研真题书', '真题资料', 89.00, 128.00, 300, b'1', '备考书籍', '📝', 'tone-orange', 3,
       '精选历年考研英语真题，按题型拆解解析，适合刷题和错题复盘。',
       '历年真题汇编|逐题解析|答案速查册', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM shop_products WHERE title = '考研真题书');

INSERT INTO shop_products (title, category, price, original_price, stock, active, tag, icon, tone, sort_order, description, points, created_at, updated_at)
SELECT 'CET4 高频词汇手册', '词汇资料', 49.00, 69.00, 500, b'1', '词汇精选', '🔖', 'tone-red', 4,
       '配合每日单词练习使用，按考试频率整理重点词、短语和例句。',
       '高频词分组|短语搭配|例句速记', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM shop_products WHERE title = 'CET4 高频词汇手册');

INSERT INTO selected_readings (title, source, type, level, suggested_for, description, url, content, sort_order)
SELECT 'The Gift of the Magi', 'O. Henry', 'Short Story', 'B1-B2', 'CET4/CET6',
       'A classic short story for practicing narrative reading and inference.',
       'https://www.gutenberg.org/ebooks/7256',
       'A public-domain classic suitable for extensive reading practice. Import the full text only when your deployment has verified copyright and source requirements.',
       1
WHERE NOT EXISTS (SELECT 1 FROM selected_readings WHERE title = 'The Gift of the Magi');

INSERT INTO selected_readings (title, source, type, level, suggested_for, description, url, content, sort_order)
SELECT 'The Happy Prince', 'Oscar Wilde', 'Short Story', 'B1-B2', 'CET4/CET6/IELTS',
       'A literary reading sample for vocabulary, theme, and detail comprehension.',
       'https://www.gutenberg.org/ebooks/902',
       'A public-domain classic suitable for extensive reading practice. Import the full text only when your deployment has verified copyright and source requirements.',
       2
WHERE NOT EXISTS (SELECT 1 FROM selected_readings WHERE title = 'The Happy Prince');
