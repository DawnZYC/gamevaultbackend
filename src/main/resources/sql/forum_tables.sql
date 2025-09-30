-- 论坛相关表结构
-- 这个脚本用于创建论坛功能所需的数据库表

-- 创建论坛帖子表
CREATE TABLE IF NOT EXISTS content (
    content_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    author_id BIGINT NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    view_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    
    -- 外键约束（引用现有的users表）
    CONSTRAINT fk_content_author FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_content_author_id ON content(author_id);
CREATE INDEX IF NOT EXISTS idx_content_status ON content(status);
CREATE INDEX IF NOT EXISTS idx_content_created_date ON content(created_date);
CREATE INDEX IF NOT EXISTS idx_content_title ON content(title);
CREATE INDEX IF NOT EXISTS idx_content_body ON content(body);

-- 创建全文搜索索引（用于搜索功能）
CREATE INDEX IF NOT EXISTS idx_content_search ON content USING gin(to_tsvector('english', title || ' ' || body));

-- 添加注释
COMMENT ON TABLE content IS '论坛帖子表';
COMMENT ON COLUMN content.content_id IS '帖子ID';
COMMENT ON COLUMN content.title IS '帖子标题';
COMMENT ON COLUMN content.body IS '帖子内容';
COMMENT ON COLUMN content.author_id IS '作者ID';
COMMENT ON COLUMN content.created_date IS '创建时间';
COMMENT ON COLUMN content.updated_date IS '更新时间';
COMMENT ON COLUMN content.view_count IS '浏览次数';
COMMENT ON COLUMN content.status IS '状态：active, deleted';
