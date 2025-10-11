package com.sg.nusiss.gamevaultbackend.entity.shopping;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 游戏实体类，对应数据库中的 games 表
 */
@Data // Lombok 注解，自动生成 getter/setter/toString 等方法
@Entity // 声明这是一个 JPA 实体
@Table(name = "games") // 指定映射的表名
public class Game {

    @Id // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键
    @Column(name = "game_id") // 对应数据库字段
    private Long gameId;

    @Column(name = "title", nullable = false, length = 255) // 非空，最大长度 200
    private String title; // 游戏标题

    @Column(length = 255)
    private String developer; // 开发者

    @Column(columnDefinition = "text")
    private String description; // 游戏描述

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price")
    private BigDecimal discountPrice; // 折扣价

    @Column(length = 100)
    private String genre; // 类型（RPG、动作等）

    @Column(length = 100)
    private String platform; // 平台（PC、Xbox、PS 等）

    @Column(name = "release_date")
    private LocalDate releaseDate; // 发售日期

    @Column(name = "is_active")
    private Boolean isActive; // 是否上架

    @Column(name = "image_url")
    private String imageUrl; // 游戏图片

    // 方法：是否有折扣
    public boolean isOnSale() {
        return discountPrice != null
                && discountPrice.compareTo(BigDecimal.ZERO) > 0
                && discountPrice.compareTo(price) < 0;
    }

    // 方法：返回当前有效价格
    public BigDecimal getCurrentPrice() {
        return isOnSale() ? discountPrice : price;
    }

    // 构造函数，方便测试快速创建对象
    public Game(String title, String developer, BigDecimal price, String imageUrl) {
        this.title = title;
        this.developer = developer;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isActive = true;
    }

    // JPA 默认需要无参构造函数
    public Game() {}
}
