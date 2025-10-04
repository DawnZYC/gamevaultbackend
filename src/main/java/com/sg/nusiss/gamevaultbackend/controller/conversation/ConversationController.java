package com.sg.nusiss.gamevaultbackend.controller.conversation;

import com.sg.nusiss.gamevaultbackend.common.BaseResponse;
import com.sg.nusiss.gamevaultbackend.common.ResultUtils;
import com.sg.nusiss.gamevaultbackend.dto.conversation.request.*;
import com.sg.nusiss.gamevaultbackend.dto.conversation.response.*;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.security.auth.SecurityUtils;
import com.sg.nusiss.gamevaultbackend.service.conversation.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName ConversationController
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * Create a conversation
     */
    @PostMapping("/create")
    public BaseResponse<CreateConversationResponse> createConversation(
            @RequestBody CreateConversationRequest request) {

        // get ownerId from JWT
        Long ownerId = SecurityUtils.getCurrentUserId();

        // create conversation
        Conversation conversation = conversationService.createConversation(
                request.getTitle(),
                ownerId
        );

        return ResultUtils.success(new CreateConversationResponse(conversation.getId()));
    }

    /**
     * List all conversation for current user
     */
    @GetMapping("/list")
    public BaseResponse<List<ConversationListResponse>> getUserConversations() {
        // get ownerId from JWT
        Long userId = SecurityUtils.getCurrentUserId();

        // get all conversations for current user
        List<ConversationListResponse> conversations =
                conversationService.getUserConversations(userId);

        return ResultUtils.success(conversations);
    }

    /**
     * dissolve conversation (only owner can do)
     * */
    @PostMapping("/dissolve")
    public BaseResponse<DissolveConversationResponse> dissolveConversation(
            @RequestBody DissolveRequest request) {
        // get ownerId from JWT
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // dissolve conversation (only owner)
        conversationService.dissolveConversation(
                request.getConversationId(),
                currentUserId
        );

        return ResultUtils.success(null);
    }

    /**
     * Get all users inside conversation
     */
    @GetMapping("/{conversationId}/members")
    public BaseResponse<List<MemberResponse>> getMembers(@PathVariable Long conversationId) {
        // 从 JWT 获取当前用户ID
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 获取成员列表
        List<MemberResponse> members = conversationService.getMembers(conversationId, currentUserId);

        return ResultUtils.success(members);
    }

    @PostMapping("/addMember")
    public BaseResponse<AddMemberResponse> addMember(
            @RequestBody AddMemberRequest request) {
        Member member = conversationService.addMember(request.getConversationId(), request.getUserId());
        return ResultUtils.success(new AddMemberResponse(member.getId()));
    }

    @PostMapping("/removeMember")
    public BaseResponse<RemoveMemberResponse> removeMember(
            @RequestBody RemoveMemberRequest request) {
        conversationService.removeMember(request.getConversationId(), request.getOwnerId(), request.getUserId());
        return ResultUtils.success(new RemoveMemberResponse("Member removed"));
    }
}
