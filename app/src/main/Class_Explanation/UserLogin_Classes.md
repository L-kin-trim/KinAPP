# UserLogin 类说明（与架构文档一致）

## 1. 类名：`UserLogin`
- 路径：`src/main/java/com/luankin/luankinstation/login/UserLogin.java`
- 用途：登录控制器。接收前端用户名密码，校验数据库用户，返回 JWT 和用户摘要信息。
- 接口：
  - `POST /api/auth/login`

### 请求数据格式（JSON）
```json
{
  "username": "admin",
  "password": "123456"
}
```

### 成功返回（HTTP 200）
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9....",
  "user": {
    "id": 1,
    "username": "admin"
  }
}
```

### 失败返回（HTTP 401）
```json
{
  "message": "invalid username or password"
}
```

## 2. 类名：`UserLoginService`
- 路径：`src/main/java/com/luankin/luankinstation/login/service/UserLoginService.java`
- 用途：登录业务逻辑。根据用户名查询用户，并使用 `BCryptPasswordEncoder` 校验密码哈希。
- 核心方法：
  - `Optional<UserAccount> authenticate(String username, String password)`

## 3. DTO：`LoginRequest`
- 路径：`src/main/java/com/luankin/luankinstation/login/dto/LoginRequest.java`
- 用途：登录请求体。
- 字段：
  - `username`（`String`，必填）
  - `password`（`String`，必填）

## 4. DTO：`LoginResponse`
- 路径：`src/main/java/com/luankin/luankinstation/login/dto/LoginResponse.java`
- 用途：登录成功返回体。
- 字段：
  - `token`（`String`，JWT）
  - `user`（`LoginUserInfo`）

## 5. DTO：`LoginUserInfo`
- 路径：`src/main/java/com/luankin/luankinstation/login/dto/LoginUserInfo.java`
- 用途：登录返回中的用户摘要对象。
- 字段：
  - `id`（`Long`）
  - `username`（`String`）

## 6. 实体类：`UserAccount`
- 路径：`src/main/java/com/luankin/luankinstation/login/entity/UserAccount.java`
- 用途：映射架构文档中的 ``user`` 表。
- 字段映射：
  - `id` -> `BIGINT PRIMARY KEY AUTO_INCREMENT`
  - `username` -> `VARCHAR(50) UNIQUE NOT NULL`
  - `password` -> `VARCHAR(100) NOT NULL`（保存哈希）
  - `email` -> `VARCHAR(100) UNIQUE NOT NULL`
  - `role` -> `VARCHAR(20) NOT NULL`
  - `created_at` -> 时间戳
  - `updated_at` -> 时间戳

## 7. 仓库接口：`UserAccountRepository`
- 路径：`src/main/java/com/luankin/luankinstation/login/repository/UserAccountRepository.java`
- 用途：用户表 JPA 访问层。
- 核心方法：
  - `Optional<UserAccount> findByUsername(String username)`
  - `boolean existsByUsername(String username)`
  - `boolean existsByEmail(String email)`

## 8. JWT 相关类
- `JwtUtil`
  - 路径：`src/main/java/com/luankin/luankinstation/security/JwtUtil.java`
  - 用途：生成和解析 JWT。
- `JwtAuthFilter`
  - 路径：`src/main/java/com/luankin/luankinstation/security/JwtAuthFilter.java`
  - 用途：从 `Authorization: Bearer <token>` 解析并注入认证上下文。

## 9. 安全配置：`SecurityConfig`
- 路径：`src/main/java/com/luankin/luankinstation/config/SecurityConfig.java`
- 行为：
  - 放行 `/api/auth/login`、`/api/auth/register`
  - 其他接口需要认证
  - 无状态会话（Stateless）+ JWT 过滤器
  - 提供密码哈希编码器（`BCryptPasswordEncoder`）
