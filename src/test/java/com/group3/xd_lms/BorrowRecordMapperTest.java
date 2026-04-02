package com.group3.xd_lms;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class BorrowRecordMapperTest {

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BookItemMapper bookItemMapper;
    @Autowired
    private BookMetaDataMapper bookMetadataMapper;

    @Test
    public void testBorrowRecordMapper() {
        // 测试数据
        Long userId = null;
        String isbn = "TEST-BORROW-ISBN";
        String rfid = "TEST-BORROW-RFID";
        Long borrowId = null;

        try {
            // 1. 创建临时用户
            User user = User.builder().user_account("test_borrow").password("123").roleId(3).build();
            userMapper.insert(user);
            userId = user.getId();

            // 2. 创建临时图书
            bookMetadataMapper.insert(BookMetaData.builder().isbn(isbn).title("借阅测试书").build());
            bookItemMapper.insert(BookItem.builder().rfidTag(rfid).isbn(isbn).status(BookItem.BookStatus.Available).build());

            // ===================== 核心功能测试 =====================
            // 3. 借书：新增借阅记录
            BorrowRecord record = BorrowRecord.builder()
                    .userId(userId)
                    .rfidTag(rfid)
                    .borrowDate(LocalDateTime.now())
                    .dueDate(LocalDateTime.now().plusDays(30))
                    .renewCount(0)
                    .build();
            borrowRecordMapper.insert(record);
            borrowId = record.getId();
            System.out.println("✅ 借书成功");

            // 4. 根据ID查询
            BorrowRecord byId = borrowRecordMapper.selectById(borrowId);
            System.out.println("✅ 查询借阅记录：" + byId);

            // 5. 查询用户所有借阅记录
            List<BorrowRecord> allUserRecords = borrowRecordMapper.selectByUserId(userId);
            System.out.println("✅ 用户借阅总数：" + allUserRecords.size());

            // 6. 查询用户未归还记录
            List<BorrowRecord> unreturned = borrowRecordMapper.selectUnreturnedByUserId(userId);
            System.out.println("✅ 用户未还数量：" + unreturned.size());

            // 7. 根据RFID查未归还记录
            BorrowRecord byRfid = borrowRecordMapper.selectUnreturnedByRfid(rfid);
            System.out.println("✅ 根据RFID查未还：" + byRfid);

            // 8. 续借：更新应还日期+续借次数
            borrowRecordMapper.updateRenewInfo(borrowId, LocalDateTime.now().plusDays(60), 1);
            System.out.println("✅ 续借成功");

            // 9. 还书：更新归还时间
            borrowRecordMapper.updateReturnDate(borrowId, LocalDateTime.now().toString());
            System.out.println("✅ 还书成功");

            // 10. 查询逾期记录
            List<BorrowRecord> overdue = borrowRecordMapper.selectOverdueRecords();
            System.out.println("✅ 查询逾期数量：" + overdue.size());

        } finally {
            // 自动清理所有测试数据（级联删除）
            if (userId != null) userMapper.deleteById(userId);
            bookItemMapper.deleteByRfidTag(rfid);
            bookMetadataMapper.deleteByIsbn(isbn);
            System.out.println("🧹 借阅测试数据已清理");
        }

        System.out.println("=== BorrowRecordMapper 全部测试通过 ===");
    }
}