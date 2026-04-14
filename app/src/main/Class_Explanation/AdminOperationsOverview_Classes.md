# AdminOperationsOverview 类说明（与 ForumPost 文档风格一致）

## 1. 类名：`AdminOperationsOverviewApi`（Controller）
- 路径：`src/main/java/com/luankin/luankinstation/admin/AdminOperationsOverviewApi.java`
- 用途：管理员运营总览接口控制器，提供全站数据分析看板。

### 接口：`GET /api/admin/operations/overview`
- 功能：获取站点运营总览数据（总量、增长、审核、分布、TOP 榜、趋势、链路状态）
- 鉴权：需要 JWT，且必须 `ROLE_ADMIN`
- 参数：
  - `windowDays`（可选，默认 `30`）：趋势窗口天数，范围 `1~180`
- 返回：`AdminOperationsOverviewResponse`

## 2. 类名：`AdminOperationsOverviewService`
- 路径：`src/main/java/com/luankin/luankinstation/admin/service/AdminOperationsOverviewService.java`
- 用途：运营总览业务统计逻辑。
- 核心能力：
  - 全站总量统计：用户、帖子、评论、我的库、收藏、站内信
  - 审核统计：帖子/评论通过、待审、拒绝、拒绝率、待审率
  - 增长统计：今日/近7天/近30天新增（用户、帖子、评论、道具战术库、收藏、站内信）
  - 互动统计：评论密度、有评论帖子数、活跃用户规模（7天/30天）
  - 分布统计：角色分布、帖子类型分布、审核状态分布、投掷物分布、我的库类型分布
  - 趋势统计：按天输出新增与累计曲线（支持 `windowDays`）
  - TOP 榜：热门地图、高产发帖用户、高活跃评论用户、站内信发送/接收 TOP、收藏帖子 TOP
  - 链路状态：发帖、评论、审核队列、站内信、APP 同步入口状态

## 3. 响应 DTO：`AdminOperationsOverviewResponse`
- 路径：`src/main/java/com/luankin/luankinstation/admin/dto/AdminOperationsOverviewResponse.java`
- 用途：管理员运营总览统一返回体。
- 主体字段：
  - `generatedAt`: 服务端生成时间
  - `windowDays`: 趋势窗口天数
  - `summary`: 全站核心总量
  - `growth`: 今日/7天/30天新增
  - `moderation`: 审核压测指标
  - `engagement`: 社区活跃度指标
  - `library`: 我的道具库/战术库与收藏指标
  - `message`: 站内信指标
  - `distribution`: 结构化分布指标
  - `dailyTrends`: 每日趋势数据
  - `topMaps/topPostAuthors/topCommentUsers/topMessageSenders/topMessageRecipients/topFavoritePosts`
  - `systemStatus`: 论坛链路状态

### 3.1 `summary` 字段
- `totalUsers/adminUsers/normalUsers`
- `totalPosts/approvedPosts/pendingPosts/rejectedPosts`
- `totalComments/approvedComments/pendingComments/rejectedComments`
- `totalSelfLibraryItems/totalFavorites/totalMessages`

### 3.2 `growth` 字段
- `newUsersToday/newUsers7Days/newUsers30Days`
- `newPostsToday/newPosts7Days/newPosts30Days`
- `newCommentsToday/newComments7Days/newComments30Days`
- `newSelfLibraryItemsToday/newSelfLibraryItems7Days/newSelfLibraryItems30Days`
- `newFavoritesToday/newFavorites7Days/newFavorites30Days`
- `newMessagesToday/newMessages7Days/newMessages30Days`

### 3.3 `moderation` 字段
- `pendingPostCount/pendingCommentCount/totalPendingCount`
- `postRejectionRate/commentRejectionRate`
- `postPendingRate/commentPendingRate`

### 3.4 `engagement` 字段
- `postsWithComments`
- `avgCommentsPerPost`
- `avgCommentsPerApprovedPost`
- `activeUsers7Days/activeUsers30Days`
- `activePostAuthors7Days/activeCommentUsers7Days/activeMessageUsers7Days`

### 3.5 `library` 字段
- `activeFavoriteCount/deletedFavoriteCount`
- `propSelfItemCount/tacticSelfItemCount`
- `favoriteFromPropPostCount/favoriteFromTacticPostCount`

### 3.6 `message` 字段
- `inboundMessageUserCount`（收信用户数）
- `outboundMessageUserCount`（发信用户数）
- `conversationPairCount`（发送-接收关系对数）
- `avgMessageLength`（站内信平均长度）

### 3.7 `distribution` 字段
- `userRoleDistribution`
- `postTypeDistribution`
- `postReviewStatusDistribution`
- `commentReviewStatusDistribution`
- `postToolTypeDistribution`
- `selfLibraryTypeDistribution`
- `selfLibraryToolTypeDistribution`

### 3.8 `dailyTrends` 字段
- `date`
- `newUsers/newPosts/newComments/newMessages/newSelfLibraryItems/newFavorites`
- `cumulativeUsers/cumulativePosts/cumulativeComments/cumulativeMessages`

### 3.9 `systemStatus` 字段
- `key`：链路标识（如 `forum_post_publish`）
- `status`：`ENABLED | BUSY | PENDING`
- `note`：状态说明

## 4. 典型返回示例（节选）
```json
{
  "generatedAt": "2026-03-30T18:20:10",
  "windowDays": 30,
  "summary": {
    "totalUsers": 1280,
    "totalPosts": 642,
    "pendingPosts": 12,
    "totalComments": 3890,
    "totalSelfLibraryItems": 204,
    "totalFavorites": 1488,
    "totalMessages": 932
  },
  "moderation": {
    "pendingPostCount": 12,
    "pendingCommentCount": 8,
    "totalPendingCount": 20,
    "postRejectionRate": 6.54
  },
  "topMaps": [
    { "name": "Mirage", "count": 210 },
    { "name": "Inferno", "count": 166 }
  ],
  "dailyTrends": [
    {
      "date": "2026-03-30",
      "newUsers": 9,
      "newPosts": 15,
      "newComments": 84,
      "cumulativeUsers": 1280
    }
  ]
}
```

## 5. 依赖数据来源
- 用户：`user`（`UserAccount`）
- 帖子：`forum_post`（`ForumPost`）
- 评论：`forum_comment`（`ForumComment`）
- 我的库：`my_library_item`（`MyLibraryItem`）
- 收藏：`my_library_favorite`（`MyLibraryFavorite`）
- 站内信：`station_message`（`StationMessage`）
