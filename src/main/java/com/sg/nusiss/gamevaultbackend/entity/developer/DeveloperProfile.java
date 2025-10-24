package com.sg.nusiss.gamevaultbackend.entity.developer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeveloperProfile {
    private String id;
    private String userId;
    private Integer projectCount;
}
