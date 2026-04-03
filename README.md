# 图书馆管理系统

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


## github协同开发
## 一、 开发前准备（仅首次操作）
1. **克隆项目到本地**
   ```bash
   git clone https://github.com/zerain050424/XD_LMS.git
   cd XD_LMS
   ```
2. **切换到团队公共开发分支 `dev`**
   ```bash
   git checkout dev
   git pull origin dev
   ```
3. **创建个人功能分支**
   *命名规范：feature-模块名-姓名简拼*
   ```bash
   # 示例：git checkout -b feature-Reader-zhangsan
   git checkout -b feature-你的模块名-你的姓名
   ```
---

## 二、 日常开发流程
1. **开始编写代码前（同步远程进度）**
   每天写代码前，先获取 `dev` 分支的最新改动，防止冲突。
   ```bash
   git checkout dev
   git pull origin dev
   git checkout 你的分支名
   git merge dev
   ```
2. **代码完成后提交到本地**
   ```bash
   git add .
   git commit -m "模块名: 完成xx功能描述"
   ```
3. **推送到远程仓库**
   ```bash
   git push origin 你的分支名
   ```
---
## 三、 提交合并申请（Pull Request）
1. 打开 [GitHub 仓库页面](https://github.com/zerain050424/XD_LMS)。
2. 点击 **Pull requests** 选项卡 → 点击 **New pull request**。
3. **base 分支** 选择：`dev`（切记不要选master）。
4. **compare 分支** 选择：`你的个人分支`。
5. 填写标题（例如：完成用户登录模块），点击 **Create pull request**。
6. 通知组长进行 Code Review（代码审核），审核通过后将合并至 `dev`。
---

## 四、 提交信息（Commit）规范
为了方便追踪历史，提交信息请统一格式：
`模块: 具体做了什么`
**常见示例：**
- `user: 完成登录接口调试`
- `book: 实现图书模糊查询功能`
- `borrow: 修复借书逻辑中日期计算的 Bug`
---

## 五、 分支管理说明
| 分支类型 | 名称 | 说明 |
| :--- | :--- | :--- |
| **主分支** | `master` | 仅存放稳定版本，**严禁直接修改或推送** |
| **开发分支** | `dev` | 团队合并基准分支，仅通过 PR 合并 |
| **功能分支** | `feature-xxx` | 个人开发专用，所有代码先在此分支编写 |
---
```



				
				
				
				
				
