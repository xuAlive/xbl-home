# xbl-home

`xbl-home` 是将原 `blog-parent` 多模块工程合并后的单体 Spring Boot 项目，统一放在 `xbl` 目录下运行与维护。

原项目仍然保留：

- `blog-parent`
- `blog`
- `calendar-app`
- `schedule-app`
- `timesheet-app`

本项目当前整合了以下能力：

- 博客与后台管理
- 日历与提醒
- 排班管理
- 工时记录
- 公共鉴权与响应组件

## 目录结构

```text
xbl-home
├── pom.xml
├── README.md
└── src
    └── main
        ├── java
        │   └── com/xu/home
        │       ├── Controller
        │       ├── Service
        │       ├── Dao
        │       ├── Param
        │       ├── mapper
        │       ├── domain
        │       ├── config
        │       ├── utils
        │       ├── Interceptor
        │       └── XblHomeApplication.java
        └── resources
            ├── application.yml
            ├── liquibase
            ├── mapper
            └── ip2region
```

启动类：

- `com.xu.home.XblHomeApplication`

## 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8

## 主要配置

默认配置文件位于：

- `src/main/resources/application.yml`

可以通过环境变量覆盖核心配置：

- `SERVER_PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `BLOG_JWT_SECRET`
- `BLOG_JWT_EXPIRE_MS`
- `DEEPSEEK_API_URL`
- `DEEPSEEK_API_KEY`
- `WECHAT_APP_ID`
- `WECHAT_APP_SECRET`
- `WECHAT_REDIRECT_DOMAIN`
- `UPLOAD_IMAGE_PATH`
- `UPLOAD_IMAGE_DOMAIN`

默认端口：

- `6101`

默认数据库名：

- `blog`

## 启动说明

### 1. 命令行启动

进入项目目录：

```bash
cd /Users/xubaolin/IdeaProjects/xbl/xbl-home
```

编译项目：

```bash
mvn clean compile
```

启动项目：

```bash
mvn spring-boot:run
```

如果需要指定环境变量，可这样启动：

```bash
DB_HOST=127.0.0.1 \
DB_PORT=3306 \
DB_NAME=blog \
DB_USERNAME=root \
DB_PASSWORD=your-password \
BLOG_JWT_SECRET=change-me \
mvn spring-boot:run
```

### 2. 打包后启动

打包：

```bash
mvn clean package -DskipTests
```

运行：

```bash
java -jar target/xbl-home-1.0-SNAPSHOT.jar
```

### 3. IDEA 启动

在 IDEA 中打开目录：

- `/Users/xubaolin/IdeaProjects/xbl/xbl-home`

运行以下主类：

- `com.xu.home.XblHomeApplication`

## 数据库迁移

项目启动时会自动执行 Liquibase 迁移。

统一入口文件：

- `src/main/resources/liquibase/master.xml`

已合并原多个模块中的表结构与变更脚本。

## 接口前缀

保留原有接口路径前缀：

- `/blog/**`
- `/calendar/**`
- `/schedule/**`
- `/timesheet/**`

健康检查示例：

- `/calendar/health`
- `/schedule/health`
- `/timesheet/health`

## 已验证内容

以下命令已通过：

```bash
mvn -q -DskipTests compile
```

## 说明

- 当前是“单项目整合”，不是删除旧工程。
- 原有目录暂时保留，便于比对和回滚。
- 如果后续要继续收敛目录，可以再单独清理旧空壳工程和无用文件。
