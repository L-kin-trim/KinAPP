# AdminUserAudit 类说明（与 ForumPost 文档风格一致）

## 1. 类名：`AdminUserAuditApi`（Controller）
- 路径：`src/main/java/com/luankin/luankinstation/admin/AdminUserAuditApi.java`
- 用途：管理员用户审核接口控制器，提供用户搜索与用户全量审核详情。

### 接口：`GET /api/admin/users/search`
- 功能：管理员搜索站点用户（支持用户ID与用户名同一个搜索框）
- 鉴权：需要 JWT，且必须 `ROLE_ADMIN`
- 参数：
  - `keyword`（可选）：搜索关键词
    - 纯数字时：可命中 `user.id`
    - 任意文本时：模糊匹配 `user.username`（不区分大小写）
  - `page`（默认0）
  - `size`（默认20，范围1~100）
- 返回：`PageResult<AdminUserSearchItemResponse>`
- 说明：当 `keyword` 为空时，返回全站用户分页列表。

### 接口：`GET /api/admin/users/{userId}/overview`
- 功能：管理员查看单个用户审核详情
- 鉴权：需要 JWT，且必须 `ROLE_ADMIN`
- 路径参数：
  - `userId`：用户ID
- 返回：`AdminUserAuditOverviewResponse`
- 详情包含：
  - 用户基础信息（ID/用户名/邮箱/角色/创建更新时间）
  - 用户发帖全部内容（含图片、审核状态）
  - 用户评论全部内容（含图片、审核状态）
  - 用户自建道具库/战术库全部内容
  - 用户收藏的论坛道具/战术内容（仅当前有效收藏，且帖子需 `APPROVED`）

## 2. 类名：`AdminUserAuditService`
- 路径：`src/main/java/com/luankin/luankinstation/admin/service/AdminUserAuditService.java`
- 用途：管理员用户审核业务逻辑。
- 核心能力：
  - 统一搜索逻辑：`ID精确 + 用户名模糊`
  - 用户审核聚合视图构建（帖子/评论/我的库/收藏）
  - 帖子图片与评论图片批量查询并回填
  - 收藏内容过滤（仅 `PROP_SHARE/TACTIC_SHARE` 且帖子审核通过）

## 3. 响应 DTO：`AdminUserSearchItemResponse`
- 路径：`src/main/java/com/luankin/luankinstation/admin/dto/AdminUserSearchItemResponse.java`
- 用途：管理员搜索用户列表项返回体
- 字段：`id/username/role/createdAt/updatedAt`

## 4. 响应 DTO：`AdminUserAuditOverviewResponse`
- 路径：`src/main/java/com/luankin/luankinstation/admin/dto/AdminUserAuditOverviewResponse.java`
- 用途：管理员查看用户审核详情返回体
- 字段：
  - 用户信息：`id/username/email/role/createdAt/updatedAt`
  - 计数：`postCount/commentCount/selfLibraryItemCount/favoriteLibraryItemCount`
  - 内容列表：
    - `posts`（`List<ForumPostResponse>`）
    - `comments`（`List<ForumCommentResponse>`）
    - `selfLibraryItems`（`List<MyLibraryItemResponse>`）
    - `favoriteLibraryItems`（`List<MyLibraryItemResponse>`）

## 5. 仓库接口调整：`UserAccountRepository`
- 路径：`src/main/java/com/luankin/luankinstation/login/repository/UserAccountRepository.java`
- 变更：扩展 `JpaSpecificationExecutor<UserAccount>`
- 用途：支持管理员搜索接口的动态条件查询

## 6. 仓库接口调整：`ForumPostRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumPostRepository.java`
- 新增方法：`findByCreatedByUsernameOrderByCreatedAtDesc`
- 用途：拉取指定用户发帖全量数据

## 7. 仓库接口调整：`ForumCommentRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumCommentRepository.java`
- 新增方法：`findByUsernameOrderByCreatedAtDesc`
- 用途：拉取指定用户评论全量数据

## 8. 仓库接口调整：`ForumPostImageRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumPostImageRepository.java`
- 新增方法：`findByPostIdInOrderByPostIdAscSortOrderAsc`
- 用途：批量查询帖子图片，避免逐条查询

## 9. 仓库接口调整：`ForumCommentImageRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumCommentImageRepository.java`
- 新增方法：`findByCommentIdInOrderByCommentIdAscSortOrderAsc`
- 用途：批量查询评论图片，避免逐条查询

## 10. 仓库接口调整：`MyLibraryItemRepository`
- 路径：`src/main/java/com/luankin/luankinstation/mylibrary/repository/MyLibraryItemRepository.java`
- 新增方法：`findAllByOwnerUsernameOrderByCreatedAtDesc`
- 用途：拉取指定用户自建道具库/战术库全量内容

## 11. 仓库接口调整：`MyLibraryFavoriteRepository`
- 路径：`src/main/java/com/luankin/luankinstation/mylibrary/repository/MyLibraryFavoriteRepository.java`
- 新增方法：`findByOwnerUsernameAndDeletedFalseOrderByUpdatedAtDesc`
- 用途：拉取指定用户当前有效收藏，再关联论坛内容用于审核
