package com.sg.nusiss.gamevaultbackend.dto.friend.response;

/**
 * @ClassName ListFriendResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

import com.sg.nusiss.gamevaultbackend.dto.friend.FriendDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListFriendResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<FriendDto> friends;
}

