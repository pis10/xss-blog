-- Drop tables if exists (for clean restart)
DROP TABLE IF EXISTS feedbacks;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS article_tags;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    avatar_url VARCHAR(255),
    banner_url VARCHAR(255),
    bio TEXT,  -- 支持较长的个人简介和XSS演示代码
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),  -- 新增：按角色查询优化
    INDEX idx_created_at (created_at)  -- 新增：按注册时间查询优化
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create articles table
CREATE TABLE articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    slug VARCHAR(160) UNIQUE,
    excerpt VARCHAR(240),
    content_html LONGTEXT,
    likes_count INT NOT NULL DEFAULT 0,
    published_at DATETIME,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_slug (slug),
    INDEX idx_author_id (author_id),
    INDEX idx_published_at (published_at),
    INDEX idx_author_published (author_id, published_at),  -- 新增：组合索引，优化按作者查询
    INDEX idx_likes_count (likes_count)  -- 新增：按点赞数排序优化
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create tags table
CREATE TABLE tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(20),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create article_tags junction table
CREATE TABLE article_tags (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create comments table
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content_html TEXT,  -- 与 CommentRequest(2000) 一致
    created_at DATETIME NOT NULL,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_article_created (article_id, created_at),
    INDEX idx_user_id (user_id)  -- 新增：按用户查询优化
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create feedbacks table
CREATE TABLE feedbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    content_html VARCHAR(5000),  -- 限制长度，与DTO校验一致
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at DATETIME NOT NULL,
    INDEX idx_status_created (status, created_at),
    INDEX idx_email (email)  -- 新增：按邮箱查询优化
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
