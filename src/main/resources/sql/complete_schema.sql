-- ================================================================
-- GameVault 完整数据库结构
-- 包含核心业务表和论坛功能表
-- ================================================================

-- 设置基本配置
SET timezone = 'Asia/Shanghai';
SET client_encoding = 'UTF8';

-- ================================================================
-- 1. 核心业务表
-- ================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar_url VARCHAR(255),
    bio TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP
);

-- 游戏表
CREATE TABLE IF NOT EXISTS games (
    game_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL
);

-- 订单项表
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    discount_price NUMERIC(10, 2),
    order_date TIMESTAMP NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_order_items_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_game FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE RESTRICT
);

-- 激活码表
CREATE TABLE IF NOT EXISTS purchased_game_activation_code (
    activation_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL UNIQUE,
    game_id BIGINT NOT NULL,
    activation_code VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT fk_pgac_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_pgac_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id) ON DELETE CASCADE,
    CONSTRAINT fk_pgac_game FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE RESTRICT,
    CONSTRAINT chk_pgac_activation_code_len CHECK (char_length(activation_code) >= 16)
);

-- ================================================================
-- 2. 论坛功能表
-- ================================================================

-- 内容表（通用内容实体，支持帖子、回复、评论等）
CREATE TABLE IF NOT EXISTS contents (
    content_id BIGSERIAL PRIMARY KEY,
    content_type VARCHAR(20) NOT NULL,
    title VARCHAR(200),
    body TEXT NOT NULL,
    body_plain TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT,
    status VARCHAR(20) DEFAULT 'active',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contents_author FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_contents_parent FOREIGN KEY (parent_id) REFERENCES contents(content_id) ON DELETE CASCADE
);

-- 属性定义表（定义可用的属性类型）
CREATE TABLE IF NOT EXISTS attribute_definitions (
    attr_id SERIAL PRIMARY KEY,
    attr_name VARCHAR(50) UNIQUE NOT NULL,
    attr_type VARCHAR(20) NOT NULL,
    description TEXT,
    is_required BOOLEAN DEFAULT false,
    default_value TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 内容属性表（存储具体的属性值）
CREATE TABLE IF NOT EXISTS content_attributes (
    id SERIAL PRIMARY KEY,
    content_id BIGINT,
    attr_id INTEGER,
    attr_value TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_attrs_content FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE,
    CONSTRAINT fk_content_attrs_attr FOREIGN KEY (attr_id) REFERENCES attribute_definitions(attr_id) ON DELETE CASCADE,
    UNIQUE(content_id, attr_id)
);

-- 统计类型定义
CREATE TABLE IF NOT EXISTS metric_definitions (
    metric_id SERIAL PRIMARY KEY,
    metric_name VARCHAR(50) UNIQUE NOT NULL,
    metric_type VARCHAR(20) NOT NULL,
    description TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 内容统计表
CREATE TABLE IF NOT EXISTS content_metrics (
    id SERIAL PRIMARY KEY,
    content_id BIGINT,
    metric_id INTEGER,
    metric_value INTEGER DEFAULT 0,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_metrics_content FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE,
    CONSTRAINT fk_content_metrics_metric FOREIGN KEY (metric_id) REFERENCES metric_definitions(metric_id) ON DELETE CASCADE,
    UNIQUE(content_id, metric_id)
);

-- 关系类型定义
CREATE TABLE IF NOT EXISTS relationship_types (
    type_id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户-内容关系表
CREATE TABLE IF NOT EXISTS user_content_relations (
    id SERIAL PRIMARY KEY,
    user_id BIGINT,
    content_id BIGINT,
    relation_type_id INTEGER,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ucr_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ucr_content FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE,
    CONSTRAINT fk_ucr_type FOREIGN KEY (relation_type_id) REFERENCES relationship_types(type_id) ON DELETE CASCADE,
    UNIQUE(user_id, content_id, relation_type_id)
);

-- ================================================================
-- 3. 创建索引
-- ================================================================

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- 订单项表索引
CREATE INDEX IF NOT EXISTS idx_order_items_user_id ON order_items(user_id);
CREATE INDEX IF NOT EXISTS idx_order_items_game_id ON order_items(game_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_date ON order_items(order_date DESC);

-- 激活码表索引
CREATE INDEX IF NOT EXISTS idx_pgac_user_id ON purchased_game_activation_code(user_id);
CREATE INDEX IF NOT EXISTS idx_pgac_game_id ON purchased_game_activation_code(game_id);

-- 内容表索引
CREATE INDEX IF NOT EXISTS idx_contents_type ON contents(content_type);
CREATE INDEX IF NOT EXISTS idx_contents_author ON contents(author_id);
CREATE INDEX IF NOT EXISTS idx_contents_parent ON contents(parent_id);
CREATE INDEX IF NOT EXISTS idx_contents_status ON contents(status);
CREATE INDEX IF NOT EXISTS idx_contents_created ON contents(created_date DESC);
CREATE INDEX IF NOT EXISTS idx_contents_type_status ON contents(content_type, status);

-- 属性查询索引
CREATE INDEX IF NOT EXISTS idx_content_attrs_content ON content_attributes(content_id);
CREATE INDEX IF NOT EXISTS idx_content_attrs_attr ON content_attributes(attr_id);

-- 统计查询索引
CREATE INDEX IF NOT EXISTS idx_content_metrics_content ON content_metrics(content_id);
CREATE INDEX IF NOT EXISTS idx_content_metrics_metric ON content_metrics(metric_id);

-- 关系查询索引
CREATE INDEX IF NOT EXISTS idx_relations_user ON user_content_relations(user_id);
CREATE INDEX IF NOT EXISTS idx_relations_content ON user_content_relations(content_id);
CREATE INDEX IF NOT EXISTS idx_relations_type ON user_content_relations(relation_type_id);

-- ================================================================
-- 4. 创建触发器
-- ================================================================

-- 用户表更新时间触发器函数
CREATE OR REPLACE FUNCTION trg_users_set_updated_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_date = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS set_users_updated_date ON users;
CREATE TRIGGER set_users_updated_date
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION trg_users_set_updated_date();

-- ================================================================
-- 5. 初始化系统数据
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
-- 6. 创建视图和函数
-- ================================================================

-- 帖子列表视图
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

-- 增加统计指标的函数
CREATE OR REPLACE FUNCTION increment_metric(p_content_id BIGINT, p_metric_name VARCHAR)
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
    p_content_id BIGINT,
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

