package com.group3.xd_lms.web;
import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/borrow")
public class BorrowController {
    // 注入Mapper
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookItemMapper bookItemMapper;
    private final UserMapper userMapper;

    public BorrowController(BorrowRecordMapper borrowRecordMapper, BookItemMapper bookItemMapper, UserMapper userMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.bookItemMapper = bookItemMapper;
        this.userMapper = userMapper;
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
    public Map<String, Object> borrowBook(@RequestParam String rfidTag,@RequestParam Long userId){
        //扫描RFID码进行借书

        BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        User user = userMapper.selectById(userId);
        if(bookItem == null){ // 未找到书目
            return Result.getResultMap(500,"查询书籍失败");
        }
        if(user == null){
            return Result.getResultMap(500,"查询用户失败");
        }
        if(bookItem.isAvailable()){
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setUserId(userId);
            borrowRecord.setRfidTag(rfidTag);
            borrowRecord.setBorrowDate(LocalDateTime.now());
            borrowRecord.setDueDate(LocalDateTime.now().plusDays(40));
            borrowRecord.setUser(user);
            bookItem.setStatus(BookItem.BookStatus.Loaned);
            borrowRecordMapper.insert(borrowRecord);
            return Result.getResultMap(200,"借阅成功");
        }else {
            return Result.getResultMap(200,"书籍不可借，借阅失败");
        }
    }

    // 还书
    // TODO R1(Reader) 接收RFID码和用户信息，进行还书
    @RequestMapping(value = {"/returnBook", "/reader/returnBook"})
    @Transactional
    public void returnBook(){
        //通过扫描RFID码进行还书
    }

    /**
     * 1. 查询所有借阅记录
     */
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
