# ForumPost 类说明（与现有登录文档风格一致）

## 1. 类名：`ForumPost`（Controller）
- 路径：`src/main/java/com/luankin/luankinstation/forum/ForumPost.java`
- 用途：论坛帖子接口控制器，负责帖子创建、更新、查询、管理员审核。

### 接口：`POST /api/forum/posts`
- 功能：创建帖子（默认审核状态 `PENDING`）
- 鉴权：需要 JWT
- 请求体字段：
  - 通用：`postType`
  - 道具分享帖：`propName` `mapName` `toolType` `throwMethod` `propPosition` `stanceImageUrl` `aimImageUrl` `landingImageUrl`
  - 战术分享帖：`tacticName` `mapName` `tacticType` `tacticDescription` `member1/member1Role ... member5/member5Role`
  - 日常闲聊帖/其他：`content` `imageUrls`（最多10张）

### 接口：`PUT /api/forum/posts/{postId}`
- 功能：更新帖子（作者本人或管理员）
- 行为：更新后重置审核状态为 `PENDING`

### 接口：`GET /api/forum/posts`
- 功能：分页查询已审核通过帖子（可按 `postType` 过滤）
- 参数：`postType`(可选), `page`(默认0), `size`(默认20)

### 接口：`GET /api/forum/posts/{postId}`
- 功能：查询单个已审核通过帖子详情

### 接口：`GET /api/forum/posts/{postId}/mine`
- 功能：查询我的帖子详情（作者本人或管理员）

### 接口：`GET /api/forum/posts/admin/review`
- 功能：管理员按审核状态分页查询帖子
- 参数：`status`(默认 `PENDING`), `page`, `size`

### 接口：`PATCH /api/forum/posts/{postId}/admin/review`
- 功能：管理员审核帖子
- 请求体：
```json
{
  "reviewStatus": "APPROVED",
  "reviewRemark": "审核通过"
}
```

## 2. 类名：`ForumPostService`
- 路径：`src/main/java/com/luankin/luankinstation/forum/service/ForumPostService.java`
- 用途：帖子业务逻辑。
- 核心能力：
  - 帖子类型字段校验（按四类帖子分别校验）
  - 闲聊/其他贴图片数量校验（最大10）
  - 帖子图片独立表维护（`forum_post_image`）
  - 审核流（`PENDING/APPROVED/REJECTED`）
  - 列表与详情 DTO 映射

## 3. 请求 DTO：`CreateForumPostRequest`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/CreateForumPostRequest.java`
- 用途：创建/更新帖子请求体
- 关键字段：
  - `postType`: `PROP_SHARE | TACTIC_SHARE | DAILY_CHAT | OTHER`
  - `toolType`: `SMOKE_GRENADE | MOLOTOV | HE_GRENADE | FLASHBANG`
  - `imageUrls`: 最多10条

## 4. 审核 DTO：`ReviewForumPostRequest`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ReviewForumPostRequest.java`
- 用途：管理员审核帖子
- 字段：
  - `reviewStatus`: `APPROVED | REJECTED`
  - `reviewRemark`: 审核备注

## 5. 响应 DTO：`ForumPostResponse`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ForumPostResponse.java`
- 用途：帖子详情返回体
- 包含：
  - 全部帖子业务字段
  - `createdByUsername`
  - `reviewStatus/reviewRemark`
  - `createdAt/updatedAt`
  - `imageUrls`

## 6. 响应 DTO：`ForumPostSummaryResponse`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ForumPostSummaryResponse.java`
- 用途：帖子列表摘要返回体
- 字段：`id/postType/title/mapName/createdByUsername/reviewStatus/createdAt`

## 7. 分页 DTO：`PageResult<T>`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/PageResult.java`
- 用途：统一分页返回结构
- 字段：`items/page/size/total/totalPages`

## 8. 实体类：`ForumPost`
- 路径：`src/main/java/com/luankin/luankinstation/forum/entity/ForumPost.java`
- 表：`forum_post`
- 用途：统一承载四类帖子的数据库实体，使用 `post_type` 区分类型。

## 9. 实体类：`ForumPostImage`
- 路径：`src/main/java/com/luankin/luankinstation/forum/entity/ForumPostImage.java`
- 表：`forum_post_image`
- 用途：闲聊/其他帖图片列表（最多10张，按 `sort_order` 排序）

## 10. 仓库接口：`ForumPostRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumPostRepository.java`
- 用途：帖子 JPA 访问层
- 关键方法：
  - `findByReviewStatusOrderByCreatedAtDesc`
  - `findByPostTypeAndReviewStatusOrderByCreatedAtDesc`
  - `findByIdAndReviewStatus`

## 11. 仓库接口：`ForumPostImageRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumPostImageRepository.java`
- 用途：帖子图片 JPA 访问层
- 关键方法：
  - `findByPostIdOrderBySortOrderAsc`
  - `deleteByPostId`

## 12. 图片上传接口（供发帖前上传）
- 说明文档：`ForumImageUpload_Classes.md`
- 典型流程：
  1. 前端先调用上传接口拿到图片 URL
  2. 发帖时把 URL 写入 `stanceImageUrl/aimImageUrl/landingImageUrl/imageUrls`
