package com.chuhezhe.raguserservice.vo.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @JsonAlias("access_token")
    private String accessToken;

    @JsonAlias("token_type")
    private String tokenType;
}
