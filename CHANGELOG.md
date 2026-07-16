# BMI 体质评估与预测系统 - 版本变更记录

## 版本管理说明
本项目采用目录式版本管理，每个版本独立存放在 `versionN/` 目录中。

| 版本 | 目录 | 说明 | 状态 |
|------|------|------|------|
| v1.0 | `version1/` | 初始版本：Java Swing 桌面应用，含 UI 升级（Nimbus + 主题系统） | ✅ 稳定 |
| v2.0 | `version2/` | UI 全面改造升级：现代配色、圆角按钮、渐变图表、动画效果 | ✅ 稳定 |
| v3.0 | `version3/` | （待开发） | 📋 规划中 |

## Git 标签
- `v1.0.0` - version1 初始发布
- `v2.0.0` - version2 UI 全面升级

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
