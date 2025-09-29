package com.sg.nusiss.gamevaultbackend.controller.friend;

import com.sg.nusiss.gamevaultbackend.common.BaseResponse;
import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.common.ResultUtils;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.AcceptFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.DeleteFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.ListFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.SendFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.ListFriendResponse;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.service.friend.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName FriendController
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

@RestController
@RequestMapping("/friend")
public class FriendController {
    @Autowired
    private FriendService friendService;

    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public BaseResponse<Boolean> sendFriendRequest(@RequestBody SendFriendRequest req) {
        if (req == null || req.getUserId() == null || req.getFriendId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (req.getUserId().equals(req.getFriendId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        }
        friendService.sendFriendRequest(req);
        return ResultUtils.success(true);
    }

    /**
     * 接受好友请求
     */
    @PostMapping("/accept")
    public BaseResponse<Boolean> acceptFriendRequest(@RequestBody AcceptFriendRequest req) {
        if (req == null || req.getUserId() == null || req.getFriendId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        friendService.acceptFriendRequest(req);
        return ResultUtils.success(true);
    }

    /**
     * 删除好友
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFriend(@RequestBody DeleteFriendRequest req) {
        if (req == null || req.getUserId() == null || req.getFriendId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        friendService.deleteFriend(req);
        return ResultUtils.success(true);
    }

    /**
     * 列出好友列表
     */
    @GetMapping("/list")
    public BaseResponse<ListFriendResponse> listFriends(@RequestParam Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ListFriendRequest req = new ListFriendRequest();
        req.setUserId(userId);
        ListFriendResponse resp = friendService.listFriends(req);
        return ResultUtils.success(resp);
    }
}
