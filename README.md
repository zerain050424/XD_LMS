# 图书馆管理系统 - 数据库说明

## 项目介绍
本项目是基于 SpringBoot 的图书馆管理系统，包含用户管理、图书管理、借阅管理等核心功能。

## 数据库文件
- 数据库：`lms_db`
- 建表SQL 路径：`/sql/create.sql`
- 插入测试数据SQL路径：'/sql/insert.sql'

## 数据表结构
1. **roles**：角色表（Admin / Librarian / Reader）
2. **users**：用户表（账号、密码、个人信息、状态）
3. **book_metadata**：图书元数据（ISBN、书名、作者、分类等）
4. **book_items**：图书实物（RFID、状态、馆藏位置）
5. **borrow_records**：借阅记录（借书、还书、续借、逾期）

## 外键与约束
- 角色保护删除
- 图书元数据与实物级联删除
- 借阅记录与用户/图书实物级联删除
