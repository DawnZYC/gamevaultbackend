package com.sg.nusiss.gamevaultbackend.service.conversation;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.conversation.response.ConversationListResponse;
import com.sg.nusiss.gamevaultbackend.dto.conversation.response.MemberResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.ConversationRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.MemberRepository;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendshipRepository;
import com.sg.nusiss.gamevaultbackend.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @ClassName ConversationService
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */


@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    /**
     * Create a conversation
     */
    @Transactional(readOnly = true)
    public Conversation createConversation(String title, Long ownerId) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "群聊标题不能为空");
        }

        if (ownerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ownerId 不能为空");
        }

        // 查询 owner 用户
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));

        // 使用 Builder 模式创建
        Conversation conversation = Conversation.builder()
                .title(title)
                .ownerId(ownerId)
                .build();

        conversation = conversationRepository.save(conversation);

        // 将群主加入成员表
        Member ownerMember = new Member();
        ownerMember.setConversation(conversation);
        ownerMember.setUser(owner);
        ownerMember.setRole("owner");
        ownerMember.setJoinedAt(LocalDateTime.now());
        memberRepository.save(ownerMember);

        log.info("群聊创建成功 - ID: {}, 群主: {}", conversation.getId(), ownerId);

        return conversation;
    }

    /**
     * List all conversation for current user
     */
    @Transactional(readOnly = true)
    public List<ConversationListResponse> getUserConversations(Long userId) {
        // 1. 查询用户加入的所有群聊成员记录
        List<Member> members = memberRepository.findByUserId(userId);

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取群聊对象
        List<Conversation> conversations = members.stream()
                .map(Member::getConversation)
                .distinct()  // 去重
                .collect(Collectors.toList());

        // 3. 转换为响应DTO
        return conversations.stream()
                .map(conv -> {
                    ConversationListResponse response = new ConversationListResponse();
                    response.setId(conv.getId());
                    response.setTitle(conv.getTitle());
                    response.setOwnerId(conv.getOwnerId());
                    response.setCreatedAt(conv.getCreatedAt());
                    response.setStatus(conv.getStatus());
                    // TODO: 可以添加最后一条消息和未读数

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * dissolve conversation
     */
    @Transactional
    public void dissolveConversation(Long conversationId, Long currentUserId) {
        // 参数校验
        if (conversationId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "群聊ID不能为空");
        }

        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 查询群聊
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "群聊不存在"));

        // 检查是否已解散
        if ("dissolved".equals(conversation.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "群聊已被解散");
        }

        // 验证权限：只有群主可以解散群聊
        if (!conversation.getOwnerId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有群主可以解散群聊");
        }

        LocalDateTime now = LocalDateTime.now();

        // 逻辑删除群聊
        conversation.setStatus("dissolved");
        conversation.setDissolvedAt(now);
        conversation.setDissolvedBy(currentUserId);
        conversation.setDissolvedReason("群主解散");
        conversationRepository.save(conversation);

        // 逻辑删除所有活跃成员
        List<Member> activeMembers = memberRepository.findByConversationIdAndIsActive(
                conversationId, true
        );

        for (Member member : activeMembers) {
            member.setIsActive(false);
            member.setLeftAt(now);
            member.setLeaveReason("群聊已解散");
        }

        if (!activeMembers.isEmpty()) {
            memberRepository.saveAll(activeMembers);
        }

        log.info("群聊已解散 - ID: {}, 群主: {}, 影响成员数: {}",
                conversationId, currentUserId, activeMembers.size());
    }

    /**
     * get conversation users
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long conversationId, Long currentUserId) {
        // 1. 验证群聊是否存在
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "群聊不存在"));

        // 2. 检查群聊是否已解散
        if ("dissolved".equals(conversation.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "群聊已解散");
        }

        // 3. 验证当前用户是否在群聊中
        boolean isMember = memberRepository.existsByConversationIdAndUserIdAndIsActive(
                conversationId, currentUserId, true
        );

        if (!isMember) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不在该群聊中");
        }

        // 4. 查询所有活跃成员
        // 4. 查询所有活跃成员
        List<Member> members = memberRepository.findByConversationIdAndIsActive(
                conversationId, true
        );

        // 5. 获取用户详细信息并转换为响应对象
        return members.stream()
                .map(member -> {
                    // 从用户表查询用户信息
                    User user = member.getUser();

                    return new MemberResponse(
                            user.getUserId(),
                            user.getUsername(),
                            user.getEmail(),
                            member.getRole(),
                            member.getJoinedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 添加成员到群聊（只能添加好友）
     */
    @Transactional
    public void addMembers(Long conversationId, List<Long> userIds, Long currentUserId) {
        // 验证群聊存在
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "群聊不存在"));

        if (!"active".equals(conversation.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "群聊已解散");
        }

        // 验证当前用户是群成员
        memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是群成员"));

        LocalDateTime now = LocalDateTime.now();

        for (Long userId : userIds) {
            // 验证用户存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));

            // 验证是好友关系
            friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, userId, true)
                    .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR,
                            "只能邀请好友加入群聊：" + user.getUsername()));

            // 检查是否已是成员
            Optional<Member> existingMember = memberRepository
                    .findByConversationIdAndUserIdAndIsActive(conversationId, userId, true);

            if (existingMember.isPresent()) {
                continue; // 已是成员，跳过
            }

            // 创建新成员
            Member newMember = new Member();
            newMember.setConversation(conversation);
            newMember.setUser(user);
            newMember.setRole("member");
            newMember.setJoinedAt(now);
            newMember.setIsActive(true);

            memberRepository.save(newMember);
        }
    }
}
