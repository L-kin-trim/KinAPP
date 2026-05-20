package com.example.kin.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FutureFeatureRegistry {
    private static final Map<String, FutureFeatureGroup> GROUPS = new LinkedHashMap<>();
    private static final Map<String, FutureFeatureDefinition> FEATURES = new LinkedHashMap<>();
    private static final Map<String, String> API_KEYS = new LinkedHashMap<>();

    static {
        registerApiKeys();
        group("content", "发帖与内容创作升级", "编辑器、模板、质量检测、多媒体、协作与定时发布。", "/api/content");
        f("content.markdown_editor", "content", "2.1", "日常闲聊帖富文本或 Markdown 编辑器", "Markdown 工具栏、预览和自动草稿。", true, false);
        f("content.post_templates", "content", "2.2", "发帖模板系统", "按帖子类型生成结构化输入区。", false, false);
        f("content.section_editor", "content", "2.3", "帖子分段编辑与目录导航", "长战术帖章节填写和目录锚点。", false, false);
        f("content.quality_check", "content", "2.4", "发帖前质量检测", "发布前检查标题、地图、图片、敏感词和格式。", false, false);
        f("content.version_history", "content", "2.5", "帖子版本历史", "查看版本快照、差异摘要和回滚记录。", false, false);
        f("content.image_annotation", "content", "2.6", "图片编辑与标注", "站位图、瞄点图、落点图标注 JSON。", false, false);
        f("content.video_gif_demo", "content", "2.7", "视频与 GIF 道具演示", "短视频/GIF 演示、封面和转码任务。", false, true);
        f("content.voice_to_text", "content", "2.8", "语音转文字发帖", "录音转写后填入编辑器。", true, true);
        f("content.multilingual_translation", "content", "2.9", "多语言发帖与自动翻译", "中文内容一键生成英文版本。", true, true);
        f("content.collaborative_draft", "content", "2.10", "发帖协作草稿", "邀请队友共创并记录编辑状态。", false, false);
        f("content.scheduled_publish", "content", "2.11", "定时发布", "未来时间发布和待发布状态管理。", false, true);
        f("content.quote_cards", "content", "2.12", "帖子引用与卡片嵌入", "引用帖子、用户、地图和道具条目。", false, false);

        group("community", "首页、社区与内容消费升级", "推荐、关注、热榜、话题、合集、问答、投票与阅读体验。", "/api/community");
        f("community.recommend_feed", "community", "3.1", "个性化推荐信息流", "基于行为、地图和道具偏好的推荐标签。", true, false);
        f("community.following_feed", "community", "3.2", "关注用户动态流", "关注作者/好友并查看动态。", false, false);
        f("community.trending_rank", "community", "3.3", "热榜与趋势榜", "今日、七日、新人、地图和评论增长榜。", true, false);
        f("community.topics", "community", "3.4", "话题系统", "发帖话题和话题聚合页。", false, false);
        f("community.collections", "community", "3.5", "内容合集", "创建合集并加入多个帖子。", false, false);
        f("community.beginner_zone", "community", "3.6", "新手专区", "地图点位、报点、经济和枪械引导。", false, false);
        f("community.qa_zone", "community", "3.7", "问答专区", "提问、回答和采纳。", false, false);
        f("community.poll_post", "community", "3.8", "投票帖", "帖子内投票选项和结果。", false, false);
        f("community.reading_progress", "community", "3.9", "帖子阅读进度", "记录长文阅读位置和已读状态。", false, false);
        f("community.read_later", "community", "3.10", "稍后阅读", "加入稍后阅读并按地图筛选。", false, false);
        f("community.share_poster", "community", "3.11", "帖子分享海报", "生成含标题、地图、作者和二维码的海报。", false, true);
        f("community.similar_content", "community", "3.12", "内容相似推荐", "同地图、同作者和相似道具推荐。", true, false);

        group("cs2", "CS2 道具与战术专业能力升级", "地图点位、轨迹、练习、战术板、经济、报点和职业索引。", "/api/cs2");
        f("cs2.interactive_map_points", "cs2", "4.1", "交互式地图道具点位", "俯视图查看站位、瞄点、落点、爆炸范围和路线。", false, false);
        f("cs2.utility_trajectory", "cs2", "4.2", "道具轨迹展示", "投掷起点、空中轨迹、落点和爆开范围。", false, false);
        f("cs2.utility_difficulty", "cs2", "4.3", "道具难度评级", "难度、容错率、跳投/跑投和练习次数。", false, false);
        f("cs2.practice_plan", "cs2", "4.4", "一键生成练习清单", "按地图和目标点位生成今日练习。", true, false);
        f("cs2.practice_checkin", "cs2", "4.5", "道具练习打卡", "成功次数、失败原因和手感备注。", false, false);
        f("cs2.tactic_board_editor", "cs2", "4.6", "战术板编辑器", "地图上拖拽队员、画路线、放道具图标。", false, false);
        f("cs2.tactic_timeline", "cs2", "4.7", "战术步骤播放", "按时间轴播放默认站位、给烟、爆弹、进点。", false, false);
        f("cs2.default_tactic_library", "cs2", "4.8", "默认战术库", "职业队默认战术和爆弹模板。", false, false);
        f("cs2.economy_strategy", "cs2", "4.9", "经济局策略推荐", "按经济、比分和连败奖励推荐购买策略。", true, false);
        f("cs2.callout_learning", "cs2", "4.10", "地图报点学习", "中英文报点、别名、发音和测验。", false, false);
        f("cs2.utility_version_tracking", "cs2", "4.11", "道具适用版本追踪", "适用版本、疑似失效和最近验证时间。", false, false);
        f("cs2.utility_invalid_feedback", "cs2", "4.12", "道具失效反馈", "已失效/仍可用反馈和验证材料。", false, true);
        f("cs2.clutch_library", "cs2", "4.13", "对局残局库", "1vX/2vX 残局条件、步骤和成功率。", false, false);
        f("cs2.default_positions", "cs2", "4.14", "进攻/防守默认站位库", "T/CT 默认站位、职责和补枪关系。", false, false);
        f("cs2.buy_plan", "cs2", "4.15", "枪械与道具购买方案", "按经济、位置和角色生成购买配置。", true, false);
        f("cs2.pro_match_index", "cs2", "4.16", "职业比赛战术索引", "按战队、赛事、地图筛选职业案例。", false, false);

        group("ai", "AI 与智能助手升级", "聊天助手、道具推荐、战术生成、审核、标签、复盘和搜索问答。", "/api/ai");
        f("ai.chat_assistant", "ai", "5.1", "AI 闲聊助手常驻入口", "常驻聊天助手与 CS2/社区问答。", true, false);
        f("ai.utility_recommendation", "ai", "5.2", "AI 道具推荐", "按地图、阵营、目标点和经济推荐道具。", true, false);
        f("ai.tactic_generator", "ai", "5.3", "AI 战术生成器", "生成完整战术步骤和道具分工。", true, false);
        f("ai.title_summary", "ai", "5.4", "AI 帖子标题与摘要生成", "一键生成标题、摘要、标签和搜索词。", true, false);
        f("ai.comment_summary", "ai", "5.5", "AI 评论总结", "总结长评论区讨论、高赞建议和争议点。", true, false);
        f("ai.review_assist", "ai", "5.6", "AI 审核辅助", "审核风险标签、违规原因和历史参考。", true, false);
        f("ai.auto_tagging", "ai", "5.7", "AI 自动打标签", "自动填充地图、道具类型、战术和难度。", true, false);
        f("ai.image_point_recognition", "ai", "5.8", "AI 图片识别道具点位", "截图识别地图、位置和准星候选。", true, true);
        f("ai.demo_review", "ai", "5.9", "AI 对局复盘", "上传战绩/Demo 片段生成复盘建议。", true, true);
        f("ai.personal_coach", "ai", "5.10", "AI 个人教练", "根据练习和收藏生成周训练计划。", true, false);
        f("ai.search_qa", "ai", "5.11", "AI 搜索问答", "自然语言搜索并返回可追溯答案。", true, false);
        f("ai.reply_suggestion", "ai", "5.12", "AI 自动回复建议", "为评论或私信提供语气化候选回复。", true, false);
        f("ai.hot_content_insight", "ai", "5.13", "AI 热门内容解读", "解释为什么火、适合谁和风险点。", true, false);
        f("ai.recommend_reason", "ai", "5.14", "AI 个性化首页解释", "为推荐卡片显示推荐理由。", true, false);

        group("user", "用户体系、个人主页与成长系统", "个人主页、等级、徽章、信誉、隐私、安全和认证。", "/api/users/features");
        f("user.full_profile", "user", "6.1", "完整个人主页", "头像、简介、地图、角色、帖子、收藏和关系。", false, false);
        f("user.level_xp", "user", "6.2", "用户等级与经验", "等级、经验条、升级提示和经验来源。", false, false);
        f("user.badges", "user", "6.3", "徽章系统", "道具大师、战术作者、热心回答者等徽章。", false, false);
        f("user.reputation", "user", "6.4", "用户信誉分", "个人主页和评论区可信标识。", false, false);
        f("user.achievements", "user", "6.5", "成就系统", "已完成/待完成成就与奖励。", false, false);
        f("user.privacy_settings", "user", "6.6", "用户隐私设置", "收藏、练习、在线状态和私信权限。", false, false);
        f("user.blocklist", "user", "6.7", "黑名单与屏蔽", "屏蔽帖子、评论、私信和提醒。", false, false);
        f("user.security_center", "user", "6.8", "账号安全中心", "登录设备、记录、密码和退出其他设备。", false, false);
        f("user.third_party_login", "user", "6.9", "第三方登录", "Steam、QQ、微信或邮箱验证码入口。", false, false);
        f("user.verification_badge", "user", "6.10", "用户认证标识", "官方作者、职业选手、教练和管理员认证。", false, false);

        group("library", "资料库、收藏与知识管理升级", "文件夹、标签、搜索、离线、备注、分享、导入导出和提醒。", "/api/library");
        f("library.folders", "library", "7.1", "资料库文件夹", "创建文件夹并分类收藏和自建条目。", false, false);
        f("library.tags", "library", "7.2", "资料库标签", "给条目打自定义标签。", false, false);
        f("library.full_text_search", "library", "7.3", "资料库全文搜索", "搜索标题、正文、标签和备注。", false, false);
        f("library.offline_library", "library", "7.4", "离线资料库", "离线包和本地可用标记。", false, true);
        f("library.favorite_notes", "library", "7.5", "收藏备注", "为收藏添加个人备注。", false, false);
        f("library.share_groups", "library", "7.6", "收藏分组分享", "公开或私密分享收藏分组。", false, false);
        f("library.import_export", "library", "7.7", "资料库导入导出", "导入/导出资料库文件。", false, true);
        f("library.duplicate_merge", "library", "7.8", "重复收藏合并", "发现并合并重复收藏。", false, false);
        f("library.recent_common", "library", "7.9", "最近使用与常用条目", "展示最近和常用资料。", false, false);
        f("library.change_reminder", "library", "7.10", "资料库变更提醒", "收藏内容更新后提醒。", false, false);

        group("interaction", "互动、私信与社区活动升级", "评论、表情、私信、推送、@、活动和兴趣圈。", "/api/interactions");
        f("interaction.comment_like", "interaction", "8.1", "评论点赞与踩", "评论支持点赞、取消点赞和热度排序。", false, false);
        f("interaction.comment_fold_tree", "interaction", "8.2", "评论折叠与楼中楼优化", "长评论串折叠和定位回复。", false, false);
        f("interaction.emoji_stickers", "interaction", "8.3", "表情与贴纸", "CS2 主题表情、战队梗图和贴纸。", false, false);
        f("interaction.dm_conversation", "interaction", "8.4", "私信会话化", "会话列表、连续对话和未读红点。", false, false);
        f("interaction.push_messages", "interaction", "8.5", "消息推送", "审核、评论、@、私信和收藏更新推送。", false, true);
        f("interaction.notification_preferences", "interaction", "8.6", "通知偏好设置", "站内信、推送和免打扰设置。", false, false);
        f("interaction.mention_autocomplete", "interaction", "8.7", "@ 用户自动补全", "最近互动和关注用户搜索建议。", false, false);
        f("interaction.co_creator_thanks", "interaction", "8.8", "内容共创感谢", "标注感谢用户并发送通知。", false, false);
        f("interaction.community_events", "interaction", "8.9", "社区活动页", "官方活动、征稿、挑战和投稿赛。", false, false);
        f("interaction.groups", "interaction", "8.10", "用户组与兴趣圈", "加入地图圈、新手圈和战术研究组。", false, false);

        group("team", "团队、战队与协作训练升级", "战队空间、团队战术、角色、训练、比赛包、复盘和数据看板。", "/api/teams");
        f("team.space", "team", "9.1", "战队空间", "创建战队、邀请队友和集中协作。", false, false);
        f("team.tactic_library", "team", "9.2", "团队战术库", "团队可见战术和权限。", false, false);
        f("team.member_roles", "team", "9.3", "队员角色分工", "IGL、突破手、补枪手等角色。", false, false);
        f("team.training_schedule", "team", "9.4", "训练日程", "训练计划、地图、目标和提醒。", false, false);
        f("team.training_tasks", "team", "9.5", "训练任务分配", "给队员分配烟、战术阅读和问答任务。", false, false);
        f("team.match_tactic_pack", "team", "9.6", "赛前战术包", "地图池、Ban/Pick、默认和应急战术。", false, false);
        f("team.post_match_review", "team", "9.7", "赛后复盘报告", "比分、问题回合、亮点和 AI 总结。", true, false);
        f("team.opponent_research", "team", "9.8", "对手研究库", "对手地图偏好、默认站位和弱点。", false, false);
        f("team.announcements", "team", "9.9", "团队公告", "队长公告、训练要求和近期安排。", false, false);
        f("team.dashboard", "team", "9.10", "团队数据看板", "训练完成率、成员活跃和地图掌握。", false, false);

        group("search", "搜索、标签与推荐系统升级", "高级搜索、联想、历史、高亮、别名、语义和反馈。", "/api/search-features");
        f("search.advanced", "search", "10.1", "高级搜索页", "多条件组合搜索和排序。", false, false);
        f("search.suggestions", "search", "10.2", "搜索联想", "地图、用户、话题、帖子和点位建议。", false, false);
        f("search.history", "search", "10.3", "搜索历史", "最近搜索、热门搜索和清空历史。", false, false);
        f("search.highlight", "search", "10.4", "搜索结果高亮", "标题、正文、地图和作者命中高亮。", false, false);
        f("search.map_alias", "search", "10.5", "地图别名增强", "沙二、D2、Dust2 归一为标准地图。", false, false);
        f("search.pinyin_typo", "search", "10.6", "拼音与错别字搜索", "拼音和轻微错别字容错。", false, false);
        f("search.semantic", "search", "10.7", "语义搜索", "自然语言查找战术和道具。", true, false);
        f("search.negative_feedback", "search", "10.8", "推荐不感兴趣反馈", "不感兴趣、不看作者、减少地图内容。", false, false);
        f("search.cold_start", "search", "10.9", "新内容冷启动推荐", "新鲜内容专区和探索流量。", false, false);
        f("search.quality_score", "search", "10.10", "内容质量分", "高质量标签和低质降曝光。", false, false);

        group("governance", "管理后台、审核与社区治理升级", "审核、申诉、处罚、敏感词、反刷、证据和运营配置。", "/api/admin/governance");
        f("governance.review_priority", "governance", "11.1", "审核队列优先级", "按风险、举报、信誉和热度排序。", true, false);
        f("governance.multi_stage_review", "governance", "11.2", "多级审核", "一审、二审、复核和意见。", false, false);
        f("governance.appeals", "governance", "11.3", "用户申诉", "驳回或处罚后提交申诉并查看进度。", false, false);
        f("governance.punishments", "governance", "11.4", "处罚系统", "禁言、限制发帖、警告和到期时间。", false, false);
        f("governance.sensitive_words", "governance", "11.5", "敏感词管理", "敏感词、替换词和命中策略。", false, false);
        f("governance.anti_spam", "governance", "11.6", "反垃圾与反刷屏", "频繁发帖评论时显示冷却提示。", false, false);
        f("governance.report_evidence", "governance", "11.7", "举报证据链", "举报截图、补充说明和证据展示。", false, true);
        f("governance.admin_stats", "governance", "11.8", "管理员工作台统计", "待审、今日处理、时长和成立率。", false, false);
        f("governance.rbac", "governance", "11.9", "管理员权限分级", "审核员、运营和超级管理员视图。", false, false);
        f("governance.audit_detail", "governance", "11.10", "审计日志详情增强", "变更前后、IP、设备和影响对象。", false, false);
        f("governance.takedown_notice", "governance", "11.11", "内容下架通知", "下架原因、规则链接和申诉入口。", false, false);
        f("governance.ops_config", "governance", "11.12", "运营配置中心", "Banner、话题、活动、公告和热门地图配置。", false, false);

        group("notification", "消息、通知与用户触达升级", "公告、弹窗、聚合卡片、订阅和每周摘要。", "/api/ops");
        f("notification.announcements", "notification", "12.1", "系统公告", "首页或消息中心展示公告。", false, false);
        f("notification.important_popup", "notification", "12.2", "重要通知弹窗", "重大更新或违规通知弹窗确认。", false, false);
        f("notification.aggregate_cards", "notification", "12.3", "消息中心聚合卡片", "同类通知合并展示。", false, false);
        f("notification.subscriptions", "notification", "12.4", "订阅提醒", "订阅地图、话题、作者更新。", false, false);
        f("notification.weekly_digest", "notification", "12.5", "每周摘要", "收藏更新、推荐道具、团队任务和热榜。", true, true);

        group("analytics", "数据统计、运营分析与商业化预留", "埋点、看板、作者、实验、会员、激励、广告和抽奖。", "/api/analytics");
        f("analytics.behavior_events", "analytics", "13.1", "用户行为埋点", "曝光、点击、收藏、分享、搜索和停留。", false, false);
        f("analytics.content_dashboard", "analytics", "13.2", "内容数据看板", "浏览、收藏、点赞、评论和转化率。", false, false);
        f("analytics.creator_center", "analytics", "13.3", "作者中心", "粉丝增长、热门内容和创作建议。", true, false);
        f("analytics.ab_experiment", "analytics", "13.4", "A/B 实验", "首页布局、策略和文案实验。", false, false);
        f("analytics.membership_reserved", "analytics", "13.5", "会员能力预留", "高级 AI、离线包、团队容量和徽章。", false, false);
        f("analytics.creator_incentive", "analytics", "13.6", "创作者激励", "创作者积分、收益、奖励和排行榜。", false, false);
        f("analytics.ads_sponsorship", "analytics", "13.7", "广告位与赞助内容", "Banner、活动页和内容流赞助标记。", false, false);
        f("analytics.lottery_events", "analytics", "13.8", "运营活动抽奖", "抽奖、任务次数和中奖记录。", false, false);

        group("client", "客户端体验与移动端能力升级", "深色、无障碍、骨架屏、图片、更新、弱网和平板。", "/api/client-features");
        f("client.dark_mode", "client", "14.1", "深色模式细化", "图片背景、输入框、卡片、弹窗和富文本一致。", false, false);
        f("client.accessibility", "client", "14.2", "字体大小与无障碍", "字体缩放、朗读、触达区域和色弱标识。", false, false);
        f("client.skeleton_loading", "client", "14.3", "首页骨架屏", "列表加载骨架和失败重试卡片。", false, false);
        f("client.image_lazy_cache", "client", "14.4", "图片懒加载与缓存", "列表图片懒加载和缓存状态。", false, false);
        f("client.image_viewer", "client", "14.5", "大图浏览器", "缩放、滑动、保存、分享和原图。", false, false);
        f("client.in_app_update", "client", "14.6", "App 内更新", "检测版本、展示更新内容和强制更新。", false, true);
        f("client.crash_feedback", "client", "14.7", "崩溃反馈", "下次启动提交反馈和日志。", false, true);
        f("client.weak_network", "client", "14.8", "网络弱环境优化", "进度、重试、断点续传和离线草稿。", false, true);
        f("client.idempotency_key", "client", "14.8+", "幂等提交保护", "弱网重试时使用幂等键，避免重复发帖、收藏或任务提交。", false, false);
        f("client.tablet_landscape", "client", "14.9", "平板横屏适配", "双栏布局和更大战术板区域。", false, false);
        f("client.desktop_web_reserved", "client", "14.10", "桌面端或 Web 端预留", "抽象接口模型和页面状态。", false, false);

        group("platform", "后端架构、安全与工程能力升级", "安全传输、续期、限流、错误、缓存、任务和发布能力的客户端面板。", "/api/platform");
        f("platform.https_security", "platform", "15.1", "HTTPS 与安全传输", "生产环境 HTTPS 提示和证书异常提示。", false, false);
        f("platform.refresh_token", "platform", "15.2", "Refresh Token 登录续期", "Token 过期刷新和失败跳登录。", false, false);
        f("platform.rate_limit", "platform", "15.3", "接口限流", "限流剩余等待和重试提示。", false, false);
        f("platform.error_codes", "platform", "15.4", "统一错误码", "按错误码显示明确提示。", false, false);
        f("platform.db_migration", "platform", "15.5", "数据库迁移规范", "版本升级时兼容新字段状态。", false, false);
        f("platform.cache_system", "platform", "15.6", "缓存体系", "热门榜、地图资源和配置项本地缓存。", false, false);
        f("platform.async_task_center", "platform", "15.7", "异步任务中心", "视频、AI 复盘、导出任务进度。", false, true);
        f("platform.file_scan", "platform", "15.8", "文件安全扫描", "上传文件扫描中和失败重传。", false, true);
        f("platform.object_storage_governance", "platform", "15.9", "对象存储资源治理", "旧资源引用清理提示。", false, false);
        f("platform.openapi_docs", "platform", "15.10", "OpenAPI 文档", "客户端查看接口文档和联调状态。", false, false);
        f("platform.automation_tests", "platform", "15.11", "自动化测试扩展", "页面单元、UI 和关键流程测试状态。", false, false);
        f("platform.request_trace", "platform", "15.12", "日志与链路追踪", "请求失败展示 requestId。", false, false);
        f("platform.gray_release", "platform", "15.13", "灰度发布", "用户、版本和地区功能开关面板。", false, false);
        f("platform.backup_restore", "platform", "15.14", "数据备份与恢复", "资料库和草稿云端备份状态。", false, true);
    }

    private static void registerApiKeys() {
        api("content.markdown_editor", "rich-text-post");
        api("content.post_templates", "post-template");
        api("content.section_editor", "post-section");
        api("content.quality_check", "quality-check");
        api("content.version_history", "post-version");
        api("content.image_annotation", "image-annotation");
        api("content.video_gif_demo", "video-demo");
        api("content.voice_to_text", "speech-transcription");
        api("content.multilingual_translation", "post-translation");
        api("content.collaborative_draft", "collaborative-draft");
        api("content.scheduled_publish", "scheduled-publish");
        api("content.quote_cards", "embedded-reference");

        api("community.recommend_feed", "recommendation-feed");
        api("community.following_feed", "following-feed");
        api("community.trending_rank", "trending-board");
        api("community.topics", "topic");
        api("community.collections", "collection");
        api("community.beginner_zone", "beginner-guide");
        api("community.qa_zone", "question-answer");
        api("community.poll_post", "poll");
        api("community.reading_progress", "reading-progress");
        api("community.read_later", "read-later");
        api("community.share_poster", "share-poster");
        api("community.similar_content", "similar-content");

        api("cs2.interactive_map_points", "map-utility-point");
        api("cs2.utility_trajectory", "utility-trajectory");
        api("cs2.utility_difficulty", "utility-difficulty");
        api("cs2.practice_plan", "practice-plan");
        api("cs2.practice_checkin", "practice-checkin");
        api("cs2.tactic_board_editor", "tactic-board");
        api("cs2.tactic_timeline", "tactic-step");
        api("cs2.default_tactic_library", "official-tactic");
        api("cs2.economy_strategy", "economy-strategy");
        api("cs2.callout_learning", "map-callout");
        api("cs2.utility_version_tracking", "game-version");
        api("cs2.utility_invalid_feedback", "utility-invalid-feedback");
        api("cs2.clutch_library", "clutch-library");
        api("cs2.default_positions", "default-position");
        api("cs2.buy_plan", "buy-plan");
        api("cs2.pro_match_index", "pro-match-tactic");

        api("ai.chat_assistant", "ai-chat");
        api("ai.utility_recommendation", "ai-utility-recommendation");
        api("ai.tactic_generator", "ai-tactic-generation");
        api("ai.title_summary", "ai-post-title-summary");
        api("ai.comment_summary", "ai-comment-summary");
        api("ai.review_assist", "ai-moderation");
        api("ai.auto_tagging", "ai-auto-tag");
        api("ai.image_point_recognition", "ai-image-recognition");
        api("ai.demo_review", "ai-match-review");
        api("ai.personal_coach", "ai-personal-coach");
        api("ai.search_qa", "ai-search-answer");
        api("ai.reply_suggestion", "ai-reply-suggestion");
        api("ai.hot_content_insight", "ai-hot-content-insight");
        api("ai.recommend_reason", "ai-recommendation-explanation");

        api("user.full_profile", "user-profile");
        api("user.level_xp", "user-level");
        api("user.badges", "badge");
        api("user.reputation", "reputation");
        api("user.achievements", "achievement");
        api("user.privacy_settings", "privacy-setting");
        api("user.blocklist", "block-user");
        api("user.security_center", "account-security");
        api("user.third_party_login", "third-party-login");
        api("user.verification_badge", "user-verification");

        api("library.folders", "library-folder");
        api("library.tags", "library-tag");
        api("library.full_text_search", "library-search");
        api("library.offline_library", "offline-library");
        api("library.favorite_notes", "favorite-note");
        api("library.share_groups", "favorite-share-group");
        api("library.import_export", "library-import-export");
        api("library.duplicate_merge", "duplicate-favorite-merge");
        api("library.recent_common", "recent-library-item");
        api("library.change_reminder", "library-change-reminder");

        api("interaction.comment_like", "comment-vote");
        api("interaction.comment_fold_tree", "comment-thread");
        api("interaction.emoji_stickers", "emoji-sticker");
        api("interaction.dm_conversation", "message-conversation");
        api("interaction.push_messages", "push-device");
        api("interaction.notification_preferences", "notification-preference");
        api("interaction.mention_autocomplete", "user-mention-suggestion");
        api("interaction.co_creator_thanks", "content-contributor");
        api("interaction.community_events", "community-event");
        api("interaction.groups", "interest-circle");

        api("team.space", "team-space");
        api("team.tactic_library", "team-tactic-library");
        api("team.member_roles", "team-member-role");
        api("team.training_schedule", "training-schedule");
        api("team.training_tasks", "training-task");
        api("team.match_tactic_pack", "match-tactic-package");
        api("team.post_match_review", "post-match-review");
        api("team.opponent_research", "opponent-research");
        api("team.announcements", "team-announcement");
        api("team.dashboard", "team-dashboard");

        api("search.advanced", "advanced-search");
        api("search.suggestions", "search-suggestion");
        api("search.history", "search-history");
        api("search.highlight", "search-highlight");
        api("search.map_alias", "map-alias");
        api("search.pinyin_typo", "pinyin-fuzzy-search");
        api("search.semantic", "semantic-search");
        api("search.negative_feedback", "recommendation-feedback");
        api("search.cold_start", "cold-start-recommendation");
        api("search.quality_score", "content-quality-score");

        api("governance.review_priority", "review-priority");
        api("governance.multi_stage_review", "multi-stage-review");
        api("governance.appeals", "appeal");
        api("governance.punishments", "penalty");
        api("governance.sensitive_words", "sensitive-word");
        api("governance.anti_spam", "anti-spam-rule");
        api("governance.report_evidence", "report-evidence");
        api("governance.admin_stats", "admin-workbench-stat");
        api("governance.rbac", "rbac");
        api("governance.audit_detail", "audit-log-detail");
        api("governance.takedown_notice", "takedown-notice");
        api("governance.ops_config", "operation-config");

        api("notification.announcements", "system-announcement");
        api("notification.important_popup", "important-popup");
        api("notification.aggregate_cards", "message-aggregation");
        api("notification.subscriptions", "subscription");
        api("notification.weekly_digest", "weekly-digest");

        api("analytics.behavior_events", "tracking-event");
        api("analytics.content_dashboard", "content-dashboard");
        api("analytics.creator_center", "author-center");
        api("analytics.ab_experiment", "ab-experiment");
        api("analytics.membership_reserved", "membership");
        api("analytics.creator_incentive", "creator-incentive");
        api("analytics.ads_sponsorship", "ad-placement");
        api("analytics.lottery_events", "lottery-event");

        api("client.dark_mode", "theme-preference");
        api("client.accessibility", "accessibility-preference");
        api("client.skeleton_loading", "homepage-loading-performance");
        api("client.image_lazy_cache", "image-variant");
        api("client.image_viewer", "signed-original-image");
        api("client.in_app_update", "app-version");
        api("client.crash_feedback", "crash-report");
        api("client.weak_network", "resumable-upload");
        api("client.idempotency_key", "idempotency-key");
        api("client.tablet_landscape", "large-screen-layout");
        api("client.desktop_web_reserved", "desktop-web-compatibility");

        api("platform.https_security", "https-security");
        api("platform.refresh_token", "refresh-token");
        api("platform.rate_limit", "api-rate-limit");
        api("platform.error_codes", "unified-error-code");
        api("platform.db_migration", "database-migration");
        api("platform.cache_system", "cache-system");
        api("platform.async_task_center", "async-task-center");
        api("platform.file_scan", "file-scan");
        api("platform.object_storage_governance", "resource-governance");
        api("platform.openapi_docs", "openapi-doc");
        api("platform.automation_tests", "automated-test");
        api("platform.request_trace", "observability-tracing");
        api("platform.gray_release", "feature-flag");
        api("platform.backup_restore", "backup-restore");
    }

    private static void api(String key, String apiFeatureKey) {
        API_KEYS.put(key, apiFeatureKey);
    }

    private FutureFeatureRegistry() {
    }

    public static List<FutureFeatureGroup> groups() {
        return new ArrayList<>(GROUPS.values());
    }

    public static FutureFeatureGroup groupByKey(String key) {
        return GROUPS.get(key);
    }

    public static FutureFeatureDefinition featureByKey(String key) {
        return FEATURES.get(key);
    }

    public static List<FutureFeatureDefinition> allFeatures() {
        return new ArrayList<>(FEATURES.values());
    }

    private static void group(String key, String title, String summary, String apiPrefix) {
        GROUPS.put(key, new FutureFeatureGroup(key, title, summary, apiPrefix));
    }

    private static void f(String key,
                          String groupKey,
                          String section,
                          String title,
                          String summary,
                          boolean aiEnabled,
                          boolean taskEnabled) {
        FutureFeatureGroup group = GROUPS.get(groupKey);
        String apiPrefix = apiPrefixFor(key, group);
        FutureFeatureDefinition definition = new FutureFeatureDefinition(key, groupKey, section, title, summary, apiFeatureKeyFor(key), apiPrefix, aiEnabled, taskEnabled)
                .withField("mapName", "地图/范围", "例如 Mirage、Inferno、全站、个人中心", false, "")
                .withField("target", "目标对象", "帖子、用户、队伍、道具点位、活动或配置项", false, "")
                .withField("details", "功能内容", "记录本功能的配置、步骤、状态或运营说明", true, summary)
                .withField("acceptance", "验收标准", "用户能看到什么、能提交什么、后端如何返回结果", true, "可创建、可编辑、可同步、可查看状态");
        FEATURES.put(key, definition);
        if (group != null) {
            group.features.add(definition);
        }
    }

    private static String apiFeatureKeyFor(String key) {
        String apiFeatureKey = API_KEYS.get(key);
        if (apiFeatureKey != null) {
            return apiFeatureKey;
        }
        int dot = key.indexOf('.');
        String suffix = dot >= 0 && dot + 1 < key.length() ? key.substring(dot + 1) : key;
        return suffix.replace('_', '-');
    }

    private static String apiPrefixFor(String key, FutureFeatureGroup group) {
        if ("governance.ops_config".equals(key)) {
            return "/api/ops";
        }
        if ("analytics.membership_reserved".equals(key)
                || "analytics.creator_incentive".equals(key)
                || "analytics.ads_sponsorship".equals(key)
                || "analytics.lottery_events".equals(key)) {
            return "/api/commerce";
        }
        return group == null ? "" : group.apiPrefix;
    }
}
