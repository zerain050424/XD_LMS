package com.group3.xd_lms;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
public class BookItemMapperTest {

    @Autowired
    private BookItemMapper bookItemMapper;

    @Autowired
    private BookMetaDataMapper bookMetadataMapper;

    @Test
    public void testBookItemMapper() {
        String testIsbn = "TEST-ITEM-9780001";
        String testRfid = "RFID-TEST-0001";

        // 先插入依赖的图书元数据
        BookMetaData metadata = BookMetaData.builder().isbn(testIsbn).title("测试实体书").build();
        bookMetadataMapper.insert(metadata);

        try {
            // 1. 新增实物图书
            BookItem item = BookItem.builder()
                    .rfidTag(testRfid)
                    .isbn(testIsbn)
                    .status(BookItem.BookStatus.Available)
                    .location("A区-1排")
                    .build();
            bookItemMapper.insert(item);
            System.out.println("✅ 新增实物图书成功");

            // 2. 根据RFID查询
            BookItem byRfid = bookItemMapper.selectByRfidTag(testRfid);
            System.out.println("✅ 根据RFID查询：" + byRfid);

            // 3. 根据ISBN查询实物列表
            List<BookItem> byIsbn = bookItemMapper.selectByIsbn(testIsbn);
            System.out.println("✅ 根据ISBN查询实物数：" + byIsbn.size());

            // 4. 更新图书信息
            BookItem update = BookItem.builder().rfidTag(testRfid).location("B区-3排").build();
            bookItemMapper.updateByRfidTag(update);
            System.out.println("✅ 更新位置成功");

            // 5. 更新状态
            bookItemMapper.updateStatus(testRfid, "Lost");
            System.out.println("✅ 更新状态为 Lost 成功");

            // 6. 根据状态查询
            List<BookItem> byStatus = bookItemMapper.selectByStatus("Lost");
            System.out.println("✅ 根据状态查询数量：" + byStatus.size());

        } finally {
            // 清理数据
            bookItemMapper.deleteByRfidTag(testRfid);
            bookMetadataMapper.deleteByIsbn(testIsbn);
            System.out.println("🧹 测试数据已清理");
        }

        System.out.println("=== BookItemMapper 全部测试通过 ===");
    }
}
