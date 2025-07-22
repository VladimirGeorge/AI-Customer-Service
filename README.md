## 一、技术设计思路

### 1. 整体架构设计
系统采用分层架构设计，基于Spring Boot框架构建，核心分为表现层、业务层、数据访问层及外部服务集成层，各层职责清晰、低耦合。
- **表现层（Controller）**：通过RESTful API对外提供服务，包括认证接口（`AuthController`）、问答接口（`ChatController`）等，处理HTTP请求与响应。
- **业务层（Service）**：封装核心业务逻辑，如用户认证（`AuthService`）、大模型调用（`ModelService`）、知识库管理（`KnowledgeBaseService`）、缓存处理（`CacheService`）等。
- **数据访问层（Mapper）**：基于MyBatis实现与MySQL数据库的交互，负责用户信息、问答历史（`QuestionHistoryMapper`）、会话数据（`ConversationMapper`）等的持久化。
- **外部服务集成层**：集成大模型服务（阿里云Dashscope、Ollama）、缓存服务（Redis）等，通过封装的工具类（如`JwtUtil`、`ModelService`）实现交互。
- **主要代码结构**：

  ai-customer-service/
   src/
     └── main/
       ├── dataset.sql
       ├── java/
       │   └── com/
       │       └── ai/
       │           ├── AiCustomerServiceApplication.java
       │           ├── controller/
       │           │   ├── ChatController.java
       │           │   ├── PageController.java
       │           │   ├── HomeController.java
       │           │   └── HistoryController.java
       │           ├── service/
       │           │   ├── ModelService.java
       │           │   ├── CacheService.java
       │           │   └── KnowledgeBaseService.java
       │           ├── entity/
       │           │   ├── Conversation.java
       │           │   ├── User.java
       │           │   ├── QuestionHistory.java
       │           │   └── KnowledgeBase.java
       │           ├── mapper/
       │           │   ├── ConversationMapper.java
       │           │   ├── QuestionHistoryMapper.java
       │           │   └── UserMapper.java
       │           ├── common/
       │           │   └── Result.java
       │           └── util/
       │               └── JwtUtil.java
       └── resources/
           ├── static/
           │   ├── chat.html
           │   └── login.html
           └── application.yml


### 2. 核心模块设计

#### （1）认证授权模块
- **认证流程**：用户通过邮箱/密码登录，`AuthService`校验用户信息后，通过`JwtUtil`生成JWT令牌，令牌包含用户ID和用户名，有效期由配置决定。
- **权限校验**：客户端请求需在Header中携带`Bearer Token`，接口通过`JwtUtil`验证令牌有效性，并提取用户ID用于后续操作（如`AuthController`的`getUserInfo`接口）。


#### （2）问答处理模块
- **请求处理流程**：支持流式（SSE）和非流式两种问答模式，优先查询缓存和知识库，未命中时调用大模型：
  1. **缓存查询**：通过`CacheService`检查问题是否在Redis缓存中，命中则直接返回结果。
  2. **知识库查询**：通过`KnowledgeBaseService`检索知识库（`knowledge_base`表），命中则返回预设答案并更新缓存。
  3. **大模型调用**：未命中缓存和知识库时，通过`ModelService`调用指定大模型（默认Qwen，支持DeepSeek/Ollama），返回结果后存入缓存。
- **流式通信**：基于SSE（Server-Sent Events）实现实时响应，`ChatController`的`stream`接口通过HTTP长连接逐字符返回大模型生成的内容。


#### （3）知识库与缓存设计
- **知识库**：基于MySQL的`knowledge_base`表存储预设问答对，支持快速检索，用于覆盖高频、固定答案的问题。
- **缓存**：使用Redis缓存大模型返回的结果，减少重复调用，提高响应速度（`CacheService`负责缓存的增删查）。


#### （4）数据存储设计
- **关系型数据库（MySQL）**：存储结构化数据，包括用户信息（用户表）、问答历史（`question_history`表）、知识库数据（`knowledge_base`表）等，通过主键和索引（如`PRIMARY`、`name`索引）优化查询效率。
- **缓存（Redis）**：存储临时数据，如用户会话、问答缓存等，提升高频访问场景的性能。


### 3. 关键技术选型
- **开发框架**：Spring Boot（快速构建RESTful服务、集成DevTools支持热部署）。
- **ORM框架**：MyBatis（灵活的SQL映射，适配复杂查询场景）。
- **数据库**：MySQL（存储结构化业务数据）。
- **缓存**：Redis（提升读写性能，减少大模型重复调用）。
- **认证**：JWT（无状态令牌，简化分布式环境下的认证）。
- **大模型集成**：支持阿里云Dashscope（Qwen模型）和Ollama（DeepSeek模型），通过HTTP接口实现同步/异步调用。
- **前端交互**：基于HTML（`chat.html`）实现用户界面，支持问答输入、历史记录查看、知识库管理等功能。


## 二、系统使用方法

### 1. 登录与认证
- 用户通过登录页面输入邮箱和密码，系统验证通过后返回JWT令牌，前端将令牌存入`localStorage`。
- 后续请求需在HTTP Header中携带`Authorization: Bearer {token}`，否则接口返回401（未授权）。


### 2. 发起问答
- **非流式问答**：调用`/api/chat/ask`接口，传入用户ID、会话ID和问题，接口返回完整答案及历史记录。
- **流式问答**：调用`/api/chat/stream`接口，传入相同参数，前端通过SSE实时接收逐字符返回的答案，提升交互体验。
- **模型切换**：通过`modelType`参数指定模型（`qwen`或`deepseek`），默认使用Qwen模型。


### 3. 历史记录与知识库管理
- **查看历史**：点击前端“历史”按钮，调用接口加载当前用户的问答历史（存储于`question_history`表）。
- **知识库操作**：点击“知识库”按钮，可查看或添加预设问答对，新增内容将存储于`knowledge_base`表，优先于大模型响应。


### 4. 退出登录
- 点击前端“退出”按钮，清除`localStorage`中的令牌和会话ID，跳转至登录页面。

## 三、后续工作
- 对于本地部署的大模型，可以用专业领域数据集进行微调，提高模型输出质量。
- 进一步完善其他的功能。
