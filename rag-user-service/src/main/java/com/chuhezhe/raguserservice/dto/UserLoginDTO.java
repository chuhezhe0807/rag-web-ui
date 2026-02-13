package com.chuhezhe.raguserservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginDTO {
    @NotBlank(message = "{validation.adminUser.username.notNull}")
    @Size(min = 3, max = 50, message = "{validation.adminUser.username.size}")
    private String username;

    @NotBlank(message = "{validation.adminUser.password.notBlank}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
            message = "{validation.adminUser.password.pattern}"
    )
    private String password;
}