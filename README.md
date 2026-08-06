# TypEnglish · 后端

Spring Boot 4 + Java 17 + MyBatis-Plus + Redis。

## 技术栈

Spring Boot 4 · Spring AI · MyBatis-Plus · MySQL · Redis · JJWT · Lombok

## 目录结构

```
src/main/java/com/typenglish/
├── controller/          # REST 接口（只做参数校验和路由）
├── service/             # 业务逻辑
├── mapper/              # MyBatis-Plus 数据访问
├── entity/              # 数据库实体
├── config/              # 线程池、CORS、Redis 配置
├── security/            # JWT 拦截器
└── common/              # Result、异常处理
```

## 核心模块

### AI 模块
通过 Spring AI 对接 DeepSeek V4 Flash。SSE 流式对话、分批生成句子（上限 200 句，每批 15 句，实时推送进度）。

### 练习模块
单词拼写、句子翻译、完形填空三种模式的提交和统计。

### 错题本
单词和句子错题收集、分页查询、批量练习、清空。

### 认证
JWT 无状态认证，拦截器从 ThreadLocal 取当前用户。

## 启动

```bash
export DEEPSEEK_API_KEY=sk-xxx
./mvnw spring-boot:run
```
