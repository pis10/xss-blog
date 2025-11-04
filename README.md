# TechBlog - XSS 漏洞演示靶场

<div align="center">

⚠️ 本项目含刻意设计的安全漏洞，仅用于授权的教学与研究。请勿部署到生产或公网。

</div>

---

## 项目简介

一个专为 Web 安全教学设计的 XSS 漏洞演示平台，通过 **VULN ↔ SECURE 双模式**对比攻击与防御效果。

**技术栈**：
- 前端：Vue 3 + Element Plus + Vite
- 后端：Spring Boot 3 + JPA + MySQL 8
- 认证：JWT (HS256)

**核心特性**：
- ✅ 5 个渐进式 XSS 攻击场景（从简单弹窗到蠕虫传播）
- ✅ 双模式一键切换（无需重启）
- ✅ 完整攻防对比（JWT 存储、内容过滤、安全响应头）

## 快速开始

### 本地运行

**前置要求**：Node.js 20+、JDK 21、MySQL 8.0、Maven 3.9+

```bash
# 1. 启动 MySQL（root/root）

# 2. 启动后端
cd apps/backend
mvn spring-boot:run
# 访问 http://localhost:8080

# 3. 启动前端（新终端）
cd apps/frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### Docker 一键部署
```
cd deploy
docker-compose up -d
```
访问 `http://localhost`

## 模式切换

### 方式 1：页面切换（推荐）
点击页面左上角徽章：
- 🔴 VULN 模式（红色）→ 点击切换
- 🟢 SECURE 模式（绿色）→ 点击切换

### 方式 2：配置文件
修改配置后需重启服务：
- 后端：`apps/backend/src/main/resources/application.yml` → `xss.mode: vuln|secure`
- 前端：`apps/frontend/.env` → `VITE_XSS_MODE=vuln|secure`

### 模式差异对比

| 项目 | VULN 模式 | SECURE 模式 |
|------|----------|-------------|
| **JWT 存储** | localStorage（可被 JS 读取） | HttpOnly Cookie（JS 无法访问） |
| **内容渲染** | v-html 直接渲染 | 场景 4 DOMPurify 过滤，其他文本渲染 |
| **后端输出** | 不转义 | 场景 1/2/3/5 HTML 转义 |
| **安全响应头** | 无 | CSP + X-Frame-Options |
| **XSS 攻击** | ✅ 成功执行 | ❌ 被拦截 |

## 测试账号

| 用户名 | 密码 | 角色 | 用途 |
|--------|------|------|------|
| admin | Admin#2025 | 管理员 | 场景 5（盲 XSS） |
| attacker | Attacker#2025 | 普通用户 | 场景 4（Bio XSS） |
| alice | Admin#2025 | 普通用户 | 场景 3（评论蠕虫） |

## 演示场景

### 场景 1：反射型 XSS「Hello, XSS」
**目标**：确认 XSS 能执行  
**入口**：`/search?q=...`  
**Payload**：`<img src=x onerror=alert(1)>`  
**预期**：弹窗显示 `1`

### 场景 2：静默画像收集
**目标**：无感窃取 JWT 凭证  
**入口**：`/search?q=...`  
**特点**：不弹窗，静默上报到攻击者服务器  
**预期**：收集器记录 `{hasToken:true, profile:true}`

### 场景 3：评论蠕虫 ⭐
**目标**：展示存储型 XSS 自传播能力  
**入口**：文章详情页评论区  
**特点**：
- 自动扩散到其他文章
- localStorage 防重复
- 目标数上限（3 个）保证可控  
**预期**：评论自动传播到 3 篇文章

### 场景 4：Bio 伪造登录页
**目标**：全屏伪造登录界面钓鱼  
**入口**：`/profile/{username}` 个人简介  
**特点**："会话已过期"提示，诱骗输入账号密码  
**预期**：用户输入后攻击者获取凭证

### 场景 5：盲 XSS 窃取管理员身份
**目标**：利用管理员会话窃取凭证  
**入口**：前台 `/feedback` → 后台 `/admin/feedbacks` 查看  
**特点**：管理员无感知  
**预期**：攻击者获取管理员 username、role、cookie

📝 详细操作步骤见：[XSS演示场景说明.md](XSS演示场景说明.md)

## 快速体验

```bash
# 场景 1：基础弹窗
http://localhost:5173/search?q=%3Cimg%20src%3Dx%20onerror%3Dalert(1)%3E

# 场景 3-5：需按文档步骤操作
```

💡 **重要提示**：Vue 中通过 `v-html` 插入的 `<script>` 标签不会执行，需使用事件处理器型 payload（如 `onerror`、`onload`）。

## 技术栈
- 前端：Vue 3、Vite、Element Plus、Pinia、Axios
- 后端：Spring Boot 3、Spring Security、JPA/Hibernate、MySQL 8
- 安全：HttpOnly Cookie、HtmlUtils 转义、DOMPurify 白名单（仅场景 4）

## 参考
- XSS 场景说明：`XSS演示场景说明.md`
- OWASP XSS 防御指南
- Content Security Policy (CSP)

## 免责声明

本项目仅用于授权的安全研究与教学。使用者须遵守法律法规，产生的后果自负。

**使用前请确认**：
- ✅ 已获得授权
- ✅ 仅在隔离环境中运行
- ✅ 不连接公网
- ✅ 理解其中的安全风险
