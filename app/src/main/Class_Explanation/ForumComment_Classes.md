# ForumComment 类说明

## 用户评论展示规则
- 用户侧评论列表只返回审核通过评论（`APPROVED`）。
- 按楼层正序展示（`floorNumber` 从小到大）。
- 时间精度到分钟，格式：`yyyy-MM-dd HH:mm`。
- 用户侧返回不包含审核字段（不显示 `reviewStatus/reviewRemark`）。

## 1. 控制器：`ForumComment`
- 路径：`src/main/java/com/luankin/luankinstation/forum/ForumComment.java`
- 基础路径：`/api/forum/comments`

接口：
1. `POST /posts/{postId}`
  - 新增评论（默认直接通过）
2. `GET /posts/{postId}`
  - 用户评论列表（仅 `APPROVED`，按楼层）
3. `GET /admin/review`
  - 管理员评论列表（默认 `status=APPROVED`）
4. `PATCH /{commentId}/admin/review`
  - 管理员审核评论
5. `DELETE /{commentId}/admin`
  - 管理员删除评论（软删除：改为 `REJECTED`）

## 2. 服务层：`ForumCommentService`
- 路径：`src/main/java/com/luankin/luankinstation/forum/service/ForumCommentService.java`
- 核心逻辑：
  - 评论创建默认 `APPROVED`
  - 楼层自动递增
  - 用户侧列表只查 `APPROVED`
  - 管理员删除评论改为 `REJECTED`
  - 时间格式化为分钟

## 3. DTO
- 用户侧：
  - `ForumCommentPublicResponse`
  - 字段：`id/postId/floorNumber/content/username/createdAt`
- 管理员侧：
  - `ForumCommentResponse`
  - 字段：`id/postId/floorNumber/content/username/reviewStatus/reviewRemark/createdAt`

## 4. 前端管理员删除按钮对接
- 在管理员评论列表中，点击删除按钮时调用：
  - `DELETE /api/forum/comments/{commentId}/admin`
- 建议放在时间左侧，仅管理员页面显示。
