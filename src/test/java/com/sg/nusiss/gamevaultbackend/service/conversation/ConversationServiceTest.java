package com.sg.nusiss.gamevaultbackend.service.conversation;


import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.conversation.ConversationRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


/**
 * @ClassName ConversationServiceTest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */
@DataJpaTest
@Import(ConversationService.class) // 把 Service 注入进来
class ConversationServiceTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long ownerId = 100L;

    private Conversation activeConv;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();
        conversationRepository.deleteAll();

        // 创建一个活跃群聊
        activeConv = new Conversation();
        activeConv.setTitle("Test Group");
        activeConv.setOwnerId(ownerId);
        activeConv.setStatus("active");
        activeConv.setNextSeq(1L);
        activeConv = conversationRepository.save(activeConv);

        // 群主自动加进去
        Member owner = new Member();
        owner.setConversation(activeConv);
        owner.setUserId(ownerId);
        owner.setRole("owner");
        memberRepository.save(owner);
    }

    @Test
    void testAddMemberSuccess() {
        Long newUserId = 200L;

        Member member = conversationService.addMember(activeConv.getId(), newUserId);

        assertThat(member.getId()).isNotNull();
        assertThat(member.getUserId()).isEqualTo(newUserId);
        assertThat(member.getConversation().getId()).isEqualTo(activeConv.getId());

        List<Member> members = memberRepository.findByConversation(activeConv);
        assertThat(members).hasSize(2); // owner + new member
    }

    @Test
    void testAddMemberMissingParams() {
        assertThatThrownBy(() -> conversationService.addMember(null, 123L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conversationId or userId missing");

        assertThatThrownBy(() -> conversationService.addMember(activeConv.getId(), 0L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conversationId or userId missing");
    }

    @Test
    void testAddMemberConversationNotFound() {
        assertThatThrownBy(() -> conversationService.addMember(9999L, 200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
    }

    @Test
    void testAddMemberConversationNotActive() {
        activeConv.setStatus("dissolved");
        conversationRepository.save(activeConv);

        assertThatThrownBy(() -> conversationService.addMember(activeConv.getId(), 200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_ERROR.getCode());
    }

    @Test
    void testAddMemberAlreadyExists() {
        Long existingUserId = ownerId; // 群主已经在群聊

        assertThatThrownBy(() -> conversationService.addMember(activeConv.getId(), existingUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User already in conversation");
    }

    @Test
    void testCreateConversationShouldAddOwnerAsMember() {
        // 创建群聊
        Conversation conv = conversationService.createConversation("Test Group", ownerId);

        // 校验群聊存在
        assertThat(conv.getId()).isNotNull();
        assertThat(conv.getTitle()).isEqualTo("Test Group");

        // 校验群主自动成为成员
        List<Member> members = memberRepository.findByConversation(conv);
        assertThat(members).hasSize(1);

        Member owner = members.get(0);
        assertThat(owner.getUserId()).isEqualTo(ownerId);
        assertThat(owner.getRole()).isEqualTo("owner");
    }

    void testDissolveConversationSuccess() {
        conversationService.dissolveConversation(activeConv.getId(), ownerId);

        Conversation conv = conversationRepository.findById(activeConv.getId()).orElseThrow();
        assertThat(conv.getStatus()).isEqualTo("dissolved");
        assertThat(conv.getDissolvedAt()).isNotNull();
    }

    @Test
    void testDissolveConversationByNonOwnerShouldFail() {
        Long nonOwnerId = 200L;

        assertThatThrownBy(() -> conversationService.dissolveConversation(activeConv.getId(), nonOwnerId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
    }

    @Test
    void testDissolveConversationAlreadyDissolvedShouldFail() {
        // 先解散一次
        conversationService.dissolveConversation(activeConv.getId(), ownerId);

        // 再解散一次，应该报错
        assertThatThrownBy(() -> conversationService.dissolveConversation(activeConv.getId(), ownerId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_ERROR.getCode());
    }
}
