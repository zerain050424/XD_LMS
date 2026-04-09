package com.group3.xd_lms.web;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书管理系统 - 用户模块控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // ==========================================
    // 1. Reader:
    // R1 Target Function:用户登录
    // ==========================================

    @PostMapping("/login")
    // TODO R1(Reader) 实现登录逻辑：校验账号密码
    public void login() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return;
        }

        String userAccount = request.getParameter("userAccount");
        if (userAccount == null || userAccount.trim().isEmpty()) {
            userAccount = request.getParameter("user_account");
        }
        if (userAccount == null || userAccount.trim().isEmpty()) {
            userAccount = request.getParameter("studentId");
        }
        if (userAccount == null || userAccount.trim().isEmpty()) {
            userAccount = request.getParameter("staffId");
        }
        String password = request.getParameter("password");

        HashMap<String, Object> result;
        if (userAccount == null || userAccount.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            result = Result.getResultMap(400, "账号或密码不能为空");
            writeJson(response, result);
            return;
        }

        User user = userMapper.selectByUserAccount(userAccount.trim());
        if (user == null) {
            result = Result.getResultMap(404, "用户不存在");
            writeJson(response, result);
            return;
        }
        if (!User.UserStatus.Active.equals(user.getStatus())) {
            result = Result.getResultMap(403, "账号已禁用");
            writeJson(response, result);
            return;
        }
        if (!password.equals(user.getPassword())) {
            result = Result.getResultMap(401, "账号或密码错误");
            writeJson(response, result);
            return;
        }

        HttpSession session = request.getSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("userRoleId", user.getRoleId());
        session.setAttribute("userAccount", user.getUser_account());

        List<String> permissions = resolvePermissions(user.getRoleId());
        session.setAttribute("permissions", permissions);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userAccount", user.getUser_account());
        data.put("userName", user.getFullName());
        data.put("roleId", user.getRoleId());
        data.put("permissions", permissions);

        result = Result.getResultMap(200, "登录成功", data);
        writeJson(response, result);
        // 执行登录逻辑
    }

    private void writeJson(HttpServletResponse response, HashMap<String, Object> payload) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(payload));
            response.getWriter().flush();
        } catch (IOException ignored) {
        }
    }

    private List<String> resolvePermissions(Integer roleId) {
        if (Integer.valueOf(1).equals(roleId)) {
            return Arrays.asList("USER_MANAGE", "BOOK_MANAGE", "BORROW_MANAGE");
        }
        if (Integer.valueOf(2).equals(roleId)) {
            return Arrays.asList("BOOK_MANAGE", "BORROW_MANAGE");
        }
        return Arrays.asList("BOOK_QUERY", "BOOK_BORROW", "BOOK_RETURN", "PROFILE");
    }


    // ==========================================
    // 2. 个人中心 (Profile - 当前登录用户)
    // ==========================================

    /**
     * 获取当前登录用户的个人资料
     */
    @GetMapping("/me")
    public HashMap<String, Object> getCurrentUserProfile(HttpSession session) {
        // 从会话获取当前登录用户ID
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }

        return Result.getResultMap(200, "获取个人资料成功", user);
    }

    /**
     * 用户更新自己的基本资料
     */
    @PutMapping("/me")
    public HashMap<String, Object> updateCurrentUserProfile(
            @RequestBody User user,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        // 只能修改自己的信息
        user.setId(userId);
        int rows = userMapper.updateById(user);

        return rows > 0
                ? Result.getResultMap(200, "资料更新成功")
                : Result.getResultMap(500, "资料更新失败");
    }

    /**
     * 用户修改自己的密码
     */
    @PutMapping("/me/password")
    public HashMap<String, Object> changePassword(
            @RequestBody Map<String, String> pwdMap,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        String newPassword = pwdMap.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.getResultMap(400, "新密码不能为空");
        }

        int rows = userMapper.updatePasswordById(userId, newPassword);
        return rows > 0
                ? Result.getResultMap(200, "密码修改成功")
                : Result.getResultMap(500, "密码修改失败");
    }


    // ==========================================
    // 3. Admin
    // R1 Target Function:
    // 手动添加新用户
    // 禁用或删除账号
    // 修改用户基本资料或权限
    // 通过用户姓名(数据库user_name字段)和账号(数据库user_account字段)搜索用户
    // ==========================================
    @PostMapping
    // TODO R1(Admin) 创建用户时检查 user_account 是否已存在，在创建时设置默认密码 123456
    public void createUser() {
        // 管理员手动添加新用户
    }

    @PutMapping("/{id}")
    // TODO R1(Admin) 根据 ID更新用户信息
    public void updateUser() {
        // 管理员修改用户信息
    }

    @DeleteMapping("/{id}")
    // TODO R1(Admin) 从数据库中删除用户
    public void deleteUser() {
        // 删除用户（注销账户）
    }

    @PatchMapping("/{id}/status")
    // TODO R1(Admin) 禁用用户
    public void updateUserStatus() {
        // 快速 启用/禁用 用户（'Active', 'Disabled'）
    }

    @PatchMapping("/{id}/role")
    // TODO R1(Admin) 修改角色权限
    public void updateUserRole() {
        // 调整用户权限角色（如reader升级为librarian/admin）
    }

    // TODO R1(Admin) 通过账号或者用户姓名搜索用户
    @GetMapping("/searchbyname")
    public void searchUsersbyname() {
        // 通过用户姓名搜索用户
    }
    @GetMapping("/searchbyaccount")
    public void searchUsersbyaccount() {
        // 通过用户账号搜索用户
    }

}
