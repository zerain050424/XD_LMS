package com.group3.xd_lms.web;
import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.utils.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/borrow")
public class BorrowController {
    // 注入Mapper
    private final BorrowRecordMapper borrowRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BookItemMapper bookItemMapper;

    public BorrowController(BorrowRecordMapper borrowRecordMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
    }
    // ==========================================
    // 1. Reader:
    // R1 Target Function:
    // 扫描RFID借书
    // 扫描RFID还书
    // ==========================================

    // 借书
    // TODO R1(Reader) 接收RFID码和用户信息，进行借书
    @RequestMapping(value = {"/borrowBook", "/reader/borrowBook"})
    @Transactional
    public void borrowBook(){
       //扫描RFID码进行借书
    }

    // 还书
    // TODO R1(Reader) 接收RFID码和用户信息，进行还书
    @RequestMapping(value = {"/returnBook", "/reader/returnBook"})
    @Transactional
    public void returnBook(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return;
        }

        String rfidTag = request.getParameter("rfidTag");
        if (rfidTag == null || rfidTag.trim().isEmpty()) {
            rfidTag = request.getParameter("rfid");
        }

        HashMap<String, Object> result;
        if (rfidTag == null || rfidTag.trim().isEmpty()) {
            result = Result.getResultMap(400, "RFID不能为空");
            writeJson(response, result);
            return;
        }

        rfidTag = rfidTag.trim();
        BorrowRecord record = borrowRecordMapper.selectUnreturnedByRfid(rfidTag);
        if (record == null) {
            result = Result.getResultMap(404, "未查询到该图书的借阅记录");
            writeJson(response, result);
            return;
        }

        String userIdParam = request.getParameter("userId");
        if (userIdParam != null && !userIdParam.trim().isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdParam.trim());
                if (!userId.equals(record.getUserId())) {
                    result = Result.getResultMap(403, "当前用户无权归还该图书");
                    writeJson(response, result);
                    return;
                }
            } catch (NumberFormatException e) {
                result = Result.getResultMap(400, "userId格式错误");
                writeJson(response, result);
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int updatedRecord = borrowRecordMapper.updateReturnDate(record.getId(), now.format(DB_DATE_TIME_FORMATTER));
        if (updatedRecord <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result = Result.getResultMap(500, "还书失败：借阅记录更新失败");
            writeJson(response, result);
            return;
        }

        int updatedBook = bookItemMapper.updateStatus(rfidTag, BookItem.BookStatus.Available.name());
        if (updatedBook <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result = Result.getResultMap(500, "还书失败：图书状态更新失败");
            writeJson(response, result);
            return;
        }

        BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        Map<String, Object> data = new HashMap<>();
        data.put("recordId", record.getId());
        data.put("userId", record.getUserId());
        data.put("rfidTag", rfidTag);
        data.put("returnTime", now);
        data.put("book", bookItem);

        result = Result.getResultMap(200, "还书成功", data);
        writeJson(response, result);
        //通过扫描RFID码进行还书
    }

    /**
     * 1. 查询所有借阅记录
     */
    private void writeJson(HttpServletResponse response, HashMap<String, Object> payload) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(payload));
            response.getWriter().flush();
        } catch (IOException ignored) {
        }
    }

    @GetMapping("/getAllRecords")
    public Map<String, Object> getAllRecords() {
        List<BorrowRecord> list = borrowRecordMapper.selectAllRecords();
        if (list == null) {
            return Result.getResultMap(500, "查询借阅记录失败");
        }
        return Result.getListResultMap(200, "查询成功", list.size(), list);
    }

    /**
     * 2. 根据用户ID查询借阅记录
     */
    @GetMapping("/getRecordsByUserId")
    public Map<String, Object> getRecordsByUserId(@RequestParam Long userId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            return Result.getResultMap(400, "用户ID不能为空");
        }

        List<BorrowRecord> list = borrowRecordMapper.selectByUserId(userId);
        if (list == null) {
            return Result.getResultMap(500, "查询失败");
        }

        return Result.getListResultMap(200, "查询成功", list.size(), list);
    }

    /**
     * 3. 根据RFID查询借阅记录
     */
    @GetMapping("/getRecordByRfid")
    public Map<String, Object> getRecordByRfid(@RequestParam String rfidTag) {
        // 参数校验
        if (rfidTag == null || rfidTag.trim().isEmpty()) {
            return Result.getResultMap(400, "RFID不能为空");
        }

        BorrowRecord record = borrowRecordMapper.selectUnreturnedByRfid(rfidTag);
        if (record == null) {
            return Result.getResultMap(404, "未查询到该图书的借阅记录");
        }

        return Result.getResultMap(200, "查询成功", record);
    }

    /**
     * 4. 获取借阅总数量
     */
    @GetMapping("/getTotalCount")
    public Map<String, Object> getTotalCount() {
        List<BorrowRecord> all = borrowRecordMapper.selectAllRecords();
        int count = all == null ? 0 : all.size();
        return Result.getListResultMap(200, "查询成功", count, null);
    }

    /**
     * 5. 获取逾期记录总数
     */
    @GetMapping("/getOverdueCount")
    public Map<String, Object> getOverdueCount() {
        List<BorrowRecord> overdue = borrowRecordMapper.selectOverdueRecords();
        int count = overdue == null ? 0 : overdue.size();
        return Result.getListResultMap(200, "查询成功", count, null);
    }

}
