package com.sg.nusiss.gamevaultbackend.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName FriendDto
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long friendId;
    private String alias;
    private String status;
}
