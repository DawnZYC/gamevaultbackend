package com.sg.nusiss.gamevaultbackend.controller.friend;

import com.sg.nusiss.gamevaultbackend.common.BaseResponse;
import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.common.ResultUtils;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.HandleFriendRequestRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.SendFriendRequestRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.FriendRequestResponse;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.FriendResponse;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.UserSearchResponse;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.security.auth.SecurityUtils;
import com.sg.nusiss.gamevaultbackend.service.friend.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName FriendController
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public BaseResponse<List<UserSearchResponse>> searchUsers(@RequestParam String keyword) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSearchResponse> users = friendService.searchUsers(keyword, currentUserId);
        return ResultUtils.success(users);
    }

    /**
     * 发送好友请求
     */
    @PostMapping("/request/send")
    public BaseResponse<Void> sendFriendRequest(@RequestBody SendFriendRequestRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        friendService.sendFriendRequest(currentUserId, request.getToUserId(), request.getMessage());
        return ResultUtils.success(null);
    }

    /**
     * 获取收到的好友请求
     */
    @GetMapping("/request/received")
    public BaseResponse<List<FriendRequestResponse>> getReceivedRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<FriendRequestResponse> requests = friendService.getReceivedRequests(currentUserId);
        return ResultUtils.success(requests);
    }

    /**
     * 获取发送的好友请求
     */
    @GetMapping("/request/sent")
    public BaseResponse<List<FriendRequestResponse>> getSentRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<FriendRequestResponse> requests = friendService.getSentRequests(currentUserId);
        return ResultUtils.success(requests);
    }

    /**
     * 处理好友请求
     */
    @PostMapping("/request/handle")
    public BaseResponse<Void> handleFriendRequest(@RequestBody HandleFriendRequestRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        friendService.handleFriendRequest(request.getRequestId(), request.getAccept(), currentUserId);
        return ResultUtils.success(null);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public BaseResponse<List<FriendResponse>> getFriends() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<FriendResponse> friends = friendService.getFriends(currentUserId);
        return ResultUtils.success(friends);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public BaseResponse<Void> deleteFriend(@PathVariable Long friendId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        friendService.deleteFriend(currentUserId, friendId);
        return ResultUtils.success(null);
    }
}
