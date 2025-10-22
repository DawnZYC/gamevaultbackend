package com.sg.nusiss.gamevaultbackend.controller.developer;

import com.sg.nusiss.gamevaultbackend.dto.developer.DevDashboardDetailedResponse;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameStatisticsDashboardService;
import com.sg.nusiss.gamevaultbackend.controller.common.AuthenticatedControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/developer/dev/statistics")
@RequiredArgsConstructor
public class DevGameStatisticsController extends AuthenticatedControllerBase {

    private final DevGameStatisticsDashboardService dashboardService;

    @GetMapping("/dashboard/me")
    public ResponseEntity<DevDashboardDetailedResponse> getMyDashboard(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        DevDashboardDetailedResponse response = dashboardService.getDashboardByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/{developerId}")
    public ResponseEntity<DevDashboardDetailedResponse> getDashboardSummary(@AuthenticationPrincipal Jwt jwt,
                                                                            @PathVariable String developerId) {
        extractUserId(jwt);
        DevDashboardDetailedResponse response = dashboardService.getDashboardDetails(developerId);
        return ResponseEntity.ok(response);
    }
}
