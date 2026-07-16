# BMI 体质评估与预测系统 v2.0 (UI 全面升级版)

## 项目简介
基于 Java Swing 的桌面健康管理系统 v2.0，在 v1.0 基础上对 UI 界面进行了全面改造升级。

## v2.0 UI 升级要点
- **现代配色方案**：深靛蓝 (Indigo) 主色 + 青绿 (Teal) 点缀，替代旧版青蓝色
- **字体系统**：分级字体 (H1/H2/Header/Body/Small/Tiny/BigNum)，优先使用 Microsoft YaHei UI
- **圆角阴影卡片**：多层柔和阴影，14px 圆角，提升层次感
- **按钮交互**：自定义 RoundButton 圆角按钮，带悬停色变效果
- **输入框焦点动画**：获取焦点时边框变粗变色
- **表格现代化**：斑马纹背景，无竖线，底部主色边框
- **渐变图表**：折线图带渐变填充区域，现代配色
- **淡入动画**：登录和主窗口启动时的淡入过渡效果
- **响应式适配**：设置 minimumSize，支持窗口缩放

## 技术栈
- **语言**: Java (JDK 8+)
- **UI框架**: Java Swing + Nimbus Look and Feel
- **数据库**: PostgreSQL
- **驱动**: postgresql-42.7.3.jar

## 目录结构
```
version2/
├── src/                          # Java 源代码
│   ├── HealthSystem.java         # 主程序（含 v2.0 UI 升级）
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
| 数据录入 | 录入身高、体重等健康数据，模拟称重，运动记录 |
| 历史趋势 | 现代渐变折线图 + 历史记录表格 |
| 分析评估 | 综合分析体质状况，生成详细报告 |
| 预测分析 | 基于线性回归预测未来趋势 |
| 目标计划 | 设定和管理健康目标 |
| 饮食管理 | 推荐饮食方案，记录饮食 |
| 成就徽章 | 健康达成成就系统 |
| 数据大屏 | 渐变指标卡片可视化仪表盘 |

## v1.0 → v2.0 变更
| 项目 | v1.0 | v2.0 |
|------|------|------|
| 主色 | 青蓝 (45,140,160) | 深靛蓝 (99,102,241) |
| 强调色 | 暖橙 (255,150,70) | 青绿 (20,184,166) |
| 按钮样式 | 普通矩形+setBorder | 圆角RoundButton+悬停动画 |
| 输入框 | 静态边框 | 焦点变色动画 |
| 卡片阴影 | 单层阴影 | 多层柔和阴影 |
| 表格 | 全网格线+深色表头 | 无竖线+斑马纹+底部主色线 |
| 折线图 | 纯线条无填充 | 渐变填充区域+白边数据点 |
| 登录窗口 | 可调窗口大小 | 无边框全屏渐变+淡入动画 |
| 字体系统 | 4级 | 8级 (含大号数字字体) |
