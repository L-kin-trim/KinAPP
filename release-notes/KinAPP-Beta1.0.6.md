# KinAPP Beta1.0.6

对比 `KinAPP Beta1.0.5`，本版本主要更新：

- 点赞功能接入后端新增的 `POST/DELETE/GET /api/forum/posts/{postId}/like` 接口。
- 帖子详情页会主动拉取当前用户的点赞状态，点击点赞图标可正确点赞或取消点赞。
- 收藏功能改为无请求体 `POST`，并接入 `GET /api/my/library/favorites/{postId}` 查询收藏状态。
- 帖子详情页收藏图标支持按真实状态切换收藏/取消收藏。

后端配套：

- Spring Boot 服务新增帖子点赞 Controller 路由与服务层逻辑。
- Spring Boot 服务新增单个帖子收藏状态查询接口。
- 已修正前端与后端点赞、收藏接口契约不一致导致的交互失败。
