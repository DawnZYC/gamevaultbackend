package com.sg.nusiss.gamevaultbackend.controller.developer;

import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameResponse;
import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameUploadRequest;
import com.sg.nusiss.gamevaultbackend.entity.developer.DeveloperProfile;
import com.sg.nusiss.gamevaultbackend.repository.developer.DeveloperProfileRepository;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameApplicationService;
import com.sg.nusiss.gamevaultbackend.controller.common.AuthenticatedControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/developer/devgame")
@RequiredArgsConstructor
public class DevGameUploadController extends AuthenticatedControllerBase {

    private final DevGameApplicationService devGameApplicationService;
    private final DeveloperProfileRepository developerProfileRepository;

    @PostMapping("/upload")
    public ResponseEntity<DevGameResponse> uploadGame(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("releaseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime releaseDate,
            @RequestPart("image") MultipartFile imageFile,
            @RequestPart("video") MultipartFile videoFile,
            @RequestPart("zip") MultipartFile zipFile
    ) {
        String userId = extractUserId(jwt);

        DeveloperProfile profile = developerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    String newId = UUID.randomUUID().toString();
                    DeveloperProfile newProfile = new DeveloperProfile(newId, userId, 0);
                    developerProfileRepository.save(newProfile);
                    return newProfile;
                });

        DevGameUploadRequest request = new DevGameUploadRequest();
        request.setDeveloperId(profile.getId());
        request.setName(name);
        request.setDescription(description);
        request.setReleaseDate(releaseDate);
        request.setImage(imageFile);
        request.setVideo(videoFile);
        request.setZip(zipFile);

        DevGameResponse response = devGameApplicationService.uploadGame(request);
        return ResponseEntity.ok(response);
    }
}
