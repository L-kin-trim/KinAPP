# ForumCommentImageUpload 类说明（与 ForumPost_Classes 文档风格一致）

## 1. 类名：`ForumComment`（Controller）
- 路径：`src/main/java/com/luankin/luankinstation/forum/ForumComment.java`
- 用途：论坛评论接口控制器，支持评论内容提交、评论图片上传后自动关联、评论查询与管理审核。
- 基础路径：`/api/forum/comments`

### 接口：`POST /api/forum/comments/posts/{postId}`（JSON）
- 功能：创建评论，支持前端直接传 OSS 图片地址列表。
- 鉴权：需要 JWT。
- `Content-Type`：`application/json`
- 请求体：
```json
{
  "content": "这套点位很实用",
  "imageUrls": [
    "https://kinapp.oss-cn-qingdao.aliyuncs.com/forum/comments/2026/03/26/a.jpg"
  ]
}
```
- 约束：
  - `content`：必填，最大 2000 字符。
  - `imageUrls`：可选，最多 10 张，每项最大 500 字符。

### 接口：`POST /api/forum/comments/posts/{postId}`（Multipart）
- 功能：创建带图片评论，后端接收图片并上传 OSS，上传成功后将 URL 保存到数据库并与评论绑定。
- 鉴权：需要 JWT。
- `Content-Type`：`multipart/form-data`
- 表单参数：
  - `content`：评论内容（必填）
  - `files`：图片文件列表（可选，多文件）
  - `module`：OSS 子目录（可选，默认 `comments`）
- 处理流程：
  1. 调用 `ForumImageUploadService.uploadBatch` 批量上传 OSS。
  2. 获取返回 URL 列表并写入评论请求对象。
  3. 创建评论主记录。
  4. 将图片 URL 写入 `forum_comment_image` 表并按顺序关联评论。

### 接口：`GET /api/forum/comments/posts/{postId}`
- 功能：查询用户侧评论列表（仅 `APPROVED`）。
- 结果：每条评论包含 `imageUrls`，可直接用于前端渲染评论图片。

## 2. 类名：`ForumCommentService`
- 路径：`src/main/java/com/luankin/luankinstation/forum/service/ForumCommentService.java`
- 用途：评论业务层，负责评论创建、图片 URL 关联存储、评论查询回填图片列表。
- 核心能力：
  - 评论创建前校验帖子是否存在且已审核通过。
  - 楼层号自动递增。
  - 评论默认审核状态 `APPROVED`。
  - 图片 URL 校验（最多 10 张、非空校验）。
  - 新增图片关联存储：
    - `replaceCommentImages(commentId, imageUrls)`
    - `getImageUrls(commentId)`
  - 对用户与管理员返回 DTO 都补充 `imageUrls`。

## 3. 请求 DTO：`CreateForumCommentRequest`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/CreateForumCommentRequest.java`
- 用途：评论创建请求。
- 字段：
  - `content`：评论内容。
  - `imageUrls`：评论图片 URL 列表（可选，最多 10 项）。

## 4. 响应 DTO：`ForumCommentPublicResponse`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ForumCommentPublicResponse.java`
- 用途：用户侧评论响应。
- 关键字段：`id/postId/floorNumber/content/imageUrls/username/createdAt`

## 5. 响应 DTO：`ForumCommentResponse`
- 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ForumCommentResponse.java`
- 用途：管理员侧评论响应。
- 关键字段：`id/postId/floorNumber/content/imageUrls/username/reviewStatus/reviewRemark/createdAt`

## 6. 新增实体：`ForumCommentImage`
- 路径：`src/main/java/com/luankin/luankinstation/forum/entity/ForumCommentImage.java`
- 表名：`forum_comment_image`
- 用途：评论与图片 URL 的关联表，支持一条评论多图。
- 字段：`id/commentId/imageUrl/sortOrder/createdAt`

## 7. 新增仓库：`ForumCommentImageRepository`
- 路径：`src/main/java/com/luankin/luankinstation/forum/repository/ForumCommentImageRepository.java`
- 用途：评论图片关联数据访问层。
- 关键方法：
  - `findByCommentIdOrderBySortOrderAsc`
  - `deleteByCommentId`

## 8. 数据库变更
- 脚本：`Class_Explanation/ForumDatabase.sql`
- 新增表：
```sql
CREATE TABLE IF NOT EXISTS forum_comment_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_comment_image_comment FOREIGN KEY (comment_id) REFERENCES forum_comment(id) ON DELETE CASCADE,
  INDEX idx_comment_image_comment_sort (comment_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 9. 前端接入建议（评论发图）
1. 优先使用 `multipart/form-data` 评论创建接口，直接上传 `files`。
2. 若前端已有 OSS URL，可使用 JSON 接口直接传 `imageUrls`。
3. 评论列表返回已带 `imageUrls`，前端无需二次拼接。
