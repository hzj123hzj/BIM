# BMI 体质评估与预测系统 - 版本变更记录

## 版本管理说明
本项目采用目录式版本管理，每个版本独立存放在 `versionN/` 目录中。

| 版本 | 目录 | 说明 | 状态 |
|------|------|------|------|
| v1.0 | `version1/` | 初始版本：Java Swing 桌面应用，含 UI 升级（Nimbus + 主题系统） | ✅ 稳定 |
| v2.0 | `version2/` | UI 全面改造升级：现代配色、圆角按钮、渐变图表、动画效果 | ✅ 稳定 |
| v3.0 | `version3/` | Aurora 设计系统：深色侧边栏导航 + 玻璃态登录 + 贝塞尔曲线图表 + 渐变按钮 | ✅ 稳定 |
| v4.0 | `version4/` | UI 全面重设计：Indigo/Emerald 设计令牌 + 深色侧边栏导航 + 自绘圆角按钮/表格 | ✅ 稳定 |

## Git 标签
- `v1.0.0` - version1 初始发布
- `v2.0.0` - version2 UI 全面升级
- `v3.0.0` - version3 Aurora 设计系统
- `v4.0.0` - version4 UI 全面重设计

---

## v4.0.0 (2026-07-16)
### UI 全面重设计（基于 v2.0 代码，保留全部业务逻辑）

> 说明：v4.0 在 `version2` 源码基础上重做 UI 表现层，数据库（PasswordUtil / DBUtil / HealthCalculator）与 8 大功能模块业务逻辑**零改动**；同时修正了 `DashboardPanel.refresh()` 原有的一处组件强转崩溃隐患。

#### 设计令牌系统（Design Tokens）
- **主色 Indigo-600** `#4F46E5`（原 Indigo-500 `#6366F1`），三级 `PRIMARY / PRIMARY_L / PRIMARY_D`
- **强调色 Emerald-500** `#10B981`（替代 Teal，更契合健康主题）
- **冷调中性灰** Slate 体系：BG / CARD_BG / HEADER_BG / FOOTER_BG / SIDEBAR_BG(#1E293B) / TEXT_DARK(#0F172A) 等
- **间距标尺** 4/8/12/16/20/24/32/40/48；**圆角分级** SM8 · MD12 · LG16 · XL20
- **8 级字体** H1(24)/H2(18)/TITLE/HEADER/BODY/BODY_B/SMALL/TINY/BIG_NUM(30)

#### 自绘圆角按钮（LaF 无关）
- 新增 `Theme.RoundedButtonUI`（继承 `BasicButtonUI`）：圆角填充 + **悬停色变动画（16ms 插值）** + 按压加深 + **焦点白色光环**
- `stylePrimary/Accent/Ghost/DangerButton` 全部改用该 UI，不再受 Nimbus 限制

#### 主窗口：深色侧边栏 + CardLayout（v4 最大改版）
- 以 `CardLayout` 替代 `JTabbedPane`，左侧 248px **深色侧边栏**（品牌区 + 8 个「图标+文字」NavItem + 底部版权）
- 选中态主色高亮、悬停态 `SIDEBAR_HOVER`；顶部白色 Header（页面标题 + 用户信息 + 健康评分）、底部状态栏保留

#### 卡片 / 表格 / 表单质感
- `RoundedPanel` 多层柔和阴影 + 浅边框；`createCardPanel` 标题左侧色条
- `styleTable` 重写：44px 行高、斑马纹、行悬停高亮、表头描边与 14px 内边距
- 文本/密码框聚焦变主色加粗；下拉、微调器统一样式

#### 数据大屏
- `createMetricCard` 增加 **图标徽标**，Dashboard 12 张指标卡全部配图标（⚖️📈🥗 等）

#### 登录 / 注册
- 无边框全屏渐变 + 圆形 Logo + 淡入动画；白色卡片内 Tab 切换，表单间距更舒展

#### 编译验证
- `javac -encoding UTF-8 -cp lib/postgresql-42.7.3.jar -d out src/HealthSystem.java` → **0 error**

---

## v3.0.0 (2026-07-16)
### Aurora 设计系统 — UI 全面重构

#### 导航系统重构
- **深色侧边栏导航**：取代 JTabbedPane，新增 `SidebarNav` + `NavItem` 组件
- 侧边栏使用深紫渐变背景 (#161424 → #232038)，220px 固定宽度
- NavItem 支持图标 + 文字，选中态使用渐变背景 (Indigo → Violet)，悬停态半透明白色
- 顶部 Logo 区域：渐变圆形图标 + 品牌文字
- 底部版本标识区域

#### 登录窗口重构
- **分屏玻璃态设计**：左侧 380px 三色渐变品牌面板 (TriGradientPanel) + 右侧白色表单
- 左侧使用 GRAD_LOGIN 三色渐变 (Indigo → Purple → Magenta)
- 品牌区域：大号 Logo 圆形图标 + 双行标题 + Aurora 标语 + 版权信息
- 右侧表单区使用 JTabbedPane 切换登录/注册
- 关闭按钮悬停变红色

#### 图表系统升级
- **贝塞尔平滑曲线**：折线图使用 `Path2D.curveTo()` 绘制三次贝塞尔曲线
- 渐变填充区域使用贝塞尔路径闭合
- 图表背景使用圆角矩形
- 预测虚线保留直线样式

#### 组件增强
- **RoundButton 渐变模式**：新增 `setGradient(true)` 支持渐变填充 + 顶部半透明高光
- **RoundedPanel 四层阴影**：从 3 层升级为 4 层渐进阴影 + 顶部白色描边高光
- **TriGradientPanel**：新增三色线性渐变面板 (LinearGradientPaint)
- **PieChartPanel 环形图**：饼图增加中心镂空形成环形效果，图例使用圆角方块
- **createMetricCard 增强**：指标卡片增加 4 层外发光 + 渐变背景 + 顶部半透明覆盖层

#### 配色系统更新
- 强调色从 Teal (#14B8A6) 更改为 Rose (#F43F94)
- 新增侧边栏配色：SIDEBAR_BG (#161424) / SIDEBAR_BG2 (#232038) / SIDEBAR_HOVER / SIDEBAR_TEXT
- 新增 GRAD_LOGIN 三色渐变、GRAD_SIDEBAR 双色渐变
- 所有按钮使用渐变模式

#### MainFrame 架构变更
- 使用 `CardLayout` 替代 `JTabbedPane` 进行内容切换
- `switchToTab(int)` 调用 `cardLayout.show()` 切换面板
- 8 个面板通过 SidebarNav 导航切换

#### 业务逻辑保持不变
- 完整保留 v2.0 全部业务逻辑：PasswordUtil / DBUtil / HealthCalculator
- 数据库操作、健康计算、预测算法零改动
- 所有 8 个功能模块业务逻辑一致

---

## v2.0.0 (2026-07-16)
### UI 全面升级
- **现代配色方案**：深靛蓝 (Indigo #6366F1) 主色 + 青绿 (Teal #14B8A6) 强调色，替代旧版青蓝+暖橙
- **分级字体系统**：8 级字体 (H1/H2/Title/Header/Body/BodyB/Small/Tiny/BigNum)，优先 Microsoft YaHei UI
- **RoundButton 组件**：自定义圆角按钮，带悬停色变效果和阴影
- **输入框焦点动画**：获取焦点时边框变粗变为主色
- **表格现代化**：斑马纹背景、无竖线、底部主色边框、32px 行高
- **折线图升级**：渐变填充区域、白边数据点、现代配色
- **淡入动画工具**：FadeIn 类实现窗口/面板淡入过渡
- **登录窗口重构**：无边框全屏渐变、Logo 圆形图标、淡入动画
- **主窗口升级**：深色渐变顶栏、浅色底部状态栏、emoji 用户信息
- **DashboardPanel**：使用渐变指标卡片 (createMetricCard)
- **响应式适配**：设置 minimumSize 支持窗口缩放

### 技术改进
- Theme 类新增 6 种渐变色对 (GRAD_PRIMARY/ACCENT/SUNSET/OCEAN/HEADER)
- RoundedPanel 增强为多层柔和阴影
- GradientPanel 支持数组构造
- 新增 styleGhostButton、styleDangerButton、stylePasswordField、styleSpinner 方法

---

## v1.0.0 (2026-07-16)
### 新增
- BMI 体质评估与预测系统初始版本
- 8 大功能模块：数据录入、历史趋势、分析评估、预测分析、目标计划、饮食管理、成就徽章、数据大屏
- PostgreSQL 数据库支持，用户注册登录系统
- Nimbus 外观 + 自定义主题系统（青蓝色主色调 + 橙色强调色）
- 卡片式布局，渐变背景，圆角阴影效果
- 一键运行脚本 `run.bat`

### 技术细节
- Java Swing 单文件架构（HealthSystem.java）
- JDBC 连接 PostgreSQL
- 密码加密存储（SHA-256）
- JFreeChart 风格的自绘图表

---

## v4.0.1 (2026-07-16)
### 报告界面重设计（Keep 风格可视化报告）

> 用户反馈分析报告 / 预测分析报告「像纯文本没用 css 渲染」，本次将两份报告从「JTextArea + ASCII 字符排版」彻底重做为结构化可视化报告。

#### 新增 Keep 风格设计令牌与组件（Theme 类）
- **鲜绿强调色 KEEP** `#12B847`（KEEP / KEEP_D / KEEP_L / KEEP_BG / KEEP_SOFT），呼应 Keep 运动风
- 复用组件升级：`createBadge`(状态徽章) / `createStatCard`(左侧强调条指标卡) / `createInfoRow`(label-value 行) / `createReportHeader`(绿渐变报告头) / `ProgressRing`(健康评分环形进度) / `emptyReportCard`(空状态卡)

#### 健康分析评估报告（AnalysisPanel）
- 绿渐变报告头（标题 + 副标题 + 更新日期）
- **健康综合评分环形进度** + 等级徽章
- 核心指标 3 列网格（BMI / 体脂率 / 内脏脂肪 / 身体年龄 / 骨骼肌肉量 / 水分率），每张卡带彩色状态徽章
- BMR 三公式对比、能量消耗与理想体重、体质与形态分节卡片（替代原 ASCII 文本）

#### 预测分析报告（PredictionPanel）
- 绿渐变报告头
- 体重趋势 4 格总览（当前 / 7天 / 14天 / 30天，含 ▲▼ 变化量）
- 每日热量差分析（信息行 + 每周增减重预估 + 状态徽章）
- 目标达成预测（目标类型 / 目标体重 / 预计达成日期）
- 30 天健康风险评估（低/中/高风险徽章）

#### 兼容性
- 仍保留 PostgreSQL 业务逻辑（DBUtil / HealthCalculator）**零改动**
- 无数据时显示「尚未录入健康数据 / 数据不足」空状态卡，不再丑陋文本
- `javac` 编译 0 error（JDK 1.8.0_291 验证）

