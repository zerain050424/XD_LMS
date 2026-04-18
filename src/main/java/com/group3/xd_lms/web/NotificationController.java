package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.Notification;
import com.group3.xd_lms.mapper.NotificationMapper;
import com.group3.xd_lms.utils.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知控制类
 * 处理 R2 阶段逾期催还提醒、取书通知以及读者的消息中心逻辑
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 图书管理员手动发送逾期提醒
     * URL: POST /notifications/send/overdue
     * 权限: Librarian / Admin
     * 功能：针对特定的逾期借阅记录，向读者发送催还通知。
     *
     * @param borrowRecordId 逾期的借阅记录ID
     * @param customMessage 可选的自定义催还话术
     * @return HashMap<String, Object> 返回通知发送结果
     */
    //Todo 图书管理员手动发送逾期提醒
    @PostMapping("/send/overdue")
    public HashMap<String, Object> sendOverdueReminder(
            @RequestParam Long borrowRecordId,
            @RequestParam(required = false) String customMessage) {
        // 核心逻辑：根据借阅记录找到用户ID和书名，封装 Notification 对象并持久化
        return null;
    }

    /**
     * 批量向所有逾期未还的读者发送系统提醒
     * URL: POST /notifications/send/overdue/batch
     * 权限: Librarian / Admin
     * 功能：一键检索系统中所有超期未还记录，并批量自动推送通知。
     *
     * @return HashMap<String, Object> 返回批量处理的任务结果
     */
    //Todo自动批量给接近逾期的用户发送提醒
    @PostMapping("/send/overdue/batch")
    public HashMap<String, Object> batchSendOverdueReminders() {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 读者获取本人的通知列表
     * URL: GET /notifications/my
     * 权限: Reader
     * 功能：检索读者的个人消息中心，包括逾期提醒、预约成功通知等。
     *
     * @param userId 用于获取当前登录用户ID
     * @return HashMap<String, Object> 包含通知记录列表
     */
    //Todo 读者获取本人所有的通知
    @GetMapping("/my")
    public HashMap<String, Object> getMyNotifications(@RequestParam Integer userId) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 读者标记消息为已读
     * URL: PATCH /notifications/{id}/read
     * 权限: Reader
     *
     * @param id 通知记录ID
     * @return HashMap<String, Object> 返回操作结果
     */
    @PatchMapping("/{id}/read")
    public HashMap<String, Object> markAsRead(@PathVariable Long id) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 读者一键清理/标记所有消息为已读
     * URL: PATCH /notifications/read-all
     */
    @PatchMapping("/read-all")
    public HashMap<String, Object> markAllAsRead(HttpSession session) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }
}