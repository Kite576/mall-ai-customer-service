# 电商购物平台（含智能客服）

一个前后端分离的电商购物平台演示项目。前端使用 Vue 3 + Vite，后端使用 Java Spring Boot，数据库默认使用 H2 内存库，并内置商品、订单、物流和智能客服演示数据。

项目覆盖完整购物流程：商品浏览、分类搜索、加入购物车、提交订单、模拟支付、订单管理、物流查询，以及“小智”智能客服咨询。

## 功能特性

- 商品中心：商品列表、分类筛选、关键词搜索、商品规格与库存展示
- 购物车：添加商品、修改数量、删除商品、实时计算合计金额
- 订单管理：购物车下单、订单列表、模拟支付、取消订单
- 物流追踪：基于演示订单返回物流节点信息
- 智能客服：支持商品咨询、订单/物流引导、售后政策问答
- 接口文档：集成 SpringDoc OpenAPI，可查看 Swagger UI
- 演示数据：启动后自动初始化热门商品和演示订单

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、原生 Fetch API |
| 后端 | Java 17+、Spring Boot 3.3、Spring Web、Spring Data JPA、WebFlux |
| 数据库 | H2 内存数据库，兼容 MySQL 模式 |
| 接口文档 | SpringDoc OpenAPI / Swagger UI |
| 智能客服 | 后端统一封装对话接口，可接入 DeepSeek 兼容 Chat Completions API |
| 构建工具 | Maven、npm |

## 项目结构

```text
.
├── backend
│   ├── pom.xml
│   └── src/main
│       ├── java/com/example/mall
│       │   ├── MallAiServiceApplication.java
│       │   ├── cart
│       │   ├── chat
│       │   ├── common
│       │   ├── config
│       │   ├── order
│       │   └── product
│       └── resources
│           ├── application.yml
│           └── data.sql
├── frontend
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src
│       ├── App.vue
│       ├── api/mall.js
│       ├── assets/styles.css
│       ├── components/ChatWidget.vue
│       └── main.js
└── README.md
```

## 程序入口

- 前端入口：`frontend/index.html` → `frontend/src/main.js` → `frontend/src/App.vue`
- 后端入口：`backend/src/main/java/com/example/mall/MallAiServiceApplication.java`

## 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本
- Node.js 18 或更高版本
- npm

如果本机没有全局 Maven，也可以使用项目内临时 Maven：`.tools/apache-maven-3.9.9/bin/mvn.cmd`。

## 快速启动

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

如果使用项目内 Maven：

```bash
cd backend
..\.tools\apache-maven-3.9.9\bin\mvn.cmd -DskipTests package
java "-Dfile.encoding=UTF-8" -jar target\mall-ai-service-0.0.1-SNAPSHOT.jar
```

后端默认地址：

```text
http://localhost:8080
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

## 访问地址

- 前端页面：`http://localhost:5173/`
- 后端接口：`http://localhost:8080`
- Swagger 文档：`http://localhost:8080/swagger-ui/index.html`
- H2 控制台：`http://localhost:8080/h2-console`

H2 连接信息：

```text
JDBC URL: jdbc:h2:mem:mall
Username: sa
Password: 留空
```

## 智能客服配置

后端提供统一智能客服接口，前端不会直接接触第三方接口密钥。

未配置密钥时，系统会使用本地演示回复，可回答热门商品、售后政策、订单号引导等基础问题。

如需接入真实接口，请在启动后端前配置环境变量：

```bash
AI_API_KEY=你的密钥
AI_API_URL=https://api.deepseek.com/chat/completions
AI_MODEL=deepseek-chat
```

Windows PowerShell 示例：

```powershell
$env:AI_API_KEY="你的密钥"
$env:AI_API_URL="https://api.deepseek.com/chat/completions"
$env:AI_MODEL="deepseek-chat"
```

## 演示数据

项目启动后会通过 `backend/src/main/resources/data.sql` 初始化数据：

| 类型 | 数据 |
| --- | --- |
| 商品 | 无线蓝牙耳机 Pro、便携充电宝 20000mAh、智能手表 S3 |
| 演示订单 | `MO202605290001` |
| 演示用户 | 固定 `userId=1` |
| 数据库 | H2 内存库，每次重启重新初始化 |

## 核心 API

### 商品模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/products` | 查询商品列表，支持 `keyword`、`categoryId` |
| GET | `/api/products/{id}` | 查询商品详情 |
| GET | `/api/categories` | 查询商品分类 |

### 购物车模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/cart` | 查询购物车 |
| POST | `/api/cart/items` | 添加商品到购物车 |
| PUT | `/api/cart/items/{cartItemId}` | 更新购物车商品数量 |
| DELETE | `/api/cart/items/{cartItemId}` | 删除购物车商品 |
| DELETE | `/api/cart` | 清空购物车 |

### 订单与物流模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/orders` | 创建订单 |
| GET | `/api/orders` | 查询订单列表 |
| GET | `/api/orders/{orderId}` | 查询订单详情 |
| PUT | `/api/orders/{orderId}/pay` | 模拟支付 |
| PUT | `/api/orders/{orderId}/cancel` | 取消订单 |
| GET | `/api/orders/{orderId}/tracking` | 查询物流轨迹 |

### 智能客服模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/chat/send` | 发送消息并返回完整回复 |
| POST | `/api/chat/stream` | SSE 流式回复接口 |
| DELETE | `/api/chat/session/{sessionId}` | 清空会话上下文 |

## 常用测试命令

前端构建：

```bash
cd frontend
npm run build
```

后端打包：

```bash
cd backend
mvn -DskipTests package
```

接口检查：

```bash
curl http://localhost:8080/api/products
```

## 注意事项

- 当前项目为演示版本，用户身份固定为 `userId=1`。
- 支付、发货和物流均为模拟流程，不包含真实支付能力。
- H2 为内存数据库，重启后数据会重新初始化。
- 请勿将真实接口密钥写入代码或提交到仓库，建议使用环境变量配置。
