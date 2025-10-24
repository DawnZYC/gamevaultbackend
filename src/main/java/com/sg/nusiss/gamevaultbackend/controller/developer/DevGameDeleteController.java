package com.sg.nusiss.gamevaultbackend.controller.developer;

import com.sg.nusiss.gamevaultbackend.dto.developer.OperationResult;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameApplicationService;
import com.sg.nusiss.gamevaultbackend.controller.common.AuthenticatedControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/developer/devgame")
@RequiredArgsConstructor
public class DevGameDeleteController extends AuthenticatedControllerBase {

    private final DevGameApplicationService devGameApplicationService;

    @DeleteMapping("/{gameId}")
    public ResponseEntity<OperationResult> deleteGame(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable String gameId) {
        String userId = extractUserId(jwt);
        OperationResult result = devGameApplicationService.deleteGame(userId, gameId);

        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
