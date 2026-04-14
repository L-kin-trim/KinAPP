# ForumImageUpload 类说明

## 1. 类名：`ForumImageUpload`（Controller）
- 路径：`src/main/java/com/luankin/luankinstation/forum/ForumImageUpload.java`
- 用途：论坛图片上传接口，调用 OSS 存储并返回可访问 URL。
- 启用条件：`oss.enabled=true`

### 接口：`POST /api/forum/images/upload`
- 功能：单张图片上传
- 鉴权：需要 JWT
- 请求类型：`multipart/form-data`
- 表单字段：
  - `file`：图片文件（必填）
  - `module`：目录模块（可选，默认 `posts`，示例：`prop-share`）
- 成功返回（201）：
```json
{
  "fileName": "smoke-a.png",
  "objectKey": "forum/prop-share/2026/03/26/xxxx.png",
  "url": "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/forum/prop-share/2026/03/26/xxxx.png",
  "contentType": "image/png",
  "size": 234567
}
```

### 接口：`POST /api/forum/images/upload/batch`
- 功能：多张图片上传（最多10张）
- 鉴权：需要 JWT
- 请求类型：`multipart/form-data`
- 表单字段：
  - `files`：图片文件数组（必填）
  - `module`：目录模块（可选）
- 成功返回（201）：
```json
{
  "items": [
    {
      "fileName": "chat1.jpg",
      "objectKey": "forum/chat/2026/03/26/xxxx.jpg",
      "url": "https://...",
      "contentType": "image/jpeg",
      "size": 123456
    }
  ],
  "total": 1
}
```

## 2. 类名：`ForumImageUploadService`
- 路径：`src/main/java/com/luankin/luankinstation/forum/service/ForumImageUploadService.java`
- 核心逻辑：
  - 校验图片扩展名（默认：`jpg,jpeg,png,webp,gif`）
  - 校验单图大小（默认上限：10MB）
  - 校验批量数量（最多10张）
  - 生成 OSS 对象路径并上传

## 3. 相关 DTO
- `ForumImageUploadResponse`
  - 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ForumImageUploadResponse.java`
- `ForumImageBatchUploadResponse`
  - 路径：`src/main/java/com/luankin/luankinstation/forum/dto/ForumImageBatchUploadResponse.java`
