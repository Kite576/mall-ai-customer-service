INSERT INTO category (id, name) VALUES (1, '数码影音');
INSERT INTO category (id, name) VALUES (2, '智能穿戴');
INSERT INTO category (id, name) VALUES (3, '移动电源');

INSERT INTO product (id, name, price, stock, specs, image_url, category_id, status) VALUES
(1, '无线蓝牙耳机 Pro', 299.00, 500, '续航8小时，支持主动降噪，蓝牙5.3', 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=800&q=80', 1, 1),
(2, '便携充电宝 20000mAh', 159.00, 100, '支持65W快充，可上飞机，含Type-C充电线', 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?auto=format&fit=crop&w=800&q=80', 3, 1),
(3, '智能手表 S3', 899.00, 300, '支持血氧检测、GPS定位、心率监测、1.8寸AMOLED屏', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=800&q=80', 2, 1);

INSERT INTO orders (id, order_no, user_id, receiver_name, receiver_phone, receiver_address, total_amount, status, created_at, paid_at, shipped_at) VALUES
(1, 'MO202605290001', 1, '测试用户', '13800000000', '上海市浦东新区演示路100号', 299.00, 'SHIPPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (id, order_id, product_id, product_name, price, quantity) VALUES
(1, 1, 1, '无线蓝牙耳机 Pro', 299.00, 1);

INSERT INTO shipment_tracking (id, order_id, status, description, occur_time) VALUES
(1, 1, 'CREATED', '订单已创建，等待仓库处理', DATEADD('HOUR', -20, CURRENT_TIMESTAMP)),
(2, 1, 'PICKED', '包裹已完成拣货', DATEADD('HOUR', -16, CURRENT_TIMESTAMP)),
(3, 1, 'SHIPPED', '包裹已揽件，正在发往目的地', DATEADD('HOUR', -8, CURRENT_TIMESTAMP));
