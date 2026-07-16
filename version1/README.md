# BMI 体质评估与预测系统 v1.0

## 项目简介
基于 Java Swing 的桌面健康管理系统，支持 BMI 计算、趋势分析、饮食管理等功能。

## 技术栈
- **语言**: Java (JDK 8+)
- **UI框架**: Java Swing + Nimbus Look and Feel
- **数据库**: PostgreSQL
- **驱动**: postgresql-42.7.3.jar

## 目录结构
```
version1/
├── src/                          # Java 源代码
│   ├── HealthSystem.java         # 主程序（含全部UI和业务逻辑）
│   ├── InitDB.java               # 数据库初始化工具
│   └── CheckDB.java              # 数据库诊断工具
├── lib/                          # 第三方依赖库
│   └── postgresql-42.7.3.jar     # PostgreSQL JDBC 驱动
├── sql/                          # 数据库脚本
│   └── init_database.sql         # 建表脚本
├── docs/                         # 项目文档
├── run.bat                       # Windows 一键运行脚本
└── README.md                     # 本文件
```

## 快速开始

### 1. 数据库准备
```sql
-- 在 PostgreSQL 中执行
CREATE DATABASE health_db;
-- 然后执行 sql/init_database.sql 建表
```

数据库配置（默认）：
- 地址: `localhost:5432`
- 数据库名: `health_db`
- 用户名: `postgres`
- 密码: `12345678`

### 2. 运行系统
- **方式一**：双击 `run.bat`
- **方式二**：手动编译运行
  ```bash
  javac -encoding UTF-8 -cp lib/postgresql-42.7.3.jar -d out src/HealthSystem.java
  java -cp out;lib/postgresql-42.7.3.jar HealthSystem
  ```

## 功能模块
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

## 默认账号
- 用户名: `admin`
- 密码: `admin123`
