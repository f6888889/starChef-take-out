# starChef-take-out

一个基于 Spring Boot 的外卖系统后端项目。

## 项目简介

starChef-take-out 是一个完整的外卖点餐系统，包含员工管理、分类管理、菜品管理、套餐管理、订单管理、用户管理等核心功能模块。支持微信登录、微信支付、阿里云 OSS 文件上传等功能。

## 技术栈

- **后端框架**：Spring Boot 2.7.3
- **数据库**：MySQL 8.x
- **ORM 框架**：MyBatis
- **连接池**：Druid
- **缓存**：Redis
- **接口文档**：Knife4j (Swagger)
- **权限认证**：JWT
- **代码简化**：Lombok
- **工具类**：FastJSON、Apache POI
- **构建工具**：Maven 3.9.x
- **JDK 版本**：Java 17

## 项目结构

```
starChef-take-out/
├── starChef-common/          # 公共模块（工具类、常量、异常处理、配置属性等）
│   ├── src/main/java/com/starChef/
│   │   ├── constant/          # 常量类
│   │   ├── context/           # 上下文
│   │   ├── enumeration/       # 枚举类型
│   │   ├── exception/         # 自定义异常
│   │   ├── json/              # JSON 工具
│   │   ├── properties/        # 配置属性类
│   │   ├── result/            # 统一返回结果
│   │   └── utils/             # 工具类
│   └── pom.xml
│
├── starChef-pojo/             # 实体模块（Entity、DTO、VO）
│   ├── src/main/java/com/starChef/
│   │   ├── entity/            # 数据库实体类
│   │   ├── dto/               # 数据传输对象
│   │   └── vo/                # 视图层返回对象
│   └── pom.xml
│
├── starChef-server/           # 服务端模块
│   ├── src/main/java/com/starChef/
│   │   ├── annotation/        # 自定义注解
│   │   ├── aspect/            # 切面
│   │   ├── config/            # 配置类
│   │   ├── controller/        # 控制器（管理端、用户端、通知）
│   │   ├── handler/           # 全局异常处理
│   │   ├── interceptor/       # JWT 拦截器
│   │   ├── mapper/            # MyBatis 接口
│   │   ├── service/           # 业务逻辑
│   │   ├── task/              # 定时任务
│   │   ├── websocket/         # WebSocket
│   │   └── StarChefApplication.java  # 启动类
│   ├── src/main/resources/
│   │   ├── mapper/            # MyBatis XML 映射文件
│   │   ├── application.yml    # 主配置文件
│   │   └── application-dev.yml # 开发环境配置
│   └── pom.xml
│
├── pom.xml                    # 父 POM
└── LICENSE
```

## 功能模块

### 管理端功能
- 员工管理（登录、增删改查、启停账号、修改密码）
- 分类管理（新增、删除、查询、启用禁用）
- 菜品管理（新增、修改、删除、起售停售、图片上传）
- 套餐管理（新增、修改、删除、起售停售）
- 订单管理（订单列表、订单详情、接单、拒单、取消、派送、完成）
- 数据统计（营业额统计、订单统计、用户统计、销售Top10、报表导出）
- 工作台（营业数据概览、订单管理）

### 用户端功能
- 用户登录（微信登录）
- 地址管理
- 菜品浏览
- 购物车管理
- 下单支付
- 订单查询

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.x+
- Redis 5.0+

### 数据库配置

1. 创建数据库：

```sql
CREATE DATABASE starChef_take_out DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `starChef-server/src/main/resources/application-dev.yml` 中的数据库配置：

```yaml
starChef:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: starChef_take_out
    username: root
    password: 123456
```

3. 导入数据库脚本（如果有）。

### Redis 配置

在 `application-dev.yml` 中修改 Redis 配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 10
```

### 编译项目

```bash
mvn clean install -DskipTests
```

### 启动项目

**方式一：使用 Maven 启动**

```bash
cd starChef-server
mvn spring-boot:run
```

**方式二：使用 IDE 启动**

在 IDEA 或 Eclipse 中直接运行 `com.starChef.StarChefApplication` 类的 `main` 方法。

**方式三：打包后运行**

```bash
cd starChef-server
mvn package -DskipTests
java -jar target/starChef-server-1.0-SNAPSHOT.jar
```

### 访问接口

项目启动后，可以通过以下地址访问：

- 服务地址：http://localhost:8080
- 接口文档（管理端）：http://localhost:8080/doc.html
- 接口文档（用户端）：http://localhost:8080/doc.html

## 扩展配置

### 阿里云 OSS 配置

在 `application-dev.yml` 中配置阿里云 OSS 用于文件上传：

```yaml
starChef:
  alioss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
```

### 微信支付配置

```yaml
starChef:
  wechat:
    appid: your-wechat-appid
    secret: your-wechat-secret
```

## 开发规范

- 统一使用 `Result` 作为接口返回类型
- 使用 JWT Token 进行权限验证
- 使用 MyBatis 进行数据库操作
- 使用 `@AutoFill` 注解实现公共字段自动填充
- 使用 `@Slf4j` 进行日志记录

## License

本项目采用 Apache License 2.0 许可证。
