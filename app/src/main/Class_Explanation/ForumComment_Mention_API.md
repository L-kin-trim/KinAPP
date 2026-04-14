# 评论区 @ 提醒功能 API 说明

## 1. 结论
- 本次**没有新增 URL 路径**。
- 在现有评论发布接口上增强了 `@用户` 触发逻辑：被 @ 的用户会自动收到站内信提醒。

## 2. 触发规则
- 评论发布成功后触发（当前评论默认自动通过）。
- `@` 用户来源：
- 从评论 `content` 自动解析 `@username`
- 以及请求体中的 `mentionUsernames`（JSON 或 multipart）
- 同一评论内去重后，最多处理 10 个被 @ 用户。
- 不给自己发提醒（自己 @ 自己会被忽略）。
- 不存在的用户名会被忽略，不影响评论发布。
- 站内信类型：`INTERACTION_REMINDER`

## 3. 受影响接口

### 3.1 JSON 评论发布（原接口，能力增强）
- `POST /api/forum/comments/posts/{postId}`
- `Content-Type: application/json`

请求体示例：
```json
{
  "content": "@tom 这个点位可以再往左一点",
  "parentCommentId": null,
  "mentionUsernames": ["tom", "jerry"],
  "imageUrls": []
}
```

说明：
- `content` 中的 `@tom` 会被自动识别。
- `mentionUsernames` 也会参与提醒名单。
- 两者会合并去重后发送提醒。

### 3.2 multipart 评论发布（原接口，新增可选参数）
- `POST /api/forum/comments/posts/{postId}`
- `Content-Type: multipart/form-data`

表单字段：
- `content`：评论文本（支持在文本里写 `@username`）
- `mentionUsernames`：可选，可重复传多个
- `files`：可选，评论图片文件
- `module`：可选，图片上传模块名

示例（伪代码）：
```text
content=@tom 这套战术很好用
mentionUsernames=tom
mentionUsernames=jerry
files=<binary>
```

## 4. 站内信读取（前端联动）
- 无新增接口，继续使用现有站内信接口。
- 收件箱可按类型筛选提醒消息：
- `GET /api/messages/inbox?messageType=INTERACTION_REMINDER`

返回示例（节选）：
```json
{
  "items": [
    {
      "id": 1001,
      "senderUsername": "alice",
      "recipientUsername": "tom",
      "content": "用户 @alice 在帖子#12 的第3楼评论中提及了你（评论ID：88）：@tom ...",
      "messageType": "INTERACTION_REMINDER",
      "read": false,
      "sentAt": "2026-04-02T10:18:32"
    }
  ]
}
```

## 5. 异常说明
- 当合并后的提醒名单超过 10 人时，评论接口返回 `400`：
```json
{"message":"mention users supports up to 10 users"}
```
