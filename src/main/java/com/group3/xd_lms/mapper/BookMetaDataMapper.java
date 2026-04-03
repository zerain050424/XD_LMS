package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.BookMetaData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMetaDataMapper {

    // 新增图书信息
    int insert(BookMetaData bookMetadata);

    // 根据ISBN更新
    int updateByIsbn(BookMetaData bookMetadata);

    // 根据ISBN删除
    int deleteByIsbn(@Param("isbn") String isbn);

    // 根据ISBN查询单本
    BookMetaData selectByIsbn(@Param("isbn") String isbn);

    // 查询所有
    List<BookMetaData> selectAll();

    // 模糊搜索：书名、作者、关键词
    List<BookMetaData> searchByKeyword(@Param("keyword") String keyword);

    // 按分类查询
    List<BookMetaData> selectByCategory(@Param("category") String category);

    //对所有字段进行匹配搜索
    List<BookMetaData> selectBySearch(BookMetaData bookMetadata);
}
