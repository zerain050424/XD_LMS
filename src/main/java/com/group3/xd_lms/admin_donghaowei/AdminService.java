package com.group3.xd_lms.admin_donghaowei;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;



@Service
public class AdminService {
    @Autowired
    private UserMapper userMapper; // 注入你刚才展示的那个 Mapper

    // 1. 获取所有用户列表
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    // 2. 修改状态 (禁用/启用)
    public boolean updateUserStatus(Long id, String status) {
        // status 传入 "DISABLED" 或 "ENABLED" (根据你数据库设计来)
        return userMapper.updateStatusById(id, status) > 0;
    }

    // 3. 删除用户 (包含逻辑判断)
    public String deleteUser(Long id) {

        User user = userMapper.selectById(id);
        
        // 验收标准：只有状态为禁用（假设叫 "DISABLED"）的账号才能删除
        if (user.getStatus() == null || !"Disabled".equalsIgnoreCase(user.getStatus().name()))  {
            return "只能删除已禁用的账号！请先禁用该账号。";
        }

        int rows = userMapper.deleteById(id);
        return rows > 0 ? "删除成功" : "删除失败";
    }
}
