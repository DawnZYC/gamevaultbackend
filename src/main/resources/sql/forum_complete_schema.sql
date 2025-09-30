-- ================================================================
-- GameVault 论坛 - 完整数据库结构
-- 基于你伙伴的 init.sql 设计
-- ================================================================

-- 设置基本配置
SET timezone = 'Asia/Shanghai';
SET client_encoding = 'UTF8';

-- ================================================================
-- 1. 核心实体表（稳定结构，很少变更）
-- ================================================================

-- 用户表（如果已存在则跳过）
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    nickname VARCHAR(100),                    -- 显示名称（可选）
    avatar_url VARCHAR(255),                  -- 头像地址（可选）
    bio TEXT,                                 -- 用户简介（可选）
    status VARCHAR(20) DEFAULT 'active',     -- 用户状态：active, banned, inactive
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 内容表（通用内容实体，支持帖子、回复、评论等）
CREATE TABLE IF NOT EXISTS contents (
    content_id SERIAL PRIMARY KEY,
    content_type VARCHAR(20) NOT NULL,       -- 'post', 'reply', 'comment', 'review' 等
    title VARCHAR(200),                      -- 标题（帖子有，回复可能没有）
    body TEXT NOT NULL,                      -- 主体内容
    body_plain TEXT NOT NULL,                -- 纯文本内容
    author_id INTEGER REFERENCES users(user_id) ON DELETE CASCADE,
    parent_id INTEGER REFERENCES contents(content_id) ON DELETE CASCADE, -- 支持层级结构
    status VARCHAR(20) DEFAULT 'active',     -- 'active', 'deleted', 'hidden', 'pending'
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- 2. 灵活属性表（EAV 模式 - 扩展属性无需改表结构）
-- ================================================================

-- 属性定义表（定义可用的属性类型）
CREATE TABLE IF NOT EXISTS attribute_definitions (
    attr_id SERIAL PRIMARY KEY,
    attr_name VARCHAR(50) UNIQUE NOT NULL,   -- 'category', 'forum', 'priority', 'tags' 等
    attr_type VARCHAR(20) NOT NULL,          -- 'string', 'integer', 'boolean', 'json'
    description TEXT,
    is_required BOOLEAN DEFAULT false,
    default_value TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 内容属性表（存储具体的属性值）
CREATE TABLE IF NOT EXISTS content_attributes (
    id SERIAL PRIMARY KEY,
    content_id INTEGER REFERENCES contents(content_id) ON DELETE CASCADE,
    attr_id INTEGER REFERENCES attribute_definitions(attr_id) ON DELETE CASCADE,
    attr_value TEXT,                         -- 统一用文本存储，应用层转换类型
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(content_id, attr_id)              -- 一个内容的同一属性只能有一个值
);

-- ================================================================
-- 3. 计数统计表（策略模式 - 支持各种统计类型）
-- ================================================================

-- 统计类型定义
CREATE TABLE IF NOT EXISTS metric_definitions (
    metric_id SERIAL PRIMARY KEY,
    metric_name VARCHAR(50) UNIQUE NOT NULL, -- 'view_count', 'like_count', 'reply_count', 'share_count'
    metric_type VARCHAR(20) NOT NULL,        -- 'counter', 'gauge', 'score'
    description TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 内容统计表
CREATE TABLE IF NOT EXISTS content_metrics (
    id SERIAL PRIMARY KEY,
    content_id INTEGER REFERENCES contents(content_id) ON DELETE CASCADE,
    metric_id INTEGER REFERENCES metric_definitions(metric_id) ON DELETE CASCADE,
    metric_value INTEGER DEFAULT 0,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(content_id, metric_id)
);

-- ================================================================
-- 4. 关系表（支持各种关联关系）
-- ================================================================

-- 关系类型定义
CREATE TABLE IF NOT EXISTS relationship_types (
    type_id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) UNIQUE NOT NULL,   -- 'like', 'follow', 'bookmark', 'report'
    description TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户-内容关系表
CREATE TABLE IF NOT EXISTS user_content_relations (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(user_id) ON DELETE CASCADE,
    content_id INTEGER REFERENCES contents(content_id) ON DELETE CASCADE,
    relation_type_id INTEGER REFERENCES relationship_types(type_id) ON DELETE CASCADE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, content_id, relation_type_id)
);

-- ================================================================
-- 5. 创建索引
-- ================================================================

-- 核心查询索引
CREATE INDEX IF NOT EXISTS idx_contents_type ON contents(content_type);
CREATE INDEX IF NOT EXISTS idx_contents_author ON contents(author_id);
CREATE INDEX IF NOT EXISTS idx_contents_parent ON contents(parent_id);
CREATE INDEX IF NOT EXISTS idx_contents_status ON contents(status);
CREATE INDEX IF NOT EXISTS idx_contents_created ON contents(created_date DESC);
CREATE INDEX IF NOT EXISTS idx_contents_type_status ON contents(content_type, status);

-- 属性查询索引
CREATE INDEX IF NOT EXISTS idx_content_attrs_content ON content_attributes(content_id);
CREATE INDEX IF NOT EXISTS idx_content_attrs_attr ON content_attributes(attr_id);
CREATE INDEX IF NOT EXISTS idx_content_attrs_value ON content_attributes(attr_value);

-- 统计查询索引
CREATE INDEX IF NOT EXISTS idx_content_metrics_content ON content_metrics(content_id);
CREATE INDEX IF NOT EXISTS idx_content_metrics_metric ON content_metrics(metric_id);

-- 关系查询索引
CREATE INDEX IF NOT EXISTS idx_relations_user ON user_content_relations(user_id);
CREATE INDEX IF NOT EXISTS idx_relations_content ON user_content_relations(content_id);
CREATE INDEX IF NOT EXISTS idx_relations_type ON user_content_relations(relation_type_id);

-- ================================================================
-- 6. 初始化系统数据
-- ================================================================

-- 初始化属性定义
INSERT INTO attribute_definitions (attr_name, attr_type, description, is_required) VALUES
('category', 'string', '帖子分类', false),
('forum', 'string', '所属论坛', false),
('tags', 'json', '标签列表', false),
('priority', 'integer', '优先级', false),
('is_pinned', 'boolean', '是否置顶', false),
('is_locked', 'boolean', '是否锁定', false),
('game_title', 'string', '相关游戏名称', false),
('difficulty_level', 'integer', '难度等级', false),
('platform', 'string', '游戏平台', false)
ON CONFLICT (attr_name) DO NOTHING;

-- 初始化统计类型
INSERT INTO metric_definitions (metric_name, metric_type, description) VALUES
('view_count', 'counter', '浏览次数'),
('like_count', 'counter', '点赞数量'),
('reply_count', 'counter', '回复数量'),
('share_count', 'counter', '分享次数'),
('bookmark_count', 'counter', '收藏次数'),
('report_count', 'counter', '举报次数'),
('score', 'score', '综合评分')
ON CONFLICT (metric_name) DO NOTHING;

-- 初始化关系类型
INSERT INTO relationship_types (type_name, description) VALUES
('like', '用户点赞内容'),
('bookmark', '用户收藏内容'),
('follow', '用户关注内容'),
('report', '用户举报内容'),
('view', '用户浏览内容')
ON CONFLICT (type_name) DO NOTHING;

-- ================================================================
-- 7. 创建视图和函数
-- ================================================================

-- 帖子列表视图（隐藏复杂性，提供简单接口）
CREATE OR REPLACE VIEW post_list_view AS
SELECT
    c.content_id as post_id,
    c.title,
    c.body as content,
    c.body_plain as content_plain,
    c.created_date,
    c.status as is_active,
    u.username as author_name,
    u.user_id as author_id,
    COALESCE(ca_category.attr_value, '未分类') as category,
    COALESCE(cm_views.metric_value, 0) as view_count,
    COALESCE(cm_likes.metric_value, 0) as like_count,
    COALESCE(cm_replies.metric_value, 0) as reply_count
FROM contents c
         LEFT JOIN users u ON c.author_id = u.user_id
         LEFT JOIN content_attributes ca_category ON (
    c.content_id = ca_category.content_id
        AND ca_category.attr_id = (SELECT attr_id FROM attribute_definitions WHERE attr_name = 'category')
    )
         LEFT JOIN content_metrics cm_views ON (
    c.content_id = cm_views.content_id
        AND cm_views.metric_id = (SELECT metric_id FROM metric_definitions WHERE metric_name = 'view_count')
    )
         LEFT JOIN content_metrics cm_likes ON (
    c.content_id = cm_likes.content_id
        AND cm_likes.metric_id = (SELECT metric_id FROM metric_definitions WHERE metric_name = 'like_count')
    )
         LEFT JOIN content_metrics cm_replies ON (
    c.content_id = cm_replies.content_id
        AND cm_replies.metric_id = (SELECT metric_id FROM metric_definitions WHERE metric_name = 'reply_count')
    )
WHERE c.content_type = 'post' AND c.status = 'active'
ORDER BY c.created_date DESC;

-- 增加浏览量的函数
CREATE OR REPLACE FUNCTION increment_metric(p_content_id INTEGER, p_metric_name VARCHAR)
RETURNS void AS $$
BEGIN
INSERT INTO content_metrics (content_id, metric_id, metric_value)
VALUES (
           p_content_id,
           (SELECT metric_id FROM metric_definitions WHERE metric_name = p_metric_name),
           1
       )
    ON CONFLICT (content_id, metric_id)
    DO UPDATE SET
    metric_value = content_metrics.metric_value + 1,
               updated_date = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- 添加属性的函数
CREATE OR REPLACE FUNCTION set_content_attribute(
    p_content_id INTEGER,
    p_attr_name VARCHAR,
    p_attr_value TEXT
)
RETURNS void AS $$
BEGIN
INSERT INTO content_attributes (content_id, attr_id, attr_value)
VALUES (
           p_content_id,
           (SELECT attr_id FROM attribute_definitions WHERE attr_name = p_attr_name),
           p_attr_value
       )
    ON CONFLICT (content_id, attr_id)
    DO UPDATE SET
    attr_value = p_attr_value,
               created_date = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;
