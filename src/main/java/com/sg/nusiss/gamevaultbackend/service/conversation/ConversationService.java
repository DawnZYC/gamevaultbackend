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
import com.sg.nusiss.gamevaultbackend.repository.message.MessageRepository;
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
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MemberRepository memberRepository,
                               MessageRepository messageRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a conversation
     */
    @Transactional
    public Conversation createConversation(String title, Long ownerId) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "群聊标题不能为空");
        }

        if (ownerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ownerId 不能为空");
        }

        // 使用 Builder 模式创建
        Conversation conversation = Conversation.builder()
                .title(title)
                .ownerId(ownerId)
                .build();

        conversation = conversationRepository.save(conversation);

        // 将群主加入成员表
        Member ownerMember = new Member();
        ownerMember.setConversation(conversation);
        ownerMember.setUserId(ownerId);
        ownerMember.setRole("owner");
        ownerMember.setJoinedAt(LocalDateTime.now());
        memberRepository.save(ownerMember);

        log.info("群聊创建成功 - ID: {}, 群主: {}", conversation.getId(), ownerId);

        return conversation;
    }

    /**
     * List all conversation for current user
     */
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
        List<Member> members = memberRepository.findByConversationIdAndIsActive(
                conversationId, true
        );

        // 5. 获取用户详细信息并转换为响应对象
        return members.stream()
                .map(member -> {
                    // 从用户表查询用户信息
                    User user = userRepository.findById(member.getUserId())
                            .orElse(null);

                    if (user == null) {
                        // 用户不存在时返回基本信息
                        return new MemberResponse(
                                member.getUserId(),
                                "未知用户",
                                "",
                                member.getRole(),
                                member.getJoinedAt()
                        );
                    }

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
     * 添加成员
     */
    public Member addMember(Long conversationId, Long userId) {
        if (conversationId == null || conversationId == 0 || userId == null || userId == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "conversationId or userId missing");
        }

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Conversation not found"));

        if (!"active".equals(conv.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Conversation not active");
        }

        // whether is user inside conversation
        Optional<Member> existing = memberRepository.findByConversation(conv).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst();

        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "User already in conversation");
        }

        Member member = new Member();
        member.setConversation(conv);
        member.setUserId(userId);
        member.setRole("member");
        member.setJoinedAt(LocalDateTime.now());

        return memberRepository.save(member);
    }



    /**
     * 移除成员
     */
    public void removeMember(Long conversationId, Long ownerId, Long userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Conversation not found"));

        if (!"active".equals(conv.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Conversation is dissolved");
        }

        if (!conv.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "Only owner can remove members");
        }

        Member member = memberRepository.findByConversation(conv).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Member not in conversation"));

        memberRepository.delete(member);
    }
}
