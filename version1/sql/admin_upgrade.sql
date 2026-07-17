-- ============================================================
-- BMI 体质评估与预测系统 — 管理员模块数据库扩展脚本
-- 在已有 health_db 上执行，用于升级管理员功能
-- ============================================================

\c health_db;

-- ============ 管理员账号表 ============
CREATE TABLE IF NOT EXISTS admins (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,              -- SHA-256 加盐哈希
    salt VARCHAR(64) NOT NULL,
    role VARCHAR(20) DEFAULT 'admin',            -- admin / super_admin
    status VARCHAR(20) DEFAULT '启用',           -- 启用 / 禁用 / 冻结
    expiry_date DATE,                            -- 账号有效期
    created_at TIMESTAMP DEFAULT NOW(),
    last_login TIMESTAMP
);

-- 插入默认管理员账号: admin / admin123
-- 注意：仅当管理员表为空时插入，避免重复
INSERT INTO admins (username, password, salt, role, status)
SELECT 'admin',
       '9f86d08...',  -- 占位，程序启动时会自动初始化
       'salt',
       'super_admin',
       '启用'
WHERE NOT EXISTS (SELECT 1 FROM admins);

-- ============ 用户表扩展字段 ============
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) DEFAULT '启用';  -- 启用 / 禁用 / 冻结
ALTER TABLE users ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;              -- 软删除标记
ALTER TABLE users ADD COLUMN IF NOT EXISTS checkin_days INT DEFAULT 0;                 -- 连续打卡天数
ALTER TABLE users ADD COLUMN IF NOT EXISTS device_type VARCHAR(50);                    -- 设备类型

-- ============ 系统配置表 ============
CREATE TABLE IF NOT EXISTS system_config (
    id SERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description VARCHAR(255),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 插入默认系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('weight_change_threshold', '5.0', '7天体重变化阈值(kg)，超过视为异常'),
('body_fat_rise_days', '30', '体脂连续上升天数阈值'),
('no_checkin_days', '7', '长期未打卡天数阈值'),
('bmi_low', '18.5', 'BMI 正常下限'),
('bmi_high', '24.9', 'BMI 正常上限'),
('bmr_formula', 'avg', 'BMR 计算公式：mifflin / harris / avg'),
('weight_loss_speed', '0.5', '减脂速度 kg/周'),
('muscle_gain_speed', '0.3', '增肌速度 kg/周'),
('checkin_reminder_time', '20:00', '打卡提醒时间'),
('checkin_reward_rule', '连续7天打卡奖励1枚徽章', '连续打卡奖励规则')
ON CONFLICT (config_key) DO NOTHING;

-- ============ 系统日志表 ============
CREATE TABLE IF NOT EXISTS system_logs (
    id SERIAL PRIMARY KEY,
    log_type VARCHAR(50),                        -- ADMIN / USER / SYSTEM
    operator VARCHAR(50),                        -- 操作者
    action VARCHAR(255),                         -- 操作内容
    detail TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============ 运动库表 ============
CREATE TABLE IF NOT EXISTS exercise_library (
    id SERIAL PRIMARY KEY,
    exercise_name VARCHAR(50) UNIQUE NOT NULL,
    exercise_type VARCHAR(20),                   -- 有氧 / 力量
    calories_per_hour INT,                       -- 每小时消耗热量
    intensity_level VARCHAR(10),                   -- 低 / 中 / 高
    description TEXT
);

INSERT INTO exercise_library (exercise_name, exercise_type, calories_per_hour, intensity_level, description) VALUES
('跑步', '有氧', 600, '中', '户外或跑步机跑步'),
('游泳', '有氧', 700, '高', '全身性有氧运动'),
('力量训练', '力量', 400, '高', '器械或自重训练'),
('骑行', '有氧', 500, '中', '户外或动感单车'),
('瑜伽', '有氧', 200, '低', '柔韧性及放松训练'),
('跳绳', '有氧', 700, '高', '高强度间歇训练'),
('快走', '有氧', 300, '低', '低强度有氧'),
('球类', '有氧', 500, '中', '篮球、足球等')
ON CONFLICT (exercise_name) DO NOTHING;

-- ============ 健康文章表 ============
CREATE TABLE IF NOT EXISTS health_articles (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category VARCHAR(50),
    published_at TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20) DEFAULT '已发布'
);

-- ============ 消息模板表 ============
CREATE TABLE IF NOT EXISTS message_templates (
    id SERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    template_content TEXT NOT NULL,
    type VARCHAR(20),                            -- 系统通知 / 健康提醒 / 活动推送
    variables VARCHAR(255)                       -- 支持的变量，如 {username}, {date}
);

INSERT INTO message_templates (template_name, template_content, type, variables) VALUES
('系统通知', '您好 {username}，系统将于 {date} 进行维护，请提前保存数据。', '系统通知', '{username},{date}'),
('健康提醒', '您好 {username}，您已 {days} 天未打卡，记得保持健康记录！', '健康提醒', '{username},{days}'),
('活动推送', '您好 {username}，本周有健康挑战活动，欢迎参加！', '活动推送', '{username}')
ON CONFLICT DO NOTHING;

-- ============ 通知消息表 ============
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    sender VARCHAR(50),                          -- 发送者（admin / system）
    receiver VARCHAR(50),                      -- 接收者（username 或 ALL）
    title VARCHAR(200),
    content TEXT,
    type VARCHAR(20),                            -- 系统通知 / 健康提醒 / 活动推送
    status VARCHAR(20) DEFAULT '未读',           -- 未读 / 已读
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============ AI 配置表 ============
CREATE TABLE IF NOT EXISTS ai_api_config (
    id SERIAL PRIMARY KEY,
    provider VARCHAR(50),                        -- 硅基流动 / OpenAI 等
    api_key VARCHAR(255),
    model_name VARCHAR(100),
    endpoint VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    call_count INT DEFAULT 0,
    cost_estimate DECIMAL(10,2) DEFAULT 0.00,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============ AI 提示词模板表 ============
CREATE TABLE IF NOT EXISTS ai_templates (
    id SERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    prompt_text TEXT NOT NULL,
    template_type VARCHAR(50),                   -- 建议 / 周报 / 饮食推荐
    status VARCHAR(20) DEFAULT '有效',           -- 有效 / 无效 / 待优化
    updated_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO ai_templates (template_name, prompt_text, template_type) VALUES
('健康建议模板', '根据用户健康数据生成个性化健康建议...', '建议'),
('周报模板', '根据本周数据生成健康周报...', '周报'),
('饮食推荐模板', '根据用户目标推荐今日饮食...', '饮食推荐')
ON CONFLICT DO NOTHING;

-- ============ AI 问答记录表 ============
CREATE TABLE IF NOT EXISTS ai_chat_records (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) REFERENCES users(username),
    question TEXT,
    answer TEXT,
    status VARCHAR(20) DEFAULT '有效',             -- 有效 / 无效 / 待优化
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============ 索引 ============
CREATE INDEX IF NOT EXISTS idx_users_status ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted);
CREATE INDEX IF NOT EXISTS idx_system_logs_time ON system_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_receiver ON notifications(receiver);
CREATE INDEX IF NOT EXISTS idx_ai_chat_user ON ai_chat_records(username);
