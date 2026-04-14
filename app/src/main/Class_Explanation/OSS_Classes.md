# OSS 工具类说明

## 1. 配置类：`OssProperties`
- 路径：`src/main/java/com/luankin/luankinstation/oss/config/OssProperties.java`
- 用途：读取 `application.properties` 中 `oss.*` 配置。
- 关键配置项：
  - `oss.enabled`：是否启用 OSS（默认 `false`）
  - `oss.endpoint`：OSS 访问域名
  - `oss.region`：区域（如 `cn-hangzhou`）
  - `oss.bucket-name`：Bucket 名称
  - `oss.access-key-id`：阿里云账号 AK
  - `oss.access-key-secret`：阿里云账号 SK
  - `oss.base-dir`：默认上传根目录
  - `oss.public-read`：是否公网读
  - `oss.cdn-domain`：可选，配置后返回 CDN 域名 URL
  - `oss.url-expire-seconds`：私有读时签名 URL 过期秒数
  - `oss.max-file-size-mb`：单图大小上限（MB）
  - `oss.allowed-extensions`：允许上传的图片扩展名列表

## 2. 配置装配类：`OssConfig`
- 路径：`src/main/java/com/luankin/luankinstation/oss/config/OssConfig.java`
- 用途：创建 OSS 客户端 Bean。
- 行为：仅当 `oss.enabled=true` 时创建 `OSS` 客户端。

## 3. 工具服务类：`OssStorageService`
- 路径：`src/main/java/com/luankin/luankinstation/oss/service/OssStorageService.java`
- 用途：提供上传、删除、URL 生成等能力。
- 核心方法：
  - `uploadBytes(byte[] content, String module, String originalFilename, String contentType)`
  - `uploadStream(InputStream inputStream, long contentLength, String contentType, String objectKey)`
  - `getSignedUrl(String objectKey, long expireSeconds)`
  - `delete(String objectKey)`
  - `buildObjectKey(String module, String originalFilename)`
  - `getPublicUrl(String objectKey)`

## 4. 你需要提供的账号信息（配置形式）
在 `src/main/resources/application.properties` 中填写：

```properties
oss.enabled=true
oss.endpoint=oss-cn-hangzhou.aliyuncs.com
oss.region=cn-hangzhou
oss.bucket-name=你的bucket
oss.access-key-id=你的AccessKeyId
oss.access-key-secret=你的AccessKeySecret
oss.base-dir=forum
oss.public-read=true
oss.cdn-domain=
oss.url-expire-seconds=3600
oss.max-file-size-mb=10
oss.allowed-extensions=jpg,jpeg,png,webp,gif
```

建议：
- 使用 RAM 子账号的 AK/SK，不要使用主账号密钥。
- 生产环境通过环境变量注入，不把密钥提交到 Git。
