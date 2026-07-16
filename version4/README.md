# BMI 体质评估与预测系统 v4.0 (UI 全面重设计方案)

## 项目简介
基于 Java Swing 的桌面健康管理系统 v4.0。在 v2.0 的基础上，对 **整套视觉设计系统** 进行了彻底重构：
保留原有的 PostgreSQL 数据库与全部业务逻辑，仅替换 UI 表现层，使界面达到现代桌面应用的水准。

> v3.0 规划中，本目录即对应「version4」设计交付物。

## v4.0 设计升级要点

### 1. 全新设计令牌系统（Design Tokens）
- **精炼配色**：主色采用 Indigo-600 (`#4F46E5`) + 强调色 Emerald-500 (`#10B981`)，比 v2 更沉稳、更具「健康」气质。
- **冷调中性灰**：Slate-50/100/200/700/800/900 构成层次分明的背景与文字体系，对比度满足 WCAG AA。
- **间距标尺**：4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 px 统一节奏。
- **圆角分级**：SM 8 · MD 12 · LG 16 · XL 20，卡片与按钮一致。

### 2. 自绘圆角按钮（LaF 无关）
- 所有主/次/幽灵/危险按钮改用 `RoundedButtonUI` 自绘渲染，不再受 Nimbus 限制。
- **悬停色变动画**（16ms 平滑插值）、**按压加深**、**焦点白边光环**，交互清晰。

### 3. 左侧导航栏 + CardLayout（v4 最大改版）
- 主窗口由顶部 Tab 改为 **深色侧边栏**：品牌区 + 8 个「图标 + 文字」导航项 + 底部版权。
- 选中态以主色高亮，内容区以 `CardLayout` 切换，体验接近现代 Web 后台。

### 4. 卡片与表格质感提升
- `RoundedPanel` 多层柔和阴影 + 1px 浅边框；`createCardPanel` 标题加左侧色条。
- `styleTable` 重写：44px 行高、斑马纹、行悬停高亮、表头描边与内边距优化。

### 5. 输入框 / 下拉 / 微调器
- 文本与密码框聚焦时边框变主色并加粗；下拉、微调器统一样式。

### 6. 数据大屏指标卡
- `createMetricCard` 增加 **图标徽标**，Dashboard 12 张指标卡全部配图标，信息更易扫读。
- 修复原 `refresh()` 因组件索引强转导致的潜在崩溃。

### 7. 登录 / 注册
- 无边框全屏渐变 + 圆形 Logo + 淡入动画；白色卡片内 Tab 切换，表单间距更舒展。

## 技术栈
- **语言**: Java (JDK 8+)
- **UI 框架**: Java Swing + Nimbus Look and Feel（按钮/表格自绘覆盖）
- **数据库**: PostgreSQL
- **驱动**: postgresql-42.7.3.jar

## 目录结构
```
version4/
├── src/                          # Java 源代码
│   ├── HealthSystem.java         # 主程序（含 v4.0 UI 设计系统）
│   ├── InitDB.java               # 数据库初始化工具
│   └── CheckDB.java              # 数据库诊断工具
├── lib/                          # 第三方依赖库
│   └── postgresql-42.7.3.jar     # PostgreSQL JDBC 驱动
├── sql/                          # 数据库脚本
│   └── init_database.sql         # 建表脚本
├── run.bat                       # Windows 一键运行脚本
└── README.md                     # 本文件
```

## 快速开始

### 1. 数据库准备
```sql
CREATE DATABASE health_db;
-- 然后执行 sql/init_database.sql 建表
```
默认连接：地址 `localhost:5432` · 库名 `health_db` · 用户 `postgres` · 密码在 `HealthSystem.java` 的 `DB_PASS` 中配置。

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
| 数据录入 | 身高体重等健康数据、运动记录 |
| 历史趋势 | 渐变折线图 + 历史记录表格 |
| 分析评估 | 综合体质报告 |
| 预测分析 | 基于线性回归的趋势预测 |
| 目标计划 | 健康目标设定与管理 |
| 饮食管理 | 饮食方案与记录 |
| 成就徽章 | 健康达成成就系统 |
| 数据大屏 | 渐变指标卡可视化仪表盘 |

## v2.0 → v4.0 UI 对比
| 项目 | v2.0 | v4.0 |
|------|------|------|
| 主色 | Indigo-500 (#6366F1) | Indigo-600 (#4F46E5) |
| 强调色 | Teal (#14B8A6) | Emerald (#10B981) |
| 导航 | 顶部文字 Tab | 深色侧边栏（图标+文字） |
| 按钮 | Nimbus 默认/自绘平淡 | 自绘圆角 + 悬停动画 + 焦点光环 |
| 卡片阴影 | 单层 | 多层柔和阴影 + 浅边框 |
| 表格 | 32px 行高 | 44px 行高 + 悬停高亮 + 斑马纹 |
| 指标卡 | 纯文字 | 图标徽标 + 渐变 |
| 登录 | 渐变背景 | 渐变 + 圆形 Logo + 淡入 |

---
**设计**: UI Designer · **版本**: v4.0 · **状态**: ✅ 已编译通过（javac 0 error）
