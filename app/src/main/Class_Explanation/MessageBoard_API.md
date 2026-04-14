# 留言板功能后端 API 说明（含 SQL）

## 1. 功能目标
- 用户可提交对网站建设的意见（留言）
- 意见状态分为：`已发布`、`已采纳`、`已实现`、`已撤销`
- 用户可撤销自己发布的意见
- 管理员可撤销任意意见，并可更新意见状态
- 普通用户看不到 `已撤销` 的意见（包括详情和“我的留言”列表）

## 2. 状态枚举
- 数据库存储值（后端枚举 `MessageBoardStatus`）：
  - `PUBLISHED`：已发布
  - `ADOPTED`：已采纳
  - `IMPLEMENTED`：已实现
  - `REVOKED`：已撤销

## 3. 用户端接口

### 3.1 发布留言
- `POST /api/message-board/entries`
- 请求体：
```json
{
  "content": "建议在个人中心增加头像裁剪功能。"
}
```

### 3.2 查看留言板列表（不含已撤销）
- `GET /api/message-board/entries?page=0&size=20`
- 支持状态筛选（仅允许非撤销状态）：
  - `status=PUBLISHED`
  - `status=ADOPTED`
  - `status=IMPLEMENTED`
- 如果传 `status=REVOKED`，后端返回 400

### 3.3 查看留言详情（不含已撤销）
- `GET /api/message-board/entries/{entryId}`
- 若该留言已撤销，返回 404（对普通用户隐藏）

### 3.4 查看我的留言（不含已撤销）
- `GET /api/message-board/entries/mine?page=0&size=20`

### 3.5 撤销自己的留言
- `PATCH /api/message-board/entries/{entryId}/revoke`
- 请求体（可选）：
```json
{
  "statusNote": "已不再需要该建议"
}
```
- 仅作者可撤销，非作者返回 403

## 4. 管理员接口

### 4.1 管理端查看留言列表（含已撤销）
- `GET /api/admin/message-board/entries?page=0&size=20`
- 支持筛选：
  - `status`：`PUBLISHED|ADOPTED|IMPLEMENTED|REVOKED`
  - `authorUsername`：按作者模糊匹配

### 4.2 管理端查看留言详情
- `GET /api/admin/message-board/entries/{entryId}`

### 4.3 管理员更新状态
- `PATCH /api/admin/message-board/entries/{entryId}/status`
- 请求体：
```json
{
  "status": "ADOPTED",
  "statusNote": "排入下个迭代"
}
```
- 可更新为任意状态（包括 `REVOKED`）

### 4.4 管理员直接撤销
- `PATCH /api/admin/message-board/entries/{entryId}/revoke`
- 请求体（可选）：
```json
{
  "statusNote": "重复建议，已归并处理"
}
```

## 5. 响应字段说明
统一返回对象 `MessageBoardEntryResponse`：
```json
{
  "id": 1,
  "authorUsername": "alice",
  "content": "建议增加深色模式切换",
  "status": "PUBLISHED",
  "statusNote": null,
  "revokedByUsername": null,
  "revokedAt": null,
  "createdAt": "2026-03-31T21:30:00",
  "updatedAt": "2026-03-31T21:30:00"
}
```

## 6. 数据库 SQL（MySQL 8）

```sql
CREATE TABLE IF NOT EXISTS message_board_entry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  author_username VARCHAR(50) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  status_note VARCHAR(500) NULL,
  revoked_by_username VARCHAR(50) NULL,
  revoked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_message_board_status_created (status, created_at),
  INDEX idx_message_board_author_created (author_username, created_at),
  INDEX idx_message_board_status_author (status, author_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 7. 权限规则摘要
- 用户：
  - 可发布留言
  - 可查看非撤销留言
  - 可撤销自己发布的留言
- 管理员：
  - 可查看全部留言（包括已撤销）
  - 可更新任意留言状态
  - 可撤销任意留言
