# UserRegister 类说明（与架构文档一致）

## 1. 类名：`UserRegister`
- 路径：`src/main/java/com/luankin/luankinstation/register/UserRegister.java`
- 用途：注册控制器。校验用户名和邮箱唯一性，写入用户数据并返回注册结果。
- 接口：
  - `POST /api/auth/register`

### 请求数据格式（JSON）
```json
{
  "username": "newuser",
  "password": "123456",
  "email": "user@example.com"
}
```

### 成功返回（HTTP 201）
```json
{
  "id": 2,
  "username": "newuser",
  "email": "user@example.com"
}
```

### 失败返回（HTTP 409）
```json
{
  "message": "username already exists"
}
```
或
```json
{
  "message": "email already exists"
}
```

## 2. 类名：`UserRegisterService`
- 路径：`src/main/java/com/luankin/luankinstation/register/service/UserRegisterService.java`
- 用途：注册业务逻辑。
  - 校验用户名/邮箱是否重复
  - 使用 `BCryptPasswordEncoder` 对密码进行哈希
  - 默认写入角色 `USER`
- 核心方法：
  - `boolean usernameExists(String username)`
  - `boolean emailExists(String email)`
  - `UserAccount registerUser(String username, String password, String email)`

## 3. DTO：`RegisterRequest`
- 路径：`src/main/java/com/luankin/luankinstation/register/dto/RegisterRequest.java`
- 用途：注册请求体与参数校验。
- 字段：
  - `username`（`String`，必填，长度 3-50）
  - `password`（`String`，必填，长度 6-100）
  - `email`（`String`，必填，邮箱格式，最大长度 100）

## 4. DTO：`RegisterResponse`
- 路径：`src/main/java/com/luankin/luankinstation/register/dto/RegisterResponse.java`
- 用途：注册成功返回体。
- 字段：
  - `id`（`Long`）
  - `username`（`String`）
  - `email`（`String`）

## 5. 复用用户模型
- 实体：`UserAccount`
  - 路径：`src/main/java/com/luankin/luankinstation/login/entity/UserAccount.java`
  - 表：``user``
- 仓库：`UserAccountRepository`
  - 路径：`src/main/java/com/luankin/luankinstation/login/repository/UserAccountRepository.java`
  - 注册使用方法：
    - `existsByUsername`
    - `existsByEmail`
    - `save`
