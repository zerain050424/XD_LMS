package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.RenewalRequest;
import com.group3.xd_lms.entity.Reservation;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.RenewalRequestMapper;
import com.group3.xd_lms.mapper.ReservationMapper;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.group3.xd_lms.entity.RenewalRequest.RequestStatus;

/**
 * 图书流转控制类
 * 处理 R2 阶段核心业务：续借审批、读者间直借(P2P)、图书预约及取书倒计时管理
 */
@RestController
@RequestMapping("/circulation")
public class BookCirculationController {

    /** 默认借阅期：30天（与 README-R2 的 max_loan_days 默认值保持一致） */
    private static final long DEFAULT_LOAN_DAYS = 30L;

    /** 转借二维码有效期：5分钟 */
    private static final long TRANSFER_QR_EXPIRE_MINUTES = 5L;

    private final BorrowRecordMapper borrowRecordMapper;
    private final RenewalRequestMapper renewalRequestMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final BookItemMapper bookItemMapper;

    // 构造函数注入
    public BookCirculationController(BorrowRecordMapper borrowRecordMapper,
                                     RenewalRequestMapper renewalRequestMapper,
                                     ReservationMapper reservationMapper,
                                     UserMapper userMapper,
                                     BookItemMapper bookItemMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.renewalRequestMapper = renewalRequestMapper;
        this.reservationMapper = reservationMapper;
        this.userMapper = userMapper;
        this.bookItemMapper = bookItemMapper;
    }

    // ==================== 一、续借管理 (Renewal Section) ====================

    /**
     * 读者发起续借请求 (核心业务分流接口)
     * URL: POST /circulation/renew/apply
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 后端查询 borrow_records 表中的 renew_count。
     * 2. 若 renew_count < 2：直接更新 borrow_records 的应还日期，返回“自动续借成功”。
     * 3. 若 renew_count >= 2：在 renewal_requests 表创建一条状态为 'Pending' 的记录，返回“已进入人工审批流”。
     *
     * @param borrowRecordId 借阅记录ID
     * @param reason 续借理由 (若次数>=2则必填，自动续借时可选)
     * @param userId 用于获取当前用户ID并校验操作权限
     * @return HashMap<String, Object> 返回处理结果（告知用户是自动续借成功，还是已提交申请）
     */
    @PostMapping("/renew/apply")
    public HashMap<String, Object> applyForRenewal(
            @RequestParam Long borrowRecordId,
            @RequestParam(required = false) String reason,
            @RequestParam Integer userId) {
        try {
            BorrowRecord borrowRecord = borrowRecordMapper.selectById(borrowRecordId);
            if (borrowRecord == null) {
                return Result.getResultMap(404, "借阅记录不存在");
            }

            if (!borrowRecord.getUserId().toString().equals(userId.toString())) {
                return Result.getResultMap(403, "无权限：只能续借自己借阅的图书");
            }

            if (borrowRecord.getReturnDate() != null) {
                return Result.getResultMap(400, "操作失败：图书已归还");
            }

            int currentRenewCount = borrowRecord.getRenewCount();
            if (currentRenewCount <= 2) {
                borrowRecord.setRenewCount(currentRenewCount + 1);
                borrowRecord.setDueDate(borrowRecord.getDueDate().plusDays(30));
                borrowRecordMapper.updateDueDateAndRenewCount(borrowRecord);
                return Result.getResultMap(200, "自动续借成功！已延长30天");
            } else {
                if (reason == null || reason.trim().isEmpty()) {
                    return Result.getResultMap(400, "续借次数超过2次，必须填写理由");
                }
                RenewalRequest request = new RenewalRequest();
                request.setBorrowRecordId(borrowRecordId.intValue());
                request.setUserId(userId);
                request.setReason(reason);
                request.setStatus(RenewalRequest.RequestStatus.Pending);
                request.setRequestDate(LocalDateTime.now());

                int rows = renewalRequestMapper.insert(request);
                if (rows <= 0) {
                    return Result.getResultMap(500, "提交续借申请失败");
                }
                return Result.getResultMap(200, "续借申请已提交，等待馆员审批");
            }
        } catch (Exception e) {
            return Result.getResultMap(500, "续借失败：" + e.getMessage());
        }
    }

    /**
     * 馆员获取待审批列表 (次数 > 2 次的申请)
     * URL: GET /circulation/renew/pending
     * 权限: Librarian
     * 功能：列出所有因超过续借次数限制而进入人工审批流程的请求。
     */
    @GetMapping("/renew/pending")
    public HashMap<String, Object> getPendingRenewalRequests() {
        try {
            List<RenewalRequest> pendingList = renewalRequestMapper.selectByStatus("Pending");
            return Result.getListResultMap(200, "获取成功", pendingList.size(), pendingList);
        } catch (Exception e) {
            return Result.getResultMap(500, "获取失败：" + e.getMessage());
        }
    }

    /**
     * 馆员执行续借审批
     * URL: PUT /circulation/renew/audit
     * 权限: Librarian
     * 功能：针对 >= 2 次的续借申请进行人工干预。
     *      若批准，则手动更新 borrow_records 表的应还日期。
     *
     * @param requestId 续借申请单ID
     * @param isApprove 是否批准 (true/false)
     * @param remark 馆员审批意见/驳回理由
     * @return HashMap<String, Object> 返回审批操作结果
     */
    @PutMapping("/renew/audit")
    public HashMap<String, Object> auditRenewalRequest(
            @RequestParam Integer requestId,
            @RequestParam Boolean isApprove,
            @RequestParam(required = false) String remark) {
        try {
            RenewalRequest request = renewalRequestMapper.selectById(requestId);
            if (request == null) {
                return Result.getResultMap(404, "续借申请不存在");
            }
            if (request.getStatus() != RequestStatus.Pending) {
                return Result.getResultMap(400, "该申请已处理，无法重复审批");
            }

            BorrowRecord borrowRecord = borrowRecordMapper.selectById(request.getBorrowRecordId().longValue());
            if (borrowRecord == null) {
                return Result.getResultMap(404, "关联的借阅记录不存在");
            }
            if (!borrowRecord.getUserId().toString().equals(request.getUserId().toString())) {
                return Result.getResultMap(403, "校验失败：申请人不是该书当前持有者");
            }

            request.setStatus(isApprove ? RequestStatus.Approved : RequestStatus.Rejected);
            request.setLibrarianRemark(remark);
            request.setProcessedAt(LocalDateTime.now());
            int updateRows = renewalRequestMapper.updateAuditStatus(request);
            if (updateRows <= 0) {
                return Result.getResultMap(500, "更新审批状态失败");
            }

            if (isApprove) {
                borrowRecord.setRenewCount(borrowRecord.getRenewCount() + 1);
                borrowRecord.setDueDate(borrowRecord.getDueDate().plusDays(30));
                borrowRecordMapper.updateDueDateAndRenewCount(borrowRecord);
            }

            return Result.getResultMap(200, isApprove ? "审批通过" : "审批驳回");
        } catch (Exception e) {
            return Result.getResultMap(500, "审批失败：" + e.getMessage());
        }
    }

    // ==================== 二、转借管理 (Transfer Section) ====================

    /**
     * 生成转借凭证信息 (供持有者生成二维码)
     * URL: GET /circulation/transfer/qr-data
     * 权限: Reader (当前持有者)
     *
     * 返回:
     * {
     *   borrowRecordId,
     *   expireMinutes,
     *   qrData
     * }
     */
    @GetMapping("/transfer/qr-data")
    public HashMap<String, Object> getTransferQrData(
            @RequestParam Long borrowRecordId,
            @RequestParam Integer userId) {
        try {
            BorrowRecord borrowRecord = borrowRecordMapper.selectById(borrowRecordId);
            if (borrowRecord == null) {
                return Result.getResultMap(404, "借阅记录不存在");
            }

            if (borrowRecord.getReturnDate() != null) {
                return Result.getResultMap(400, "图书已归还，不能转借");
            }

            if (!borrowRecord.getUserId().toString().equals(userId.toString())) {
                return Result.getResultMap(403, "无权限：只有当前持有者才能生成转借码");
            }

            if (borrowRecord.getDueDate() != null && borrowRecord.getDueDate().isBefore(LocalDateTime.now())) {
                return Result.getResultMap(400, "图书已逾期，不能转借");
            }

            BookItem bookItem = bookItemMapper.selectByRfidTag(borrowRecord.getRfidTag());
            if (bookItem == null) {
                return Result.getResultMap(404, "图书实体不存在");
            }

            String status = String.valueOf(bookItem.getStatus());
            if ("Lost".equalsIgnoreCase(status)) {
                return Result.getResultMap(400, "图书已标记为遗失，不能转借");
            }
            if ("Reserved".equalsIgnoreCase(status)) {
                return Result.getResultMap(400, "图书已被预约，不能转借");
            }

            long ts = System.currentTimeMillis();
            String raw = borrowRecordId + "|" + userId + "|" + ts;
            String qrData = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> data = new HashMap<>();
            data.put("borrowRecordId", borrowRecordId);
            data.put("expireMinutes", TRANSFER_QR_EXPIRE_MINUTES);
            data.put("qrData", qrData);

            return Result.getResultMap(200, "转借码生成成功", data);
        } catch (Exception e) {
            return Result.getResultMap(500, "生成转借码失败：" + e.getMessage());
        }
    }

    /**
     * 扫码确认转借 (接收者触发)
     * URL: POST /circulation/transfer/scan-confirm
     * 权限: Reader (扫码者/接收方)
     *
     * 注意：
     * 这里改成接收 qrData，而不是直接只收 borrowRecordId。
     * 因为你要求二维码里必须包含时间戳并做 5 分钟有效期校验。
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/transfer/scan-confirm")
    public HashMap<String, Object> confirmTransferByScan(
            @RequestParam String qrData,
            @RequestParam Integer userId) {
        try {
            TransferQrPayload payload = parseTransferQrData(qrData);
            if (payload == null) {
                return Result.getResultMap(400, "二维码无效或格式错误");
            }

            long nowMillis = System.currentTimeMillis();
            if (nowMillis - payload.getTimestamp() > Duration.ofMinutes(TRANSFER_QR_EXPIRE_MINUTES).toMillis()) {
                return Result.getResultMap(400, "二维码已过期，请持有者重新生成");
            }

            BorrowRecord borrowRecord = borrowRecordMapper.selectById(payload.getBorrowRecordId());
            if (borrowRecord == null) {
                return Result.getResultMap(404, "借阅记录不存在");
            }

            if (borrowRecord.getReturnDate() != null) {
                return Result.getResultMap(400, "该图书已归还，无法转借");
            }

            Long currentHolderId = borrowRecord.getUserId();
            if (currentHolderId == null) {
                return Result.getResultMap(500, "借阅记录数据异常：当前持有者为空");
            }

            // 防止二维码过期前已发生别的转借，导致当前持有者和二维码里的持有者不一致
            if (!currentHolderId.equals(payload.getFromUserId())) {
                return Result.getResultMap(400, "转借码已失效，请当前持有者重新生成");
            }

            // 对象校验：扫码者不能是持有者本人
            if (currentHolderId.equals(Long.valueOf(userId))) {
                return Result.getResultMap(400, "扫码者不能是持有者本人");
            }

            // 接收者必须存在且状态正常
            User receiver = userMapper.selectById(Long.valueOf(userId));
            if (receiver == null) {
                return Result.getResultMap(404, "接收者不存在");
            }
            if (receiver.getStatus() == null || !"Active".equalsIgnoreCase(String.valueOf(receiver.getStatus()))) {
                return Result.getResultMap(400, "接收者账户已禁用，不能接收转借");
            }

            // 图书状态校验
            BookItem bookItem = bookItemMapper.selectByRfidTag(borrowRecord.getRfidTag());
            if (bookItem == null) {
                return Result.getResultMap(404, "图书实体不存在");
            }

            String status = String.valueOf(bookItem.getStatus());
            if ("Lost".equalsIgnoreCase(status)) {
                return Result.getResultMap(400, "图书已标记为遗失，不能转借");
            }
            if ("Reserved".equalsIgnoreCase(status)) {
                return Result.getResultMap(400, "图书已被预约，不能转借");
            }

            // 不可逾期
            if (borrowRecord.getDueDate() != null && borrowRecord.getDueDate().isBefore(LocalDateTime.now())) {
                return Result.getResultMap(400, "图书已逾期，不能转借");
            }

            // 循环校验：不能 A->B 后 B 再转回 A
            // 当前借阅记录里 transfer_from_user_id 记录的是“上一个转出人”
            if (borrowRecord.getTransferFromUserId() != null
                    && borrowRecord.getTransferFromUserId().equals(Long.valueOf(userId))) {
                return Result.getResultMap(400, "不能循环转借：该图书不能转回上一位持有者");
            }

            // 原子更新借阅记录
            BorrowRecord updateRecord = new BorrowRecord();
            updateRecord.setId(borrowRecord.getId());
            updateRecord.setUserId(Long.valueOf(userId));
            updateRecord.setTransferFromUserId(currentHolderId);
            updateRecord.setIsP2pTransfer(true);
            updateRecord.setDueDate(LocalDateTime.now().plusDays(DEFAULT_LOAN_DAYS));

            int rows = borrowRecordMapper.updateForP2pTransfer(updateRecord);
            if (rows <= 0) {
                throw new RuntimeException("更新借阅记录失败");
            }

            // book_items 仍保持 Loaned，无需改状态；这里只返回转借后的信息
            Map<String, Object> data = new HashMap<>();
            data.put("borrowRecordId", borrowRecord.getId());
            data.put("rfidTag", borrowRecord.getRfidTag());
            data.put("transferFromUserId", currentHolderId);
            data.put("newUserId", Long.valueOf(userId));
            data.put("newBorrowDate", LocalDateTime.now());
            data.put("newDueDate", updateRecord.getDueDate());

            return Result.getResultMap(200, "转借成功", data);
        } catch (Exception e) {
            return Result.getResultMap(500, "扫码转借失败：" + e.getMessage());
        }
    }

    // ==================== 三、预约管理 (Reservation Section) ====================

    @PostMapping("/reserve/apply")
    public HashMap<String, Object> applyForReservation(
            @RequestParam String rfidTag,
            @RequestParam Integer userId) {
        return null;
    }

    @GetMapping("/reserve/qr-data")
    public HashMap<String, Object> getReservationQrData(
            @RequestParam Integer reservationId,
            HttpSession session) {
        return null;
    }

    @PostMapping("/reserve/verify-scan")
    public HashMap<String, Object> verifyAndCompleteBorrow(@RequestParam Integer reservationId) {
        return null;
    }

    @GetMapping("/reserve/librarian/todo")
    public HashMap<String, Object> getPendingAllocations() {
        return null;
    }

    // ==================== 内部工具方法 ====================

    private TransferQrPayload parseTransferQrData(String qrData) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(qrData),
                    StandardCharsets.UTF_8
            );
            String[] arr = decoded.split("\\|");
            if (arr.length != 3) {
                return null;
            }

            TransferQrPayload payload = new TransferQrPayload();
            payload.setBorrowRecordId(Long.parseLong(arr[0]));
            payload.setFromUserId(Long.parseLong(arr[1]));
            payload.setTimestamp(Long.parseLong(arr[2]));
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private static class TransferQrPayload {
        private Long borrowRecordId;
        private Long fromUserId;
        private Long timestamp;

        public Long getBorrowRecordId() {
            return borrowRecordId;
        }

        public void setBorrowRecordId(Long borrowRecordId) {
            this.borrowRecordId = borrowRecordId;
        }

        public Long getFromUserId() {
            return fromUserId;
        }

        public void setFromUserId(Long fromUserId) {
            this.fromUserId = fromUserId;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}