package com.sg.nusiss.gamevaultbackend.dto.developer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevGameListResponse {
    private List<DevGameSummaryResponse> games;
    private int currentPage;
    private int pageSize;
    private long totalCount;
    private int totalPages;
}
