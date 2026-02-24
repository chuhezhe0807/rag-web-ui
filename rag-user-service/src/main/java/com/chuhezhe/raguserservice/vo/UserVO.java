package com.chuhezhe.raguserservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    private Integer id;

    private String username;

    private String email;

    private boolean isActive;

    private boolean isSuperuser;
}
