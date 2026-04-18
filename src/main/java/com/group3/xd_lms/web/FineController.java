package com.group3.xd_lms.web;

import com.group3.xd_lms.mapper.FineMapper;
import com.group3.xd_lms.utils.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务与罚款控制类
 * 处理 R2 阶段读者的逾期欠款查询、实时金额计算及缴纳确认逻辑
 */
@RestController
@RequestMapping("/fines")
public class FineController {

    private final FineMapper fineMapper;
    FineController(FineMapper fineMapper) {
        this.fineMapper = fineMapper;
    }

    /**
     * 实时查询单笔借阅记录的预计罚款
     * URL: GET /fines/calculate/{borrowRecordId}
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 后端根据 borrowRecordId 获取该笔借阅的应还时间(due_date)。
     * 2. 从 system_settings 表读取配置项 "fine_per_day" (每日罚金)。
     * 3. 计算当前日期与 due_date 的天数差。
     * 4. 计算逻辑：预计金额 = 逾期天数 * 每日罚金。
     *
     * @param borrowRecordId 前端传入的逾期借阅记录ID
     * @return HashMap<String, Object> 包含逾期天数、罚金单价及实时计算的总金额
     */
    //Todo 查询单笔未还借阅记录产生的罚款
    @GetMapping("/calculate/{borrowRecordId}")
    public HashMap<String, Object> calculateOverdueFine(@PathVariable Long borrowRecordId) {
        return null;
    }

    /**
     * 获取当前读者所有未缴纳的欠款清单
     * URL: GET /fines/my-unpaid
     * 权限: Librarian
     * 功能：检索该读者所有已产生且未支付的罚款记录(fines表)，用于财务页面展示。
     * @param userId 用于获取当前登录用户ID
     * @return HashMap<String, Object> 返回未付罚款记录列表
     */
    //Todo 图书管理员获取该读者名下所有的未缴纳罚款
    @GetMapping("/my-unpaid")
    public HashMap<String, Object> getMyUnpaidFines(@RequestParam Integer userId) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 缴纳罚款确认
     * URL: POST /fines/pay
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 读者点击“缴纳”并完成模拟支付。
     * 2. 后端修改 fines 表对应记录的 status 为 'Paid'。
     * 3. 同步更新 users 表中的 total_fines (减去已付金额)。
     * 4. 发送一条支付成功的 Notification。
     *
     * @param fineId 罚款记录ID
     * @param paymentMethod 支付方式 (如 "Balance", "AliPay" 等)
     * @return HashMap<String, Object> 返回支付操作结果
     */
    // Todo 读者确认缴纳罚款
    @PostMapping("/pay")
    public HashMap<String, Object> payFine(
            @RequestParam Integer fineId,
            @RequestParam String paymentMethod) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 管理员查询全馆未缴清罚款统计
     * URL: GET /fines/admin/summary
     * 权限: Librarian
     * 功能：R2 Librarian 角色查看当前图书馆的所有未收账款明细。
     */
    //Todo 图书管理员查看所有未缴纳罚款
    @GetMapping("/admin/summary")
    public HashMap<String, Object> getAdminFineSummary() {
        // 此处仅定义接口，不实现逻辑
        return null;
    }
}
