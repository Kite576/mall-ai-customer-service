# 电商平台智能客服

一个移动端电商平台演示项目，包含商品浏览、商品详情、购物车、订单、登录注册和 DeepSeek 智能客服。前端以手机壳形式呈现，内容在手机屏幕内部滚动；后端提供商品、购物车、订单、用户和 AI 客服接口。

## 功能特性

- 移动端首页：搜索栏、功能入口、活动卡片、频道区和双列商品瀑布流。
- 商品详情：商品主图、价格、规格服务、商品亮点、加入购物车和立即购买。
- 购物车：加入商品、数量调整、删除、清空、合计金额和提交订单。
- 订单中心：待付款、待收货、退换售后、全部订单。
- 订单操作：支付、取消订单、查看物流、确认收货、申请售后入口。
- 用户模块：注册、登录、退出登录，本地保持当前用户状态。
- AI 客服：支持 DeepSeek 对话，能根据用户需求调用后端工具检索商品、订单、物流、售后政策和国补商品。
- 会话保持：切换页面时客服组件不销毁，对话不会丢失。
- 安全配置：API Key 通过环境变量配置，前端不暴露密钥，仓库只提供 `.env.example` 模板。

## 技术栈

前端：

- Vue 3
- Vite
- 原生 CSS
- lucide-vue-next
- Fetch API / SSE 流式读取

后端：

- Java 17+
- Spring Boot 3
- Spring Web / WebFlux
- Spring Data JPA
- H2 数据库，可扩展 MySQL

AI：

- DeepSeek Chat Completions API
- Tool Calls：由模型决定是否调用后端商品、订单、售后等工具

## 项目结构

```text
.
├── backend
│   ├── pom.xml
│   └── src/main
│       ├── java/com/example/mall
│       │   ├── cart        # 购物车
│       │   ├── chat        # AI 客服与 DeepSeek 调用
│       │   ├── common      # 通用响应与异常处理
│       │   ├── config      # 跨域配置
│       │   ├── order       # 订单与物流
│       │   ├── product     # 商品与分类
│       │   └── user        # 登录注册
│       └── resources
│           ├── application.yml
│           └── data.sql
├── frontend
│   ├── package.json
│   └── src
│       ├── api
│       ├── assets
│       ├── components
│       ├── App.vue
│       └── main.js
├── .env.example
├── .gitignore
└── README.md
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.8+，也可以使用项目内置 `.tools/apache-maven-3.9.9`
- Node.js 18+
- npm
- DeepSeek API Key

## 配置 AI Key

不要把真实 API Key 写进源码。推荐使用环境变量：

```powershell
$env:AI_API_KEY="请填写你的 DeepSeek API Key"
$env:AI_MODEL="deepseek-v4-flash"
$env:AI_API_URL="https://api.deepseek.com/chat/completions"
```

也可以复制 `.env.example` 作为本地模板，把里面的占位文字替换为自己的 Key。`.env` 已被 `.gitignore` 忽略，不应提交真实密钥。

后端默认配置在 `backend/src/main/resources/application.yml`：

```yml
mall:
  ai:
    api-url: ${AI_API_URL:https://api.deepseek.com/chat/completions}
    api-key: ${AI_API_KEY:}
    model: ${AI_MODEL:deepseek-v4-flash}
```

未配置 `AI_API_KEY` 时，客服会使用本地检索兜底回复；配置后会调用 DeepSeek，并通过工具查询真实项目数据。

## 本地运行

### 启动后端

如果本机安装了 Maven：

```bash
cd backend
mvn spring-boot:run
```

如果使用项目内置 Maven：

```powershell
cd backend
..\.tools\apache-maven-3.9.9\bin\mvn.cmd test
..\.tools\apache-maven-3.9.9\bin\mvn.cmd dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
$cp = "target/classes;" + (Get-Content target\classpath.txt)
java -cp $cp com.example.mall.MallAiServiceApplication
```

后端默认地址：

```text
http://localhost:8080
```

H2 控制台：

```text
http://localhost:8080/h2-console
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

## 主要接口

商品：

- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/categories`

用户：

- `POST /api/auth/register`
- `POST /api/auth/login`

购物车：

- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{cartItemId}`
- `DELETE /api/cart/items/{cartItemId}`
- `DELETE /api/cart`

订单：

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `PUT /api/orders/{orderId}/pay`
- `PUT /api/orders/{orderId}/cancel`
- `PUT /api/orders/{orderId}/confirm`
- `GET /api/orders/{orderId}/tracking`

AI 客服：

- `POST /api/chat/send`
- `POST /api/chat/stream`
- `DELETE /api/chat/session/{sessionId}`

## AI 客服实现

客服不是固定规则回复。后端为 DeepSeek 提供了工具：

- `search_products`：根据自然语言需求检索商品，支持预算、库存、品类和国补条件。
- `list_subsidy_products`：查询当前带国补/补贴/以旧换新标签的商品。
- `get_order_detail`：根据订单号查询订单、明细和物流。
- `get_after_sale_policy`：查询退换货和质保政策。

模型先判断是否需要调用工具，后端执行真实查询，再把结果交给模型生成客服回复。这样用户可以直接问：

- “预算 200 以内，运动用蓝牙耳机推荐哪个？”
- “现在有哪些商品有国补？”
- “帮我查订单 MO202605290001 的物流。”
- “这个订单怎么申请退货？”

## 数据说明

开发环境使用 H2 文件数据库：

```text
backend/data/mall.mv.db
```

该目录是本地运行数据，已加入 `.gitignore`，不会提交到仓库。初始化演示数据位于：

```text
backend/src/main/resources/data.sql
```

## 构建检查

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run build
```

## 注意事项

- 不要提交真实 DeepSeek API Key。
- 前端只调用本地后端接口，不直接请求 DeepSeek。
- 如果修改了后端 Java 代码，需要重启后端。
- 如果端口被占用，先停止旧的 `java` 或 `node` 进程。
