# BMI 体质评估与预测系统 · v5（Web 版）

> version2 的 Java Swing 界面「太丑」了？v5 用 **Vue 3 + Vite + TypeScript** 重写了整套 UI，
> 把原来的桌面客户端升级为一个真正现代化的 Web 健康管理应用，并完整保留全部业务逻辑与算法。

设计语言：**Botanical Calm（植物疗愈）** —— 深常绿侧边栏 + 暖奶油画布 + 草本绿 / 杏橘配色，
手绘 SVG 图表，无图表库依赖，告别「紫蓝渐变发光」的套壳 AI 风。

## ✨ 特性

- 🎨 全新设计系统：设计令牌（颜色 / 间距 / 圆角 / 阴影 / 缓动）、Sora + Manrope 字体
- 📱 响应式布局：桌面侧边栏导航，移动端抽屉式导航
- ♿ 可访问性：语义标签、键盘可达、`focus-visible` 焦点环、尊重 `prefers-reduced-motion`
- 🧮 业务算法 1:1 移植自 Java `HealthCalculator`（BMI / 体脂 / 内脏脂肪 / 身体年龄 / 五维健康评分 / 运动热量 / 线性回归预测 / 目标达成天数）
- 🔌 可切换数据层：默认 `mock`（localStorage，开箱即用，含体验账号与 30 天样例数据）；可一键切换到真实后端
- 🚀 性能优先：手绘 SVG 图表（零图表库）、按路由懒加载、纯静态产物可直接部署

## 🧩 功能模块（与 Java 版 8 大模块对齐）

| 模块 | 说明 |
|------|------|
| 数据大屏 | 健康评分环 + 8 项指标卡 + 体重趋势 + 快捷入口 |
| 数据录入 | 每日称重打卡，自动派生 BMI / BMR / TDEE / 身体年龄 / 体质 |
| 历史趋势 | 多指标切换 + 7/30/90 天区间 + 预测延伸 |
| 分析评估 | 体成分构成 + 健康评分五维拆解 + 体质解读 |
| 预测分析 | 7/14/30 天体重预测 + 30 天健康风险评估 |
| 目标计划 | 设定减脂 / 增肌目标 + 基于热量差的达成天数估算 |
| 饮食管理 | 80 种食物库检索 + 宏量营养环形图 + 今日热量差 |
| 成就徽章 | 坚持打卡解锁的成就墙 |

## 🚀 快速开始

```bash
cd version5
npm install
npm run dev        # 本地开发 http://localhost:5173
npm run build      # 生产构建到 dist/
npm run preview    # 预览构建产物
```

**体验账号**（已预置 30 天样例数据）：

```
用户名：demo
密码：  demo123
```

> 也可在登录页直接注册新账号。

## 🔌 对接真实后端（Java / Spring Boot 等）

默认 `VITE_API_MODE=mock` 使用浏览器本地数据，无需数据库即可体验完整功能。
若要接入真实后端，把 Java 版的业务逻辑包装为 REST API，然后：

```bash
cp env.example .env
# 修改 .env
VITE_API_MODE=http
VITE_API_BASE=https://your-backend.example.com/api
```

`src/services/api.ts` 已按以下契约预留好 `http` 模式请求（用户名经 URL 编码）：

```
POST   /auth/register         { username, password, gender, age, height, activityLevel }
POST   /auth/login            { username, password }
GET    /users/:username
GET    /records/:username?days=
POST   /records
GET    /exercise/:username?days=
POST   /exercise
GET    /goals/:username
POST   /goals
GET    /diet/:username?days=
POST   /diet
GET    /achievements/:username
POST   /achievements/:username/check
GET    /foods
GET    /records/:username/checked
```

## 🗂 目录结构

```
version5/
├─ index.html
├─ package.json  vite.config.ts  tsconfig.json
├─ public/favicon.svg
└─ src/
   ├─ main.ts  App.vue  style.css  env.d.ts  nav.ts  types.ts
   ├─ lib/health.ts             # 移植自 Java 的全部健康算法
   ├─ services/                 # api.ts（mock/http 双模式）+ mockDb.ts（localStorage）
   ├─ stores/                   # auth.ts（认证）+ toast.ts（轻提示）
   ├─ components/               # Sidebar / Header / MetricCard / LineChart / DonutChart / ProgressRing / BarMeter / StatPill / EmptyState / ToastHost
   └─ views/                    # Login + AppLayout + 8 大模块视图
```

## 🛠 技术栈

- Vue 3.4（`<script setup>` + TypeScript）
- Vue Router 4（路由守卫 + 懒加载）
- Vite 5（构建 / 开发服务器）
- 零 UI 组件库、零图表库（全部手写 SVG，保证轻量与可控视觉）
