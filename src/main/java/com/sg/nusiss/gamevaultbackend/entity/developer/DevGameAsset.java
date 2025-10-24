package com.sg.nusiss.gamevaultbackend.entity.developer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevGameAsset {
    private String id;
    private String devGameId;
    private String assetType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
}
