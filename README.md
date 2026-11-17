# DayStreamMusic

一个基于 Spring Boot 3 的在线音乐平台后端，涵盖音乐播放、社区互动、VIP 订阅与秒杀、AI 音乐助手等完整业务链路。

![Java](https://img.shields.io/badge/Java-17-orange)



![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)



![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)

![Redis](https://img.shields.io/badge/Redis-7.x-red)

![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-ff69b4)

![MinIO](https://img.shields.io/badge/MinIO-S3%20Compatible-c72e49)

![License](https://img.shields.io/badge/License-MIT-lightgrey)



---

## 功能特性

- **音乐核心**：歌曲 / MV 上传与播放、专辑、歌单、排行榜、音乐人主页，基于 MinIO 的分片上传与大文件断点续传
- **音频处理**：用 ffmpeg / javacv 做音频转码，jaudiotagger 解析 ID3 元数据（标题、歌手、封面），thumbnailator 压缩封面图
- **用户体系**：邮箱注册登录、图形验证码、JWT 鉴权、QQ 第三方登录与账号绑定、找回密码
- **社区互动**：发帖、评论、点赞、关注、消息通知，WebSocket 实时私聊
- **交易与并发**：VIP 订阅、订单管理、优惠促销、**秒杀抢购**（Redis + Lua 原子预扣减 → RabbitMQ 异步落单 → 支付回调对账）
- **AI 能力**：基于 langchain4j 的流式对话（SSE），支持 RAG 知识库检索、AI 音乐创作、歌曲标题生成
- **运营后台**：内容审核、音乐人认证审核、社区圈子管理、数据报表

---

## 技术栈

| 分类      | 选型                                                  |
| ------- | --------------------------------------------------- |
| 基础框架    | Spring Boot 3.5.7、Java 17、Maven 多模块                 |
| 持久层     | MyBatis-Plus 3.5.6、MySQL 8、28 个 Mapper XML          |
| 缓存 / 并发 | Redis、Redisson 分布式锁、Lua 脚本原子操作                      |
| 消息队列    | RabbitMQ（订单异步、秒杀削峰、签到、通知）                           |
| 对象存储    | MinIO 8.5（S3 协议，分片上传）                               |
| 安全      | Spring Security、JJWT 0.11.5                         |
| AI      | langchain4j、阿里云百炼（qwen / text-embedding-v4）、WebFlux |
| 音频 / 图像 | ffmpeg-platform、javacv、jaudiotagger、Thumbnailator   |
| 实时通信    | Spring WebSocket                                    |

---

## 模块结构

项目是 **模块化单体**：8 个 Maven 模块共享同一个 Spring Boot 进程，唯一启动类为  
`dsm-regular-functions` 下的 `com.future.music.DemoApplication`，各模块通过 `@ComponentScan` 统一装配。

| 模块                      | 职责                                                             |
| ----------------------- | -------------------------------------------------------------- |
| `dsm-common`            | 公共能力：实体基类、`JwtUtil`、MinIO 客户端、常量、全局异常、分布式锁 Lua 脚本              |
| `dsm-regular-functions` | 核心业务：音乐 / MV / 歌单 / 排行 / 用户主页 / 签到任务 / 小游戏 / 分片上传。**启动入口在此模块** |
| `dsm-user-security`     | 注册登录、JWT 过滤链、邮箱验证码、QQ 互联 OAuth                                 |
| `dsm-social-community`  | 社区帖子、评论、点赞、关注、通知、WebSocket 聊天（`/ws/chat`）                      |
| `dsm-trade-concurrent`  | VIP 订阅、订单、秒杀、优惠促销、支付状态回调                                       |
| `dsm-data-ai`           | AI 助手会话、流式对话、RAG 检索、AI 音乐创作                                    |
| `dsm-music-player`      | 播放器端接口：专辑、音乐人申请                                                |
| `dsm-music-admin`       | 运营后台：用户 / 评论 / 社区 / 音乐人审核、运营管理、报表                              |

```
Music/
├── dsm-common/                  # 公共组件
├── dsm-regular-functions/       # 核心业务 + 启动类
├── dsm-user-security/           # 认证授权
├── dsm-social-community/        # 社区与 IM
├── dsm-trade-concurrent/        # 交易与高并发
├── dsm-data-ai/                 # AI 能力
├── dsm-music-player/            # 播放器接口
├── dsm-music-admin/             # 后台管理
├── application-example.yaml     # 配置变量模板
└── pom.xml
```

---

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0、Redis、RabbitMQ、MinIO

### 2. 准备中间件

```bash
# MySQL：创建数据库
mysql -uroot -p -e "CREATE DATABASE music DEFAULT CHARACTER SET utf8mb4;"

# RabbitMQ：创建虚拟主机（也可以直接用 /）
rabbitmqctl add_vhost DayStreamMusic

# MinIO：创建存储桶（默认桶名 file）
mc alias set local http://localhost:9000 <accessKey> <secretKey>
mc mb local/file
```

> 建表脚本需自行导入。项目未内置 SQL，可从 `*Mapper.xml` 中的字段反推表结构。

### 3. 配置

所有敏感配置均以环境变量占位，模板见根目录 [`application-example.yaml`](./application-example.yaml)。

本地开发推荐**本地覆盖文件**方式（该文件已被 `.gitignore` 忽略）：

```bash
cp application-example.yaml dsm-regular-functions/src/main/resources/application-local.yaml
# 编辑 application-local.yaml，填入真实的数据库 / Redis / MinIO / MQ / AI Key
```

或直接设置环境变量（IDEA 中配置 Run Configuration → Environment variables）：

```bash
export DB_HOST=localhost DB_USERNAME=root DB_PASSWORD=your_password
export REDIS_HOST=localhost REDIS_PASSWORD=
export MQ_HOST=localhost MQ_USERNAME=guest MQ_PASSWORD=guest
export MINIO_ENDPOINT=http://localhost:9000 MINIO_ACCESS_KEY=minioadmin MINIO_SECRET_KEY=minioadmin
export AI_API_KEY=sk-xxxx
export JWT_SECRET=$(openssl rand -base64 32)
```

**JWT 密钥**：不配置时会自动生成临时密钥并打印警告，服务能正常启动，但重启后所有 token 失效。生产环境务必配置固定值。

### 4. 启动

```bash
# 方式一：Maven 运行
mvn clean install -DskipTests
mvn -pl dsm-regular-functions spring-boot:run -Dspring-boot.run.profiles=local

# 方式二：打包运行
mvn clean package -DskipTests
java -jar dsm-regular-functions/target/dsm-regular-functions-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=local
```

默认端口 `8080`。

### 5. 验证

```bash
curl http://localhost:8080/login/getCaptcha
```

---

## 接口概览

| 前缀                                                                            | 模块                | 说明                    |
| ----------------------------------------------------------------------------- | ----------------- | --------------------- |
| `/login/**`                                                                   | user-security     | 登录、图形验证码、QQ 授权与回调     |
| `/rejester/**`                                                                | user-security     | 注册                    |
| `/api/upload/**`                                                              | regular-functions | 分片上传                  |
| `/music/**` `/mv/**` `/playlist/**` `/rank/**`                                | regular-functions | 音乐、MV、歌单、排行           |
| `/user/**` `/user/home/**` `/user/task/**`                                    | regular-functions | 用户主页、签到任务             |
| `/musician/**` `/musician/home/**`                                            | regular-functions | 音乐人主页                 |
| `/game/**`                                                                    | regular-functions | 听歌小游戏                 |
| `/ai/**`                                                                      | data-ai           | 会话列表、SSE 流式对话、AI 音乐创作 |
| `/community/**` `/comment/**` `/userbase/**`                                  | social-community  | 帖子、评论、用户关系            |
| `/api/notification/**` `/api/chat/**`                                         | social-community  | 通知、聊天记录               |
| `/ws/chat`                                                                    | social-community  | WebSocket 实时聊天        |
| `/vip/core/**` `/vip/order/**` `/vip/promotion/**` `/vip/analytics/**`        | trade-concurrent  | VIP 权益、订单、促销、统计       |
| `/basicOrder/**` `/seckill/**`                                                | trade-concurrent  | 普通订单、秒杀               |
| `/api/album/**` `/applyMusician`                                              | music-player      | 专辑、音乐人申请              |
| `/admin/**` `/adminUser/**` `/adminMusician/**` `/operations/**` `/report/**` | music-admin       | 后台审核、运营、报表            |

---

## 设计要点

**秒杀链路**：请求先经 Redis + Lua（`seckill.lua`）原子校验库存并预扣减，成功后才投递 RabbitMQ 消息，  
由消费者异步创建订单。这样把「判断库存」和「写库」拆开，抗住瞬时流量，避免超卖。

**分布式锁**：基于 Redisson，释放锁使用 `unlock.lua` 保证「判断持有者 + 删除」的原子性。

**AI 流式输出**：`/ai/chat` 与 `/ai/music/create` 返回 `text/event-stream`，配合 langchain4j-reactor 逐字下发。

**鉴权**：登录签发 JWT，`JwtFilter` 在过滤链中解析并注入用户上下文，WebSocket 握手时通过 query 参数传递 token。

---
