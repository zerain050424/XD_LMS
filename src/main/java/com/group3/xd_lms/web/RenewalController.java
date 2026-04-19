package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.RenewalRequest;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.RenewalRequestMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 续借管理控制器
 * 负责人：夏盛豪
 * 功能：处理读者续借申请业务
 */
@RestController
@RequestMapping("/renewal")
public class RenewalController {

    private final BorrowRecordMapper borrowRecordMapper;
    private final RenewalRequestMapper renewalRequestMapper;

    public RenewalController(BorrowRecordMapper borrowRecordMapper,
                             RenewalRequestMapper renewalRequestMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.renewalRequestMapper = renewalRequestMapper;
    }

    /**
     * 读者发起续借申请
     * URL: POST /renewal/apply
     * 权限: Reader
     * 业务逻辑：
     * 1. renew_count <= 2：自动续借成功，直接更新应还日期
     * 2. renew_count > 2：必须填写理由，创建 Pending 申请，转入人工审批
     * 3. 若已有 Pending 状态的申请，禁止重复提交
     *
     * @param borrowRecordId 借阅记录ID
     * @param reason         续借理由（次数>2时必填）
     * @param userId         当前登录用户ID
     * @return 处理结果
     */
    @PostMapping("/apply")
    @Transactional(rollbackFor = Exception.class)
    public HashMap<String, Object> applyForRenewal(
            @RequestParam Long borrowRecordId,
            @RequestParam(required = false) String reason,
            @RequestParam Integer userId) {

        try {
            // ========== 1. 查询并校验借阅记录 ==========
            BorrowRecord borrowRecord = borrowRecordMapper.selectById(borrowRecordId);
            if (borrowRecord == null) {
                return Result.getResultMap(404, "借阅记录不存在");
            }

            // 权限校验：只能续借自己借的书
            if (!borrowRecord.getUserId().toString().equals(userId.toString())) {
                return Result.getResultMap(403, "无权限：只能续借自己借阅的图书");
            }

            // 校验图书是否已归还
            if (borrowRecord.getReturnDate() != null) {
                return Result.getResultMap(400, "操作失败：图书已归还，无法续借");
            }

            // 校验是否逾期
            if (borrowRecord.isOverdue()) {
                return Result.getResultMap(400, "操作失败：图书已逾期，请先归还并处理逾期事宜");
            }

            // ========== 2. 防重复提交校验 ==========
            RenewalRequest existingRequest = renewalRequestMapper
                    .selectPendingByUserAndBorrowId(userId, borrowRecordId.intValue());
            if (existingRequest != null) {
                return Result.getResultMap(409, "该图书已有待审批的续借申请，请勿重复提交");
            }

            int currentRenewCount = borrowRecord.getRenewCount() != null ? borrowRecord.getRenewCount() : 0;

            // ========== 3. 核心业务分流 ==========
            if (currentRenewCount <= 2) {
                // 场景1：续借次数 ≤ 2 → 自动续借成功
                return processAutoRenewal(borrowRecord);

            } else {
                // 场景2：续借次数 > 2 → 必须填写理由，转入人工审批
                if (reason == null || reason.trim().isEmpty()) {
                    return Result.getResultMap(400, "续借次数已达上限，必须填写续借理由才能提交申请");
                }
                return processManualRenewal(borrowRecord, userId, reason);
            }

        } catch (Exception e) {
            return Result.getResultMap(500, "续借失败：" + e.getMessage());
        }
    }

    /**
     * 处理自动续借（续借次数 ≤ 2）
     */
    private HashMap<String, Object> processAutoRenewal(BorrowRecord borrowRecord) {
        int newRenewCount = borrowRecord.getRenewCount() + 1;
        LocalDateTime newDueDate = borrowRecord.getDueDate().plusDays(30);

        // 更新数据库
        int rows = borrowRecordMapper.updateDueDateAndRenewCount(
                BorrowRecord.builder()
                        .id(borrowRecord.getId())
                        .dueDate(newDueDate)
                        .renewCount(newRenewCount)
                        .build()
        );

        if (rows > 0) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("borrowId", borrowRecord.getId());
            data.put("newDueDate", newDueDate);
            data.put("renewCount", newRenewCount);
            data.put("status", "Approved");
            data.put("message", "自动续借成功，已延长30天");
            return Result.getResultMap(200, "续借成功", data);
        } else {
            return Result.getResultMap(500, "续借失败，数据库更新异常");
        }
    }

    /**
     * 处理人工审批续借（续借次数 > 2）
     */
    private HashMap<String, Object> processManualRenewal(BorrowRecord borrowRecord, Integer userId, String reason) {
        // 创建续借申请记录
        RenewalRequest request = RenewalRequest.builder()
                .borrowRecordId(borrowRecord.getId().intValue())
                .userId(userId)
                .reason(reason.trim())
                .status(RenewalRequest.RequestStatus.Pending)
                .requestDate(LocalDateTime.now())
                .build();

        int rows = renewalRequestMapper.insert(request);

        if (rows > 0) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("requestId", request.getId());
            data.put("borrowId", borrowRecord.getId());
            data.put("status", "Pending");
            data.put("message", "续借申请已提交，等待管理员审批");
            return Result.getResultMap(200, "申请已提交", data);
        } else {
            return Result.getResultMap(500, "提交续借申请失败");
        }
    }
}