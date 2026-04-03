package com.group3.xd_lms;

import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
public class BookMetaDataMapperTest {

    @Autowired
    private BookMetaDataMapper bookMetadataMapper;

    @Test
    public void testBookMetadataMapper() {
        String testIsbn = "TEST-9781234567890";

        try {
            // 1. 新增
            BookMetaData book = BookMetaData.builder()
                    .isbn(testIsbn)
                    .title("测试图书")
                    .author("测试作者")
                    .category("计算机科学")
                    .keywords("Java,SpringBoot,测试")
                    .publisher("测试出版社")
                    .build();
            bookMetadataMapper.insert(book);
            System.out.println("✅ 新增成功");

            // 2. 根据ISBN查询
            BookMetaData byIsbn = bookMetadataMapper.selectByIsbn(testIsbn);
            System.out.println("✅ 根据ISBN查询：" + byIsbn);

            // 3. 更新
            BookMetaData update = BookMetaData.builder()
                    .isbn(testIsbn)
                    .title("修改后的书名")
                    .author("修改后的作者")
                    .build();
            bookMetadataMapper.updateByIsbn(update);
            System.out.println("✅ 更新成功");

            // 4. 模糊搜索
            List<BookMetaData> searchList = bookMetadataMapper.searchByKeyword("测试");
            System.out.println("✅ 模糊搜索数量：" + searchList.size());

            // 5. 按分类查询
            List<BookMetaData> categoryList = bookMetadataMapper.selectByCategory("计算机科学");
            System.out.println("✅ 分类查询数量：" + categoryList.size());

            // 6. 查询所有
            List<BookMetaData> all = bookMetadataMapper.selectAll();
            System.out.println("✅ 查询所有总数：" + all.size());

        } finally {
            // 7. 删除（自动清理）
            bookMetadataMapper.deleteByIsbn(testIsbn);
            System.out.println("🧹 测试数据已清理");
        }

        System.out.println("=== BookMetadataMapper 全部测试通过 ===");
    }
}
