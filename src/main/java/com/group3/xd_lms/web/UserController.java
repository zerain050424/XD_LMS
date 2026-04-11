package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;


@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    /**
     * 用户注册接口
     * URL: POST /users/register
     * 功能：允许新用户自主注册账号。默认设置为“读者”角色 (RoleID: 3) 且状态为“活跃”。
     *
     * @param user 包含注册信息的实体对象 (需提供 user_account, password, fullName, email)
     * @return HashMap<String, Object> 注册结果状态及用户信息
     */
    @PostMapping("/register")
    public HashMap<String, Object> register(@RequestBody User user) {
        // 1. 基础非空校验
        if (user.getUser_account() == null || user.getUser_account().trim().isEmpty()) {
            return Result.getResultMap(400, "注册失败：账号不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.getResultMap(400, "注册失败：密码不能为空");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return Result.getResultMap(400, "注册失败：姓名不能为空");
        }

        // 2. 账号唯一性检查
        // 防止数据库 user_account 字段冲突
        User existingUser = userMapper.selectByUserAccount(user.getUser_account());
        if (existingUser != null) {
            return Result.getResultMap(409, "注册失败：该账号已被占用");
        }

        // 3. 设置注册用户的默认属性
        // 默认角色设为 3 (读者 Reader)
        user.setRoleId(3);
        // 默认状态设为 Active (正常使用)
        user.setStatus(User.UserStatus.Active);

        // 如果你的数据库没有设置自动生成时间，也可以在 Java 层设置
        // user.setCreatedAt(new LocalDateTime());

        // 4. 执行持久化操作
        try {
            int rows = userMapper.insert(user);
            System.out.println(rows);
            if (rows > 0) {
                // 5. 注册成功处理
                // 安全起见，返回给前端的对象中抹除密码
                user.setPassword(null);
                return Result.getResultMap(200, "注册成功", user);
            } else {
                return Result.getResultMap(500, "注册失败：服务器保存数据时出错");
            }
        } catch (Exception e) {
            // 捕获可能的数据库异常（如字段长度超出等）
            return Result.getResultMap(500, "注册异常：" + e.getMessage());
        }
    }


    /**
     * 用户登录接口
     * URL: POST /users/login
     * 功能：验证账号密码，设置Session，返回用户信息及权限
     *
     * @param loginParams 包含密码和用户名
     * @param session 会话对象
     * @return Result 封装的登录结果
     */
    @PostMapping("/login")
    public HashMap<String, Object> login(
            @RequestBody Map<String, String> loginParams,
            HttpSession session) {

        // 2. 从 Map 中获取前端传来的 key
        String user_account = loginParams.get("user_account");
        String password = loginParams.get("password");

        // 3. 参数基础校验 (加上非空判断，防止 get 拿到 null)
        if (user_account == null || password == null ||
                user_account.trim().isEmpty() || password.trim().isEmpty()) {
            return Result.getResultMap(400, "账号或密码不能为空");
        }

        // --- 以下逻辑保持不变 ---

        // 2. 根据账号查询用户
        User user = userMapper.selectByUserAccount(user_account);

        // 3. 用户不存在
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }

        // 4. 检查账号状态
        if (!user.isActive()) {
            return Result.getResultMap(403, "账号已被禁用，请联系管理员");
        }

        // 5. 密码验证
        if (!password.equals(user.getPassword())) {
            return Result.getResultMap(401, "账号或密码错误");
        }

        // 6. 登录成功：写入 Session
        session.setAttribute("userId", user.getId());
        session.setAttribute("userAccount", user.getUser_account());
        session.setAttribute("roleId", user.getRoleId());

        // 权限列表逻辑...
        List<String> permissions = new ArrayList<>();
        if (user.isAdmin()) {
            permissions.add("admin");
            permissions.add("librarian");
            permissions.add("reader");
        } else if (user.isLibrarian()) {
            permissions.add("librarian");
            permissions.add("reader");
        } else {
            permissions.add("reader");
        }
        session.setAttribute("permissions", permissions);

        // 7. 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("userAccount", user.getUser_account());
        data.put("fullName", user.getFullName());
        data.put("roleId", user.getRoleId()); // 确保这个 roleId 会返回给前端
        data.put("permissions", permissions);

        return Result.getResultMap(200, "登录成功", data);
    }

    /**
     * 获取当前登录用户的个人资料
     * URL: GET /users/me
     * 功能：从 Session 中获取当前用户 ID，并查询完整的用户详情
     *
     * @param session 会话对象，用于获取当前登录用户 ID
     * @return HashMap<String, Object> 包含用户实体的通用结果
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
     * URL: PUT /users/me
     * 功能：允许当前登录用户修改自己的基础信息（如姓名、邮箱）
     *
     * @param user 前端传入的用户实体对象
     * @param session 会话对象，用于获取操作者的用户 ID
     * @return HashMap<String, Object> 返回更新操作的结果状态
     */
    @PutMapping("/me/update")
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
        return rows > 0 ? Result.getResultMap(200, "资料更新成功") : Result.getResultMap(500, "资料更新失败");
    }

    /**
     * 用户修改自己的密码
     * URL: PUT /users/me/password
     * 功能：允许当前登录用户修改登录密码
     *
     * @param pwdMap 包含新密码的 Map，Key 为 "newPassword"
     * @param session 会话对象，用于获取操作者的用户 ID
     * @return HashMap<String, Object> 返回密码修改操作的结果状态
     **/
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
        return rows > 0 ? Result.getResultMap(200, "密码修改成功") : Result.getResultMap(500, "密码修改失败");
    }

    /**
     * 管理员创建用户
     * URL: POST /users
     * 权限: R1 (Admin)
     * 功能：创建新用户时检查 user_account 是否已存在，若未指定密码则设置默认密码 123456
     *
     * @param userAccount 用户账号
     * @param fullName 用户姓名
     * @param email 用户邮箱 (可选)
     * @param roleId 用户角色ID (可选)
     * @param password 用户初始密码 (可选)
     * @return HashMap<String, Object> 返回创建结果及新用户信息
     */
    @PostMapping
    public HashMap<String, Object> createUser(
            @RequestParam String userAccount,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String password) {
        // 1. 参数基础校验
        if (userAccount.trim().isEmpty() || fullName.trim().isEmpty()) {
            return Result.getResultMap(400, "账号和姓名不能为空");
        }
        // 2. 检查账号是否已存在
        User existingUser = userMapper.selectByUserAccount(userAccount);
        if (existingUser != null) {
            return Result.getResultMap(409, "该账号已存在，请勿重复创建");
        }
        // 3. 处理默认密码
        String finalPassword = (password == null || password.trim().isEmpty()) ? "123456" : password;
        // 4. 构建用户对象
        User newUser = User.builder()
                .user_account(userAccount)
                .fullName(fullName)
                .email(email)
                .password(finalPassword) // 设置默认密码或用户指定的密码
                .roleId(roleId != null ? roleId : 3) // 默认为读者 (3)
                .status(User.UserStatus.Active) // 默认为活跃状态
                .build();
        // 5. 执行插入
        int rows = userMapper.insert(newUser);
        if (rows > 0) {
            // 移除密码字段后再返回，避免泄露
            newUser.setPassword(null);
            return Result.getResultMap(200, "创建成功", newUser);
        }
        return Result.getResultMap(500, "创建失败，请稍后重试");
    }

    /**
     * 管理员更新用户信息
     * URL: PUT /users/{id}
     * 权限: R1 (Admin)
     * 功能：根据 ID 更新用户信息（包括角色、邮箱、状态等）
     *
     * @param id 路径变量，目标用户 ID
     * @param newUser 前端传入的包含新数据的用户实体
     * @return Map<String, Object> 返回更新操作的结果状态
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable("id") Long id, @RequestBody User newUser) {
        User oldUser = userMapper.selectById(id);
        if (oldUser == null) {
            return Result.getResultMap(500, "查询用户失败");
        }
        if (newUser == null) {
            return Result.getResultMap(500, "缺少更新后的用户信息");
        }
        if (!Objects.equals(newUser.getId(), oldUser.getId())) {
            return Result.getResultMap(500, "更新后的用户ID与原用户ID不匹配");
        }
        // 更新除ID外的所有用户信息
        oldUser.setRoleId(newUser.getRoleId());
        oldUser.setPassword(newUser.getPassword());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setStatus(newUser.getStatus());
        oldUser.setFullName(newUser.getFullName());
        oldUser.setCreatedAt(newUser.getCreatedAt());
        int rows = userMapper.updateById(oldUser);
        return Result.getResultMap(200, "更新用户信息成功");
    }

    /**
     * 管理员删除/注销用户
     * URL: DELETE /users/{id}
     * 权限: R1 (Admin)
     * 功能：将用户状态更新为 Disabled（逻辑删除），而非物理删除
     *
     * @param id 路径变量，目标用户 ID
     * @return HashMap<String, Object> 返回删除结果
     */
    @DeleteMapping("/{id}")
    public HashMap<String, Object> deleteUser(@PathVariable Long id) {
        // 1. 参数校验
        if (id == null || id <= 0) {
            return Result.getResultMap(400, "用户ID无效");
        }
        // 2. 查询用户是否存在
        User user = userMapper.selectById(id); // 假设你的 Mapper 中有此方法
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }
        // 3. 执行逻辑删除
        // 策略：不物理删除数据，而是将状态更新为 Disabled
        // 这样可以保留借阅记录等历史数据的完整性
        user.setStatus(User.UserStatus.Disabled);
        int rows = userMapper.updateById(user); // 假设你的 Mapper 中有此方法
        if (rows > 0) {
            return Result.getResultMap(200, "用户已注销（禁用）成功");
        }
        return Result.getResultMap(500, "注销失败，请稍后重试");
    }

    /**
     * 管理员修改用户状态
     * URL: PATCH /users/{id}/status
     * 权限: R1 (Admin)
     * 功能：快速启用或禁用用户账号
     *
     * @param id 路径变量，目标用户 ID
     * @param status 目标状态字符串 (Active 或 Disabled)
     * @return HashMap<String, Object> 返回状态更新操作的结果
     */
    @PatchMapping("/{id}/status")
    public HashMap<String, Object> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        // 1. 参数校验
        if (id == null || id <= 0) {
            return Result.getResultMap(400, "用户ID无效");
        }
        // 2. 状态值校验 (防止非法字符注入)
        User.UserStatus targetStatus;
        try {
            // 将字符串转换为枚举，如果转换失败会抛出异常
            targetStatus = User.UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Result.getResultMap(400, "状态值非法，仅支持 'Active' 或 'Disabled'");
        }
        // 3. 查询用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }
        // 4. 执行状态更新
        // 策略：构建一个只包含 id 和 status 的对象进行更新
        // 这样可以避免覆盖密码、创建时间等其他字段
        User updateEntity = User.builder()
                .id(id)
                .status(targetStatus)
                .build();
        int rows = userMapper.updateById(updateEntity); // 假设使用 MyBatis-Plus 或支持动态更新的 Mapper
        if (rows > 0) {
            return Result.getResultMap(200, "用户状态已更新为: " + status);
        }
        return Result.getResultMap(500, "状态更新失败");
    }

    /**
     * 管理员更新用户角色
     * URL: PATCH /users/{id}/role
     * 权限: R1 (Admin)
     * 功能：调整用户权限角色（如 reader 升级为 librarian/admin）
     *
     * @param id 路径变量，目标用户 ID
     * @param params 包含新角色 ID 的 Map 对象
     * @return Map<String, Object> 返回角色更新操作的结果状态
     */
    @PatchMapping("/{id}/role")
    public Map<String, Object> updateUserRole(@PathVariable("id") Long id,
                                              @RequestBody Map<String, Integer> params) {
        // 1. 获取URL中的ID
        // 从 Map 中提取新角色
        Integer newRoleId = params.get("role");
        if (newRoleId == null) {
            return Result.getResultMap(500, "缺少角色参数");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.getResultMap(500, "查询用户失败");
        }
        System.out.println("正在将用户 " + id + " 的角色修改为: " + newRoleId);
        user.setRoleId(newRoleId);
        int rows = userMapper.updateById(user);
        return Result.getResultMap(200, "更新用户角色成功");
    }

    /**
     * 管理员模糊搜索用户
     * URL: GET /users/searchbyname
     * 权限: R1 (Admin)
     * 功能：通过账号(user_account)或姓名(fullName)模糊搜索用户
     *
     * @param pattern 搜索关键词 (账号或姓名)
     * @return HashMap<String, Object> 返回包含用户列表和总数的结果
     */
    @GetMapping("/searchbyname")
    public HashMap<String, Object> searchUsersByPattern(@RequestParam String pattern) {
        // 1. 参数校验
        if (pattern == null || pattern.trim().isEmpty()) {
            return Result.getResultMap(400, "搜索关键词不能为空");
        }
        // 2. 执行搜索
        // 策略：在 SQL 层使用 LIKE 语句，同时匹配 user_account 和 fullName
        // 假设 Mapper 中定义了 selectByKeyword 方法
        List<User> userList = userMapper.searchByKeyword(pattern);
        // 3. 结果处理
        // 即使列表为空，也返回成功状态，只是 total 为 0
        if (userList == null) {
            userList = new ArrayList<>();
        }
        // 4. 安全处理 (可选)
        // 在返回列表前，将密码字段置空，防止敏感信息泄露
        userList.forEach(user -> user.setPassword(null));
        return Result.getListResultMap(200, "搜索成功", userList.size(), userList);
    }

    /**
     * 管理员根据角色ID搜索用户
     * URL: GET /users/searchbyroleid
     * 权限: R1 (Admin)
     * 功能：根据用户角色ID（1-Admin, 2-Librarian, 3-Reader）查询用户列表
     *
     * @param roleId 角色ID
     * @return HashMap<String, Object> 返回包含用户列表和总数的结果
     */
    @GetMapping("/searchbyroleid")
    public HashMap<String, Object> searchUsersByRoleId(@RequestParam Integer roleId) {
        // 1. 参数校验
        if (roleId == null) {
            return Result.getResultMap(400, "角色ID不能为空");
        }
        // 2. 角色ID有效性校验 (可选)
        if (roleId < 1 || roleId > 3) {
            return Result.getResultMap(400, "无效的角色ID，仅支持 1(Admin), 2(Librarian), 3(Reader)");
        }
        // 3. 执行搜索
        // 假设 Mapper 中定义了 selectByRoleId 方法
        List<User> userList = userMapper.selectByRoleId(roleId);
        // 4. 结果处理
        if (userList == null) {
            userList = new ArrayList<>();
        }
        // 5. 安全处理：移除密码字段
        userList.forEach(user -> user.setPassword(null));
        return Result.getListResultMap(200, "查询成功", userList.size(), userList);
    }
}
