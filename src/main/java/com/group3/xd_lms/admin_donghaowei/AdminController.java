package com.group3.xd_lms.admin_donghaowei;
import com.group3.xd_lms.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

    // 获取所有账号
    @GetMapping("/users")
    public List<User> getUsers() {
        return adminService.getAllUsers();
    }

    // 修改账号状态 (参数通过 URL 或 Query 传入)
    // 示例: PUT /api/admin/users/1/status?status=DISABLED
    @PutMapping("/users/{id}/status")
    public ResponseEntity<String> changeStatus(@PathVariable Long id, @RequestParam String status) {
        boolean success = adminService.updateUserStatus(id, status);
        return success ? ResponseEntity.ok("状态更新成功") : ResponseEntity.badRequest().body("更新失败");
    }

    // 删除账号
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        String result = adminService.deleteUser(id);
        if (result.contains("成功")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
