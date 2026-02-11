package com.chuhezhe.raguserservice.vo.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String email;

    private String username;

    @JsonAlias("is_active")
    private boolean isActive;

    @JsonAlias("is_superuser")
    private boolean isSuperuser;

    private Integer id;
}
