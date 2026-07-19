# BMI 体质评估与预测系统 v2.0 (JavaFX 重制版)

## 项目简介
基于 **JavaFX 8** 重制的桌面健康管理系统。本版本将 v1.0 / 旧版 v2.0 的 **Java Swing** 用户界面整体迁移为 **JavaFX**，数据层、业务计算与数据库逻辑完全复用，仅替换表现层。v1.0 与旧版 v2.0 的代码与文件保持不变。

> 说明：原 `version1/` 目录为 Swing 实现的原始系统，本次迁移不影响其任何文件；`version2/` 为本次 JavaFX 重制结果。

## JavaFX 重制要点
- **UI 框架**：由 Swing + Nimbus 全面替换为 JavaFX 8（JDK 8 自带 `jfxrt.jar`，无需额外依赖）
- **布局体系**：`BorderPane` / `VBox` / `HBox` / `GridPane` / `TabPane` 组织界面，场景在登录 / 用户端 / 管理员端之间切换
- **样式系统**：`Theme` 配色常量 + `style.css`（CSS）统一外观，圆角卡片、主色按钮、斑马纹表格、渐变背景
- **图表**：原生 `LineChart` / `BarChart` / `PieChart` 替代旧版自绘图形
- **交互体验**：响应式布局、悬停过渡、消息中心角标、主题色贯穿
- **表现层与逻辑层解耦**：`DBUtil` / `HealthCalculator` / `PasswordUtil` 为独立顶层类，无 UI 依赖，可直接复用

## 技术栈
- **语言**: Java (JDK 8，Oracle JDK 1.8.0_291)
- **UI 框架**: JavaFX 8（随 JDK 自带）
- **数据库**: PostgreSQL
- **驱动**: postgresql-42.7.3.jar

## 目录结构
```
version2/
├── src/                          # Java 源代码
│   ├── App.java                  # JavaFX 入口 (extends Application)，场景切换
│   ├── LoginView.java            # 登录 / 注册界面
│   ├── MainView.java             # 用户端主框架（11 个功能 Tab）
│   ├── AdminView.java            # 管理员端主框架（11 个功能 Tab）
│   ├── Theme.java                # 配色常量与场景样式工具
│   ├── DBUtil.java               # 数据访问层（登录/注册/健康记录/AI记录/统计/资讯等）
│   ├── HealthCalculator.java     # 体质指标计算（BMI/体脂/基础代谢等）
│   ├── PasswordUtil.java         # 密码哈希工具
│   ├── InitDB.java               # 数据库初始化工具
│   ├── style.css                 # JavaFX 样式表
│   ├── DataInputPanel.java       # 数据录入
│   ├── HistoryTrendPanel.java    # 历史趋势
│   ├── AnalysisPanel.java        # 分析评估
│   ├── PredictionPanel.java      # 预测分析
│   ├── GoalPlanPanel.java        # 目标计划
│   ├── DietPanel.java            # 饮食管理
│   ├── AIChatPanel.java          # AI 对话
│   ├── AIDietPanel.java          # AI 饮食建议
│   ├── AICookbookPanel.java      # AI 食谱
│   ├── AchievementPanel.java     # 成就徽章
│   ├── DashboardPanel.java       # 数据大屏
│   ├── HealthArticlePanel.java   # 健康资讯
│   ├── UserManagePanel.java      # 用户管理
│   ├── FoodManagePanel.java      # 食物库管理
│   ├── ExerciseManagePanel.java  # 运动库管理
│   ├── ArticleManagePanel.java   # 健康文章管理
│   ├── AIChatRecordPanel.java    # AI 对话记录
│   ├── AIDietRecordPanel.java    # AI 饮食记录
│   ├── AICookbookRecordPanel.java# AI 食谱记录
│   ├── AITemplatePanel.java      # AI 提示词模板
│   ├── ApiConfigPanel.java       # AI 接口配置
│   ├── AIUsagePanel.java         # AI 调用统计
│   └── DataMonitorPanel.java     # 数据监控
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
- **方式一**：双击 `run.bat`（自动编译并启动）
- **方式二**：手动编译运行
  ```bash
  javac -encoding UTF-8 -cp lib/postgresql-42.7.3.jar -d out src/*.java
  java -cp out;lib/postgresql-42.7.3.jar App
  ```

## 功能模块
### 用户端
| 模块 | 说明 |
|------|------|
| 数据录入 | 录入身高、体重等健康数据，模拟称重，运动记录 |
| 历史趋势 | 折线图 + 历史记录表格 |
| 分析评估 | 综合分析体质状况，生成详细报告 |
| 预测分析 | 基于线性回归预测未来趋势 |
| 目标计划 | 设定和管理健康目标 |
| 饮食管理 | 推荐饮食方案，记录饮食 |
| AI 对话 | 与 AI 健康助手对话 |
| AI 饮食建议 | 个性化饮食建议生成 |
| AI 食谱 | 智能食谱推荐 |
| 成就徽章 | 健康达成成就系统 |
| 数据大屏 | 指标卡片可视化仪表盘 |
| 健康资讯 | 浏览管理员发布的健康文章 |

### 管理员端
| 模块 | 说明 |
|------|------|
| 用户管理 | 查看与管理注册用户 |
| 食物库管理 | 维护食物营养数据 |
| 运动库管理 | 维护运动消耗数据 |
| 健康文章管理 | 发布 / 编辑健康资讯 |
| AI 对话记录 | 查看用户 AI 对话日志 |
| AI 饮食记录 | 查看 AI 饮食建议日志 |
| AI 食谱记录 | 查看 AI 食谱日志 |
| AI 提示词模板 | 维护 AI 提示词模板 |
| AI 接口配置 | 配置 AI 服务（如智谱 GLM） |
| AI 调用统计 | AI 接口调用量与趋势 |
| 数据监控 | 关键指标监控，卡片可下钻查看具体用户 |

## 与旧版差异
| 项目 | 旧版 (Swing) | 本版 (JavaFX) |
|------|------|------|
| UI 框架 | Java Swing + Nimbus | JavaFX 8 |
| 入口类 | `HealthSystem` | `App` (extends Application) |
| 布局 | 手写绝对值布局 / GridBag | BorderPane/VBox/HBox/GridPane/TabPane |
| 样式 | Java 代码内联 | CSS (`style.css`) + `Theme` |
| 图表 | 自绘 Graphics2D | 原生 LineChart/BarChart/PieChart |
| 登录流程 | 单一窗口切换 | 场景切换（登录 → 用户端 / 管理员端） |
| 逻辑层 | 内嵌于 UI 类 | 独立 `DBUtil` / `HealthCalculator` / `PasswordUtil` |

## 备注
- 本版本仅替换表现层（Swing → JavaFX），业务逻辑与数据库结构沿用现有实现。
- 编译需要 JDK 8；JavaFX 8 已随 JDK 提供，无需额外下载。
