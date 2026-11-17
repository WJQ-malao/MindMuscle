# 基于 SpringBoot 融合 RAG 与 AI Agent 的个性化健身智能可视化系统

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4 + Spring AI 1.0 + MyBatis-Plus + MySQL 8 |
| 大模型 | DeepSeek（对话，官方 API）+ 硅基流动 BAAI/bge-m3（Embedding） |
| 向量库 | Milvus 2.5（IVF_FLAT / COSINE / 1024 维） |
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts |

## 架构

```
浏览器 (Vue3 + ECharts)
   │  HTTP / SSE
SpringBoot 应用 (单进程)
   ├── 业务模块：用户 / 体测 / 训练计划 / 打卡
   ├── RAG 模块：Tika 解析 → 切片 → bge-m3 向量化 → Milvus
   ├── Agent 模块：ChatClient + RetrievalAugmentationAdvisor + @Tool 工具组
   └── 可视化接口：统计聚合 → ECharts
```

AI 能力挂在一个 ChatClient 上：

- **RAG**：`RetrievalAugmentationAdvisor` 每次提问先检索 Milvus 知识库（top 5），检索结果注入提示词
- **AI Agent**：`FitnessTools` 中 6 个 `@Tool` 工具（查体测/算BMI/生成计划/查计划/打卡/查打卡），由大模型通过 ReAct 循环自主决策调用；userId 从 JWT 上下文获取，模型无法伪造，防越权
- **护栏**：BMI 计算、计划编排全部由 Java 确定性逻辑完成，模型只负责对话与工具选择

## 启动步骤

### 1. 准备环境

- JDK 21、Maven 3.9+、Node 18+、Docker Desktop
- 申请 API Key：[DeepSeek 开放平台](https://platform.deepseek.com/)（对话）、[硅基流动](https://cloud.siliconflow.cn)（Embedding）

### 2. 初始化数据库

本机已有 MySQL（推荐）：在 Navicat 中执行 `发版/2026-11-17/20261117000001_init_fitness_db.sql`，
并在 `application-local.yml` 中把数据库密码改为本机 root 密码。

### 3. 启动基础设施（Milvus）

```bash
docker compose up -d
```

首次启动 Milvus 需要 1-2 分钟初始化，等容器 healthy：

```bash
docker compose ps
```

### 4. 配置密钥

编辑 `backend/src/main/resources/application-local.yml`，填入 DeepSeek 的 key 和本机 MySQL 密码（硅基流动的 key 已预填）。
该文件已被 `.gitignore` 排除，不要提交。

### 5. 启动后端

```bash
cd backend
# 已装 Docker（Milvus 可用）：标准模式
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 未装 Docker：降级模式（本地 SimpleVectorStore 顶替 Milvus，功能可跑，检索质量弱，答辩需切回）
mvn spring-boot:run -Dspring-boot.run.profiles=local,simple-vector
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173 ，注册账号即可使用。

## 演示路径

1. 在「身体数据」页录入体重/体脂
2. 在「AI 教练」页说"帮我制定一个每周四练的增肌计划" → 观察工具调用，计划自动生成
3. 在「训练计划」页查看 AI 生成的计划并打卡
4. 在「数据看板」页查看趋势图、热力图、肌群分布
5. 在「知识库管理」页上传健身 PDF，回到 AI 教练提问专业知识 → 观察 RAG 检索增强回答

## 目录结构

```
bs/
├── backend/                  # SpringBoot 后端
│   └── src/main/java/com/fitness/ai/
│       ├── config/           # ChatClient / Embedding / JWT / CORS 配置
│       ├── controller/       # REST 接口（含 SSE 流式对话）
│       ├── service/          # 业务 + RAG 知识库服务
│       ├── tools/            # AI Agent 工具组（@Tool）
│       ├── entity/mapper/    # MyBatis-Plus
│       └── dto/              # 请求参数校验
├── frontend/                 # Vue3 前端
├── 发版/2026-11-17/           # 发版 SQL 脚本（按日期分目录，时间戳前缀命名）
├── docs/                      # 开发过程文档（日期-标题.md）
└── docker-compose.yml         # Milvus + MySQL 一键启动
```

## 环境变量（可选覆盖）

| 变量 | 默认 | 说明 |
|---|---|---|
| `DEEPSEEK_API_KEY` | - | DeepSeek 对话 key |
| `SILICONFLOW_API_KEY` | - | 硅基流动 Embedding key |
| `CHAT_MODEL` | deepseek-chat | 对话模型 |
| `MYSQL_HOST/PASSWORD` | localhost/root | 数据库连接 |
| `MILVUS_HOST/PORT` | localhost/19530 | Milvus 连接 |
