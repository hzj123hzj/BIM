# BMI 体质评估与预测系统 v1.0（含管理员后台）

## 项目简介
基于 Java Swing 的桌面健康管理系统，支持 BMI 计算、趋势分析、饮食管理等功能。
**本次升级**：新增完整的管理员后台，支持用户管理、数据监控、内容管理、AI 系统管理、系统配置、报表导出、消息推送、健康顾问 8 大模块。

## 技术栈
- **语言**: Java (JDK 8+)
- **UI框架**: Java Swing + Nimbus Look and Feel
- **数据库**: PostgreSQL
- **驱动**: postgresql-42.7.3.jar

## 目录结构
```
version1/
├── src/                          # Java 源代码
│   ├── HealthSystem.java         # 主程序（含全部用户端UI和业务逻辑）
│   ├── AdminSystem.java          # 管理员后台模块
│   ├── InitDB.java               # 数据库初始化工具
│   └── CheckDB.java              # 数据库诊断工具
├── lib/                          # 第三方依赖库
│   └── postgresql-42.7.3.jar     # PostgreSQL JDBC 驱动
├── sql/                          # 数据库脚本
│   ├── init_database.sql         # 原始建表脚本
│   └── admin_upgrade.sql         # 管理员模块数据库扩展脚本
├── docs/                         # 项目文档
├── run.bat                       # Windows 一键运行脚本
└── README.md                     # 本文件
```

## 快速开始

### 1. 数据库准备
如果是新项目，运行：
```bash
java -cp out;lib/postgresql-42.7.3.jar InitDB
```

如果是从旧版本升级，运行 `InitDB` 即可自动添加管理员模块所需的表和字段（全部使用 IF NOT EXISTS，不会删除已有数据）。

数据库配置（默认）：
- 地址: `localhost:5432`
- 数据库名: `health_db`
- 用户名: `postgres`
- 密码: `12345678`

### 2. 运行系统
- **方式一**：双击 `run.bat`
- **方式二**：手动编译运行
  ```bash
  javac -encoding UTF-8 -cp lib/postgresql-42.7.3.jar -d out src/HealthSystem.java src/AdminSystem.java
  java -cp out;lib/postgresql-42.7.3.jar HealthSystem
  ```

## 功能模块

### 用户端
| 模块 | 说明 |
|------|------|
| 数据录入 | 录入身高、体重等健康数据 |
| 历史趋势 | 查看BMI变化趋势曲线和数据表格 |
| 分析评估 | 综合分析体质状况，生成报告 |
| 预测分析 | 基于历史数据预测未来趋势 |
| 目标计划 | 设定和管理健康目标 |
| 饮食管理 | 推荐饮食方案，记录饮食 |
| 成就徽章 | 健康达成成就系统 |
| 数据大屏 | 可视化数据仪表盘 |

### 管理员后台
| 模块 | 说明 |
|------|------|
| 用户管理 | 用户列表、详情、账号启用/禁用/冻结、软删除、批量导出 |
| 数据监控 | 全局健康看板、异常用户列表、健康风险评估说明 |
| 内容管理 | 食物数据库、运动库、健康文章管理 |
| AI系统 | AI问答记录审核、Prompt模板、API配置 |
| 系统配置 | 预警阈值、BMR公式、目标参数、打卡规则、数据备份、日志 |
| 报表导出 | 用户CSV、健康数据JSON、饮食记录CSV |
| 消息推送 | 系统通知、健康提醒、活动推送 |
| 健康顾问 | 用户健康档案、干预建议、随访记录 |

## 默认账号
- **管理员**: `admin` / `admin123`
- **普通用户**: 在登录界面注册
