package com.sg.nusiss.gamevaultbackend.service.friend;


import com.sg.nusiss.gamevaultbackend.dto.friend.request.AcceptFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.DeleteFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.ListFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.request.SendFriendRequest;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.ListFriendResponse;
import com.sg.nusiss.gamevaultbackend.entity.friend.Friend;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @ClassName FriendServiceTest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

@DataJpaTest
@Import(FriendService.class) // 只加载 JPA 和 FriendService
public class FriendServiceTest {
    @Autowired
    private FriendService friendService;

    @Autowired
    private FriendRepository friendRepository;

    @BeforeEach
    void setUp() {
        friendRepository.deleteAll();
    }

    @Test
    void testSendFriendRequest() {
        SendFriendRequest req = new SendFriendRequest();
        req.setUserId(1L);
        req.setFriendId(2L);

        friendService.sendFriendRequest(req);

        List<Friend> all = friendRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getStatus()).isEqualTo("pending");
    }

    @Test
    void testAcceptFriendRequest() {
        // 先发一个请求
        SendFriendRequest req = new SendFriendRequest();
        req.setUserId(1L);
        req.setFriendId(2L);
        friendService.sendFriendRequest(req);

        // 2 接受请求
        AcceptFriendRequest acceptReq = new AcceptFriendRequest();
        acceptReq.setUserId(2L);   // 接收方
        acceptReq.setFriendId(1L); // 发起方
        friendService.acceptFriendRequest(acceptReq);

        List<Friend> all = friendRepository.findAll();
        assertThat(all).hasSize(2);

        assertThat(all)
                .extracting(Friend::getStatus)
                .containsOnly("accepted");
    }

    @Test
    void testDeleteFriend() {
        // 建立双向关系
        Friend f1 = new Friend();
        f1.setUserId(1L);
        f1.setFriendId(2L);
        f1.setStatus("accepted");
        friendRepository.save(f1);

        Friend f2 = new Friend();
        f2.setUserId(2L);
        f2.setFriendId(1L);
        f2.setStatus("accepted");
        friendRepository.save(f2);

        DeleteFriendRequest delReq = new DeleteFriendRequest();
        delReq.setUserId(1L);
        delReq.setFriendId(2L);
        friendService.deleteFriend(delReq);

        assertThat(friendRepository.findAll()).isEmpty();
    }

    @Test
    void testListFriends() {
        Friend f1 = new Friend();
        f1.setUserId(1L);
        f1.setFriendId(2L);
        f1.setStatus("accepted");
        friendRepository.save(f1);

        ListFriendRequest req = new ListFriendRequest();
        req.setUserId(1L);

        ListFriendResponse resp = friendService.listFriends(req);

        assertThat(resp.getFriends()).hasSize(1);
        assertThat(resp.getFriends().get(0).getFriendId()).isEqualTo(2L);
    }

    @Test
    void testSendFriendRequestTwiceShouldFail() {
        SendFriendRequest req = new SendFriendRequest();
        req.setUserId(1L);
        req.setFriendId(2L);
        friendService.sendFriendRequest(req);

        // 再发一次，应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            friendService.sendFriendRequest(req);
        });
    }
}
