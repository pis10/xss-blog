import DOMPurify from 'dompurify';

/**
 * 使用 DOMPurify 按白名单净化 HTML
 * SECURE 模式下用于阻止 XSS 执行
 * 
 * 安全增强版：
 * - 禁用 SVG/MathML（防止复杂攻击）
 * - 显式禁止危险标签和属性
 * - 更严格的 URL 协议限制
 */
export const pure = (html) => {
  if (!html) return '';

  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true }, 
    
    ALLOWED_TAGS: [
      'b','i','em','strong','a','p','code','pre',
      'ul','ol','li','h1','h2','h3','h4',
      'blockquote','br','hr','span','div'
    ],
    
    ALLOWED_ATTR: ['href','title','target','rel','class'],
    
    FORBID_ATTR: ['on*','style','srcset','formaction'],
    
    FORBID_TAGS: [
      'img','iframe','object','embed','form','input','textarea',
      'button','select','canvas','video','audio','link','base'
    ],
    
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto|tel):|\/(?!\/)|#)/i
  });
};

/**
 * 获取当前 XSS 模式
 * 优先读取 window.__XSS_MODE__，否则回落到环境变量
 */
export const getXssMode = () => {
  // 如已注入全局模式（由配置接口返回），优先使用
  if (typeof window !== 'undefined' && window.__XSS_MODE__) {
    return window.__XSS_MODE__;
  }
  return import.meta.env.VITE_XSS_MODE || 'vuln';
};

/** 判断是否为 VULN 模式 */
export const isVulnMode = () => {
  return getXssMode() === 'vuln';
};

/** 判断是否为 SECURE 模式 */
export const isSecureMode = () => {
  return getXssMode() === 'secure';
};

/**
 * 增强链接安全性（渲染后调用）
 * 为所有 <a> 标签添加安全属性
 * 
 * @param {HTMLElement} el - 包含链接的容器元素
 */
export function enforceLinkSafety(el) {
  if (!el) return;
  
  for (const a of el.querySelectorAll('a')) {
    // 在新标签页打开
    if (!a.hasAttribute('target')) {
      a.setAttribute('target', '_blank');
    }
    
    // 确保 rel 包含必要的安全值
    const rel = a.getAttribute('rel') || '';
    const relValues = new Set(rel.split(/\s+/).filter(Boolean));
    relValues.add('noopener');    // 防止 window.opener 攻击
    relValues.add('noreferrer');  // 不发送 Referer 头
    relValues.add('nofollow');    // SEO 优化
    a.setAttribute('rel', Array.from(relValues).join(' '));
  }
}
