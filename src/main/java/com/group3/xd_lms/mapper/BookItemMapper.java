package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.BookItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookItemMapper {

    // 新增实物图书
    int insert(BookItem bookItem);

    // 根据RFID更新
    int updateByRfidTag(BookItem bookItem);

    // 根据RFID删除
    int deleteByRfidTag(@Param("rfidTag") String rfidTag);

    // 根据RFID查询
    BookItem selectByRfidTag(@Param("rfidTag") String rfidTag);

    // 查询某ISBN下所有实物
    List<BookItem> selectByIsbn(@Param("isbn") String isbn);

    // 按状态查询（可借/丢失等）
    List<BookItem> selectByStatus(@Param("status") String status);

    // 查询某ISBN下所有Available状态的实物
    List<BookItem> selectAvailableByIsbn(@Param("isbn") String isbn);

    // 修改图书状态（上架、下架、丢失等）
    int updateStatus(@Param("rfidTag") String rfidTag, @Param("status") String status);

}
