package com.sg.nusiss.gamevaultbackend.service.friend;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.friend.FriendDto;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.AcceptFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.DeleteFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.ListFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.SendFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.ListFriendResponse;
import com.sg.nusiss.gamevaultbackend.entity.friend.Friend;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName FriendService
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;

    /**
     * 发送好友请求
     */
    public void sendFriendRequest(SendFriendRequest req) {
        // 先检查是否已存在
        friendRepository.findByUserIdAndFriendId(req.getUserId(), req.getFriendId())
                .ifPresent(f -> {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经发送过请求或已是好友");
                });

        Friend friend = new Friend();
        friend.setUserId(req.getUserId());
        friend.setFriendId(req.getFriendId());
        friend.setStatus("pending");
        friendRepository.save(friend);
    }

    /**
     * 接受好友请求
     */
    public void acceptFriendRequest(AcceptFriendRequest req) {
        Friend friend = friendRepository.findByUserIdAndFriendId(req.getFriendId(), req.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友请求不存在"));

        if (!"pending".equals(friend.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "好友请求已处理");
        }

        // 更新状态为 accepted
        friend.setStatus("accepted");
        friendRepository.save(friend);

        // 插入双向关系
        Friend reverse = new Friend();
        reverse.setUserId(req.getUserId());
        reverse.setFriendId(req.getFriendId());
        reverse.setStatus("accepted");
        friendRepository.save(reverse);
    }

    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(DeleteFriendRequest req) {
        friendRepository.deleteByUserIdAndFriendId(req.getUserId(), req.getFriendId());
        friendRepository.deleteByUserIdAndFriendId(req.getFriendId(), req.getUserId());
    }

    /**
     * 好友列表
     */
    public ListFriendResponse listFriends(ListFriendRequest req) {
        List<Friend> friends = friendRepository.findAllByUserIdAndStatus(req.getUserId(), "accepted");

        List<FriendDto> friendDtos = friends.stream()
                .map(f -> new FriendDto(f.getFriendId(), f.getAlias(), f.getStatus()))
                .collect(Collectors.toList());

        return new ListFriendResponse(friendDtos);
    }
}