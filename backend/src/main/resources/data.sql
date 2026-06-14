MERGE INTO category (id, name) KEY(id) VALUES (1, '数码影音');
MERGE INTO category (id, name) KEY(id) VALUES (2, '智能穿戴');
MERGE INTO category (id, name) KEY(id) VALUES (3, '移动电源');
MERGE INTO category (id, name) KEY(id) VALUES (4, '电脑办公');
MERGE INTO category (id, name) KEY(id) VALUES (5, '智能家居');
MERGE INTO category (id, name) KEY(id) VALUES (6, '手机配件');

MERGE INTO mall_user (id, username, phone, password_hash, salt, created_at) KEY(id) VALUES
(1, 'demo', '13800000000', '836ea4d28e6cfe4e3e4f6c069ec34958ab86d9bd5c1680b86bd70181b04edbde', 'demo-salt', CURRENT_TIMESTAMP);

MERGE INTO product (id, name, price, stock, specs, tags, image_url, category_id, status) KEY(id) VALUES
(1, '无线蓝牙耳机 Pro', 299.00, 500, '续航8小时，支持主动降噪，蓝牙5.3', '官方,满减,爆款', 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=800&q=80', 1, 1),
(2, '便携充电宝 20000mAh', 159.00, 100, '支持65W快充，可上飞机，含Type-C充电线', '官方,学生价,低价', 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?auto=format&fit=crop&w=800&q=80', 3, 1),
(3, '智能手表 S3', 899.00, 300, '支持血氧检测、GPS定位、心率监测、1.8寸AMOLED屏', '国补,健康,新品', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=800&q=80', 2, 1),
(4, '轻薄办公笔记本 Air 14', 4599.00, 80, '14英寸高色域屏，16GB内存，512GB固态硬盘', '国补贴,电脑办公,以旧换新', 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=800&q=80', 4, 1),
(5, '人体工学无线鼠标', 129.00, 260, '静音按键，三档DPI，多设备蓝牙连接', '办公,热卖', 'https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=800&q=80', 4, 1),
(6, '机械键盘 K87', 399.00, 180, '热插拔轴体，RGB背光，PBT键帽', '办公,游戏,热卖', 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=800&q=80', 4, 1),
(7, '智能音箱 Mini', 199.00, 220, '语音控制，蓝牙播放，智能家居联动', '智能家居,官方', 'https://images.unsplash.com/photo-1589003077984-894e133dabab?auto=format&fit=crop&w=800&q=80', 5, 1),
(8, '扫地机器人 X1', 1699.00, 90, '激光导航，自动回充，拖扫一体', '国补,智能家居,送装服务', 'https://images.unsplash.com/photo-1603618090561-412154b4bd1b?auto=format&fit=crop&w=800&q=80', 5, 1),
(9, '智能台灯 EyeCare', 239.00, 160, '无频闪照明，触控调光，定时休息提醒', '智能家居,护眼', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80', 5, 1),
(10, '磁吸快充数据线', 39.00, 800, 'Type-C接口，60W快充，1.2米耐弯折线材', '配件,9.9包邮', 'https://images.unsplash.com/photo-1619953942547-233eab5a70d6?auto=format&fit=crop&w=800&q=80', 6, 1),
(11, '手机防摔保护壳', 59.00, 620, '四角气囊防摔，磨砂手感，镜头全包保护', '配件,热卖', 'https://images.unsplash.com/photo-1603313011101-320f26a4f6f6?auto=format&fit=crop&w=800&q=80', 6, 1),
(12, '高清钢化膜两片装', 29.00, 900, '高清透亮，疏油防指纹，自动吸附', '配件,低价', 'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?auto=format&fit=crop&w=800&q=80', 6, 1),
(13, '运动蓝牙耳机 Lite', 189.00, 410, '耳挂式佩戴，IPX5防水，低延迟游戏模式', '运动,蓝牙,低价', 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=800&q=80', 1, 1),
(14, '桌面无线充电座', 119.00, 330, '15W无线快充，立式支架，兼容多机型', '配件,桌面', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=800&q=80', 3, 1),
(15, '智能手环 Fit 5', 249.00, 520, '睡眠监测，运动记录，14天长续航', '健康,智能穿戴', 'https://images.unsplash.com/photo-1576243345690-4e4b79b63288?auto=format&fit=crop&w=800&q=80', 2, 1),
(16, '4K高清运动相机', 1299.00, 120, '4K防抖拍摄，防水机身，广角镜头', '国补贴,运动,数码影音', 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80', 1, 1),
(17, '桌面显示器 27英寸', 1399.00, 75, '2K分辨率，低蓝光护眼，升降旋转支架', '国补贴,电脑办公,护眼', 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80', 4, 1),
(18, '智能门锁 C2', 899.00, 140, '指纹解锁，临时密码，异常报警提醒', '国补,智能家居,安全', 'https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=800&q=80', 5, 1);

MERGE INTO orders (id, order_no, user_id, receiver_name, receiver_phone, receiver_address, total_amount, status, created_at, paid_at, shipped_at) KEY(id) VALUES
(1, 'MO202605290001', 1, '测试用户', '13800000000', '上海市浦东新区演示路100号', 299.00, 'SHIPPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO order_item (id, order_id, product_id, product_name, price, quantity) KEY(id) VALUES
(1, 1, 1, '无线蓝牙耳机 Pro', 299.00, 1);

MERGE INTO shipment_tracking (id, order_id, status, description, occur_time) KEY(id) VALUES
(1, 1, 'CREATED', '订单已创建，等待仓库处理', DATEADD('HOUR', -20, CURRENT_TIMESTAMP)),
(2, 1, 'PICKED', '包裹已完成拣货', DATEADD('HOUR', -16, CURRENT_TIMESTAMP)),
(3, 1, 'SHIPPED', '包裹已揽件，正在发往目的地', DATEADD('HOUR', -8, CURRENT_TIMESTAMP));
