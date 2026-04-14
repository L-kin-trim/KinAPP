# P0 功能后端 API 与 SQL（供前端联调）

## 1. 通用说明
- 所有接口都需要 `Authorization: Bearer <token>`
- 返回错误统一结构：`{"message":"..."}` 或 `{"error":"..."}`（沿用现有项目）
- 分页统一参数：`page`（默认 0）、`size`（默认 20，最大 100）

## 2. 消息中心升级

### 2.1 枚举
- `messageType`
  - `SYSTEM_NOTICE`
  - `REVIEW_RESULT`
  - `INTERACTION_REMINDER`
  - `DIRECT`
- `readFilter`
  - `ALL`
  - `UNREAD`
  - `READ`

### 2.2 发送消息
- `POST /api/messages`
- 请求体：
```json
{
  "recipientUsername": "tom",
  "content": "你的帖子已审核通过",
  "messageType": "REVIEW_RESULT"
}
```
- `messageType` 可选，不传默认 `DIRECT`

### 2.3 收件箱（支持未读/类型筛选）
- `GET /api/messages/inbox?page=0&size=20&readFilter=UNREAD&messageType=REVIEW_RESULT`

### 2.4 发件箱（支持类型筛选）
- `GET /api/messages/sent?page=0&size=20&messageType=DIRECT`

### 2.5 顶部红点与未读数量
- `GET /api/messages/unread-summary`
- 返回示例：
```json
{
  "unreadCount": 12,
  "systemNoticeUnreadCount": 1,
  "reviewResultUnreadCount": 4,
  "interactionReminderUnreadCount": 5,
  "directUnreadCount": 2
}
```

### 2.6 单条已读
- `PATCH /api/messages/{messageId}/read`

### 2.7 全部已读
- `POST /api/messages/read-all`
- 可选按类型：`POST /api/messages/read-all?messageType=REVIEW_RESULT`
- 返回示例：
```json
{
  "updated": 8
}
```

## 3. 草稿箱与自动保存（服务端）

### 3.1 枚举
- `draftType`
  - `FORUM_POST`
  - `MY_LIBRARY_ITEM`

### 3.2 保存/自动保存草稿（新增与更新共用）
- `POST /api/drafts`
- 请求体：
```json
{
  "id": 123,
  "draftType": "FORUM_POST",
  "title": "荒漠迷城A点烟",
  "payloadJson": "{\"postType\":\"PROP_SHARE\",\"mapName\":\"mirage\"}",
  "autoSaved": true
}
```
- `id` 不传表示新增，传了表示更新

### 3.3 草稿列表
- `GET /api/drafts?draftType=FORUM_POST&page=0&size=20`

### 3.4 草稿详情
- `GET /api/drafts/{draftId}`

### 3.5 删除草稿
- `DELETE /api/drafts/{draftId}`

### 3.6 提交后自动清理草稿（新增字段）
- 发帖请求 `CreateForumPostRequest` 新增：
  - `draftId`（可选）
- 自建库存请求 `CreateMyLibraryItemRequest` 新增：
  - `draftId`（可选）
- 当创建成功时，后端会自动删除该 `draftId`（如果属于当前用户）

## 4. 举报与工单化处理

### 4.1 枚举
- `targetType`
  - `POST`
  - `COMMENT`
- `reasonType`
  - `VIOLATION`
  - `ADVERTISEMENT`
  - `FLAME_WAR`
  - `LOW_QUALITY`
  - `OTHER`
- `status`
  - `PENDING`
  - `PROCESSED`

### 4.2 用户提交举报
- `POST /api/reports`
- 请求体：
```json
{
  "targetType": "POST",
  "targetId": 1001,
  "reasonType": "ADVERTISEMENT",
  "reasonDetail": "反复发QQ群广告"
}
```

### 4.3 用户查看我的举报
- `GET /api/reports/mine?page=0&size=20`

### 4.4 管理员举报队列（待处理/已处理）
- `GET /api/admin/reports?status=PENDING&targetType=POST&reasonType=VIOLATION&reporterUsername=lkin&page=0&size=20`

### 4.5 管理员处理举报
- `PATCH /api/admin/reports/{reportId}/handle`
- 请求体：
```json
{
  "processNote": "已核实并完成处理"
}
```
- 调用后状态置为 `PROCESSED`

## 5. 管理员审核提效（批量+模板+筛选）

### 5.1 驳回模板管理
- 查询模板：
  - `GET /api/admin/review/templates?enabledOnly=true`
- 新建模板：
  - `POST /api/admin/review/templates`
- 更新模板：
  - `PUT /api/admin/review/templates/{templateId}`
- 删除模板：
  - `DELETE /api/admin/review/templates/{templateId}`
- 新建/更新请求体：
```json
{
  "templateName": "广告驳回模板",
  "templateContent": "内容包含明显广告导流信息，请修改后重提。",
  "enabled": true
}
```

### 5.2 单条审核支持模板快捷填充
- `PATCH /api/forum/posts/{postId}/admin/review`
- 请求体新增 `rejectTemplateId`：
```json
{
  "reviewStatus": "REJECTED",
  "reviewRemark": "",
  "rejectTemplateId": 3
}
```
- 当 `reviewStatus=REJECTED` 且 `reviewRemark` 为空时，后端会自动使用模板内容

### 5.3 批量审核
- `POST /api/forum/posts/admin/review/batch`
- 请求体：
```json
{
  "postIds": [101, 102, 103],
  "reviewStatus": "APPROVED",
  "reviewRemark": "批量通过",
  "rejectTemplateId": null
}
```
- 返回示例：
```json
{
  "total": 3,
  "succeeded": 3,
  "failed": 0,
  "successPostIds": [101, 102, 103],
  "failedPostIds": []
}
```

### 5.4 审核列表增强筛选
- `GET /api/forum/posts/admin/review`
- 支持参数：
  - `status`（默认 `PENDING`）
  - `postType`
  - `mapName`
  - `createdByUsername`
  - `createdFrom`（ISO datetime）
  - `createdTo`（ISO datetime）
  - `page`
  - `size`
- 示例：
`/api/forum/posts/admin/review?status=PENDING&postType=PROP_SHARE&mapName=mirage&createdByUsername=tom&createdFrom=2026-03-01T00:00:00&createdTo=2026-03-31T23:59:59`

## 6. 帖子编辑与撤回机制

### 6.1 状态流转
- `ReviewStatus` 新增：
  - `WITHDRAWN`
- 规则：
  - 审核中（`PENDING`）可撤回
  - 审核通过（`APPROVED`）后在编辑窗口内可编辑
  - 审核通过后编辑会自动回到 `PENDING`（重新审核）

### 6.2 撤回接口
- `PATCH /api/forum/posts/{postId}/withdraw`
- 仅作者或管理员可调用，且帖子必须是 `PENDING`

### 6.3 帖子版本与编辑提示字段
- `ForumPostResponse` 新增字段：
  - `version`
  - `approvedAt`
  - `withdrawnAt`
  - `editableUntil`
  - `canEdit`
  - `canWithdraw`

### 6.4 编辑窗口
- 当前后端配置：`60 分钟`
- 审核通过时会刷新 `editableUntil`

## 7. 自动消息触达（审核结果）
- 管理员审核帖子（单条/批量/删除驳回）后，后端会自动向作者发送站内信：
  - `messageType = REVIEW_RESULT`

## 8. SQL 变更（MySQL 8）

### 8.1 站内信表升级
```sql
ALTER TABLE station_message
  ADD COLUMN message_type VARCHAR(30) NOT NULL DEFAULT 'DIRECT' AFTER content,
  ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 AFTER message_type,
  ADD COLUMN read_at DATETIME NULL AFTER is_read;

CREATE INDEX idx_station_message_recipient_read_type
  ON station_message(recipient_username, is_read, message_type, sent_at);

CREATE INDEX idx_station_message_sender_type
  ON station_message(sender_username, message_type, sent_at);
```

### 8.2 帖子表升级（编辑/撤回/版本）
```sql
ALTER TABLE forum_post
  ADD COLUMN version INT NOT NULL DEFAULT 1 AFTER review_remark,
  ADD COLUMN approved_at DATETIME NULL AFTER version,
  ADD COLUMN withdrawn_at DATETIME NULL AFTER approved_at,
  ADD COLUMN editable_until DATETIME NULL AFTER withdrawn_at;

CREATE INDEX idx_forum_post_review_status_created_at
  ON forum_post(review_status, created_at);

CREATE INDEX idx_forum_post_review_filter
  ON forum_post(review_status, post_type, map_name, created_by_username, created_at);
```

### 8.3 草稿表
```sql
CREATE TABLE IF NOT EXISTS editor_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_username VARCHAR(50) NOT NULL,
  draft_type VARCHAR(30) NOT NULL,
  title VARCHAR(120) NULL,
  payload_json LONGTEXT NULL,
  auto_saved TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_editor_draft_owner_type_updated(owner_username, draft_type, updated_at),
  INDEX idx_editor_draft_owner_updated(owner_username, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.4 举报工单表
```sql
CREATE TABLE IF NOT EXISTS content_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_username VARCHAR(50) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  reason_type VARCHAR(30) NOT NULL,
  reason_detail VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  processed_by_username VARCHAR(50) NULL,
  processed_at DATETIME NULL,
  process_note TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_content_report_status_created(status, created_at),
  INDEX idx_content_report_target(target_type, target_id),
  INDEX idx_content_report_reporter(reporter_username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.5 驳回模板表
```sql
CREATE TABLE IF NOT EXISTS admin_review_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(100) NOT NULL,
  template_content TEXT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_admin_review_template_enabled_updated(enabled, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
