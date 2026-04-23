# KinAPP 代码更新记录与项目总览

最后更新: 2026-04-21  
当前基线分支: `master`  
文档用途: 本文件作为项目代码更新展示主文档。当前为初始版本（V0），用于完整记录当前项目状态。后续每次代码变更将在本文件追加或修订。

---

## 1. 项目定位与业务目标

`KinAPP` 是一个面向 `CS2` 玩家社区的 Android 应用，核心围绕两条主线展开:

1. 道具与战术内容管理  
2. 玩家论坛讨论与社区互动

项目在客户端已覆盖完整社区闭环:

- 用户注册/登录
- 发帖与帖子浏览
- 评论与@提及
- 收藏/点赞/举报
- 私信消息中心
- 留言板
- 草稿与个人资料库
- 管理员审核与运营后台
- AI 推荐（OCR + 流式建议）

---

## 2. 技术栈与工程结构

### 2.1 技术栈

- Android 原生（Java）
- AndroidX + Material3
- Room（本地数据库）
- EncryptedSharedPreferences（AI 配置安全存储）
- ML Kit Text Recognition（OCR）
- HttpURLConnection（自实现网络层）
- JUnit4 + AndroidX Test
- Gradle + Version Catalog

### 2.2 构建参数

- `compileSdk`: 35
- `targetSdk`: 35
- `minSdk`: 31
- Java: 11
- 模块: 单模块 `:app`

### 2.3 顶层结构（核心）

- `app/src/main/java/com/example/kin/`
  - `ui/` 页面层（Activity/Fragment）
  - `data/` 仓库与本地存储
  - `net/` 网络与流式 AI 客户端
  - `model/` 业务模型
  - `util/` 工具类（JSON、线程、OCR 解析）
- `app/src/main/res/`
  - `layout/` 主布局
  - `menu/` 底部导航
  - `values/` 主题与颜色
- `app/src/main/Class_Explanation/`
  - 后端 API 与业务说明文档（大量接口契约说明）

---

## 3. 客户端架构与数据流

### 3.1 启动与登录链路

- `LaunchActivity`:
  - 检查本地会话状态
  - 已登录 -> `MainActivity`
  - 未登录 -> `AuthActivity`
- `AuthActivity`:
  - 登录/注册双模式
  - 成功后跳转主页面
- `SessionManager`:
  - 基于 Room 持久化 token 与用户信息
  - 当前默认后端地址: `http://47.105.102.113:9126`

### 3.2 主容器结构

`MainActivity` 使用 `ViewPager2 + BottomNavigationView` 承载 5 个主标签页:

1. `HomeFragment`（首页/论坛）
2. `LibraryFragment`（个人资料库/收藏）
3. `PublishFragment`（发布入口）
4. `AiRecommendFragment`（AI 推荐）
5. `ProfileFragment`（我的）

### 3.3 网络访问分层

- `KinRepository`:
  - 集中封装几乎全部业务接口
  - 对 UI 暴露高层方法（帖子、评论、消息、管理等）
- `ApiClient`:
  - 统一 HTTP GET/POST/PUT/PATCH/DELETE
  - 支持 JSON、multipart、文本响应
  - 自动注入 Bearer Token
- `JsonUtils`:
  - 将 JSON 映射为业务模型
  - 支持分页结构解析

---

## 4. 功能模块详解（V0 基线）

### 4.1 首页论坛（`HomeFragment`）

能力包括:

- 帖子列表（分页）
- 按类型筛选:
  - `PROP_SHARE`（道具分享）
  - `TACTIC_SHARE`（战术分享）
  - `DAILY_CHAT`（日常）
- 排序筛选:
  - `LATEST`
  - `HOT`
  - `MOST_FAVORITE`
- 搜索（关键词、地图、作者）
- 热门关键词展示
- 帖子卡片动作:
  - 详情
  - 点赞/取消点赞
  - 收藏
  - 举报

### 4.2 资料库与收藏（`LibraryFragment`）

双模式:

- 我的条目（`/api/my/library/items`）
- 我的收藏（`/api/my/library/favorites`）

支持:

- 新建个人条目（道具/战术）
- 打开原帖
- 取消收藏
- 图片预览

### 4.3 发布模块（`PublishFragment` + `PublishEditorActivity`）

发布编辑器支持:

- 三类帖子:
  - 道具分享
  - 战术分享
  - 日常聊天
- 本地自动草稿（Room）
- 远端草稿同步（`/api/drafts`）
- 图片选择与上传:
  - 道具贴三图: 站位/瞄点/落点
  - 日常贴多图（最多 10）
- 提交成功后进入帖子详情页

### 4.4 帖子详情与评论（`PostDetailActivity`）

支持:

- 帖子详情展示（状态、审核信息、编辑窗口等）
- 点赞、收藏、私信作者、举报
- 帖子更新/撤回（基于权限）
- 评论列表
- 评论发布（含图片上传）
- 楼中回复（`parentCommentId`）
- @提及用户名提取并提交

### 4.5 消息中心（`MessagesActivity`）

支持:

- 收件箱/发件箱切换
- 消息类型筛选:
  - `SYSTEM_NOTICE`
  - `INTERACTION_REMINDER`
  - `REVIEW_RESULT`
  - `DIRECT`
- 已读状态筛选（全部/未读/已读）
- 未读汇总统计
- 单条标记已读
- 全部已读
- 发送站内信

### 4.6 留言板（`MessageBoardActivity`）

支持:

- 公开留言列表
- 我的留言列表
- 发布留言
- 查看留言详情
- 撤回我的留言

### 4.7 个人中心（`ProfileFragment`）

入口聚合:

- 登录/切换账号
- 我的主页（`UserProfileActivity`）
- 草稿箱（`DraftsActivity`）
- 消息中心
- 留言板
- 我的举报（`MyReportsActivity`）
- 管理员中心（管理员可见）
- 退出登录

### 4.8 管理员中心（`AdminCenterActivity`）

运营与审核能力已非常完整:

- 运营总览指标
- 帖子审核（单审、批量审、驳回模板）
- 评论审核
- 举报处理
- 留言板状态管理
- 审核模板 CRUD
- 审计日志查询与导出
- 用户搜索与用户审计详情

### 4.9 AI 推荐（`AiRecommendFragment`）

新增关键能力:

- AI 配置管理:
  - Base URL
  - API Key（加密存储）
  - Model
  - System Prompt
- 计分板图片输入:
  - 相册选图
  - 拍照
- OCR 识别:
  - 分数
  - 经济
  - KDA
  - 原文
- 流式 AI 输出:
  - 通过 `/v1/chat/completions` SSE 解析增量内容
  - 支持停止与重试

---

## 5. 本地数据与安全

### 5.1 Room 数据库（`kin_local.db`）

包含实体:

- `SessionEntity`
- `LocalDraftEntity`

当前策略:

- `allowMainThreadQueries()`（存在性能风险，后续建议改为异步 DAO）
- `fallbackToDestructiveMigration()`

### 5.2 配置安全

- AI 配置通过 `EncryptedSharedPreferences` 保存
- 若加密初始化失败，回退普通 `SharedPreferences`

### 5.3 网络安全现状

- Manifest 开启 `usesCleartextTraffic=true`
- 当前默认 Base URL 为 HTTP
- 生产环境建议切换 HTTPS 并收紧明文流量策略

---

## 6. 后端 API 覆盖面（客户端已接入）

`KinRepository` 已封装大量接口，主要分组如下:

- 认证: 登录、注册
- 论坛帖子: 列表、搜索、详情、创建、更新、撤回、点赞
- 评论: 列表、创建、审核、删除
- 图片上传: 单图、批量
- 草稿: 保存、列表、详情、删除
- 资料库: 个人条目、收藏、取消收藏
- 消息: 收件箱、发件箱、未读统计、已读
- 留言板: 用户侧 + 管理侧
- 举报: 用户提交 + 管理处理
- 管理后台:
  - 总览
  - 审核模板
  - 用户审计
  - 审计日志导出

---

## 7. 测试现状

已存在单元测试:

- `AiConfigTest`
- `OpenAiStreamClientTest`
- `ScoreboardParserTest`
- 示例测试 `ExampleUnitTest`

仪器测试:

- `ExampleInstrumentedTest`

当前测试重点集中在 AI/OCR 相关工具层，业务页面层自动化覆盖仍较少。

---

## 8. 当前已识别问题与改进建议

### 8.1 编码与文案显示问题

- 多处中文字符串疑似存在编码异常（显示为乱码）
- 建议统一:
  - 源文件编码 UTF-8
  - 文案收敛到 `strings.xml`，减少硬编码

### 8.2 UI 与业务耦合偏高

- 多数页面使用纯 Java 动态构建 UI
- 页面逻辑与网络调用耦合，后续维护成本较高
- 建议逐步引入 ViewModel（MVVM）与状态分层

### 8.3 数据层可维护性

- `KinRepository` 体量较大（600+ 行，职责过多）
- 建议拆分为多业务仓库:
  - PostRepository
  - MessageRepository
  - AdminRepository
  - AiRepository

### 8.4 网络与稳定性

- API 客户端基于 HttpURLConnection 手写实现，功能可用但扩展性有限
- 建议中长期迁移到 Retrofit + OkHttp（拦截器、日志、重试、超时策略更完善）

### 8.5 Room 使用方式

- `allowMainThreadQueries()` 不适合复杂场景
- 建议替换为:
  - DAO 异步调用
  - 或 coroutines（未来 Kotlin 化）

---

## 9. 关键代码清单（便于后续更新对照）

- 启动与容器:
  - `app/src/main/java/com/example/kin/ui/LaunchActivity.java`
  - `app/src/main/java/com/example/kin/MainActivity.java`
- 核心数据层:
  - `app/src/main/java/com/example/kin/data/KinRepository.java`
  - `app/src/main/java/com/example/kin/net/ApiClient.java`
  - `app/src/main/java/com/example/kin/util/JsonUtils.java`
- 论坛与发布:
  - `.../ui/HomeFragment.java`
  - `.../ui/PublishEditorActivity.java`
  - `.../ui/PostDetailActivity.java`
- 消息与留言:
  - `.../ui/MessagesActivity.java`
  - `.../ui/MessageBoardActivity.java`
- 管理端:
  - `.../ui/admin/AdminCenterActivity.java`
- AI 模块:
  - `.../ui/AiRecommendFragment.java`
  - `.../net/OpenAiStreamClient.java`
  - `.../util/ScoreboardParser.java`
  - `.../data/AiConfigStore.java`

---

## 10. 更新记录

### V0 (2026-04-21)

- 新建本文件 `CODE_UPDATE_LOG.md` 作为代码更新展示主文档
- 完成当前 Android 客户端项目的全量初始梳理与基线总结
- 后续所有功能迭代、重构、修复将在此文件持续更新


### V1 (2026-04-23)

#### Scope
Implemented the full V1 package in one release:
- brand icon refresh
- bottom navigation height optimization and direct tab switching
- 30-day session keepalive with encrypted remembered credentials
- local dual-model OCR (Latin + Chinese) with structured scoreboard extraction
- dedicated AI settings page with provider presets
- AI recommendation strategy upgrade (local library/favorites first, then general CS2 knowledge)

#### 1) Brand and Navigation UX
- Replaced launcher foreground with a centered pure-black crown and kept adaptive icon wiring unchanged:
  - @mipmap/ic_launcher and @mipmap/ic_launcher_round still point to adaptive icon XML.
  - foreground vector now renders crown at roughly 70% width / 50% height visual footprint.
- Updated launcher background to light gray-white layered style matching app base tone.
- Reduced bottom navigation visual height:
  - decreased container padding in g_bottom_bar.xml
  - tuned item top/bottom paddings and min height in ctivity_main.xml
- Eliminated intermediate page slide behavior during tab jumps:
  - all setCurrentItem(..., true) changed to alse
  - includes switchToPublish() and bottom-nav selection handling
- Added top-right AI settings entry in main toolbar via menu_main_top.xml.

#### 2) Session Keepalive (30 Days)
- Extended local session model:
  - SessionEntity added expiresAt
  - DB version bumped 1 -> 2 in KinDatabase
- SessionManager now enforces expiry-based login validity:
  - isLoggedIn() only true when token exists and expiresAt is valid
  - expired session is cleared automatically
  - session TTL is fixed at 30 days (SESSION_TTL_MS)
- Added encrypted remembered-credential store:
  - new LoginCredentialStore using EncryptedSharedPreferences (fallback to normal preferences if needed)
  - persists username/password + credential expiry
- Updated login flow in KinRepository:
  - login(...) now supports remember behavior
  - successful login stores both session and encrypted remembered credentials
- Added silent auto-login API:
  - KinRepository.tryAutoLogin(...)
- Added unified logout:
  - KinRepository.logout() clears session + encrypted remembered credentials
- Startup/resume behavior:
  - LaunchActivity now attempts silent auto-login when no active token
  - MainActivity.onResume() attempts session restore before forcing auth screen
- Manual sign-out semantics preserved:
  - profile sign-out uses repository logout and requires explicit re-login.

#### 3) Local OCR Upgrade (Scoreboard Structuring)
- Added Chinese OCR dependency:
  - com.google.mlkit:text-recognition-chinese
- New orchestrator:
  - ScoreboardOcrOrchestrator
  - runs Latin and Chinese recognizers locally, merges outputs, then calls parser
- Expanded scoreboard model:
  - ScoreboardSnapshot now includes:
    - map name
    - dual raw text fields (latinRawText, chineseRawText)
    - player structured summary
    - hot-hand summary
    - structured player list (PlayerStat with K/D/A/money)
- Reworked parser (ScoreboardParser):
  - score extraction
  - money extraction
  - K/D/A extraction
  - map alias matching (EN + CN aliases via unicode escapes)
  - heuristic player assembly
  - hot-hand candidate summary
- AI page now displays structured OCR results, not only raw OCR text.

#### 4) AI Configuration and Recommendation Strategy
- Added dedicated AiSettingsActivity:
  - provider preset selector
  - baseUrl / apiKey / model / system prompt
  - preset auto-fill + manual override
- Added profile entry (Me page) to open AI settings.
- Added top toolbar settings entry from main container.
- AiConfig extended with providerId.
- AiConfigStore now persists provider id in encrypted storage.
- Added provider preset registry (AiProviderPreset) with defaults:
  - Tongyi (Qwen)
  - OpenAI
  - Claude (OpenAI-compatible route)
  - Doubao
  - DeepSeek
- AI recommendation page behavior changes:
  - when model config is missing, shows a feed-like reminder card guiding user to settings
  - recommendation request now injects prioritized local context from:
    - user library records
    - favorites
  - local candidates are ranked by map/type relevance before prompt injection
  - when local match score is weak, prompt explicitly allows model to fill with general CS2 knowledge
  - first release intentionally does not connect external search API.

#### 5) Streaming Prompt Chain Upgrade
- OpenAiStreamClient extended:
  - new streamScoreboardAdvice(..., libraryContext, ...) overload
  - user prompt now includes:
    - structured OCR fields
    - hot-hand signal
    - local library priority context
    - explicit fallback behavior
- Maintained backward compatibility by keeping old method signature overload.

#### 6) Files Added
- pp/src/main/java/com/example/kin/data/LoginCredentialStore.java
- pp/src/main/java/com/example/kin/model/AiProviderPreset.java
- pp/src/main/java/com/example/kin/ui/AiSettingsActivity.java
- pp/src/main/java/com/example/kin/util/ScoreboardOcrOrchestrator.java
- pp/src/main/res/menu/menu_main_top.xml

#### 7) Files Updated (Core)
- pp/src/main/java/com/example/kin/MainActivity.java
- pp/src/main/java/com/example/kin/ui/LaunchActivity.java
- pp/src/main/java/com/example/kin/ui/ProfileFragment.java
- pp/src/main/java/com/example/kin/ui/AiRecommendFragment.java
- pp/src/main/java/com/example/kin/data/KinRepository.java
- pp/src/main/java/com/example/kin/data/SessionManager.java
- pp/src/main/java/com/example/kin/data/AiConfigStore.java
- pp/src/main/java/com/example/kin/model/AiConfig.java
- pp/src/main/java/com/example/kin/model/ScoreboardSnapshot.java
- pp/src/main/java/com/example/kin/util/ScoreboardParser.java
- pp/src/main/java/com/example/kin/net/OpenAiStreamClient.java
- pp/src/main/java/com/example/kin/data/local/SessionEntity.java
- pp/src/main/java/com/example/kin/data/local/KinDatabase.java
- pp/src/main/res/layout/activity_main.xml
- pp/src/main/res/drawable/bg_bottom_bar.xml
- pp/src/main/res/drawable/ic_launcher_foreground.xml
- pp/src/main/AndroidManifest.xml
- pp/build.gradle
- gradle/libs.versions.toml
- pp/src/test/java/com/example/kin/ScoreboardParserTest.java

#### 8) Verification Result
- Unit tests:
  - command: ./gradlew.bat testDebugUnitTest
  - result: PASS
- Debug build:
  - command: ./gradlew.bat assembleDebug
  - result: PASS
- Noted build warning:
  - libmlkit_google_ocr_pipeline.so strip warning (packaged as-is), no build block.

#### 9) Behavior Outcome Checklist
- Icon style updated to light background + black crown: done
- Bottom nav visually reduced: done
- Cross-tab jump no intermediate swipe animation: done
- Session persistence for 30 days + silent restore + logout clear: done
- OCR local enhanced extraction (score/map/user/money/KDA/hot-hand): done
- AI settings entry from top-right and Me page: done
- AI recommendation local-library-first prompt strategy: done
