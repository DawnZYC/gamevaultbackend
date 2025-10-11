package com.sg.nusiss.gamevaultbackend.controller.message;

import com.sg.nusiss.gamevaultbackend.common.BaseResponse;
import com.sg.nusiss.gamevaultbackend.common.ResultUtils;
import com.sg.nusiss.gamevaultbackend.dto.message.request.SendMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.request.SendPrivateMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import com.sg.nusiss.gamevaultbackend.security.auth.SecurityUtils;
import com.sg.nusiss.gamevaultbackend.service.message.MessageService;
import com.sg.nusiss.gamevaultbackend.service.message.PrivateMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName MessageController
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */


@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final PrivateMessageService privateMessageService;


    /**
     * 发送消息（REST API，主要用于测试）
     */
    @PostMapping("/send")
    public BaseResponse<MessageResponse> sendMessage(@RequestBody SendMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MessageResponse response = messageService.sendMessage(request, currentUserId);
        return ResultUtils.success(response);
    }

    /**
     * 获取群聊历史消息
     */
    @GetMapping("/{conversationId}")
    public BaseResponse<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<MessageResponse> messages = messageService.getMessages(conversationId, currentUserId, page, size);
        return ResultUtils.success(messages);
    }

    /**
     * 发送私聊消息
     */
    @PostMapping("/private/send")
    public BaseResponse<MessageResponse> sendPrivateMessage(@RequestBody SendPrivateMessageRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MessageResponse response = privateMessageService.sendPrivateMessage(request, currentUserId);
        return ResultUtils.success(response);
    }

    /**
     * 获取私聊历史消息
     */
    @GetMapping("/private/{friendId}")
    public BaseResponse<List<MessageResponse>> getPrivateMessages(
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<MessageResponse> messages = privateMessageService.getPrivateMessages(
                currentUserId, friendId, page, size);
        return ResultUtils.success(messages);
    }
}
