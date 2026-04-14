# 阿里云 OSS 对接所需信息

为完成论坛图片上传（站位图、瞄点图、落点图、闲聊图），请提供以下信息：

1. `endpoint`
- 示例：`oss-cn-hangzhou.aliyuncs.com`

2. `bucketName`
- 存储桶名称（建议单独用于论坛模块）

3. `accessKeyId`
4. `accessKeySecret`
- 建议创建 RAM 子账号，不使用主账号密钥

5. `region`
- 示例：`cn-hangzhou`

6. 访问策略
- 桶是否公网读
- 是否要求仅通过签名 URL 访问
- 上传目录规划（例如：`forum/posts/yyyy/MM/dd/`）

7. CORS 配置要求
- 允许来源（前端域名）
- 允许方法（`PUT/POST/GET`）
- 允许请求头（`Content-Type`, `Authorization` 等）

8. 文件限制策略
- 单文件大小上限（例如 10MB）
- 允许类型（`jpg/png/webp/gif`）
- 是否需要服务端二次校验

9. 回调/审计（可选）
- 是否需要 OSS 回调到业务服务
- 是否需要操作日志与审计追踪

## 推荐安全做法
- 后端签发短期上传凭证（STS）或预签名 URL，前端直传 OSS。
- 数据库仅保存图片 URL，不保存二进制。
- 生产环境使用环境变量注入密钥，不写死在 `application.properties`。
