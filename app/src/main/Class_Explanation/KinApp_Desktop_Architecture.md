# KinApp 桌面端网站技术架构设计文档

## 1. 项目概述

KinApp 桌面端网站是基于现有移动应用的扩展，旨在为用户提供桌面端访问能力，与移动端共享同一套后端服务。

### 1.1 核心功能
- 用户认证与授权
- 道具管理
- 战术信息管理
- 地图管理
- 数据可视化

## 2. 技术栈选择

### 2.1 后端技术栈
- **Spring Boot 4.0.3** - 后端框架
- **Spring Security** - 认证与授权
- **JWT** - 无状态认证
- **Spring Data JPA** - 数据访问
- **MySQL 8.0** - 数据库
- **Lombok** - 代码简化
- **Spring Boot Actuator** - 应用监控

### 2.2 前端技术栈
- **Vue 3** - 前端框架
- **Vite** - 构建工具
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Axios** - HTTP客户端
- **Element Plus** - UI组件库
- **TypeScript** - 类型安全
- **ESLint & Prettier** - 代码质量

## 3. 系统架构设计

### 3.1 整体架构
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  Vue 3 前端      │────>│  Spring Boot 后端 │────>│  MySQL 数据库   │
│  (TypeScript)   │     │  (RESTful API)  │     │                 │
│                 │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### 3.2 架构特点
- **前后端分离** - 前端与后端完全解耦
- **RESTful API** - 标准化的API接口
- **无状态认证** - 使用JWT实现
- **统一数据模型** - 前后端共享数据结构
- **模块化设计** - 便于维护和扩展

## 4. 后端设计（Spring Boot）

### 4.1 项目结构
```
kinapp-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── kinapp/
│   │   │           ├── KinappServerApplication.java   # 应用启动类
│   │   │           ├── config/                         # 配置类
│   │   │           │   ├── SecurityConfig.java         # 安全配置
│   │   │           │   ├── JwtConfig.java             # JWT配置
│   │   │           │   └── WebConfig.java             # Web配置
│   │   │           ├── controller/                     # 控制器
│   │   │           │   ├── AuthController.java         # 认证相关
│   │   │           │   ├── DaojuController.java        # 道具管理
│   │   │           │   ├── ZhanShuController.java      # 战术管理
│   │   │           │   └── MapController.java          # 地图管理
│   │   │           ├── entity/                         # 实体类
│   │   │           │   ├── User.java                   # 用户实体
│   │   │           │   ├── Daoju.java                  # 道具实体
│   │   │           │   ├── ZhanShu.java                # 战术实体
│   │   │           │   └── Map.java                    # 地图实体
│   │   │           ├── repository/                     # 数据访问
│   │   │           │   ├── UserRepository.java         # 用户数据
│   │   │           │   ├── DaojuRepository.java        # 道具数据
│   │   │           │   ├── ZhanShuRepository.java      # 战术数据
│   │   │           │   └── MapRepository.java          # 地图数据
│   │   │           ├── service/                        # 业务逻辑
│   │   │           │   ├── AuthService.java            # 认证服务
│   │   │           │   ├── DaojuService.java           # 道具服务
│   │   │           │   ├── ZhanShuService.java         # 战术服务
│   │   │           │   └── MapService.java             # 地图服务
│   │   │           ├── dto/                            # 数据传输对象
│   │   │           │   ├── LoginRequest.java           # 登录请求
│   │   │           │   ├── LoginResponse.java          # 登录响应
│   │   │           │   ├── RegisterRequest.java        # 注册请求
│   │   │           │   └── UserDto.java                # 用户DTO
│   │   │           ├── filter/                         # 过滤器
│   │   │           │   └── JwtAuthFilter.java          # JWT认证过滤器
│   │   │           └── util/                           # 工具类
│   │   │               └── JwtUtil.java                # JWT工具
│   │   └── resources/
│   │       ├── application.properties                 # 应用配置
│   │       └── application-dev.properties             # 开发环境配置
│   └── test/
│       └── java/
│           └── com/
│               └── kinapp/
│                   └── service/
│                       └── AuthServiceTest.java        # 测试类
├── pom.xml                                            # Maven配置
└── README.md                                          # 项目说明
```

### 4.2 API接口规范

#### 4.2.1 认证相关接口
| API路径 | 方法 | 功能 | 请求体 (JSON) | 成功响应 (200 OK) |
|---------|------|------|----------------|-------------------|
| `/api/auth/login` | POST | 用户登录 | `{"username": "admin", "password": "123456"}` | `{"token": "eyJhbGciOiJIUzI1NiJ9...", "user": {"id": 1, "username": "admin"}}` |
| `/api/auth/register` | POST | 用户注册 | `{"username": "newuser", "password": "123456", "email": "user@example.com"}` | `{"id": 2, "username": "newuser", "email": "user@example.com"}` |
| `/api/auth/me` | GET | 获取当前用户 | N/A | `{"id": 1, "username": "admin", "email": "admin@example.com"}` |

#### 4.2.2 道具管理接口
| API路径 | 方法 | 功能 | 请求体 (JSON) | 成功响应 (200 OK) |
|---------|------|------|----------------|-------------------|
| `/api/daoju` | GET | 获取道具列表 | N/A | `[{"id": 1, "name": "闪光弹", "category": "投掷物", "price": 200}]` |
| `/api/daoju` | POST | 创建道具 | `{"name": "烟雾弹", "category": "投掷物", "price": 300, "description": "产生烟雾"}` | `{"id": 2, "name": "烟雾弹", "category": "投掷物", "price": 300}` |
| `/api/daoju/{id}` | GET | 获取道具详情 | N/A | `{"id": 1, "name": "闪光弹", "category": "投掷物", "price": 200, "description": "致盲效果"}` |
| `/api/daoju/{id}` | PUT | 更新道具 | `{"name": "闪光弹", "price": 250, "description": "增强致盲效果"}` | `{"id": 1, "name": "闪光弹", "price": 250}` |
| `/api/daoju/{id}` | DELETE | 删除道具 | N/A | `{"status": "success", "message": "道具删除成功"}` |

#### 4.2.3 战术管理接口
| API路径 | 方法 | 功能 | 请求体 (JSON) | 成功响应 (200 OK) |
|---------|------|------|----------------|-------------------|
| `/api/zhanshu` | GET | 获取战术列表 | N/A | `[{"id": 1, "name": "A点进攻", "mapId": 1, "type": 1}]` |
| `/api/zhanshu` | POST | 创建战术 | `{"name": "B点防守", "mapId": 1, "type": 2, "description": "B点防守战术"}` | `{"id": 2, "name": "B点防守", "mapId": 1, "type": 2}` |
| `/api/zhanshu/{id}` | GET | 获取战术详情 | N/A | `{"id": 1, "name": "A点进攻", "mapId": 1, "type": 1, "description": "A点进攻战术"}` |
| `/api/zhanshu/{id}` | PUT | 更新战术 | `{"name": "A点进攻强化", "description": "增强版A点进攻"}` | `{"id": 1, "name": "A点进攻强化", "description": "增强版A点进攻"}` |
| `/api/zhanshu/{id}` | DELETE | 删除战术 | N/A | `{"status": "success", "message": "战术删除成功"}` |

#### 4.2.4 地图管理接口
| API路径 | 方法 | 功能 | 请求体 (JSON) | 成功响应 (200 OK) |
|---------|------|------|----------------|-------------------|
| `/api/map` | GET | 获取地图列表 | N/A | `[{"id": 1, "name": "dust2", "iconPosition": ""}]` |
| `/api/map` | POST | 创建地图 | `{"name": "mirage", "iconPosition": "0,0"}` | `{"id": 2, "name": "mirage", "iconPosition": "0,0"}` |
| `/api/map/{id}` | GET | 获取地图详情 | N/A | `{"id": 1, "name": "dust2", "iconPosition": ""}` |
| `/api/map/{id}` | PUT | 更新地图 | `{"name": "dust2", "iconPosition": "10,20"}` | `{"id": 1, "name": "dust2", "iconPosition": "10,20"}` |
| `/api/map/{id}` | DELETE | 删除地图 | N/A | `{"status": "success", "message": "地图删除成功"}` |

### 4.3 数据库设计

#### 4.3.1 用户表 (`user`)
| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | 用户ID |
| `username` | `VARCHAR(50)` | `UNIQUE NOT NULL` | 用户名 |
| `password` | `VARCHAR(100)` | `NOT NULL` | 密码哈希 |
| `email` | `VARCHAR(100)` | `UNIQUE NOT NULL` | 邮箱 |
| `role` | `VARCHAR(20)` | `NOT NULL DEFAULT 'USER'` | 角色 |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

#### 4.3.2 地图表 (`map`)
| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | 地图ID |
| `name` | `VARCHAR(50)` | `UNIQUE NOT NULL` | 地图名称 |
| `icon_position` | `VARCHAR(100)` | | 图标位置 |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

#### 4.3.3 道具表 (`prop`)
| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | 道具ID |
| `name` | `VARCHAR(50)` | `NOT NULL` | 道具名称 |
| `description` | `TEXT` | | 道具描述 |
| `category` | `VARCHAR(50)` | | 道具类别 |
| `price` | `INT` | | 道具价格 |
| `image_path` | `VARCHAR(255)` | | 图片路径 |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

#### 4.3.4 战术信息表 (`zhanshu_information`)
| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | 战术ID |
| `map_id` | `BIGINT` | `REFERENCES map(id)` | 地图ID |
| `type` | `INT` | | 战术类型 |
| `name` | `VARCHAR(100)` | `NOT NULL` | 战术名称 |
| `description` | `TEXT` | | 战术描述 |
| `image_path` | `VARCHAR(255)` | | 图片路径 |
| `video_path` | `VARCHAR(255)` | | 视频路径 |
| `notes` | `TEXT` | | 备注 |
| `member1` | `VARCHAR(50)` | | 成员1 |
| `member1_role` | `VARCHAR(50)` | | 成员1角色 |
| `member2` | `VARCHAR(50)` | | 成员2 |
| `member2_role` | `VARCHAR(50)` | | 成员2角色 |
| `member3` | `VARCHAR(50)` | | 成员3 |
| `member3_role` | `VARCHAR(50)` | | 成员3角色 |
| `member4` | `VARCHAR(50)` | | 成员4 |
| `member4_role` | `VARCHAR(50)` | | 成员4角色 |
| `member5` | `VARCHAR(50)` | | 成员5 |
| `member5_role` | `VARCHAR(50)` | | 成员5角色 |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

#### 4.3.5 道具信息表 (`daoju_information`)
| 字段名 | 数据类型 | 约束 | 描述 |
|--------|----------|------|------|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | 信息ID |
| `tool_name` | `VARCHAR(100)` | | 道具名称 |
| `throwing_method` | `TEXT` | | 投掷方式 |
| `stance_image` | `VARCHAR(255)` | | 站位图片 |
| `aim_point_image` | `VARCHAR(255)` | | 瞄点图片 |
| `landing_point_image` | `VARCHAR(255)` | | 落点图片 |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

### 4.4 认证授权设计

#### 4.4.1 JWT认证流程
1. 用户登录时，后端验证用户名和密码
2. 验证通过后，生成JWT令牌
3. 前端存储JWT令牌
4. 后续请求携带JWT令牌
5. 后端验证JWT令牌有效性
6. 验证通过后处理请求

#### 4.4.2 权限控制
- 使用Spring Security实现基于角色的权限控制
- 配置不同接口的访问权限
- 支持自定义权限注解

## 5. 前端设计（Vue3）

### 5.1 项目结构
```
kinapp-frontend/
├── public/                  # 静态资源
│   └── favicon.ico
├── src/
│   ├── assets/              # 资源文件
│   │   ├── images/          # 图片
│   │   └── styles/          # 样式
│   ├── components/          # 公共组件
│   │   ├── layout/          # 布局组件
│   │   │   ├── AppHeader.vue
│   │   │   ├── AppSidebar.vue
│   │   │   └── AppFooter.vue
│   │   ├── common/          # 通用组件
│   │   │   ├── Loading.vue
│   │   │   └── Message.vue
│   │   └── auth/            # 认证相关组件
│   │       ├── LoginForm.vue
│   │       └── RegisterForm.vue
│   ├── views/               # 页面组件
│   │   ├── auth/            # 认证页面
│   │   │   ├── Login.vue
│   │   │   └── Register.vue
│   │   ├── daoju/           # 道具管理页面
│   │   │   ├── DaojuList.vue
│   │   │   ├── DaojuCreate.vue
│   │   │   └── DaojuEdit.vue
│   │   ├── zhanshu/         # 战术管理页面
│   │   │   ├── ZhanShuList.vue
│   │   │   ├── ZhanShuCreate.vue
│   │   │   └── ZhanShuEdit.vue
│   │   ├── map/             # 地图管理页面
│   │   │   ├── MapList.vue
│   │   │   ├── MapCreate.vue
│   │   │   └── MapEdit.vue
│   │   └── dashboard/       # 仪表盘
│   │       └── Dashboard.vue
│   ├── router/              # 路由配置
│   │   └── index.ts
│   ├── store/               # 状态管理
│   │   ├── modules/         # 模块状态
│   │   │   ├── auth.ts      # 认证状态
│   │   │   ├── daoju.ts     # 道具状态
│   │   │   ├── zhanshu.ts   # 战术状态
│   │   │   └── map.ts       # 地图状态
│   │   └── index.ts         # 状态管理入口
│   ├── services/            # API服务
│   │   ├── api.ts           # API基础配置
│   │   ├── authService.ts   # 认证服务
│   │   ├── daojuService.ts  # 道具服务
│   │   ├── zhanshuService.ts # 战术服务
│   │   └── mapService.ts    # 地图服务
│   ├── utils/               # 工具函数
│   │   ├── jwt.ts           # JWT工具
│   │   └── http.ts          # HTTP工具
│   ├── types/               # TypeScript类型
│   │   ├── auth.ts          # 认证相关类型
│   │   ├── daoju.ts         # 道具相关类型
│   │   ├── zhanshu.ts       # 战术相关类型
│   │   └── map.ts           # 地图相关类型
│   ├── App.vue              # 根组件
│   └── main.ts              # 应用入口
├── .eslintrc.js             # ESLint配置
├── .prettierrc              # Prettier配置
├── vite.config.ts           # Vite配置
├── tsconfig.json            # TypeScript配置
├── package.json             # 项目依赖
└── README.md                # 项目说明
```

### 5.2 组件设计

#### 5.2.1 布局组件
- **AppHeader** - 应用顶部导航栏
- **AppSidebar** - 侧边菜单
- **AppFooter** - 页脚信息

#### 5.2.2 认证组件
- **LoginForm** - 登录表单
- **RegisterForm** - 注册表单

#### 5.2.3 业务组件
- **DaojuList** - 道具列表
- **DaojuCreate** - 创建道具
- **DaojuEdit** - 编辑道具
- **ZhanShuList** - 战术列表
- **ZhanShuCreate** - 创建战术
- **ZhanShuEdit** - 编辑战术
- **MapList** - 地图列表
- **MapCreate** - 创建地图
- **MapEdit** - 编辑地图

### 5.3 状态管理设计

#### 5.3.1 认证状态
```typescript
// store/modules/auth.ts
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    loading: false,
    error: null
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    currentUser: (state) => state.user
  },
  actions: {
    async login(username: string, password: string) {
      // 登录逻辑
    },
    async register(userData: any) {
      // 注册逻辑
    },
    logout() {
      // 登出逻辑
    }
  }
})
```

#### 5.3.2 道具状态
```typescript
// store/modules/daoju.ts
import { defineStore } from 'pinia'

export const useDaojuStore = defineStore('daoju', {
  state: () => ({
    daojus: [],
    loading: false,
    error: null
  }),
  getters: {
    daojuList: (state) => state.daojus
  },
  actions: {
    async fetchDaojus() {
      // 获取道具列表
    },
    async createDaoju(daojuData: any) {
      // 创建道具
    },
    async updateDaoju(id: number, daojuData: any) {
      // 更新道具
    },
    async deleteDaoju(id: number) {
      // 删除道具
    }
  }
})
```

### 5.4 API调用设计

#### 5.4.1 API基础配置
```typescript
// services/api.ts
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // 处理未授权
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
```

#### 5.4.2 认证服务
```typescript
// services/authService.ts
import api from './api'

export const authService = {
  login: (username: string, password: string) => {
    return api.post('/auth/login', { username, password })
  },
  register: (userData: any) => {
    return api.post('/auth/register', userData)
  },
  getCurrentUser: () => {
    return api.get('/auth/me')
  }
}
```

## 6. 开发流程

### 6.1 后端开发流程
1. **环境搭建** - 安装JDK、Maven、MySQL
2. **项目初始化** - 使用Spring Initializr创建项目
3. **依赖配置** - 添加所需依赖
4. **数据库设计** - 创建数据库和表结构
5. **核心功能开发** - 实现认证、道具、战术、地图管理
6. **API测试** - 使用Postman测试API接口
7. **性能优化** - 数据库索引、缓存等
8. **部署准备** - 配置生产环境

### 6.2 前端开发流程
1. **环境搭建** - 安装Node.js、npm/yarn
2. **项目初始化** - 使用Vite创建Vue3项目
3. **依赖配置** - 添加所需依赖
4. **项目结构搭建** - 创建目录结构
5. **核心功能开发** - 实现登录、道具、战术、地图管理页面
6. **API集成** - 与后端API对接
7. **UI优化** - 响应式设计、用户体验
8. **测试** - 功能测试、兼容性测试
9. **构建部署** - 打包生产版本

### 6.3 联调流程
1. **后端API文档** - 提供API接口文档
2. **前端API集成** - 前端调用后端API
3. **数据格式验证** - 确保前后端数据格式一致
4. **认证流程测试** - 测试登录、注册、权限控制
5. **功能测试** - 测试核心功能
6. **问题修复** - 解决联调过程中遇到的问题

## 7. 部署方案

### 7.1 后端部署
- **容器化** - 使用Docker容器
- **服务器** - 云服务器或本地服务器
- **数据库** - 独立的MySQL服务器
- **配置管理** - 环境变量或配置文件
- **监控** - 使用Spring Boot Actuator

### 7.2 前端部署
- **构建** - 执行`npm run build`生成静态文件
- **服务器** - Nginx或Apache
- **CDN** - 静态资源使用CDN加速
- **缓存策略** - 合理的缓存配置
- **HTTPS** - 配置SSL证书

### 7.3 集成部署
- **CI/CD** - 使用GitHub Actions或Jenkins
- **自动化测试** - 集成测试、单元测试
- **部署流程** - 自动化部署脚本
- **回滚机制** - 部署失败时的回滚策略

## 8. 技术文档维护

- **API文档** - 使用Swagger或Postman生成
- **架构文档** - 定期更新架构设计
- **部署文档** - 详细的部署步骤
- **开发规范** - 代码风格、命名规范
- **安全文档** - 安全最佳实践

## 9. 总结

本设计文档提供了KinApp桌面端网站的完整技术架构方案，包括后端Spring Boot和前端Vue3的详细设计。通过标准化的API接口和模块化的项目结构，实现了前后端的清晰分离和高效协作。

该架构具有以下优势：
- **可扩展性** - 模块化设计便于添加新功能
- **安全性** - 完善的认证授权机制
- **性能** - 优化的数据库设计和API调用
- **维护性** - 清晰的代码结构和文档

此设计方案为KinApp桌面端网站的开发提供了全面的技术指导，确保项目能够高质量、高效率地完成。