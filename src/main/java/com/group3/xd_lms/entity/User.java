package com.group3.xd_lms.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * User实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /**
     * 对应数据库: id
     */
    private Long id;

    /**
     * 学号/工号 - 对应数据库: user_account
     */
    @NotBlank(message = "学号/工号不能为空")
    @Size(max = 50, message = "学号/工号长度不能超过50")
    private String user_account;

    /**
     * 密码（加密存储） - 对应数据库: password
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度需在6-255之间")
    @ToString.Exclude // 核心安全：排除在toString之外，防止日志泄露密码
    private String password;

    /**
     * 用户姓名 - 对应数据库: user_name
     */
    @Size(max = 100, message = "姓名长度不能超过100")
    private String fullName;

    /**
     * 邮箱 - 对应数据库: email
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    /**
     * 用户状态：Active (活跃), Disabled (禁用) - 对应数据库: status
     */
    @Default // 修复Builder默认值失效问题
    private UserStatus status = UserStatus.Active;

    /**
     * 用户身份：1-Admin, 2-Librarian, 3-Reader - 对应数据库: role_id
     */
    private Integer roleId;

    /**
     * 账号创建时间 - 对应数据库: created_at
     */
    @Default // 修复Builder默认值失效问题
    private LocalDateTime createdAt = LocalDateTime.now();

    // 定义状态枚举
    public enum UserStatus {
        Active, Disabled
    }


    /**
     * 判断用户是否为管理员
     */
    public boolean isAdmin() {
        return Integer.valueOf(1).equals(this.roleId);
    }

    /**
     * 判断用户是否为馆员
     */
    public boolean isLibrarian() {
        return Integer.valueOf(2).equals(this.roleId);
    }

    /**
     * 判断用户是否为读者
     */
    public boolean isReader() {
        return Integer.valueOf(3).equals(this.roleId);
    }

    /**
     * 判断账号是否处于活跃状态
     */
    public boolean isActive() {
        return UserStatus.Active.equals(this.status);
    }
}
