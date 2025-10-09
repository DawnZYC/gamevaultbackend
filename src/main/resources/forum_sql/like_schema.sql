-- ================================================================
-- 1. 创建点赞关系表（利用现有的 user_content_relations）
-- ================================================================

-- 注意：你已经有 relationship_types 表，并且已经插入了 'like' 类型
-- 所以我们直接使用现有的 user_content_relations 表即可！

-- 但为了性能优化，我们额外创建一个专门的索引
CREATE INDEX IF NOT EXISTS idx_relations_like
    ON user_content_relations(user_id, content_id)
    WHERE relation_type_id = (SELECT type_id FROM relationship_types WHERE type_name = 'like');

-- ================================================================
-- 2. 创建触发器函数 - 自动维护点赞数
-- ================================================================

-- 点赞时增加计数
CREATE OR REPLACE FUNCTION increment_like_count()
    RETURNS TRIGGER AS $$
DECLARE
    like_metric_id INTEGER;
BEGIN
    -- 只处理 'like' 类型的关系
    IF NEW.relation_type_id = (SELECT type_id FROM relationship_types WHERE type_name = 'like') THEN
        -- 获取 like_count 的 metric_id
        SELECT metric_id INTO like_metric_id
        FROM metric_definitions
        WHERE metric_name = 'like_count';

        -- 插入或更新点赞数
        INSERT INTO content_metrics (content_id, metric_id, metric_value, updated_date)
        VALUES (NEW.content_id, like_metric_id, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (content_id, metric_id)
            DO UPDATE SET
                          metric_value = content_metrics.metric_value + 1,
                          updated_date = CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 取消点赞时减少计数
CREATE OR REPLACE FUNCTION decrement_like_count()
    RETURNS TRIGGER AS $$
DECLARE
    like_metric_id INTEGER;
BEGIN
    -- 只处理 'like' 类型的关系
    IF OLD.relation_type_id = (SELECT type_id FROM relationship_types WHERE type_name = 'like') THEN
        -- 获取 like_count 的 metric_id
        SELECT metric_id INTO like_metric_id
        FROM metric_definitions
        WHERE metric_name = 'like_count';

        -- 减少点赞数（防止变成负数）
        UPDATE content_metrics
        SET
            metric_value = GREATEST(metric_value - 1, 0),
            updated_date = CURRENT_TIMESTAMP
        WHERE content_id = OLD.content_id
          AND metric_id = like_metric_id;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- ================================================================
-- 3. 创建触发器
-- ================================================================

-- 删除旧触发器（如果存在）
DROP TRIGGER IF EXISTS trigger_increment_like ON user_content_relations;
DROP TRIGGER IF EXISTS trigger_decrement_like ON user_content_relations;

-- 创建新触发器
CREATE TRIGGER trigger_increment_like
    AFTER INSERT ON user_content_relations
    FOR EACH ROW
EXECUTE FUNCTION increment_like_count();

CREATE TRIGGER trigger_decrement_like
    AFTER DELETE ON user_content_relations
    FOR EACH ROW
EXECUTE FUNCTION decrement_like_count();

-- ================================================================
-- 4. 创建便捷视图（可选，方便查询）
-- ================================================================

-- 点赞详情视图
CREATE OR REPLACE VIEW user_likes_view AS
SELECT
    ucr.id as relation_id,
    ucr.user_id,
    u.username,
    u.nickname,
    ucr.content_id,
    c.title as content_title,
    c.content_type,
    ucr.created_date as liked_at
FROM user_content_relations ucr
         JOIN users u ON ucr.user_id = u.user_id
         JOIN contents c ON ucr.content_id = c.content_id
WHERE ucr.relation_type_id = (SELECT type_id FROM relationship_types WHERE type_name = 'like')
ORDER BY ucr.created_date DESC;

-- 内容点赞统计视图
CREATE OR REPLACE VIEW content_likes_stats AS
SELECT
    c.content_id,
    c.content_type,
    c.title,
    c.author_id,
    COALESCE(cm.metric_value, 0) as like_count,
    COUNT(ucr.id) as actual_like_count  -- 实际关系表中的点赞数（用于验证）
FROM contents c
         LEFT JOIN content_metrics cm ON (
    c.content_id = cm.content_id
        AND cm.metric_id = (SELECT metric_id FROM metric_definitions WHERE metric_name = 'like_count')
    )
         LEFT JOIN user_content_relations ucr ON (
    c.content_id = ucr.content_id
        AND ucr.relation_type_id = (SELECT type_id FROM relationship_types WHERE type_name = 'like')
    )
WHERE c.status = 'active'
GROUP BY c.content_id, c.content_type, c.title, c.author_id, cm.metric_value
ORDER BY like_count DESC;

-- ================================================================
-- 5. 创建便捷函数
-- ================================================================

-- 点赞函数（返回是否成功）
CREATE OR REPLACE FUNCTION like_content(p_user_id INTEGER, p_content_id INTEGER)
    RETURNS BOOLEAN AS $$
DECLARE
    like_type_id INTEGER;
    already_liked BOOLEAN;
BEGIN
    -- 获取 'like' 类型的 ID
    SELECT type_id INTO like_type_id
    FROM relationship_types
    WHERE type_name = 'like';

    -- 检查是否已点赞
    SELECT EXISTS(
        SELECT 1 FROM user_content_relations
        WHERE user_id = p_user_id
          AND content_id = p_content_id
          AND relation_type_id = like_type_id
    ) INTO already_liked;

    IF already_liked THEN
        RETURN FALSE;  -- 已经点赞过
    END IF;

    -- 插入点赞记录（触发器会自动更新计数）
    INSERT INTO user_content_relations (user_id, content_id, relation_type_id, created_date)
    VALUES (p_user_id, p_content_id, like_type_id, CURRENT_TIMESTAMP);

    RETURN TRUE;  -- 点赞成功
EXCEPTION
    WHEN unique_violation THEN
        RETURN FALSE;  -- 并发情况下可能重复，返回失败
END;
$$ LANGUAGE plpgsql;

-- 取消点赞函数
CREATE OR REPLACE FUNCTION unlike_content(p_user_id INTEGER, p_content_id INTEGER)
    RETURNS BOOLEAN AS $$
DECLARE
    like_type_id INTEGER;
    delete_count INTEGER;
BEGIN
    -- 获取 'like' 类型的 ID
    SELECT type_id INTO like_type_id
    FROM relationship_types
    WHERE type_name = 'like';

    -- 删除点赞记录（触发器会自动更新计数）
    DELETE FROM user_content_relations
    WHERE user_id = p_user_id
      AND content_id = p_content_id
      AND relation_type_id = like_type_id;

    GET DIAGNOSTICS delete_count = ROW_COUNT;

    RETURN delete_count > 0;  -- 返回是否删除成功
END;
$$ LANGUAGE plpgsql;

-- 切换点赞状态函数
CREATE OR REPLACE FUNCTION toggle_like(p_user_id INTEGER, p_content_id INTEGER)
    RETURNS BOOLEAN AS $$
DECLARE
    like_type_id INTEGER;
    is_liked BOOLEAN;
BEGIN
    -- 获取 'like' 类型的 ID
    SELECT type_id INTO like_type_id
    FROM relationship_types
    WHERE type_name = 'like';

    -- 检查当前状态
    SELECT EXISTS(
        SELECT 1 FROM user_content_relations
        WHERE user_id = p_user_id
          AND content_id = p_content_id
          AND relation_type_id = like_type_id
    ) INTO is_liked;

    IF is_liked THEN
        -- 已点赞，取消点赞
        PERFORM unlike_content(p_user_id, p_content_id);
        RETURN FALSE;
    ELSE
        -- 未点赞，执行点赞
        PERFORM like_content(p_user_id, p_content_id);
        RETURN TRUE;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 获取点赞数函数
CREATE OR REPLACE FUNCTION get_like_count(p_content_id INTEGER)
    RETURNS INTEGER AS $$
DECLARE
    count_value INTEGER;
BEGIN
    SELECT metric_value INTO count_value
    FROM content_metrics
    WHERE content_id = p_content_id
      AND metric_id = (SELECT metric_id FROM metric_definitions WHERE metric_name = 'like_count');

    RETURN COALESCE(count_value, 0);
END;
$$ LANGUAGE plpgsql;

-- 检查是否已点赞函数
CREATE OR REPLACE FUNCTION is_liked(p_user_id INTEGER, p_content_id INTEGER)
    RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS(
        SELECT 1 FROM user_content_relations
        WHERE user_id = p_user_id
          AND content_id = p_content_id
          AND relation_type_id = (SELECT type_id FROM relationship_types WHERE type_name = 'like')
    );
END;
$$ LANGUAGE plpgsql;

-- ================================================================
-- 6. 数据同步函数（修复不一致）
-- ================================================================

-- 同步所有内容的点赞数
CREATE OR REPLACE FUNCTION sync_all_like_counts()
    RETURNS INTEGER AS $$
DECLARE
    like_type_id INTEGER;
    like_metric_id INTEGER;
    sync_count INTEGER := 0;
BEGIN
    -- 获取 ID
    SELECT type_id INTO like_type_id
    FROM relationship_types
    WHERE type_name = 'like';

    SELECT metric_id INTO like_metric_id
    FROM metric_definitions
    WHERE metric_name = 'like_count';

    -- 同步点赞数
    INSERT INTO content_metrics (content_id, metric_id, metric_value, updated_date)
    SELECT
        content_id,
        like_metric_id,
        COUNT(*) as like_count,
        CURRENT_TIMESTAMP
    FROM user_content_relations
    WHERE relation_type_id = like_type_id
    GROUP BY content_id
    ON CONFLICT (content_id, metric_id)
        DO UPDATE SET
                      metric_value = EXCLUDED.metric_value,
                      updated_date = EXCLUDED.updated_date;

    GET DIAGNOSTICS sync_count = ROW_COUNT;

    RETURN sync_count;
END;
$$ LANGUAGE plpgsql;

-- ================================================================
-- 7. 测试数据（可选）
-- ================================================================

-- 添加一些测试点赞
DO $$
    DECLARE
        like_type_id INTEGER;
    BEGIN
        -- 获取 'like' 类型的 ID
        SELECT type_id INTO like_type_id
        FROM relationship_types
        WHERE type_name = 'like';

        -- 测试点赞（如果用户和内容存在的话）
        IF EXISTS(SELECT 1 FROM users WHERE user_id = 2) AND
           EXISTS(SELECT 1 FROM contents WHERE content_id = 1) THEN
            INSERT INTO user_content_relations (user_id, content_id, relation_type_id)
            VALUES (2, 1, like_type_id)
            ON CONFLICT DO NOTHING;
        END IF;

        IF EXISTS(SELECT 1 FROM users WHERE user_id = 3) AND
           EXISTS(SELECT 1 FROM contents WHERE content_id = 1) THEN
            INSERT INTO user_content_relations (user_id, content_id, relation_type_id)
            VALUES (3, 1, like_type_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END $$;

-- ================================================================
-- 8. 验证输出
-- ================================================================

DO $$
    DECLARE
        like_type_id INTEGER;
        total_likes INTEGER;
    BEGIN
        SELECT type_id INTO like_type_id
        FROM relationship_types
        WHERE type_name = 'like';

        SELECT COUNT(*) INTO total_likes
        FROM user_content_relations
        WHERE relation_type_id = like_type_id;

        RAISE NOTICE '========================================';
        RAISE NOTICE 'GameVault 点赞功能初始化完成！';
        RAISE NOTICE '========================================';
        RAISE NOTICE '使用现有表: user_content_relations';
        RAISE NOTICE '点赞类型ID: %', like_type_id;
        RAISE NOTICE '当前点赞总数: %', total_likes;
        RAISE NOTICE '========================================';
        RAISE NOTICE '可用函数:';
        RAISE NOTICE '  - like_content(user_id, content_id)';
        RAISE NOTICE '  - unlike_content(user_id, content_id)';
        RAISE NOTICE '  - toggle_like(user_id, content_id)';
        RAISE NOTICE '  - get_like_count(content_id)';
        RAISE NOTICE '  - is_liked(user_id, content_id)';
        RAISE NOTICE '========================================';
    END $$;