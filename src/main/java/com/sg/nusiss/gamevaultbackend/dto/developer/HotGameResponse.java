package com.sg.nusiss.gamevaultbackend.dto.developer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotGameResponse {
    private String id;
    private String name;
    private String description;
    private String coverImageUrl;
    private double score; // 热度分数
}
