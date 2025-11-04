# TechBlog XSS 演示靶场项目 - 全面分析与优化重构设计

## 一、项目概述

### 1.1 项目定位
本项目是一个面向安全教学的 XSS 漏洞演示靶场，采用前后端分离架构，支持 **VULN（漏洞）和 SECURE（安全）双模式一键动态切换**，用于对比展示 XSS 攻击与防御机制。

**核心特性（必须保留）**：
- ✅ 支持运行时动态切换 VULN/SECURE 模式（通过页面左上角徽章点击）
- ✅ 切换后自动退出登录，重新登录即生效新模式
- ✅ VULN 模式：JWT 存 localStorage、v-html 直接渲染、后端不转义
- ✅ SECURE 模式：JWT 存 HttpOnly Cookie、DOMPurify 过滤、后端 HtmlUtils 转义
- ✅ 支持四个层级的 XSS 演示场景（L0-L3）

**⚠️ 重要约束**：
**所有重构优化必须完全保留上述核心功能，不得破坏模式切换机制和教学演示能力。**

### 1.2 当前技术栈
**前端**：
- Vue 3.4.0
- Vite 5.0.0
- Element Plus 2.5.0
- Pinia 2.1.7
- Axios 1.6.0
- DOMPurify 3.0.8

**后端**：
- Spring Boot 3.2.0
- JDK 21
- MySQL 8.0
- JJWT 0.12.3
- Lombok（需移除）

**构建与运行**：
- Maven 3.9+
- Node 20+
- Docker + Docker Compose

---

## 二、全面问题分析

### 2.0 核心功能确认

**在进行任何优化前，必须明确以下核心功能不可改变**：

1. **双模式切换机制**
   - 后端：`XssProperties.mode` 支持运行时修改（volatile 变量）
   - 前端：`ConfigStore.xssMode` 与后端同步
   - 切换接口：`POST /api/config/mode`
   - 页面控制：左上角徽章点击触发切换

2. **VULN 模式行为**
   - JWT 通过响应体返回，前端存入 localStorage
   - Axios 从 localStorage 读取 Token 放入 Authorization 头
   - 前端使用 `v-html` 直接渲染用户输入（Search、Profile、Feedback）
   - 后端不对用户输入进行 HTML 转义

3. **SECURE 模式行为**
   - JWT 通过 HttpOnly Cookie 返回，前端 JS 不可访问
   - Axios 使用 `withCredentials: true` 自动携带 Cookie
   - 前端使用 `DOMPurify.sanitize()` 过滤后再渲染
   - 后端使用 `HtmlUtils.htmlEscape()` 转义用户输入

4. **XSS 演示场景**
   - L0：反射型 XSS（搜索页面）
   - L1：反射型 XSS + JWT 窃取（搜索 + localStorage）
   - L2：存储型 XSS（用户 Bio）
   - L3：盲 XSS（反馈详情）

**所有重构必须确保以上功能正常工作，否则将失去项目的教学演示价值。**

---

### 2.1 代码质量问题

#### 问题 1：过度依赖 Lombok
**严重程度**：高

**问题描述**：
项目所有实体类、DTO、配置类都使用了 Lombok 的 `@Data`、`@AllArgsConstructor` 等注解，共计 17 处。这违背了用户明确要求的"不要使用 Lombok"。

**影响范围**：
- 所有实体类（User, Article, Comment, Tag, Feedback）
- 所有 DTO 类（UserDto, ArticleDto, CommentDto, TagDto, FeedbackDto, AuthResponse, LoginRequest, RegisterRequest, FeedbackRequest）
- 配置类（JwtProperties, XssProperties）
- 服务类（使用 `@RequiredArgsConstructor`）

**问题根源**：
Lombok 虽然减少样板代码，但会降低代码可读性、增加调试难度、对 IDE 支持要求高，不符合"代码要清晰友好，不要过度炫技"的原则。

#### 问题 2：缺乏中文注释
**严重程度**：中

**问题描述**：
虽然部分关键类（如 SecurityConfig、JwtTokenProvider）有详细的 JavaDoc，但注释覆盖不全面，且缺少方法内部逻辑的行内注释。

**需改进的地方**：
- 实体类字段缺少业务含义说明
- DTO 类缺少字段验证规则说明
- Service 层方法缺少业务逻辑注释
- Controller 层缺少接口用途说明

#### 问题 3：异常处理不规范
**严重程度**：中

**问题描述**：
- Service 层直接抛出 `RuntimeException`，缺少自定义业务异常
- 异常信息过于简单，不便于前端展示和调试
- 缺少详细的错误码机制

**示例**：
```java
// AuthService.java
throw new RuntimeException("Username already exists");
throw new RuntimeException("Invalid credentials");
```

#### 问题 4：数据校验不充分
**严重程度**：中

**问题描述**：
- DTO 类虽然使用了 `@Valid` 注解，但具体字段缺少详细的校验规则
- 缺少自定义校验器（如密码强度、用户名格式）
- 前后端校验规则不一致

### 2.2 架构设计问题

#### 问题 5：Service 层职责混乱
**严重程度**：中

**问题描述**：
- ArticleService 同时处理文章、评论、标签的映射逻辑
- 映射逻辑（mapToDto）应该抽离到独立的 Mapper 层
- 缺少统一的对象转换机制

#### 问题 6：安全配置不完善
**严重程度**：中

**问题描述**：
1. **Cookie 安全属性不足**：
   - `cookie.setSecure(false)` 在生产环境中应该为 true
   - 缺少 `SameSite=Strict` 的统一配置
2. **CORS 配置过于宽松**：允许所有请求头，存在安全隐患

**说明**：JWT 密钥硬编码在教学演示项目中可以接受，不做过度设计。

#### 问题 7：数据库设计缺陷
**严重程度**：中

**问题描述**：
- 缺少数据库索引优化建议
- 字段长度设置不合理（如 bio 字段为 TEXT 类型，无长度限制）
- 缺少审计字段（如 updated_by、created_by）
- 缺少软删除机制

#### 问题 8：前端状态管理混乱
**严重程度**：中

**问题描述**：
- 登录态判断逻辑分散在多处（auth.js、axios.js）
- XSS 模式判断逻辑重复（main.js、xss.js、axios.js）
- 缺少统一的错误处理机制

### 2.3 性能问题

#### 问题 9：N+1 查询问题
**严重程度**：高

**问题描述**：
- ArticleService.getAllArticles 中，获取文章列表时，每篇文章的 author 和 tags 都会触发额外查询
- Comment 查询同样存在 N+1 问题

**解决方案**：
需要使用 `@EntityGraph` 或 JOIN FETCH 优化关联查询。

#### 问题 10：缺少缓存机制
**严重程度**：中

**问题描述**：
- 配置信息（XSS 模式）每次都从数据库读取
- 用户信息、文章列表等热点数据缺少缓存
- JWT Token 验证时缺少黑名单缓存

### 2.4 技术栈版本问题

#### 问题 11：版本过时
**严重程度**：高

**问题描述**：
当前技术栈版本与用户要求不符：

| 组件 | 当前版本 | 目标版本 | 差距 |
|------|---------|---------|------|
| Spring Boot | 3.2.0 | 3.5.x | 需升级 |
| Vue | 3.4.0 | 3.5 | 需升级 |
| Vite | 5.0.0 | 7 | 需升级 |
| Element Plus | 2.5.0 | 2.11 | 需升级 |
| Pinia | 2.1.7 | 3 | 需升级 |
| Axios | 1.6.0 | 1.13 | 需升级 |
| JJWT | 0.12.3 | 0.13.0 | 需升级 |
| MySQL | 8.0 | 8.4 LTS | 需明确版本 |
| Node | 20 | 24 LTS | 需升级 |
| Nginx | alpine | 1.27.x | 需明确版本 |
| Docker Engine | 未明确 | 27.x | 需明确版本 |
| Compose | 3.8 | 2.36+ | 需升级 |

### 2.5 未使用的组件

#### 问题 12：引入但未使用的依赖
**严重程度**：低

**问题描述**：
- Spring Boot DevTools：生产环境不需要
- Spring Security Test：测试依赖，但项目中没有单元测试
- Sass：前端引入了 Sass 依赖，但只有一个 dark.scss 文件，使用率低

### 2.6 文档问题

#### 问题 13：文档不一致
**严重程度**：中

**问题描述**：
- README.md 中提到的技术栈与实际 package.json/pom.xml 不完全一致
- XSS 演示场景说明.md 过于详细，适合教学但不适合快速上手
- 缺少架构设计文档和接口文档

---

## 三、优化重构方案

### 3.1 代码质量优化

**⚠️ 优化原则**：所有代码重构不得改变业务逻辑，特别是双模式切换机制。

#### 优化 1：移除 Lombok 依赖

**实施范围**：所有使用 Lombok 注解的类

**改造策略**：
1. **实体类**：手动编写 Getter、Setter、构造函数、equals、hashCode、toString
2. **DTO 类**：只保留必要的 Getter/Setter，避免生成不必要的方法
3. **配置类**：手动编写 Getter/Setter（**特别注意 XssProperties.setMode() 的 volatile 语义必须保留**）
4. **服务类**：将 `@RequiredArgsConstructor` 改为标准构造函数注入

**关键注意事项**：
- `XssProperties.mode` 字段必须保留 `volatile` 修饰符，支持运行时切换
- `XssProperties.setMode()` 方法必须保留模式验证逻辑（只接受 vuln/secure）
- 移除 Lombok 后，手动编写的代码必须保持相同的行为语义

**代码规范**：
- 构造函数按照字段声明顺序排列
- Getter/Setter 方法紧跟字段定义
- equals/hashCode 只比较业务主键
- toString 不输出敏感字段（如 passwordHash）

#### 优化 2：增加友好中文注释

**注释原则**：
- 类级别：说明类的职责、使用场景、注意事项
- 字段级别：说明业务含义、取值范围、默认值
- 方法级别：说明功能、参数含义、返回值、异常情况
- 关键逻辑：增加行内注释，解释"为什么这样做"

**注释示例**：
```java
/**
 * 用户实体类
 * 
 * 职责：
 * - 存储用户基本信息和认证凭证
 * - 关联用户发布的文章和评论
 * 
 * 安全注意：
 * - passwordHash 字段存储 BCrypt 加密后的密码，不可逆
 * - bio 字段在 VULN 模式下存在 XSS 风险，需前端净化
 */
public class User {
    /**
     * 用户唯一标识
     * 数据库自增主键
     */
    private Long id;
    
    /**
     * 用户名
     * 规则：3-32 个字符，字母数字下划线，全局唯一
     */
    private String username;
    // ... 其他字段
}
```

#### 优化 3：规范异常处理

**方案设计**：

1. **自定义业务异常体系**
   - 创建 `BusinessException` 基类
   - 派生具体异常：`UserExistsException`、`InvalidCredentialsException`、`ResourceNotFoundException` 等

2. **统一错误响应格式**
   ```java
   {
     "code": "USER_EXISTS",
     "message": "用户名已存在",
     "timestamp": "2025-01-01T12:00:00Z",
     "path": "/api/auth/register"
   }
   ```

3. **错误码枚举**
   - 定义 `ErrorCode` 枚举类
   - 包含错误码、HTTP 状态码、默认消息

#### 优化 4：完善数据校验

**改进方案**：

1. **DTO 字段校验**
   ```java
   public class RegisterRequest {
       // 用户名：3-32 个字符，只能包含字母数字下划线
       @NotBlank(message = "用户名不能为空")
       @Size(min = 3, max = 32, message = "用户名长度为 3-32 个字符")
       @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
       private String username;
       
       // 密码：8-64 个字符，必须包含大小写字母和数字
       @NotBlank(message = "密码不能为空")
       @Size(min = 8, max = 64, message = "密码长度为 8-64 个字符")
       @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", 
                message = "密码必须包含大小写字母和数字")
       private String password;
       
       // 邮箱：标准邮箱格式
       @NotBlank(message = "邮箱不能为空")
       @Email(message = "邮箱格式不正确")
       @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
       private String email;
   }
   ```

2. **自定义校验器**
   - 创建 `@ValidPassword` 注解用于密码强度校验
   - 创建 `@ValidUsername` 注解用于用户名格式校验

### 3.2 架构设计优化

#### 优化 5：引入 Mapper 层

**方案设计**：

1. **创建独立的 Mapper 接口和实现**
   - `UserMapper`：负责 User 实体与 UserDto 的转换
   - `ArticleMapper`：负责 Article 实体与 ArticleDto 的转换
   - `CommentMapper`：负责 Comment 实体与 CommentDto 的转换

2. **Mapper 职责**
   - 实体到 DTO 的转换
   - DTO 到实体的转换（用于创建和更新）
   - 批量转换（如 List 转换）
   - 关联对象的递归转换控制

3. **实现方式**
   - 纯手工编写（符合"不要过度炫技"原则）
   - 不使用 MapStruct（避免引入额外复杂性）

#### 优化 6：增强安全配置

**⚠️ 重要约束**：安全配置增强**不得破坏** VULN 模式的演示效果。VULN 模式必须保持「不安全」状态以便展示攻击。

**改进方案**：

1. **Cookie 安全属性配置化**
   ```java
   // 创建 CookieProperties 配置类
   @ConfigurationProperties(prefix = "security.cookie")
   public class CookieProperties {
       private Boolean httpOnly = true;
       private Boolean secure = false; // 生产环境通过环境变量覆盖
       private String sameSite = "Strict";
       private Integer maxAge = 1800;
   }
   ```
   
   **注意**：这些配置仅在 **SECURE 模式**下生效，VULN 模式下依然使用不安全配置以便教学演示。

2. **CORS 配置细化**
   - 明确允许的请求头白名单
   - 区分开发环境和生产环境的配置
   - 增加环境变量配置支持

3. **增加安全响应头（仅 SECURE 模式）**
   ```java
   // WebSecurityConfig 中增加（根据 XSS 模式动态控制）
   if (xssProperties.isSecure()) {
       http.headers(headers -> headers
           .contentSecurityPolicy(csp -> csp
               .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")
           )
           .frameOptions(frame -> frame.deny())
           .xssProtection(xss -> xss.block(true))
       );
   }
   // VULN 模式下不启用这些安全响应头，以便 XSS 攻击能够成功执行
   ```

#### 优化 7：优化数据库设计

**改进方案**：

1. **索引优化**
   ```sql
   -- 用户表索引
   CREATE INDEX idx_username ON users(username);
   CREATE INDEX idx_email ON users(email);
   
   -- 文章表索引
   CREATE INDEX idx_author_id ON articles(author_id);
   CREATE INDEX idx_slug ON articles(slug);
   CREATE INDEX idx_published_at ON articles(published_at);
   
   -- 评论表索引
   CREATE INDEX idx_article_id ON comments(article_id);
   CREATE INDEX idx_user_id ON comments(user_id);
   CREATE INDEX idx_created_at ON comments(created_at);
   ```

2. **字段长度优化**
   ```sql
   -- 限制 bio 字段长度，防止恶意注入过长内容
   ALTER TABLE users MODIFY COLUMN bio VARCHAR(1000);
   
   -- 限制反馈内容长度
   ALTER TABLE feedbacks MODIFY COLUMN content_html VARCHAR(5000);
   ```

3. **增加审计字段**
   ```sql
   ALTER TABLE users 
     ADD COLUMN updated_by VARCHAR(32),
     ADD COLUMN deleted_at DATETIME;
   
   ALTER TABLE articles 
     ADD COLUMN updated_by VARCHAR(32),
     ADD COLUMN deleted_at DATETIME;
   ```

4. **数据完整性约束**
   - 增加外键约束（在教学环境可选）
   - 增加字段默认值
   - 增加 CHECK 约束（如 likes_count >= 0）

#### 优化 8：规范前端状态管理

**改进方案**：

1. **统一配置管理**
   - ConfigStore 增加持久化支持
   - 模式切换后自动同步到所有组件
   - 增加配置变更监听机制

2. **统一错误处理**
   ```javascript
   // 创建 errors.js
   export class ApiError extends Error {
     constructor(code, message, status) {
       super(message);
       this.code = code;
       this.status = status;
     }
   }
   
   // axios 拦截器统一处理
   instance.interceptors.response.use(
     response => response,
     error => {
       const apiError = new ApiError(
         error.response?.data?.code || 'UNKNOWN_ERROR',
         error.response?.data?.message || '系统错误',
         error.response?.status || 500
       );
       return Promise.reject(apiError);
     }
   );
   ```

3. **登录态统一管理**
   - 抽离 Token 存储逻辑到独立模块
   - 统一 Token 过期处理
   - 增加 Token 自动刷新机制（可选）

### 3.3 性能优化

#### 优化 9：解决 N+1 查询问题

**方案设计**：

1. **使用 EntityGraph 优化**
   ```java
   // ArticleRepository.java
   @EntityGraph(attributePaths = {"author", "tags", "comments"})
   Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);
   
   @EntityGraph(attributePaths = {"author", "tags"})
   Optional<Article> findBySlug(String slug);
   ```

2. **使用 JOIN FETCH 优化**
   ```java
   // 自定义查询方法
   @Query("SELECT a FROM Article a " +
          "LEFT JOIN FETCH a.author " +
          "LEFT JOIN FETCH a.tags " +
          "WHERE a.id = :id")
   Optional<Article> findByIdWithDetails(@Param("id") Long id);
   ```

3. **分页查询优化**
   - 使用子查询先获取 ID 列表
   - 再通过 IN 查询获取完整数据
   - 避免在分页时使用 JOIN FETCH

#### 优化 10：引入缓存机制

**方案设计**：

1. **配置缓存**
   - 使用 Spring Cache 抽象
   - 配置信息（XSS 模式）使用 Caffeine 本地缓存
   - 过期策略：手动失效（切换模式时清除）

2. **数据缓存（可选）**
   - 文章列表：缓存 5 分钟
   - 用户信息：缓存 10 分钟
   - 标签列表：缓存 30 分钟

3. **缓存配置**
   ```java
   @Configuration
   @EnableCaching
   public class CacheConfig {
       @Bean
       public CacheManager cacheManager() {
           CaffeineCacheManager cacheManager = new CaffeineCacheManager(
               "config", "users", "articles", "tags"
           );
           cacheManager.setCaffeine(Caffeine.newBuilder()
               .maximumSize(1000)
               .expireAfterWrite(10, TimeUnit.MINUTES)
           );
           return cacheManager;
       }
   }
   ```

### 3.4 技术栈升级

#### 升级方案

**后端升级**：

1. **Spring Boot 3.2.0 → 3.5.x**
   - 更新 pom.xml 中的 parent 版本
   - 检查 Spring Security 6.x 的 API 变更
   - 测试数据库连接池配置兼容性

2. **JJWT 0.12.3 → 0.13.0**
   - 检查 API 变更（主要是安全增强）
   - 更新密钥生成方式
   - 测试 Token 签名验证

3. **MySQL 8.0 → 8.4 LTS**
   - 更新 Docker Compose 镜像版本
   - 测试 SQL 语法兼容性
   - 优化连接参数配置

**前端升级**：

1. **Vue 3.4.0 → 3.5**
   - 更新 package.json 依赖
   - 检查 Composition API 变更
   - 测试响应式系统兼容性

2. **Vite 5.0.0 → 7**
   - 更新 vite.config.js 配置
   - 检查插件兼容性
   - 测试构建性能提升

3. **Element Plus 2.5.0 → 2.11**
   - 检查组件 API 变更
   - 更新主题配置
   - 测试暗黑模式兼容性

4. **Pinia 2.1.7 → 3**
   - 检查 Store 定义方式变更
   - 更新持久化插件
   - 测试 TypeScript 支持

5. **Axios 1.6.0 → 1.13**
   - 检查拦截器 API 变更
   - 更新错误处理逻辑
   - 测试跨域配置

**构建工具升级**：

1. **Node 20 → 24 LTS**
   - 更新 Dockerfile 基础镜像
   - 测试 npm 兼容性
   - 验证构建性能

2. **Maven 3.9 → 3.9.11**
   - 更新 Dockerfile 基础镜像
   - 测试依赖解析
   - 验证构建稳定性

3. **Docker Engine 27.x**
   - 更新 docker-compose.yml 版本声明为 3.9
   - 使用 Compose v2 语法（如 `depends_on.condition`）
   - 明确镜像版本标签

4. **Nginx 1.27.x**
   - 更新 Dockerfile 基础镜像为 `nginx:1.27-alpine`
   - 优化 nginx.conf 配置
   - 增加 HTTP/2 支持

**升级策略**：
- 优先升级底层依赖（JDK、Node、数据库）
- 再升级框架（Spring Boot、Vue）
- 最后升级工具库（Element Plus、Axios）
- 每次升级后进行完整回归测试

### 3.5 代码结构重构

#### 重构方案

**后端项目结构**：
```
backend/
├── src/main/java/com/techblog/backend/
│   ├── common/                    # 公共组件（新增）
│   │   ├── exception/             # 异常体系
│   │   │   ├── BusinessException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── InvalidCredentialsException.java
│   │   ├── enums/                 # 枚举类
│   │   │   ├── ErrorCode.java
│   │   │   └── UserRole.java（从 User 中提取）
│   │   └── response/              # 统一响应
│   │       ├── Result.java
│   │       └── ErrorResponse.java
│   ├── config/                    # 配置类
│   │   ├── CacheConfig.java（新增）
│   │   ├── CookieProperties.java（新增）
│   │   └── ...
│   ├── controller/                # 控制器
│   ├── dto/                       # 数据传输对象
│   ├── entity/                    # 实体类
│   ├── mapper/                    # 对象映射（新增）
│   │   ├── UserMapper.java
│   │   ├── ArticleMapper.java
│   │   └── CommentMapper.java
│   ├── repository/                # 数据访问层
│   ├── security/                  # 安全组件
│   └── service/                   # 业务逻辑层
```

**前端项目结构**：
```
frontend/
├── src/
│   ├── api/
│   │   ├── axios.js              # Axios 实例
│   │   ├── modules/              # API 模块（新增）
│   │   │   ├── auth.js
│   │   │   ├── article.js
│   │   │   └── user.js
│   ├── common/                    # 公共模块（新增）
│   │   ├── constants.js          # 常量定义
│   │   ├── errors.js             # 错误处理
│   │   └── validators.js         # 表单验证
│   ├── components/               # 组件
│   ├── composables/              # 组合式函数（新增）
│   │   ├── useAuth.js
│   │   ├── useConfig.js
│   │   └── useXss.js
│   ├── pages/                    # 页面
│   ├── stores/                   # Pinia 状态管理
│   ├── utils/                    # 工具函数
│   │   ├── xss.js
│   │   ├── storage.js（新增）
│   │   └── format.js（新增）
│   └── ...
```

---

## 四、重构实施计划

### 4.1 第一阶段：基础重构（不改变功能）

**⚠️ 阶段目标**：重构代码结构，但**完全保留**双模式切换功能和所有 XSS 演示场景。

#### 任务 1：移除 Lombok（优先级：最高）
**预计工作量**：2-3 天

**实施步骤**：
1. 创建实体类的标准 Getter/Setter/构造函数
2. 更新所有 DTO 类
3. **特别注意**：更新 `XssProperties` 配置类，保留 `volatile` 和模式验证逻辑
4. 更新服务类的构造函数注入
5. 移除 pom.xml 中的 Lombok 依赖
6. 全量编译测试

**验收标准**：
- ✅ 编译通过，无 Lombok 相关错误
- ✅ 所有接口功能正常
- ✅ **双模式切换功能正常**（重点测试）
- ✅ **L0-L3 四个 XSS 场景演示正常**（重点测试）
  - VULN 模式：XSS 攻击能够成功执行
  - SECURE 模式：XSS 攻击被成功拦截
- ✅ 单元测试通过（如果存在）

#### 任务 2：增加中文注释（优先级：高）
**预计工作量**：2 天

**实施步骤**：
1. 为所有实体类增加类级和字段级注释
2. 为所有 DTO 类增加字段注释和校验规则说明
3. 为所有 Service 类增加方法注释
4. 为所有 Controller 类增加接口注释
5. 为关键业务逻辑增加行内注释

**注释模板**：
- 类注释：职责、使用场景、注意事项
- 方法注释：功能、参数、返回值、异常
- 字段注释：业务含义、取值范围、默认值

#### 任务 3：规范异常处理（优先级：高）
**预计工作量**：1-2 天

**实施步骤**：
1. 创建 `common.exception` 包
2. 定义 `BusinessException` 基类
3. 定义具体异常类（用户存在、凭证无效、资源未找到等）
4. 创建 `ErrorCode` 枚举
5. 更新 `GlobalExceptionHandler`
6. 重构所有 Service 层的异常抛出逻辑

**验收标准**：
- 所有异常都有明确的错误码
- 前端能够正确解析错误信息
- 日志中能够清晰追踪异常来源

### 4.2 第二阶段：架构优化（优先级：高）

#### 任务 4：引入 Mapper 层（优先级：中）
**预计工作量**：2 天

**实施步骤**：
1. 创建 `mapper` 包
2. 实现 `UserMapper`
3. 实现 `ArticleMapper`
4. 实现 `CommentMapper`
5. 重构 Service 层，使用 Mapper 替代手工映射
6. 测试对象转换逻辑

#### 任务 5：完善数据校验（优先级：中）
**预计工作量**：1-2 天

**实施步骤**：
1. 为所有 DTO 字段增加详细校验注解
2. 创建自定义校验器（密码强度、用户名格式）
3. 前端增加对应的校验规则
4. 测试校验逻辑

#### 任务 6：增强安全配置（优先级：高）
**预计工作量**：1 天

**实施步骤**：
1. 创建 `CookieProperties` 配置类
2. 更新 SecurityConfig 根据 XSS 模式**动态控制**安全响应头
3. 优化 CORS 配置
4. **重点测试**：确保 VULN 模式下不启用安全响应头，SECURE 模式下启用

**验收标准**：
- ✅ VULN 模式：无 CSP、X-Frame-Options、X-XSS-Protection 响应头，XSS 攻击成功
- ✅ SECURE 模式：有完整安全响应头，XSS 攻击被拦截
- ✅ Cookie 配置在 SECURE 模式下正确应用
- ✅ 模式切换后配置即时生效

### 4.3 第三阶段：性能优化（优先级：中）

#### 任务 7：解决 N+1 查询问题（优先级：高）
**预计工作量**：1 天

**实施步骤**：
1. 为 ArticleRepository 增加 `@EntityGraph` 注解
2. 优化文章详情查询
3. 优化评论查询
4. 使用 Hibernate 日志验证 SQL 优化效果

#### 任务 8：引入缓存机制（优先级：低）
**预计工作量**：1 天

**实施步骤**：
1. 添加 Caffeine 依赖
2. 创建 `CacheConfig` 配置类
3. 为配置接口增加缓存
4. （可选）为文章列表、用户信息增加缓存
5. 测试缓存失效逻辑

### 4.4 第四阶段：技术栈升级（优先级：最高）

#### 任务 9：后端技术栈升级（优先级：最高）
**预计工作量**：2-3 天

**实施步骤**：
1. 升级 Spring Boot 到 3.5.x
2. 升级 JJWT 到 0.13.0
3. 升级 Maven 到 3.9.11
4. 更新 Dockerfile 使用 JDK 21
5. 更新 MySQL Docker 镜像到 8.4 LTS
6. 全量回归测试

**兼容性检查**：
- Spring Security 6.x API 变更
- JJWT 密钥生成方式变更
- MySQL 8.4 新特性和废弃功能

#### 任务 10：前端技术栈升级（优先级：最高）
**预计工作量**：2-3 天

**实施步骤**：
1. 升级 Vue 到 3.5
2. 升级 Vite 到 7
3. 升级 Element Plus 到 2.11
4. 升级 Pinia 到 3
5. 升级 Axios 到 1.13
6. 更新 Dockerfile 使用 Node 24 LTS
7. 更新 Nginx 镜像到 1.27.x
8. 全量回归测试

**兼容性检查**：
- Vite 7 配置变更
- Pinia 3 API 变更
- Element Plus 2.11 组件变更

#### 任务 11：构建工具升级（优先级：高）
**预计工作量**：1 天

**实施步骤**：
1. 更新 docker-compose.yml 使用 Compose v2 语法
2. 明确 Docker Engine 版本要求为 27.x
3. 更新所有 Dockerfile 的基础镜像版本
4. 测试容器化部署

### 4.5 第五阶段：代码重构（优先级：中）

#### 任务 12：优化数据库设计（优先级：中）
**预计工作量**：1-2 天

**实施步骤**：
1. 创建索引优化脚本
2. 调整字段长度限制
3. 增加审计字段（可选）
4. 增加数据完整性约束
5. 更新 schema.sql
6. 测试数据库迁移

#### 任务 13：规范前端状态管理（优先级：中）
**预计工作量**：1-2 天

**实施步骤**：
1. 创建 `common/errors.js` 统一错误处理
2. 创建 `common/storage.js` 统一 Token 存储
3. 优化 ConfigStore 增加持久化
4. 抽离公共逻辑到 composables
5. 测试状态管理

#### 任务 14：清理未使用的依赖（优先级：低）
**预计工作量**：0.5 天

**实施步骤**：
1. 移除 Spring Boot DevTools（生产环境）
2. 评估 Sass 使用情况，考虑移除
3. 检查其他未使用的依赖
4. 更新 pom.xml 和 package.json

### 4.6 第六阶段：文档完善（优先级：中）

#### 任务 15：更新项目文档（优先级：中）
**预计工作量**：1 天

**实施步骤**：
1. 更新 README.md 中的技术栈说明
2. 简化 XSS 演示场景说明，增加快速开始章节
3. 创建 ARCHITECTURE.md 架构设计文档
4. 创建 API.md 接口文档（可选）
5. 更新 Docker 部署文档

---

## 五、质量保障

### 5.1 双模式切换测试规范

**每次重构后必须进行的回归测试**：

#### 测试用例 1：模式切换功能
1. 启动项目，默认 VULN 模式
2. 点击页面左上角红色「VULN」徽章
3. 确认弹窗提示模式切换
4. 确认自动退出登录
5. 重新登录，确认模式已切换到 SECURE（绿色徽章）
6. 再次点击徽章，确认能切换回 VULN 模式

#### 测试用例 2：VULN 模式 XSS 演示
1. 切换到 VULN 模式
2. **L0 测试**：访问 `/search?q=<img src=x onerror=alert('XSS')>`，确认弹窗出现
3. **L1 测试**：登录后访问搜索页，确认 localStorage 中有 `accessToken`
4. **L2 测试**：访问 `/profile/attacker`，确认伪装登录框出现
5. **L3 测试**：提交恶意反馈，管理员查看时确认脚本执行

#### 测试用例 3：SECURE 模式防御
1. 切换到 SECURE 模式
2. **L0 测试**：访问同样的搜索 URL，确认只显示转义后的文本，无弹窗
3. **L1 测试**：登录后确认 localStorage 中**无** `accessToken`（存在 HttpOnly Cookie）
4. **L2 测试**：访问 `/profile/attacker`，确认只显示纯文本，无伪装登录框
5. **L3 测试**：提交同样的反馈，管理员查看时确认脚本被过滤

#### 测试用例 4：JWT 存储策略切换
1. VULN 模式登录：
   - 检查网络请求，响应体包含 `accessToken`
   - 检查 localStorage，存在 `accessToken`
   - 检查后续请求，Authorization 头包含 `Bearer {token}`
2. SECURE 模式登录：
   - 检查网络请求，响应体无 `accessToken`
   - 检查 Set-Cookie 响应头，包含 `access={token}; HttpOnly; SameSite=Strict`
   - 检查 localStorage，不存在 `accessToken`
   - 检查后续请求，自动携带 Cookie

**⚠️ 如果以上任何测试失败，则重构不通过，必须修复后再继续。**

---

### 5.2 代码规范

#### Java 代码规范
1. **命名规范**
   - 类名：大驼峰（PascalCase）
   - 方法名、变量名：小驼峰（camelCase）
   - 常量：全大写下划线分隔（UPPER_SNAKE_CASE）
   - 包名：全小写，单词之间不使用分隔符

2. **注释规范**
   - 公开 API 必须有 JavaDoc
   - 类级注释包含：职责、使用场景、注意事项
   - 方法注释包含：功能、参数、返回值、异常
   - 关键逻辑增加行内注释

3. **代码风格**
   - 缩进：4 个空格
   - 单行长度：不超过 120 字符
   - 方法长度：不超过 50 行
   - 类长度：不超过 500 行

#### JavaScript 代码规范
1. **命名规范**
   - 组件名：大驼峰（PascalCase）
   - 函数名、变量名：小驼峰（camelCase）
   - 常量：全大写下划线分隔（UPPER_SNAKE_CASE）
   - 文件名：小写短横线分隔（kebab-case）

2. **注释规范**
   - 组件顶部增加功能说明注释
   - 复杂函数增加 JSDoc 注释
   - 关键逻辑增加行内注释

3. **代码风格**
   - 缩进：2 个空格
   - 单行长度：不超过 100 字符
   - 优先使用 Composition API
   - 优先使用 const/let，禁用 var

---

### 5.3 测试策略

#### 后端测试
1. **单元测试**（可选）
   - Service 层核心业务逻辑
   - Mapper 层对象转换逻辑
   - 工具类方法

2. **集成测试**（推荐）
   - Controller 层接口测试
   - 数据库操作测试
   - 安全认证测试

3. **测试覆盖率目标**
   - 核心业务逻辑：80%+
   - 整体覆盖率：60%+

#### 前端测试
1. **组件测试**（可选）
   - 公共组件单元测试
   - Store 逻辑测试

2. **E2E 测试**（推荐）
   - 登录流程
   - 文章浏览流程
   - **XSS 攻击与防御演示流程（核心）**
   - **双模式切换流程（核心）**

---

### 5.4 版本控制

#### Git 分支策略
- `main`：主分支，保持稳定
- `develop`：开发分支
- `feature/*`：功能分支
- `hotfix/*`：紧急修复分支

#### Commit 规范
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**：
- `feat`：新功能
- `fix`：Bug 修复
- `refactor`：重构
- `docs`：文档更新
- `style`：代码格式调整
- `test`：测试相关
- `chore`：构建工具、依赖更新

**示例**：
```
feat(auth): 移除 Lombok 依赖，增加标准 Getter/Setter

- 重写 User 实体类的 Getter/Setter
- 重写所有 DTO 类的 Getter/Setter
- 更新服务类使用标准构造函数注入
- 移除 pom.xml 中的 Lombok 依赖

Closes #123
```

---

## 六、风险评估

### 6.1 技术风险

#### 风险 1：技术栈升级兼容性问题
**风险等级**：高

**风险描述**：
- Spring Boot 3.2 → 3.5 可能存在 API 变更
- Vite 5 → 7 配置格式可能不兼容
- Pinia 2 → 3 API 可能有破坏性变更

**应对措施**：
1. 升级前仔细阅读官方迁移指南
2. 先在独立分支进行升级测试
3. 保留原版本的可回退方案
4. 逐个依赖升级，每次升级后进行完整测试

#### 风险 2：移除 Lombok 导致代码量激增
**风险等级**：中

**风险描述**：
- 实体类代码量可能增加 3-5 倍
- 维护成本增加
- 可能引入手工编写的 Bug

**应对措施**：
1. 使用 IDE 自动生成功能
2. 编写代码生成脚本辅助
3. 增加代码审查环节
4. 优先重构核心类，非核心类分批处理

### 6.2 进度风险

#### 风险 3：重构周期过长影响项目交付
**风险等级**：中

**风险描述**：
- 全量重构预计需要 15-20 个工作日
- 可能影响其他功能开发

**应对措施**：
1. 分阶段实施，优先完成高优先级任务
2. 第一阶段（移除 Lombok、增加注释）必须完成
3. 性能优化和缓存机制可延后实施
4. 技术栈升级单独排期

### 6.3 质量风险

#### 风险 4：重构引入新 Bug 或破坏核心功能
**风险等级**：**极高**

**风险描述**：
- 大规模重构可能引入功能性 Bug
- **性能优化可能破坏双模式切换机制**（最大风险）
- **安全配置增强可能导致 VULN 模式无法演示 XSS 攻击**（最大风险）
- 前端状态管理优化可能导致 JWT 存储策略失效

**应对措施**：
1. **必须遵守的原则**：
   - 所有重构不得改变 `XssProperties.mode` 的 volatile 语义
   - 所有重构不得改变 `AuthController` 中的双模式分支逻辑
   - 所有重构不得改变前端的 `v-html` 渲染条件判断
   - VULN 模式下必须保持不安全状态（不启用 CSP、不过滤 HTML、JWT 存 localStorage）
2. 每个任务完成后进行完整回归测试（必须包含 5.1 章节的所有测试用例）
3. 使用自动化测试覆盖核心流程
4. Code Review 必须覆盖所有变更，特别关注双模式相关代码
5. 灰度发布，先在测试环境验证

---

## 七、预期成果

### 7.1 代码质量提升

1. **可读性提升**
   - 移除 Lombok，代码逻辑一目了然
   - 增加友好的中文注释，降低理解成本
   - 规范异常处理，错误信息清晰明确

2. **可维护性提升**
   - 引入 Mapper 层，职责更加清晰
   - 规范前端状态管理，逻辑不再分散
   - 优化项目结构，模块划分更加合理

3. **健壮性提升**
   - 完善数据校验，减少无效输入
   - 增强安全配置，防御常见攻击
   - 规范异常处理，提高容错能力

### 7.2 性能提升

1. **查询性能**
   - 解决 N+1 查询问题，减少数据库交互
   - 增加索引优化，提升查询速度
   - 引入缓存机制，降低数据库压力

2. **构建性能**
   - Vite 7 构建速度提升
   - Node 24 LTS 性能优化
   - Docker 多阶段构建优化镜像大小

### 7.3 安全性提升

1. **JWT 安全**
   - 密钥环境化，避免硬编码泄露
   - Cookie 安全属性增强，防 XSS 窃取
   - 增加 Token 过期和刷新机制

2. **CORS 安全**
   - 细化允许的请求头白名单
   - 区分开发和生产环境配置
   - 增加安全响应头

3. **输入校验**
   - 前后端双重校验
   - 使用正则表达式严格限制格式
   - 增加自定义校验器

### 7.4 符合最佳实践

1. **Spring Boot 3 最佳实践**
   - 使用配置属性类（@ConfigurationProperties）
   - 使用构造函数注入替代字段注入
   - 规范异常处理体系
   - 使用 Spring Cache 抽象

2. **Vue 3 最佳实践**
   - 优先使用 Composition API
   - 使用 Pinia 替代 Vuex
   - 使用 Composables 复用逻辑
   - 规范组件设计（单一职责）

3. **代码规范**
   - 统一命名规范
   - 统一注释规范
   - 统一代码格式
   - 统一错误处理

### 7.5 技术栈现代化

1. **版本全面更新**
   - 所有依赖升级到最新稳定版本
   - 享受新版本的性能优化和新特性
   - 减少安全漏洞风险

2. **长期维护性**
   - 使用 LTS 版本（Node 24 LTS、MySQL 8.4 LTS）
   - 技术栈主流且活跃，社区支持好
   - 便于后续升级和迁移

3. **核心功能完整保留**
   - ✅ **双模式切换机制完全保留**
   - ✅ **四个 XSS 演示场景完全保留**
   - ✅ **JWT 存储策略双模式分支完全保留**
   - ✅ **前端渲染策略双模式分支完全保留**
   - ✅ **项目作为安全教学靶场的核心价值完全保留**

---

## 八、后续优化建议

### 8.1 短期优化（1-3 个月）

1. **增加单元测试**
   - Service 层核心业务逻辑测试
   - Mapper 层对象转换测试
   - 工具类方法测试

2. **增加 E2E 测试**
   - 使用 Playwright 或 Cypress
   - 覆盖核心业务流程
   - 自动化回归测试

3. **性能监控**
   - 集成 Micrometer + Prometheus
   - 监控接口响应时间
   - 监控数据库查询性能

4. **日志增强**
   - 使用 ELK 或 Loki 集中日志
   - 增加链路追踪（如 Spring Cloud Sleuth）
   - 规范日志格式和级别

### 8.2 中期优化（3-6 个月）

1. **引入 API 文档**
   - 集成 Springdoc OpenAPI
   - 自动生成接口文档
   - 支持在线调试

2. **前端组件库扩展**
   - 提取公共业务组件
   - 建立组件文档（如 Storybook）
   - 规范组件设计模式

3. **数据库读写分离**（可选）
   - 主从复制
   - 读写路由
   - 提升并发性能

4. **国际化支持**（可选）
   - 使用 i18n
   - 支持中英文切换
   - 扩展语言包

### 8.3 长期优化（6-12 个月）

1. **微服务改造**（可选）
   - 按业务拆分服务
   - 引入服务注册与发现
   - 使用 API 网关

2. **前端工程化增强**
   - 引入 Monorepo 管理
   - 组件库独立发布
   - 增加自动化 CI/CD

3. **安全加固**
   - 引入 WAF（Web 应用防火墙）
   - 增加 API 限流
   - 增加敏感操作二次认证

4. **云原生改造**
   - Kubernetes 部署
   - 服务网格（Istio）
   - 自动扩缩容   - 服务网格（Istio）
