package com.chuhezhe.raguserservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("`users`")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField
    private String username;

    @TableField
    private String email;

    @TableField("hashed_password")
    private String hashedPassword;

    @TableField("is_active")
    private boolean isActive;

    @TableField(value = "is_superuser")
    private boolean isSuperuser;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
