# LuankinStation 后端 API 全量文档（APP 对接版）

## 1. 基础信息
- 服务端口：`9126`（默认）
- 鉴权方式：`Authorization: Bearer <token>`
- 放行接口：
- `POST /api/auth/login`
- `POST /api/auth/register`
- 管理接口：`/api/admin/**` 需 `ROLE_ADMIN`
- 分页参数约定：
- `page` 默认 `0`
- `size` 默认 `20`
- 后端内部会限制最大 `100`（审计日志部分为 `200`）

## 2. 通用响应格式

### 2.1 分页响应（大多数列表接口）
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "total": 0,
  "totalPages": 0
}
```

### 2.2 错误响应
```json
{
  "message": "error detail"
}
```

## 3. 枚举值总表

### 3.1 论坛
- `PostType`：`PROP_SHARE` | `TACTIC_SHARE` | `DAILY_CHAT` | `OTHER`
- `ToolType`：`SMOKE_GRENADE` | `MOLOTOV` | `HE_GRENADE` | `FLASHBANG`
- `ReviewStatus`：`PENDING` | `APPROVED` | `REJECTED` | `WITHDRAWN`

### 3.2 站内信
- `MessageType`：`SYSTEM_NOTICE` | `REVIEW_RESULT` | `INTERACTION_REMINDER` | `DIRECT`
- `MessageReadFilter`：`ALL` | `UNREAD` | `READ`

### 3.3 留言板
- `MessageBoardStatus`：`PUBLISHED` | `ADOPTED` | `IMPLEMENTED` | `REVOKED`

### 3.4 举报
- `ReportTargetType`：`POST` | `COMMENT`
- `ReportReasonType`：`VIOLATION` | `ADVERTISEMENT` | `FLAME_WAR` | `LOW_QUALITY` | `OTHER`
- `ReportStatus`：`PENDING` | `PROCESSED`

### 3.5 草稿
- `DraftType`：`FORUM_POST` | `MY_LIBRARY_ITEM`

### 3.6 管理审计
- `AdminAuditActionType`：`REVIEW_POST` | `DELETE_POST` | `REVIEW_COMMENT` | `DELETE_COMMENT` | `SEND_STATION_MESSAGE`

## 4. 认证模块

### 4.1 登录
- `POST /api/auth/login`
- 鉴权：否
- 请求体：
```json
{
  "username": "alice",
  "password": "123456"
}
```
- 成功响应：
```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "username": "alice"
  }
}
```

### 4.2 注册
- `POST /api/auth/register`
- 鉴权：否
- 请求体：
```json
{
  "username": "alice",
  "password": "123456",
  "email": "alice@example.com"
}
```
- 成功响应（201）：
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com"
}
```

## 5. 论坛帖子模块
路径前缀：`/api/forum/posts`

### 5.1 发帖
- `POST /api/forum/posts`
- 请求体：`CreateForumPostRequest`
- 关键字段：
- `draftId`（可选）
- `postType`（必填）
- 不同 `postType` 对应字段要求不同（见 5.11）

### 5.2 编辑帖子
- `PUT /api/forum/posts/{postId}`
- 权限：作者或管理员
- 请求体同发帖

### 5.3 已通过帖子列表
- `GET /api/forum/posts?postType=&page=&size=`
- 参数：
- `postType` 可选

### 5.4 已通过帖子搜索
- `GET /api/forum/posts/search?keyword=&postType=&mapName=&createdByUsername=&page=&size=`

### 5.5 已通过帖子详情
- `GET /api/forum/posts/{postId}`

### 5.6 我的帖子详情（作者/管理员）
- `GET /api/forum/posts/{postId}/mine`

### 5.7 管理员审核列表
- `GET /api/forum/posts/admin/review?status=PENDING&postType=&mapName=&createdByUsername=&createdFrom=&createdTo=&page=&size=`
- 时间参数：ISO 日期时间（如 `2026-04-01T10:00:00`）

### 5.8 管理员单条审核
- `PATCH /api/forum/posts/{postId}/admin/review`
- 请求体：
```json
{
  "reviewStatus": "APPROVED",
  "reviewRemark": "通过",
  "rejectTemplateId": null
}
```

### 5.9 管理员批量审核
- `POST /api/forum/posts/admin/review/batch`
- 请求体：
```json
{
  "postIds": [1, 2, 3],
  "reviewStatus": "REJECTED",
  "reviewRemark": "不符合规范",
  "rejectTemplateId": null
}
```
- 响应：成功/失败 ID 汇总

### 5.10 撤回待审核帖子
- `PATCH /api/forum/posts/{postId}/withdraw`
- 仅 `PENDING` 状态可撤回

### 5.11 管理员删除帖子（转驳回）
- `DELETE /api/forum/posts/{postId}/admin`
- 请求体（可选）：
```json
{
  "reason": "违规内容"
}
```

### 5.12 发帖字段规则（重要）
- 当 `postType=PROP_SHARE`：
- 必填：`propName,mapName,toolType,throwMethod,propPosition,stanceImageUrl,aimImageUrl,landingImageUrl`
- `imageUrls` 不允许传
- 当 `postType=TACTIC_SHARE`：
- 必填：`tacticName,mapName,tacticType,tacticDescription,member1~member5及对应role`
- `imageUrls` 不允许传
- 当 `postType=DAILY_CHAT/OTHER`：
- 必填：`content`
- `imageUrls` 最多 10 张

### 5.13 帖子响应对象
- 列表项：`ForumPostSummaryResponse`
- 详情：`ForumPostResponse`
- 详情包含：审核状态、备注、版本、编辑截止时间、`canEdit`、`canWithdraw`、图片列表等

## 6. 论坛评论模块
路径前缀：`/api/forum/comments`

### 6.1 JSON 创建评论
- `POST /api/forum/comments/posts/{postId}`
- 请求体：
```json
{
  "content": "@tom 这个点可以再往左",
  "parentCommentId": null,
  "mentionUsernames": ["tom"],
  "imageUrls": []
}
```

### 6.2 multipart 创建评论（带图）
- `POST /api/forum/comments/posts/{postId}`
- `Content-Type: multipart/form-data`
- 表单字段：
- `content`（必填）
- `mentionUsernames`（可选，可重复）
- `files`（可选）
- `module`（可选）

### 6.3 评论列表（仅已通过）
- `GET /api/forum/comments/posts/{postId}?page=&size=`

### 6.4 管理员评论审核列表
- `GET /api/forum/comments/admin/review?status=APPROVED&page=&size=`

### 6.5 管理员审核评论
- `PATCH /api/forum/comments/{commentId}/admin/review`
- 请求体：
```json
{
  "reviewStatus": "REJECTED",
  "reviewRemark": "违规"
}
```

### 6.6 管理员删除评论（转驳回）
- `DELETE /api/forum/comments/{commentId}/admin`

### 6.7 评论 @ 提醒规则
- 评论创建成功后自动处理 `@用户名`
- 来源：`content` 解析 + `mentionUsernames` 字段
- 去重后最多 10 个
- 忽略自己和不存在用户
- 自动发送站内信，类型为 `INTERACTION_REMINDER`

## 7. 论坛图片上传模块（OSS）
路径前缀：`/api/forum/images`
说明：仅 `oss.enabled=true` 时生效。

### 7.1 单图上传
- `POST /api/forum/images/upload`
- 表单字段：
- `file`（必填）
- `module`（可选）

### 7.2 批量上传
- `POST /api/forum/images/upload/batch`
- 表单字段：
- `files`（必填，最多 10）
- `module`（可选）

### 7.3 响应结构
- 单图：`ForumImageUploadResponse`（`fileName,objectKey,url,contentType,size`）
- 批量：`ForumImageBatchUploadResponse`（`items,total`）

## 8. 草稿模块
路径前缀：`/api/drafts`

### 8.1 保存草稿（新增/更新）
- `POST /api/drafts`
- 请求体：
```json
{
  "id": null,
  "draftType": "FORUM_POST",
  "title": "我的草稿",
  "payloadJson": "{\"a\":1}",
  "autoSaved": true
}
```

### 8.2 草稿列表
- `GET /api/drafts?draftType=&page=&size=`

### 8.3 草稿详情
- `GET /api/drafts/{draftId}`

### 8.4 删除草稿
- `DELETE /api/drafts/{draftId}`

## 9. 我的库模块
路径前缀：`/api/my/library`

### 9.1 新建自建条目
- `POST /api/my/library/items`
- 请求体：`CreateMyLibraryItemRequest`
- 仅支持 `PROP_SHARE`、`TACTIC_SHARE`

### 9.2 我的自建条目列表
- `GET /api/my/library/items?postType=&page=&size=`

### 9.3 收藏论坛帖子
- `POST /api/my/library/favorites/{postId}`

### 9.4 取消收藏
- `DELETE /api/my/library/favorites/{postId}`

### 9.5 我的收藏列表
- `GET /api/my/library/favorites?postType=&page=&size=`

### 9.6 响应结构
- 条目：`MyLibraryItemResponse`
- 收藏状态：`MyFavoriteStatusResponse`

## 10. 站内信模块
路径前缀：`/api/messages`

### 10.1 发送站内信
- `POST /api/messages`
- 请求体：
```json
{
  "recipientUsername": "tom",
  "content": "你好",
  "messageType": "DIRECT"
}
```

### 10.2 收件箱
- `GET /api/messages/inbox?page=&size=&readFilter=ALL&messageType=`

### 10.3 发件箱
- `GET /api/messages/sent?page=&size=&messageType=`

### 10.4 未读汇总
- `GET /api/messages/unread-summary`

### 10.5 标记单条已读
- `PATCH /api/messages/{messageId}/read`

### 10.6 一键已读
- `POST /api/messages/read-all?messageType=`
- 响应：
```json
{
  "updated": 5
}
```

### 10.7 响应结构
- 消息对象：`StationMessageResponse`
- 未读汇总：`MessageUnreadSummaryResponse`

## 11. 留言板模块（用户侧）
路径前缀：`/api/message-board/entries`

### 11.1 创建留言
- `POST /api/message-board/entries`
- 请求体：
```json
{
  "content": "希望新增功能"
}
```

### 11.2 留言列表
- `GET /api/message-board/entries?status=&page=&size=`
- 用户侧可见状态：`PUBLISHED/ADOPTED/IMPLEMENTED`

### 11.3 留言详情
- `GET /api/message-board/entries/{entryId}`

### 11.4 我的留言
- `GET /api/message-board/entries/mine?page=&size=`

### 11.5 用户撤回留言
- `PATCH /api/message-board/entries/{entryId}/revoke`
- 请求体（可选）：
```json
{
  "statusNote": "撤回原因"
}
```

## 12. 举报模块（用户侧）
路径前缀：`/api/reports`

### 12.1 创建举报
- `POST /api/reports`
- 请求体：
```json
{
  "targetType": "POST",
  "targetId": 123,
  "reasonType": "VIOLATION",
  "reasonDetail": "具体说明"
}
```

### 12.2 我的举报列表
- `GET /api/reports/mine?page=&size=`

### 12.3 响应结构
- `ContentReportResponse`

## 13. 管理后台 API

### 13.1 审计日志
路径前缀：`/api/admin/audit-logs`
- `GET /api/admin/audit-logs?adminUsername=&actionType=&from=&to=&page=&size=`
- `GET /api/admin/audit-logs/export?...`（导出 CSV）

### 13.2 留言板管理
路径前缀：`/api/admin/message-board/entries`
- `GET /api/admin/message-board/entries?status=&authorUsername=&page=&size=`
- `GET /api/admin/message-board/entries/{entryId}`
- `PATCH /api/admin/message-board/entries/{entryId}/status`
- `PATCH /api/admin/message-board/entries/{entryId}/revoke`

### 13.3 运营总览
路径前缀：`/api/admin/operations`
- `GET /api/admin/operations/overview?windowDays=30`
- 返回 `AdminOperationsOverviewResponse`（多层统计结构）

### 13.4 举报管理
路径前缀：`/api/admin/reports`
- `GET /api/admin/reports?status=PENDING&targetType=&reasonType=&reporterUsername=&page=&size=`
- `PATCH /api/admin/reports/{reportId}/handle`

### 13.5 审核模板管理
路径前缀：`/api/admin/review/templates`
- `GET /api/admin/review/templates?enabledOnly=true`
- `POST /api/admin/review/templates`
- `PUT /api/admin/review/templates/{templateId}`
- `DELETE /api/admin/review/templates/{templateId}`

### 13.6 用户审计
路径前缀：`/api/admin/users`
- `GET /api/admin/users/search?keyword=&page=&size=`
- `GET /api/admin/users/{userId}/overview`

## 14. 当前未开放为 API 的能力
- `profile` 模块已有 `UserProfileService`（成长徽章、活跃天数等计算），当前无对外 Controller。
- `ForumPostLikeStatusResponse`、`HotKeywordResponse`、`SearchSuggestionResponse`、`ForumPostSortType` 目前未对应到现有 Controller 路由。

## 15. APP 接入建议（顺序）
1. 先接 `/api/auth/login` 获取 token 并统一注入请求头。
2. 首页接帖子列表与搜索：`/api/forum/posts`、`/api/forum/posts/search`。
3. 详情页接帖子详情 + 评论列表：`/api/forum/posts/{id}` + `/api/forum/comments/posts/{id}`。
4. 编辑器接发帖/评论 + 图片上传：`/api/forum/posts`、`/api/forum/comments/posts/{id}`、`/api/forum/images/*`。
5. 消息中心接：`/api/messages/inbox`、`/api/messages/unread-summary`。
6. 我的页面接：草稿、我的库、收藏、我的举报、我的留言。
