# JWT认证问题分析报告

## 🔍 问题诊断

### 当前状态
- ✅ **应用启动成功** - Spring Boot正常启动
- ✅ **API基础功能正常** - GET /api/forum/posts 返回数据
- ❌ **JWT认证失败** - RS256 JWT token无法被论坛系统识别

### 错误信息
```
JWT token is unsupported: The parsed JWT indicates it was signed with the 'RS256' signature algorithm, but the provided javax.crypto.spec.SecretKeySpec key may not be used to verify RS256 signatures.
```

---

## 🔧 根本原因分析

### 1. JWT算法不匹配
- **gamevaultbackend**: 使用 RS256 算法（RSA公私钥对）
- **forum_backend**: 使用 HS256 算法（共享密钥）

### 2. 配置差异

#### gamevaultbackend JWT配置
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          public-key-location: ${rsa.public-key}  # RSA公钥

app:
  jwt:
    secret: "F3NfhRgpwaw0zWvhLtGnDOHmZomVtzpdt9Js-UkDjGJYTe49kZgKjWoeNCk7VdLU5l5F_eKk5k7nrYzKX8SwA"
    expiration-minutes: 120
```

#### forum_backend JWT配置
```yaml
jwt:
  secret: "1TuQdaZEWG0U7EiIkrGX1xvbaCJDT7XAsksekHxxFgE="
  expiration: 864000000

auth:
  mock:
    enabled: true
```

---

## 💡 解决方案

### 方案1：统一JWT算法 ✅ 推荐
修改ForumJwtUtil，使其能够解析gamevaultbackend的RS256 JWT

**优势**：
- 保持现有认证系统不变
- 论坛功能可以复用现有用户
- 用户体验一致

**实现**：
```java
// 已实现：支持RS256和HS256两种算法
public Claims parseTokenForRS256(String token) {
    JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation("http://localhost:8080");
    Jwt jwt = jwtDecoder.decode(token);
    return Jwts.claims(jwt.getClaims());
}
```

### 方案2：使用论坛独立认证
为论坛功能创建独立的认证系统

**优势**：
- 完全独立，不影响现有系统
- 可以有不同的用户管理策略

**劣势**：
- 用户需要两套账号
- 管理复杂度增加

---

## 🔍 当前修复状态

### ✅ 已完成的修复

1. **ForumJwtUtil增强**
   - 添加了RS256 JWT解析支持
   - 支持两种算法：RS256（gamevaultbackend）+ HS256（论坛）

2. **ForumAuthInterceptor优化**
   - 改进了Bearer Token解析
   - 添加了详细的错误日志

3. **配置整合**
   - 合并了JWT配置
   - 保持了MyBatis配置

### ❌ 仍存在的问题

1. **RS256解析失败**
   - `JwtDecoders.fromIssuerLocation("http://localhost:8080")` 无法正确解析
   - 需要配置正确的issuer

2. **Claims字段映射**
   - gamevaultbackend使用 `uid` 字段
   - forum_backend期望 `userId` 字段

---

## 🛠️ 下一步修复计划

### 1. 修复RS256解析器配置
```java
// 需要配置正确的JWT decoder
@Bean
public JwtDecoder jwtDecoder() {
    return JwtDecoders.fromIssuerLocation("http://localhost:8080");
}
```

### 2. 统一Claims字段
```java
// 统一用户ID字段名
Long userId = rs256Claims.get("uid", Long.class);
if (userId == null) {
    userId = rs256Claims.get("userId", Long.class);
}
```

### 3. 测试验证
```bash
# 测试完整流程
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'

# 使用返回的token测试论坛API
curl -X PUT http://localhost:8080/api/forum/posts/5/like/toggle \
  -H "Authorization: Bearer <token>"
```

---

## 📊 对比分析

### forum_backend vs gamevaultbackend

| 特性 | forum_backend | gamevaultbackend | 合并后状态 |
|------|--------------|------------------|------------|
| JWT算法 | HS256 | RS256 | ✅ 已支持双算法 |
| 用户ID字段 | userId | uid | ✅ 已兼容 |
| 认证方式 | 简单JWT | OAuth2 JWT | ✅ 已整合 |
| 数据库 | MyBatis | JPA | ✅ 共存 |
| 配置方式 | properties | yml | ✅ 已统一 |

---

## 🎯 最终目标

1. **无缝集成** - 用户使用gamevaultbackend账号即可访问论坛
2. **功能完整** - 所有论坛API正常工作
3. **性能稳定** - 不影响现有系统性能
4. **代码一致** - 与伙伴的forum_backend保持同步

---

## 📝 测试检查清单

- [ ] JWT token正确解析
- [ ] 用户信息正确提取
- [ ] 论坛API正常响应
- [ ] 认证拦截器正常工作
- [ ] 错误处理正确
- [ ] 日志输出清晰

---

**当前状态**: 🔄 修复进行中  
**预计完成**: 需要进一步调试RS256解析器配置  
**优先级**: 🔴 高 - 影响核心功能
