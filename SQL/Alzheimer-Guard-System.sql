/*
 Navicat Premium Data Transfer

 Source Server         : alzheimer_gaurd_system_db
 Source Server Type    : PostgreSQL
 Source Server Version : 160011 (160011)
 Source Host           : 8.138.144.238:5432
 Source Catalog        : alzheimer_guard_system_db
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 160011 (160011)
 File Encoding         : 65001

 Date: 24/12/2025 19:07:52
*/


-- ----------------------------
-- Table structure for ai_session
-- ----------------------------
CREATE TABLE "ai_session" (
  "id" int8 NOT NULL DEFAULT nextval('ai_session_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "patient_id" int8,
  "prompt" text COLLATE "pg_catalog"."default",
  "response" text COLLATE "pg_catalog"."default",
  "model_name" varchar(50) COLLATE "pg_catalog"."default",
  "token_usage" int4,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "user_ip" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "ai_session"."user_id" IS '用户id';
COMMENT ON COLUMN "ai_session"."patient_id" IS '老人档案id';
COMMENT ON COLUMN "ai_session"."prompt" IS '用户提问内容';
COMMENT ON COLUMN "ai_session"."response" IS 'AI回答内容';
COMMENT ON COLUMN "ai_session"."model_name" IS '使用的模型版本 (如: qwen-turbo)';
COMMENT ON COLUMN "ai_session"."token_usage" IS '本次对话消耗Token数';
COMMENT ON COLUMN "ai_session"."created_at" IS '创建时间';
COMMENT ON COLUMN "ai_session"."user_ip" IS '用户ip';
COMMENT ON TABLE "ai_session" IS 'AI大模型对话历史记录';

-- ----------------------------
-- Table structure for clue_record
-- ----------------------------
CREATE TABLE "clue_record" (
  "id" int8 NOT NULL DEFAULT nextval('clue_record_id_seq'::regclass),
  "patient_id" int8 NOT NULL,
  "finder_phone" varchar(20) COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "photo_url" varchar(255) COLLATE "pg_catalog"."default",
  "location" geometry,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "sent_ip" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "clue_record"."patient_id" IS '关联老人ID';
COMMENT ON COLUMN "clue_record"."finder_phone" IS '路人/发现者手机号';
COMMENT ON COLUMN "clue_record"."description" IS '现场情况描述';
COMMENT ON COLUMN "clue_record"."photo_url" IS '现场拍摄照片URL';
COMMENT ON COLUMN "clue_record"."location" IS '扫码时的GIS坐标';
COMMENT ON COLUMN "clue_record"."created_at" IS '创建时间';
COMMENT ON COLUMN "clue_record"."sent_ip" IS '发送者ip';
COMMENT ON TABLE "clue_record" IS '走失线索记录表 (路人扫码上传)';

-- ----------------------------
-- Table structure for factory_order
-- ----------------------------
CREATE TABLE "factory_order" (
  "id" int8 NOT NULL DEFAULT nextval('factory_order_id_seq'::regclass),
  "order_no" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8 NOT NULL,
  "product_name" varchar(100) COLLATE "pg_catalog"."default",
  "quantity" int4 DEFAULT 1,
  "amount" numeric(10,2),
  "receiver_name" varchar(50) COLLATE "pg_catalog"."default",
  "receiver_phone" varchar(20) COLLATE "pg_catalog"."default",
  "address" text COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'CREATED'::character varying,
  "tracking_number" varchar(64) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "produced_at" timestamp(6),
  "cancelled_at" timestamp(6),
  "shipped_at" timestamp(6),
  "delivered_at" timestamp(6),
  "completed_at" timestamp(6),
  "last_modify_id" int8 NOT NULL
)
;
COMMENT ON COLUMN "factory_order"."order_no" IS '业务订单号 (唯一)';
COMMENT ON COLUMN "factory_order"."user_id" IS '下单用户ID';
COMMENT ON COLUMN "factory_order"."product_name" IS '商品名称';
COMMENT ON COLUMN "factory_order"."quantity" IS '购买数量';
COMMENT ON COLUMN "factory_order"."amount" IS '订单总金额';
COMMENT ON COLUMN "factory_order"."receiver_name" IS '收货人姓名 (快照)';
COMMENT ON COLUMN "factory_order"."receiver_phone" IS '收货人电话 (快照)';
COMMENT ON COLUMN "factory_order"."address" IS '收货详细地址 (快照)';
COMMENT ON COLUMN "factory_order"."status" IS '订单状态: CREATED, PRODUCING, CANCELLED，SHIPPING, DELIVERED, COMPLETED';
COMMENT ON COLUMN "factory_order"."tracking_number" IS '快递单号';
COMMENT ON COLUMN "factory_order"."created_at" IS '创建时间';
COMMENT ON COLUMN "factory_order"."produced_at" IS '生产时间';
COMMENT ON COLUMN "factory_order"."cancelled_at" IS '取消时间';
COMMENT ON COLUMN "factory_order"."shipped_at" IS '发货时间';
COMMENT ON COLUMN "factory_order"."delivered_at" IS '送达时间';
COMMENT ON COLUMN "factory_order"."completed_at" IS '完成时间';
COMMENT ON COLUMN "factory_order"."last_modify_id" IS '最后操作者';
COMMENT ON TABLE "factory_order" IS '设备购买订单表';

-- ----------------------------
-- Table structure for patient_profile
-- ----------------------------
CREATE TABLE "patient_profile" (
  "id" int8 NOT NULL DEFAULT nextval('patient_profile_id_seq'::regclass),
  "guardian_id" int8 NOT NULL,
  "community_id" int8,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "age" int4,
  "short_code" varchar(10) COLLATE "pg_catalog"."default",
  "contact_phone" varchar(20) COLLATE "pg_catalog"."default",
  "medical_history" text COLLATE "pg_catalog"."default",
  "lost_status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'NORMAL'::character varying,
  "last_location" geometry,
  "last_seen_time" timestamp(6),
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6),
  "id_number" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "patient_profile"."guardian_id" IS '法定监护人ID (拥有最高修改权限)';
COMMENT ON COLUMN "patient_profile"."community_id" IS '所属社区ID (由该社区负责管辖)';
COMMENT ON COLUMN "patient_profile"."name" IS '老人姓名';
COMMENT ON COLUMN "patient_profile"."age" IS '老人年龄';
COMMENT ON COLUMN "patient_profile"."short_code" IS '防走失短码 (印刷在设备上的唯一码)';
COMMENT ON COLUMN "patient_profile"."contact_phone" IS '紧急联系电话 (通常印在衣服上)';
COMMENT ON COLUMN "patient_profile"."medical_history" IS '既往病史 (供AI分析用)';
COMMENT ON COLUMN "patient_profile"."lost_status" IS '走失状态: NORMAL(正常), LOST(走失中)';
COMMENT ON COLUMN "patient_profile"."last_location" IS '最后一次出现的GIS坐标';
COMMENT ON COLUMN "patient_profile"."last_seen_time" IS '最后位置更新时间';
COMMENT ON COLUMN "patient_profile"."id_number" IS '身份证号';
COMMENT ON TABLE "patient_profile" IS '老人核心档案表';

-- ----------------------------
-- Table structure for sys_community
-- ----------------------------
CREATE TABLE "sys_community" (
  "id" int8 NOT NULL DEFAULT nextval('sys_community_id_seq'::regclass),
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "address" varchar(200) COLLATE "pg_catalog"."default",
  "contact_phone" varchar(20) COLLATE "pg_catalog"."default",
  "center_location" geometry,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6),
  "status" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "sys_community"."id" IS '主键ID';
COMMENT ON COLUMN "sys_community"."name" IS '社区名称 (如: 幸福里街道)';
COMMENT ON COLUMN "sys_community"."address" IS '社区办公地址';
COMMENT ON COLUMN "sys_community"."contact_phone" IS '社区值班电话';
COMMENT ON COLUMN "sys_community"."center_location" IS '社区中心点坐标 (GIS Point)';
COMMENT ON COLUMN "sys_community"."created_at" IS '创建时间';
COMMENT ON COLUMN "sys_community"."updated_at" IS '修改时间';
COMMENT ON COLUMN "sys_community"."status" IS '社区状态';
COMMENT ON TABLE "sys_community" IS '社区/辖区信息表';

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
CREATE TABLE "sys_log" (
  "id" int8 NOT NULL DEFAULT nextval('sys_log_id_seq'::regclass),
  "user_id" int8,
  "username" varchar(50) COLLATE "pg_catalog"."default",
  "module" varchar(50) COLLATE "pg_catalog"."default",
  "action" varchar(50) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "ip_address" varchar(50) COLLATE "pg_catalog"."default",
  "params" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "sys_log"."user_id" IS '操作人ID';
COMMENT ON COLUMN "sys_log"."username" IS '操作人账号 (冗余记录)';
COMMENT ON COLUMN "sys_log"."module" IS '操作模块 (如: 老人管理)';
COMMENT ON COLUMN "sys_log"."action" IS '具体动作 (如: DELETE)';
COMMENT ON COLUMN "sys_log"."content" IS '操作描述';
COMMENT ON COLUMN "sys_log"."ip_address" IS '操作者IP地址';
COMMENT ON COLUMN "sys_log"."params" IS '请求参数JSON';
COMMENT ON TABLE "sys_log" IS '系统操作安全日志';

-- ----------------------------
-- Table structure for sys_notification
-- ----------------------------
CREATE TABLE "sys_notification" (
  "id" int8 NOT NULL DEFAULT nextval('sys_notification_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "type" varchar(20) COLLATE "pg_catalog"."default",
  "title" varchar(100) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "is_read" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "sys_notification"."type" IS '通知类型 (SMS, APP)';
COMMENT ON COLUMN "sys_notification"."title" IS '通知标题';
COMMENT ON COLUMN "sys_notification"."content" IS '通知内容';
COMMENT ON COLUMN "sys_notification"."is_read" IS '是否已读';
COMMENT ON TABLE "sys_notification" IS '站内信/通知记录表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
CREATE TABLE "sys_user" (
  "id" int8 NOT NULL DEFAULT nextval('sys_user_id_seq'::regclass),
  "username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(50) COLLATE "pg_catalog"."default",
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "avatar" varchar(255) COLLATE "pg_catalog"."default",
  "role" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'FAMILY'::character varying,
  "community_id" int8,
  "status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'NORMAL'::character varying,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "sys_user"."id" IS '用户ID';
COMMENT ON COLUMN "sys_user"."username" IS '登录账号 (唯一)';
COMMENT ON COLUMN "sys_user"."password" IS '加密后的密码';
COMMENT ON COLUMN "sys_user"."nickname" IS '用户昵称';
COMMENT ON COLUMN "sys_user"."phone" IS '注册手机号';
COMMENT ON COLUMN "sys_user"."avatar" IS '用户头像URL';
COMMENT ON COLUMN "sys_user"."role" IS '角色: FAMILY(家属), ADMIN(超管), FACTORY(工厂), COMMUNITY(社工), VOLUNTEER(志愿者)';
COMMENT ON COLUMN "sys_user"."community_id" IS '所属社区ID (志愿者和社工必填)';
COMMENT ON COLUMN "sys_user"."status" IS '账号状态: NORMAL(正常), BANNED(封禁), PENDING(待审核)';
COMMENT ON COLUMN "sys_user"."created_at" IS '注册时间';
COMMENT ON TABLE "sys_user" IS '系统用户表 (包含家属、管理员、志愿者等)';

-- ----------------------------
-- Table structure for sys_user_address
-- ----------------------------
CREATE TABLE "sys_user_address" (
  "id" int8 NOT NULL DEFAULT nextval('sys_user_address_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "receiver_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "receiver_phone" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "province" varchar(50) COLLATE "pg_catalog"."default",
  "city" varchar(50) COLLATE "pg_catalog"."default",
  "district" varchar(50) COLLATE "pg_catalog"."default",
  "detail_address" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "is_default" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6)
)
;
COMMENT ON COLUMN "sys_user_address"."user_id" IS '关联用户ID';
COMMENT ON COLUMN "sys_user_address"."receiver_name" IS '收货人姓名';
COMMENT ON COLUMN "sys_user_address"."receiver_phone" IS '收货人电话';
COMMENT ON COLUMN "sys_user_address"."province" IS '省';
COMMENT ON COLUMN "sys_user_address"."city" IS '市';
COMMENT ON COLUMN "sys_user_address"."district" IS '区/县';
COMMENT ON COLUMN "sys_user_address"."detail_address" IS '详细街道地址';
COMMENT ON COLUMN "sys_user_address"."is_default" IS '是否为默认地址';
COMMENT ON TABLE "sys_user_address" IS '用户收货地址簿';

-- ----------------------------
-- Table structure for sys_user_patient
-- ----------------------------
CREATE TABLE "sys_user_patient" (
  "id" int8 NOT NULL DEFAULT nextval('sys_user_patient_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "patient_id" int8 NOT NULL,
  "relation" varchar(50) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6)
)
;
COMMENT ON COLUMN "sys_user_patient"."user_id" IS '家属用户ID';
COMMENT ON COLUMN "sys_user_patient"."patient_id" IS '关联老人ID';
COMMENT ON COLUMN "sys_user_patient"."relation" IS '亲戚关系描述 (如: 二舅, 孙子)';
COMMENT ON TABLE "sys_user_patient" IS '家属关联表 (多对多关系)';

-- ----------------------------
-- Primary Key structure for table ai_session
-- ----------------------------
ALTER TABLE "ai_session" ADD CONSTRAINT "ai_session_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table clue_record
-- ----------------------------
ALTER TABLE "clue_record" ADD CONSTRAINT "clue_record_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table factory_order
-- ----------------------------
ALTER TABLE "factory_order" ADD CONSTRAINT "factory_order_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table patient_profile
-- ----------------------------
ALTER TABLE "patient_profile" ADD CONSTRAINT "patient_profile_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_community
-- ----------------------------
ALTER TABLE "sys_community" ADD CONSTRAINT "sys_community_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_log
-- ----------------------------
ALTER TABLE "sys_log" ADD CONSTRAINT "sys_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_notification
-- ----------------------------
ALTER TABLE "sys_notification" ADD CONSTRAINT "sys_notification_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user_address
-- ----------------------------
ALTER TABLE "sys_user_address" ADD CONSTRAINT "sys_user_address_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user_patient
-- ----------------------------
ALTER TABLE "sys_user_patient" ADD CONSTRAINT "sys_user_patient_pkey" PRIMARY KEY ("id");
