# 图书馆管理系统 - 数据库说明

## 项目介绍
本项目是基于 SpringBoot 的图书馆管理系统，包含用户管理、图书管理、借阅管理等核心功能。

## 数据库文件
- 数据库：`lms_db`
- 建表SQL 路径：`/sql/create.sql`
- 插入测试数据SQL路径：`/sql/insert.sql`

## R1数据库表结构

### 2. users 用户表
**功能**：存储所有用户信息（学号/工号、密码、个人资料、状态）
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 用户ID |
| user_account | VARCHAR(50) | NOT NULL, UNIQUE | 学号/工号（唯一登录账号） |
| password | VARCHAR(255) | NOT NULL | 密码 |
| user_name | VARCHAR(100) | NULL | 姓名 |
| email | VARCHAR(100) | NULL | 邮箱 |
| role_id | INT | NOT NULL | 角色ID（关联roles表） |
| status | ENUM | DEFAULT 'Active' | 账号状态：Active / Disabled |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**外键**
- `role_id` → `roles(id)`
- 删除规则：RESTRICT（禁止删除被引用的角色）

---

### 3. book_metadata 图书元数据表
**功能**：存储图书的基础信息（一本书的所有信息）
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| isbn | VARCHAR(20) | PK | ISBN编号（图书唯一标识） |
| title | VARCHAR(255) | NOT NULL | 书名 |
| author | VARCHAR(255) | NULL | 作者 |
| category | VARCHAR(100) | NULL | 分类 |
| keywords | TEXT | NULL | 关键词 |
| publisher | VARCHAR(255) | NULL | 出版社 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**
- 全文索引：`title, author, keywords`（优化搜索）

---

### 4. book_items 图书实物表
**功能**：存储每一本实体书（RFID唯一标识）
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| rfid_tag | VARCHAR(50) | PK | RFID标签（实物唯一标识） |
| isbn | VARCHAR(20) | NOT NULL | 关联图书ISBN |
| status | ENUM | DEFAULT 'Available' | 状态：Available / Loaned / Lost / Reserved |
| location | VARCHAR(100) | NULL | 馆藏位置 |
| added_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 入库时间 |

**外键**
- `isbn` → `book_metadata(isbn)`
- 删除规则：CASCADE（元数据删除 → 实物自动删除）

---

### 5. borrow_records 借阅记录表
**功能**：记录所有借书、还书、续借、逾期行为
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 记录ID |
| user_id | INT | NOT NULL | 用户ID |
| rfid_tag | VARCHAR(50) | NOT NULL | 图书RFID |
| borrow_date | DATETIME | DEFAULT CURRENT_TIMESTAMP | 借书时间 |
| due_date | DATETIME | NOT NULL | 应还日期 |
| return_date | DATETIME | NULL | 实际归还日期（未还则为空） |
| renew_count | INT | DEFAULT 0 | 续借次数 |

**外键**
- `user_id` → `users(id)`（CASCADE）
- `rfid_tag` → `book_items(rfid_tag)`（CASCADE）

## 外键与约束
- 角色保护删除
- 图书元数据与实物级联删除
- 借阅记录与用户/图书实物级联删除

## 函数分配
### Reader
| 函数 |负责人 |
| UserController/login | 赵汶潼 | 
| BorrowController/borrowBook |戚翰石|
| BOrrowController/returnBook | 赵汶潼 |
				
				
				
				
				
