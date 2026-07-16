# BMI 体质评估与预测系统 v3.0 — Aurora Design System

## 概述
v3.0 Aurora 设计系统是对 v2.0 的全面 UI 重构，引入深色侧边栏导航、分屏玻璃态登录、贝塞尔曲线图表、渐变按钮和增强阴影系统，在保持全部业务逻辑不变的前提下大幅提升视觉品质。

## 设计亮点

### Aurora 设计系统
- **深色侧边栏导航**：取代 JTabbedPane，使用 `SidebarNav` + `NavItem` 组件，支持图标+文字、渐变选中态、悬停高亮
- **分屏玻璃态登录**：左侧三色渐变品牌面板 (TriGradientPanel) + 右侧白色表单卡片
- **贝塞尔平滑曲线**：折线图使用 `Path2D.curveTo()` 绘制平滑贝塞尔曲线，替代直线连接
- **渐变按钮**：`RoundButton` 支持 `setGradient(true)` 渐变填充 + 顶部高光
- **四层阴影系统**：RoundedPanel 增强为 4 层渐进阴影 + 顶部白色描边高光
- **渐变指标卡片**：DashboardPanel 指标卡片使用渐变背景 + 半透明覆盖层

### 配色系统
| 角色 | 颜色 | Hex |
|------|------|-----|
| 主色 Primary | Indigo | #6366F1 |
| 强调色 Accent | Rose | #F43F94 |
| 侧边栏背景 | Deep Violet | #161424 |
| 成功 Success | Emerald | #10B981 |
| 警告 Warning | Amber | #F59E0B |
| 危险 Danger | Red | #EF4444 |

### 字体系统
- 优先使用 Microsoft YaHei UI
- 9 级字体：H1(24) / H2(18) / Title(16) / Header(14) / Body(13) / BodyB(13) / Small(12) / Tiny(11) / BigNum(28) / Nav(13)

## 功能模块
1. **数据录入** — 健康数据 7 项指标 + 运动记录
2. **历史趋势** — 贝塞尔曲线趋势图 + 历史明细表格
3. **分析评估** — BMI/BMR(三公式)/TDEE/体脂/内脏脂肪/体质分类/健康评分
4. **预测分析** — 线性回归趋势预测 + 热量差分析 + 风险评估
5. **目标计划** — 4 周分阶段进度条 + 运动建议 + 达成日期预测
6. **饮食管理** — 食物记录 + 营养饼图(环形) + 拍照识别 + CSV 导出
7. **成就徽章** — 7 种成就徽章 + AI 周/月报告生成
8. **数据大屏** — 14 项核心指标渐变卡片

## 技术栈
- Java (JDK 8+) + Java Swing
- PostgreSQL 数据库 + JDBC
- Nimbus Look and Feel

## 快速开始
1. 确保 PostgreSQL 运行在 `localhost:5432`
2. 执行 `sql/init_database.sql` 初始化数据库
3. 双击 `run.bat` 运行

## 目录结构
```
version3/
├── src/
│   ├── HealthSystem.java    # 主程序 (Aurora Design System)
│   ├── InitDB.java           # 数据库初始化工具
│   └── CheckDB.java          # 数据库连接检查
├── lib/
│   └── postgresql-42.7.3.jar # JDBC 驱动
├── sql/
│   └── init_database.sql     # 建表脚本
├── docs/
└── run.bat                   # 一键编译运行
```
