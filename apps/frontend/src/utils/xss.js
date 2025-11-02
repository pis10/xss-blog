import DOMPurify from 'dompurify';

/**
 * 使用 DOMPurify 按白名单净化 HTML
 * SECURE 模式下用于阻止 XSS 执行
 */
export const pure = (html) => {
  if (!html) return '';
  
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'b', 'i', 'em', 'strong', 'a', 'p', 'code', 'pre', 
      'ul', 'ol', 'li', 'img', 'h1', 'h2', 'h3', 'h4', 
      'blockquote', 'br', 'hr', 'span', 'div'
    ],
    ALLOWED_ATTR: {
      'a': ['href', 'title', 'target', 'rel'],
      'img': ['src', 'alt', 'width', 'height'],
      'code': ['class'],
      'pre': ['class'],
      'span': ['class'],
      'div': ['class']
    },
    ALLOWED_URI_REGEXP: /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|cid|xmpp):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i
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
