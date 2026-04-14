# LuankinStation 功能实现说明（APP 研发协作文档）

## 1. 项目目标与定位
LuankinStation 后端定位为社区型业务服务，核心实现包括：
- 用户体系（注册/登录/JWT）
- 论坛内容（发帖、评论、审核、图片）
- 互动触达（站内信、@提醒）
- 用户沉淀（草稿、我的库、收藏）
- 治理能力（举报、管理员处理、审计）
- 运营分析（管理总览）

## 2. 技术架构

### 2.1 技术栈
- Spring Boot 4.0.3
- Spring MVC + Spring Security
- JWT（jjwt）
- Spring Data JPA + MySQL
- 阿里云 OSS（可开关）

### 2.2 代码分层
- `Api/Controller`：接收参数、鉴权、返回 HTTP 状态
- `Service`：业务规则与流程编排
- `Repository`：数据库访问
- `Entity`：数据模型
- `DTO`：输入输出协议

### 2.3 安全机制
- 无状态认证（Stateless）
- `JwtAuthFilter` 每次请求解析 token 注入上下文
- `SecurityConfig` 统一配置：
- `/api/auth/login`、`/api/auth/register` 放行
- `/api/admin/**` 强制管理员角色
- 其他接口默认必须登录

## 3. 核心业务实现

## 3.1 用户注册与登录
- 注册：
- 校验用户名/邮箱唯一性
- 密码 BCrypt 哈希存储
- 默认角色 `USER`
- 登录：
- 用户名+密码匹配
- 成功签发 JWT（带 `username` 与 `role` claim）

## 3.2 论坛帖子

### 3.2.1 帖子类型化模型
- `PROP_SHARE`：道具点分享
- `TACTIC_SHARE`：战术分享
- `DAILY_CHAT`：日常讨论
- `OTHER`：其他

系统根据 `postType` 做强校验，不同类型有不同必填字段约束，避免脏数据。

### 3.2.2 审核与状态流转
- 新建/编辑帖子后进入 `PENDING`
- 管理员可改为 `APPROVED` 或 `REJECTED`
- 作者可在 `PENDING` 状态下主动 `WITHDRAWN`
- 管理员删除帖子时，底层实现为“转驳回”（`REJECTED`）并记录原因

### 3.2.3 编辑窗口控制
- 审核通过后存在 60 分钟编辑窗口（`editableUntil`）
- 普通用户超时后不可编辑
- 管理员不受该限制

### 3.2.4 搜索能力
- 支持关键词、类型、地图、作者等条件
- 内置地图别名扩展（中英文别名归一搜索）

### 3.2.5 审核结果通知
- 帖子审核后会自动给作者发站内信
- 消息类型：`REVIEW_RESULT`

## 3.3 论坛评论与 @提醒

### 3.3.1 评论发布
- 仅允许对“已通过帖子”评论
- 评论当前默认自动通过（`APPROVED`）
- 支持 JSON 与 multipart 两种发布方式

### 3.3.2 评论图片
- 支持多图，最多 10 张
- 图片 URL 存储在独立表并保序（`sortOrder`）

### 3.3.3 @提醒实现
- 触发时机：评论创建成功后
- 提及来源：
- `content` 文本解析 `@username`
- `mentionUsernames` 字段显式传入
- 规则：
- 去重后最多 10 人
- 忽略自己
- 忽略不存在用户
- 发送站内信类型：`INTERACTION_REMINDER`
- 当前模板文案：
- `用户 @A 在帖子#X 的第Y楼评论中提及了你（评论ID：Z）：<评论预览>`

## 3.4 图片上传（OSS）
- `oss.enabled=true` 时开放上传接口
- 校验：
- 扩展名白名单（默认 `jpg/jpeg/png/webp/gif`）
- 大小限制（默认 10MB）
- 批量限制（最多 10 张）
- 存储：
- 按 `baseDir/module/yyyy/MM/dd/uuid.ext` 生成对象路径
- 可返回公网 URL（public-read）或签名 URL

## 3.5 草稿系统
- 支持 `FORUM_POST`、`MY_LIBRARY_ITEM`
- 支持新增/更新、分页查询、详情、删除
- 正式创建帖子或我的库条目后会尝试静默删除关联草稿

## 3.6 我的库与收藏
- 自建条目支持 `PROP_SHARE`、`TACTIC_SHARE`
- 收藏仅允许收藏“已审核通过”的论坛帖子
- 不允许收藏自己的帖子
- 取消收藏采用逻辑删除（保留历史关系）

## 3.7 站内信
- 消息类型：
- 系统通知 `SYSTEM_NOTICE`
- 审核结果 `REVIEW_RESULT`
- 互动提醒 `INTERACTION_REMINDER`
- 私信 `DIRECT`
- 功能：
- 发信、收件箱、发件箱
- 单条已读、批量已读
- 未读聚合统计（总量+分类型）

## 3.8 留言板
- 用户可创建留言、查看公开状态留言、查看自己的留言
- 用户或管理员可撤回
- 状态包含：`PUBLISHED`、`ADOPTED`、`IMPLEMENTED`、`REVOKED`
- 用户侧不可见 `REVOKED`

## 3.9 举报治理
- 用户可举报帖子/评论
- 同一用户对同一目标在 `PENDING` 状态不可重复举报
- 管理员处理后置为 `PROCESSED`

## 3.10 管理后台
- 审核模板管理（驳回模板）
- 举报处理台
- 留言板管理台
- 用户审计（用户维度全量行为聚合）
- 操作审计日志（查询+CSV 导出）
- 运营总览（多维统计、趋势、分布、Top 榜）

## 4. 数据设计要点

### 4.1 模块化表设计
- 论坛主表 + 图片子表（帖子/评论分离）
- 收藏、点赞关系表单独维护
- 举报、站内信、草稿、留言板各自独立

### 4.2 时间字段约定
- 创建时间：`@CreationTimestamp`
- 更新时间：`@UpdateTimestamp`

### 4.3 约束策略
- 关键业务唯一约束：
- 收藏唯一：`owner_username + forum_post_id`
- 点赞唯一：`post_id + username`
- 其他关系多为逻辑关联（`id/username`）实现，便于演进

## 5. APP 端对接建议

## 5.1 状态驱动 UI
- 帖子列表/详情根据 `reviewStatus`、`canEdit`、`canWithdraw` 控制按钮显隐
- 评论区按 `INTERACTION_REMINDER` 展示 @提醒入口
- 留言板按状态展示业务进度（已发布/已采纳/已实现）

## 5.2 推荐接入顺序
1. 账号与 token 体系
2. 帖子列表/详情/发布
3. 评论与图片上传
4. 消息中心（未读统计、提醒）
5. 草稿与我的库
6. 留言板与举报
7. 管理端（若 APP 含管理能力）

## 5.3 错误处理建议
- 对所有 `4xx/5xx` 统一读取 `message`
- 对 `401/403` 做 token 失效或权限不足分流
- 对分页接口统一封装 `items/page/size/total/totalPages`

## 6. 当前能力边界与后续扩展
- `profile` 服务（成长徽章、活跃天数）已实现但尚未开放 API。
- 部分 DTO（如热词、搜索建议、点赞状态）已存在，但当前路由未暴露，后续可按 APP 需求逐步开放。
- 管理审计日志写入点已具备基础能力，可继续覆盖更多管理员动作以增强可追踪性。
