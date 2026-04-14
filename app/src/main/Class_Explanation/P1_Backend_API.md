# P1 功能后端 API 说明（互动、搜索、主页、审计）

## 1. 通用说明
- 鉴权：除登录/注册外，接口默认要求 `Authorization: Bearer <token>`
- 分页参数：`page` 默认 `0`，`size` 默认 `20`，最大 `100`
- 错误返回：`{"message":"..."}` 或 `{"error":"..."}`

## 2. 互动能力增强

### 2.1 帖子点赞
- 点赞：`POST /api/forum/posts/{postId}/like`
- 取消点赞：`DELETE /api/forum/posts/{postId}/like`
- 查询点赞状态：`GET /api/forum/posts/{postId}/like`
- 返回示例：
```json
{
  "postId": 101,
  "liked": true,
  "likeCount": 23
}
```

### 2.2 评论回复（楼中楼）
- 发表评论/回复：`POST /api/forum/comments/posts/{postId}`
- 请求体新增字段：
```json
{
  "content": "我补充一下这个点位",
  "parentCommentId": 1201,
  "mentionUsernames": ["tom"],
  "imageUrls": []
}
```
- 说明：
  - `parentCommentId` 为空表示一级评论
  - `parentCommentId` 非空表示回复某条评论
  - 自动写入 `replyToUsername` 供前端渲染“回复某人”

### 2.3 @用户提醒
- 触发时机：
  - 评论中带 `mentionUsernames`
  - 楼中楼回复命中父评论作者
- 发送方式：
  - 站内信 `messageType = INTERACTION_REMINDER`

## 3. 搜索与发现升级

### 3.1 标签体系
- 标签来源：
  - `MAP`（地图）
  - `TOOL_TYPE`（道具类型）
  - `TACTIC_STAGE`（战术阶段）
- 帖子响应中新增：
  - `tags`（示例：`["MAP:Mirage","TOOL_TYPE:SMOKE_GRENADE"]`）

### 3.2 排序方式
- 帖子列表与搜索新增参数：
  - `sortType=LATEST|HOT|MOST_FAVORITE`
- 支持接口：
  - `GET /api/forum/posts`
  - `GET /api/forum/posts/search`

### 3.3 标签筛选
- 搜索新增参数：
  - `mapTag`
  - `toolTag`
  - `tacticStageTag`
- 示例：
`/api/forum/posts/search?keyword=mirage&mapTag=Mirage&toolTag=SMOKE_GRENADE&sortType=HOT`

### 3.4 热门关键词
- `GET /api/forum/posts/search/hot-keywords?limit=10`
- 返回示例：
```json
[
  {"keyword":"mirage", "searchCount":128, "lastSearchedAt":"2026-03-31T20:12:30"},
  {"keyword":"banana", "searchCount":95, "lastSearchedAt":"2026-03-31T20:10:01"}
]
```

### 3.5 搜索建议
- `GET /api/forum/posts/search/suggestions?keyword=mi&limit=10`
- 返回示例：
```json
[
  {"keyword":"mirage", "score":128, "source":"SEARCH_HISTORY"},
  {"keyword":"mid smoke", "score":55, "source":"TAG"}
]
```

## 4. 个人主页与成长体系

### 4.1 个人主页
- 查询任意用户主页：`GET /api/users/{username}/profile`
- 查询我的主页：`GET /api/users/me/profile`
- 返回示例：
```json
{
  "username": "alice",
  "postCount": 36,
  "approvedPostCount": 28,
  "approvalRate": 0.78,
  "favoriteReceivedCount": 120,
  "likeReceivedCount": 240,
  "commentReceivedCount": 98,
  "activeStreakDays": 7,
  "badges": ["QUALITY_CREATOR", "HIGH_INTERACTION_AUTHOR"]
}
```

### 4.2 徽章规则
- `QUALITY_CREATOR`：高通过率且有稳定优质内容产出
- `HIGH_INTERACTION_AUTHOR`：内容获得较高互动量（点赞/评论/收藏）

### 4.3 连续活跃天数
- 基于发帖、评论、点赞、收藏等行为日期计算连续天数

## 5. 管理后台审计日志

### 5.1 记录范围
- 审核帖子/评论
- 删除帖子/评论
- 发送站内信（管理员行为）

### 5.2 查询接口
- `GET /api/admin/audit-logs`
- 参数：
  - `adminUsername`（可选）
  - `actionType`（可选）
  - `from`（可选，ISO datetime）
  - `to`（可选，ISO datetime）
  - `page`、`size`

### 5.3 导出 CSV
- `GET /api/admin/audit-logs/export`
- 与查询接口参数一致，返回 `text/csv`

### 5.4 actionType 枚举
- `REVIEW_POST`
- `DELETE_POST`
- `REVIEW_COMMENT`
- `DELETE_COMMENT`
- `SEND_STATION_MESSAGE`

