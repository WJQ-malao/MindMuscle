# 基于 SpringBoot 融合 RAG 与 AI Agent 的个性化健身智能可视化系统

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4 + Spring AI 1.0 + MyBatis-Plus + MySQL 8 |
| 大模型 | DeepSeek（对话，官方 API）+ 硅基流动 BAAI/bge-m3（Embedding） |
| 向量库 | Qdrant 1.13（默认，单容器轻量部署）/ Milvus 2.5（可选，`milvus` profile 切回） |
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts |

## 架构

```
浏览器 (Vue3 + ECharts)
   │  HTTP / SSE
SpringBoot 应用 (单进程)
   ├── 业务模块：用户 / 体测 / 训练计划 / 打卡
   ├── RAG 模块：Tika 解析 → 切片 → bge-m3 向量化 → 向量库（Qdrant 默认 / Milvus 可选）
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

本机已有 MySQL（推荐）：在 Navicat 中执行 `发版/2026-11-17/20261117000001_init_fitness_db.sql`（RBAC 升级后还需执行 `20261117180001_rbac_upgrade.sql`），
并在 `application-local.yml` 中把数据库密码改为本机 root 密码。

### 3. 启动基础设施（Qdrant 向量库，单容器）

```bash
# 3.1 启动 Qdrant（首次拉镜像约几百 MB，已配置国内镜像加速）
docker compose up -d

# 3.2 等 Qdrant 就绪（看到 healthy 即可）
docker compose ps
# Web 控制台（可选）：http://localhost:6333/dashboard
```

> 向量库默认为 **Qdrant**（单容器，内存 <1G）。如需切回 Milvus 三件套（etcd + Milvus + MinIO），
> 用 `git show HEAD:docker-compose.yml` 找回原编排，并将后端以 `local,milvus` profile 启动（见步骤 5）。

### 4. 配置密钥

编辑 `backend/src/main/resources/application-local.yml`，填入 DeepSeek 的 key 和本机 MySQL 密码（硅基流动的 key 已预填）。
该文件已被 `.gitignore` 排除，不要提交。

### 5. 启动后端

```bash
cd backend
# 标准模式（Qdrant 向量库，需要步骤3的基础设施已启动）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 切回 Milvus（需先启动 Milvus 三件套，见步骤3注释）
mvn spring-boot:run -Dspring-boot.run.profiles=local,milvus

# 无 Docker 的降级模式（本地 SimpleVectorStore，功能可跑，检索质量弱，答辩需切回）
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
│       ├── config/           # ChatClient / Embedding / JWT / CORS / 向量库降级 配置
│       ├── controller/       # REST 接口（含 SSE 流式对话）
│       ├── service/          # 业务 + RAG 知识库服务
│       ├── security/         # RBAC: @RequireRole注解 + AOP鉴权切面
│       ├── tools/            # AI Agent 工具组（@Tool）
│       ├── entity/mapper/    # MyBatis-Plus
│       └── dto/              # 请求参数校验
├── frontend/                 # Vue3 前端（PC + 移动端双端页面）
├── 发版/2026-11-17/           # 发版 SQL 脚本（按日期分目录，时间戳前缀命名）
├── docs/                      # 开发过程文档（日期-标题.md）
└── docker-compose.yml         # Qdrant 一键启动（默认向量库；Milvus 编排见 git 历史）
```

## 环境变量（可选覆盖）

| 变量 | 默认 | 说明 |
|---|---|---|
| `DEEPSEEK_API_KEY` | - | DeepSeek 对话 key |
| `SILICONFLOW_API_KEY` | - | 硅基流动 Embedding key |
| `CHAT_MODEL` | deepseek-chat | 对话模型 |
| `MYSQL_HOST/PASSWORD` | localhost/root | 数据库连接 |
| `VECTOR_STORE_TYPE` | qdrant | 向量库类型：qdrant（默认）/ milvus（需 milvus profile） |
| `QDRANT_HOST/PORT` | localhost/6334 | Qdrant 连接 |
| `MILVUS_HOST/PORT` | localhost/19530 | Milvus 连接（仅 milvus profile） |
