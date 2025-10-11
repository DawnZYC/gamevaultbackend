package com.sg.nusiss.gamevaultbackend.dto.shopping;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GameDTO {
    private Long gameId;
    private String title;
    private String developer;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String genre;       // 类型（RPG、动作等）
    private String platform;    // 平台（PC、Xbox、PS 等）
    private LocalDate releaseDate;
    private Boolean isActive;
    private String imageUrl;    // 游戏图片
}
