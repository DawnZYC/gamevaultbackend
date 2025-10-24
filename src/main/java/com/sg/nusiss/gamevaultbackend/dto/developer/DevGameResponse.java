package com.sg.nusiss.gamevaultbackend.dto.developer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DevGameResponse {
    private String id;
    private String name;
    private String description;
    private String coverImageUrl;
    private String videoUrl;
    private String zipUrl;
}
