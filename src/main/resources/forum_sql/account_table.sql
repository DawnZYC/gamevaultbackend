-- ========================================
-- 创建认证账户表
-- 请在 PostgreSQL 中执行此脚本
-- ========================================

-- 1. 创建 accounts 表（独立的认证表）
CREATE TABLE IF NOT EXISTS accounts (
                                        account_id SERIAL PRIMARY KEY,
                                        username VARCHAR(50) UNIQUE NOT NULL,
                                        password VARCHAR(100) NOT NULL,  -- 明文密码（仅测试用）
                                        user_id BIGINT,  -- 关联到 users 表
                                        created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,


    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 2. 创建索引
CREATE INDEX IF NOT EXISTS idx_accounts_username ON accounts(username);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);

-- 3. 添加注释
COMMENT ON TABLE accounts IS '认证账户表';
COMMENT ON COLUMN accounts.account_id IS '账户ID';
COMMENT ON COLUMN accounts.username IS '登录用户名';
COMMENT ON COLUMN accounts.password IS '密码（明文）';
COMMENT ON COLUMN accounts.user_id IS '关联的用户ID';
COMMENT ON COLUMN accounts.created_date IS '创建时间';

-- 4. 插入测试数据（可选）
-- 注意：需要先在 users 表中有对应的记录
INSERT INTO accounts (username, password, user_id) VALUES
                                                       ('admin', 'admin123', 1),
                                                       ('test', 'test123', 2),
                                                       ('demo', 'demo123', 3)
ON CONFLICT (username) DO NOTHING;

-- 5. 查看表结构（验证用）
-- \d accounts

-- 6. 查看数据（验证用）
-- SELECT * FROM accounts;