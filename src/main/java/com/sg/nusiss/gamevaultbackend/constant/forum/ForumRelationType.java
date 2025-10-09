package com.sg.nusiss.gamevaultbackend.constant.forum;

/**
 * 关系类型常量
 * 对应 relationship_types 表中的数据
 *
 * 注意：这些ID需要与数据库中的实际ID保持一致
 * 可以通过查询 SELECT * FROM relationship_types; 来确认
 */
public class ForumRelationType {

    /**
     * 点赞关系
     * 对应 relationship_types 表中 type_name = 'like' 的记录
     */
    public static final Long LIKE = 1L;

    /**
     * 收藏关系
     * 对应 relationship_types 表中 type_name = 'bookmark' 的记录
     */
    public static final Long BOOKMARK = 2L;

    /**
     * 关注关系
     * 对应 relationship_types 表中 type_name = 'follow' 的记录
     */
    public static final Long FOLLOW = 3L;

    /**
     * 举报关系
     * 对应 relationship_types 表中 type_name = 'report' 的记录
     */
    public static final Long REPORT = 4L;

    /**
     * 浏览关系
     * 对应 relationship_types 表中 type_name = 'view' 的记录
     */
    public static final Long VIEW = 5L;

    // 私有构造函数，防止实例化
    private ForumRelationType() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /**
     * 根据类型名称获取ID（备用方法）
     *
     * @param typeName 类型名称
     * @return 类型ID，如果不存在返回null
     */
    public static Long getIdByName(String typeName) {
        switch (typeName.toLowerCase()) {
            case "like":
                return LIKE;
            case "bookmark":
                return BOOKMARK;
            case "follow":
                return FOLLOW;
            case "report":
                return REPORT;
            case "view":
                return VIEW;
            default:
                return null;
        }
    }

    /**
     * 根据ID获取类型名称
     *
     * @param typeId 类型ID
     * @return 类型名称，如果不存在返回null
     */
    public static String getNameById(Long typeId) {
        if (LIKE.equals(typeId)) return "like";
        if (BOOKMARK.equals(typeId)) return "bookmark";
        if (FOLLOW.equals(typeId)) return "follow";
        if (REPORT.equals(typeId)) return "report";
        if (VIEW.equals(typeId)) return "view";
        return null;
    }
}

