# 数据库结构文档

## 1. 本地数据库结构

### 1.1 地图表 (map)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 地图ID |
| `name` | TEXT | | 地图名称 |

### 1.2 道具表 (prop)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 道具ID |
| `name` | TEXT | | 道具名称 |
| `description` | TEXT | | 道具描述 |
| `category` | TEXT | | 道具类别 |
| `price` | INTEGER | | 道具价格 |
| `image_path` | TEXT | | 道具图片路径 |

### 1.3 道具类别表 (prop_category)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 类别ID |
| `name` | TEXT | | 类别名称 |

### 1.4 战术信息表 (zhanshu_information)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 战术ID |
| `map_id` | INTEGER | | 关联的地图ID |
| `type` | INTEGER | | 战术类型 (1-RUSH, 2-Split, 3-Contain, 4-Lurk, 5-Default) |
| `name` | TEXT | | 战术名称 |
| `description` | TEXT | | 战术描述 |
| `image_path` | TEXT | | 图片路径 |
| `video_path` | TEXT | | 视频路径 |
| `notes` | TEXT | | 备注 |
| `member1` | TEXT | | 成员1名称 |
| `member1_role` | TEXT | | 成员1角色/任务 |
| `member2` | TEXT | | 成员2名称 |
| `member2_role` | TEXT | | 成员2角色/任务 |
| `member3` | TEXT | | 成员3名称 |
| `member3_role` | TEXT | | 成员3角色/任务 |
| `member4` | TEXT | | 成员4名称 |
| `member4_role` | TEXT | | 成员4角色/任务 |
| `member5` | TEXT | | 成员5名称 |
| `member5_role` | TEXT | | 成员5角色/任务 |

## 2. 服务器数据库设计

### 2.1 用户表 (users)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 用户ID |
| `username` | VARCHAR(50) | UNIQUE NOT NULL | 用户名 |
| `email` | VARCHAR(100) | UNIQUE NOT NULL | 邮箱 |
| `password_hash` | VARCHAR(255) | NOT NULL | 密码哈希 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 2.2 地图表 (maps)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 地图ID |
| `name` | VARCHAR(50) | NOT NULL | 地图名称 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 2.3 道具表 (props)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 道具ID |
| `user_id` | INTEGER | FOREIGN KEY REFERENCES users(id) | 所属用户ID |
| `map_id` | INTEGER | FOREIGN KEY REFERENCES maps(id) | 关联的地图ID |
| `name` | VARCHAR(100) | NOT NULL | 道具名称 |
| `description` | TEXT | | 道具描述 |
| `category` | VARCHAR(50) | | 道具类别 |
| `price` | INTEGER | | 道具价格 |
| `image_path` | VARCHAR(255) | | 道具图片路径 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 2.4 道具类别表 (prop_categories)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 类别ID |
| `name` | VARCHAR(50) | NOT NULL | 类别名称 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 2.5 战术信息表 (tactics)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 战术ID |
| `user_id` | INTEGER | FOREIGN KEY REFERENCES users(id) | 所属用户ID |
| `map_id` | INTEGER | FOREIGN KEY REFERENCES maps(id) | 关联的地图ID |
| `type` | INTEGER | NOT NULL | 战术类型 (1-RUSH, 2-Split, 3-Contain, 4-Lurk, 5-Default) |
| `name` | VARCHAR(100) | NOT NULL | 战术名称 |
| `description` | TEXT | | 战术描述 |
| `image_path` | VARCHAR(255) | | 图片路径 |
| `video_path` | VARCHAR(255) | | 视频路径 |
| `notes` | TEXT | | 备注 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 2.6 战术成员表 (tactic_members)
| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 成员ID |
| `tactic_id` | INTEGER | FOREIGN KEY REFERENCES tactics(id) | 关联的战术ID |
| `member_name` | VARCHAR(50) | | 成员名称 |
| `role` | VARCHAR(100) | | 成员角色/任务 |
| `position` | INTEGER | NOT NULL | 成员位置 (1-5) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

## 3. 数据同步策略

### 3.1 本地到服务器同步
1. **增量同步**：只同步修改或新增的数据
2. **冲突处理**：以服务器数据为准，或根据时间戳判断最新数据
3. **同步频率**：用户手动触发或定期自动同步

### 3.2 服务器到本地同步
1. **全量同步**：首次登录时同步所有数据
2. **增量同步**：后续同步只获取新数据
3. **离线操作**：支持离线编辑，重新联网后自动同步

## 4. API 设计

### 4.1 用户相关 API
- `POST /api/auth/register` - 注册新用户
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/profile` - 获取用户信息

### 4.2 地图相关 API
- `GET /api/maps` - 获取地图列表
- `POST /api/maps` - 创建新地图
- `PUT /api/maps/{id}` - 更新地图信息
- `DELETE /api/maps/{id}` - 删除地图

### 4.3 道具相关 API
- `GET /api/props` - 获取道具列表
- `POST /api/props` - 创建新道具
- `PUT /api/props/{id}` - 更新道具信息
- `DELETE /api/props/{id}` - 删除道具

### 4.4 战术相关 API
- `GET /api/tactics` - 获取战术列表
- `POST /api/tactics` - 创建新战术
- `PUT /api/tactics/{id}` - 更新战术信息
- `DELETE /api/tactics/{id}` - 删除战术
- `GET /api/tactics/{id}/members` - 获取战术成员列表
- `POST /api/tactics/{id}/members` - 添加战术成员
- `PUT /api/tactics/{id}/members/{memberId}` - 更新战术成员
- `DELETE /api/tactics/{id}/members/{memberId}` - 删除战术成员

## 5. 数据类型映射

### 5.1 SQLite 到 MySQL/PostgreSQL 映射
| SQLite 类型 | MySQL 类型 | PostgreSQL 类型 |
| :--- | :--- | :--- |
| INTEGER | INT | INTEGER |
| TEXT | TEXT | TEXT |
| TIMESTAMP | TIMESTAMP | TIMESTAMP |

### 5.2 数据传输对象 (DTOs)
- **UserDTO**：包含用户基本信息
- **MapDTO**：包含地图基本信息
- **PropDTO**：包含道具详细信息
- **TacticDTO**：包含战术详细信息和成员列表
- **TacticMemberDTO**：包含战术成员信息

## 6. 安全性考虑

### 6.1 数据加密
- 密码使用 bcrypt 等安全哈希算法
- 敏感数据传输使用 HTTPS

### 6.2 权限控制
- 基于用户ID的数据访问控制
- 确保用户只能访问和修改自己的数据

### 6.3 数据验证
- 服务器端数据验证
- 输入参数校验

## 7. 索引设计

### 7.1 本地数据库索引
- `map(name)` - 加速地图名称查询
- `zhanshu_information(map_id, type)` - 加速战术按地图和类型查询

### 7.2 服务器数据库索引
- `users(username, email)` - 加速用户登录和注册
- `props(user_id, map_id)` - 加速用户道具查询
- `tactics(user_id, map_id)` - 加速用户战术查询
- `tactic_members(tactic_id)` - 加速战术成员查询

## 8. 数据库版本管理

### 8.1 本地数据库版本
- 当前版本：5
- 版本升级通过 `onUpgrade` 方法处理

### 8.2 服务器数据库版本
- 使用数据库迁移工具（如 Flyway 或 Liquibase）管理版本
- 每次结构变更都创建对应的迁移脚本

## 9. 性能优化

### 9.1 本地数据库优化
- 使用事务处理批量操作
- 合理使用索引
- 避免频繁数据库操作

### 9.2 服务器数据库优化
- 读写分离
- 缓存热点数据
- 优化查询语句

## 10. 未来扩展

### 10.1 表结构扩展
- **道具投掷方法表**：记录道具的具体投掷方法
- **战术步骤表**：记录战术的详细执行步骤
- **分享表**：记录用户分享的道具和战术

### 10.2 功能扩展
- 支持多人协作编辑
- 支持评论和点赞
- 支持标签系统
- 支持搜索功能