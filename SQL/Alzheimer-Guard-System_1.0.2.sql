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

 Date: 24/12/2025 23:48:19
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
  "user_ip" varchar(50) COLLATE "pg_catalog"."default",
  "session_id" varchar(64) COLLATE "pg_catalog"."default",
  "messages" jsonb,
  "function_calls" text COLLATE "pg_catalog"."default",
  "request_tokens" int4,
  "response_tokens" int4
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
COMMENT ON COLUMN "ai_session"."session_id" IS '会话ID（用于多轮对话关联）';
COMMENT ON COLUMN "ai_session"."messages" IS '完整消息历史（JSON数组，包含用户和AI的所有对话）';
COMMENT ON COLUMN "ai_session"."function_calls" IS '本次调用的工具列表（用于审计，如：query_elder_info;update_order_status）';
COMMENT ON COLUMN "ai_session"."request_tokens" IS '请求消耗Token数';
COMMENT ON COLUMN "ai_session"."response_tokens" IS '响应消耗Token数';
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
  "id_number" varchar(20) COLLATE "pg_catalog"."default",
  "fence_radius" int4 DEFAULT 500,
  "fence_enabled" bool DEFAULT true
)
;
COMMENT ON COLUMN "patient_profile"."guardian_id" IS '法定监护人ID (拥有最高修改权限)';
COMMENT ON COLUMN "patient_profile"."community_id" IS '所属社区ID (由该社区负责管辖)';
COMMENT ON COLUMN "patient_profile"."name" IS '老人姓名';
COMMENT ON COLUMN "patient_profile"."age" IS '老人年龄';
COMMENT ON COLUMN "patient_profile"."short_code" IS '防走失短码 (印刷在设备上的唯一码)';
COMMENT ON COLUMN "patient_profile"."contact_phone" IS '紧急联系电话 (通常印在衣服上)';
COMMENT ON COLUMN "patient_profile"."medical_history" IS '既往病史 (供AI分析用)';
COMMENT ON COLUMN "patient_profile"."lost_status" IS '走失状态：NORMAL(正常), REPORTED(已上报走失), SEARCHING(社区搜索中), FOUND(已找到)';
COMMENT ON COLUMN "patient_profile"."last_location" IS '最后一次出现的GIS坐标';
COMMENT ON COLUMN "patient_profile"."last_seen_time" IS '最后位置更新时间';
COMMENT ON COLUMN "patient_profile"."id_number" IS '身份证号';
COMMENT ON COLUMN "patient_profile"."fence_radius" IS '电子围栏半径（米），以last_location为中心';
COMMENT ON COLUMN "patient_profile"."fence_enabled" IS '是否启用围栏告警';
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
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "push_status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'PENDING'::character varying,
  "push_time" timestamp(6),
  "extras" jsonb,
  "related_id" int8,
  "related_type" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "sys_notification"."type" IS '通知类型 (SMS, APP)';
COMMENT ON COLUMN "sys_notification"."title" IS '通知标题';
COMMENT ON COLUMN "sys_notification"."content" IS '通知内容';
COMMENT ON COLUMN "sys_notification"."is_read" IS '是否已读';
COMMENT ON COLUMN "sys_notification"."push_status" IS '推送状态：PENDING(待推送), SENT(已推送), FAILED(推送失败)';
COMMENT ON COLUMN "sys_notification"."push_time" IS '实际推送时间';
COMMENT ON COLUMN "sys_notification"."extras" IS '扩展数据（如taskId, patientId等，用于App端跳转）';
COMMENT ON COLUMN "sys_notification"."related_id" IS '关联业务ID';
COMMENT ON COLUMN "sys_notification"."related_type" IS '关联业务类型：TASK(工单), ALARM(告警), ORDER(订单), CLUE(线索)';
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
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6),
  "mail" varchar(255) COLLATE "pg_catalog"."default",
  "current_location" geometry,
  "registration_id" varchar(255) COLLATE "pg_catalog"."default",
  "client_type" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'WEB'::character varying,
  "last_location_time" timestamp(6)
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
COMMENT ON COLUMN "sys_user"."updated_at" IS '更新时间';
COMMENT ON COLUMN "sys_user"."mail" IS '用户邮箱';
COMMENT ON COLUMN "sys_user"."current_location" IS '当前位置（志愿者实时位置，用于5km范围匹配）';
COMMENT ON COLUMN "sys_user"."registration_id" IS '极光推送设备ID（Android端）';
COMMENT ON COLUMN "sys_user"."client_type" IS '客户端类型：WEB(网页端), ANDROID(安卓App)';
COMMENT ON COLUMN "sys_user"."last_location_time" IS '最后位置更新时间';
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
-- Table structure for volunteer_task
-- ----------------------------
CREATE TABLE "volunteer_task" (
  "id" int8 NOT NULL DEFAULT nextval('volunteer_task_id_seq'::regclass),
  "patient_id" int8 NOT NULL,
  "volunteer_id" int8,
  "community_id" int8 NOT NULL,
  "task_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'BROADCAST'::character varying,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'PENDING'::character varying,
  "title" varchar(100) COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default",
  "patient_location" geometry,
  "assigned_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "accepted_at" timestamp(6),
  "started_at" timestamp(6),
  "completed_at" timestamp(6),
  "cancelled_at" timestamp(6),
  "remark" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "volunteer_task"."patient_id" IS '走失老人ID';
COMMENT ON COLUMN "volunteer_task"."volunteer_id" IS '接单志愿者ID（派发时为空，接单后填充）';
COMMENT ON COLUMN "volunteer_task"."community_id" IS '所属社区ID';
COMMENT ON COLUMN "volunteer_task"."task_type" IS '工单类型：ASSIGNED(社区指派), BROADCAST(5km广播)';
COMMENT ON COLUMN "volunteer_task"."status" IS '状态：PENDING(待接单), ACCEPTED(已接单), IN_PROGRESS(进行中), COMPLETED(已完成), CANCELLED(已取消)';
COMMENT ON COLUMN "volunteer_task"."patient_location" IS '老人走失位置（快照）';
COMMENT ON COLUMN "volunteer_task"."assigned_at" IS '派发时间';
COMMENT ON COLUMN "volunteer_task"."accepted_at" IS '接单时间';
COMMENT ON COLUMN "volunteer_task"."started_at" IS '开始执行时间';
COMMENT ON COLUMN "volunteer_task"."completed_at" IS '完成时间';
COMMENT ON COLUMN "volunteer_task"."cancelled_at" IS '取消时间';
COMMENT ON COLUMN "volunteer_task"."remark" IS '备注（完成时填写）';
COMMENT ON TABLE "volunteer_task" IS '志愿者工单表';

-- ----------------------------
-- Indexes structure for table ai_session
-- ----------------------------
CREATE INDEX "idx_ai_session_created_at" ON "ai_session" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_ai_session_sid" ON "ai_session" USING btree (
  "session_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE session_id IS NOT NULL;
CREATE INDEX "idx_ai_session_user" ON "ai_session" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table ai_session
-- ----------------------------
ALTER TABLE "ai_session" ADD CONSTRAINT "ai_session_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table clue_record
-- ----------------------------
CREATE INDEX "idx_clue_created_at" ON "clue_record" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_clue_loc" ON "clue_record" USING gist (
  "location" "public"."gist_geometry_ops_2d"
);
CREATE INDEX "idx_clue_patient" ON "clue_record" USING btree (
  "patient_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table clue_record
-- ----------------------------
ALTER TABLE "clue_record" ADD CONSTRAINT "clue_record_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table factory_order
-- ----------------------------
CREATE INDEX "idx_order_created_at" ON "factory_order" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_order_status" ON "factory_order" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_order_user" ON "factory_order" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table factory_order
-- ----------------------------
ALTER TABLE "factory_order" ADD CONSTRAINT "factory_order_order_no_key" UNIQUE ("order_no");

-- ----------------------------
-- Primary Key structure for table factory_order
-- ----------------------------
ALTER TABLE "factory_order" ADD CONSTRAINT "factory_order_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table patient_profile
-- ----------------------------
CREATE INDEX "idx_patient_community" ON "patient_profile" USING btree (
  "community_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE community_id IS NOT NULL;
CREATE INDEX "idx_patient_guardian" ON "patient_profile" USING btree (
  "guardian_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_patient_loc" ON "patient_profile" USING gist (
  "last_location" "public"."gist_geometry_ops_2d"
);
CREATE INDEX "idx_patient_lost_status" ON "patient_profile" USING btree (
  "lost_status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table patient_profile
-- ----------------------------
ALTER TABLE "patient_profile" ADD CONSTRAINT "patient_profile_short_code_key" UNIQUE ("short_code");
ALTER TABLE "patient_profile" ADD CONSTRAINT "patient_profile_id_number" UNIQUE ("id_number");

-- ----------------------------
-- Primary Key structure for table patient_profile
-- ----------------------------
ALTER TABLE "patient_profile" ADD CONSTRAINT "patient_profile_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_community
-- ----------------------------
CREATE INDEX "idx_community_status" ON "sys_community" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE status IS NOT NULL;

-- ----------------------------
-- Primary Key structure for table sys_community
-- ----------------------------
ALTER TABLE "sys_community" ADD CONSTRAINT "sys_community_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_log
-- ----------------------------
CREATE INDEX "idx_log_created_at" ON "sys_log" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_log_module" ON "sys_log" USING btree (
  "module" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_log_user" ON "sys_log" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE user_id IS NOT NULL;

-- ----------------------------
-- Primary Key structure for table sys_log
-- ----------------------------
ALTER TABLE "sys_log" ADD CONSTRAINT "sys_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_notification
-- ----------------------------
CREATE INDEX "idx_notification_created_at" ON "sys_notification" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_notification_push_status" ON "sys_notification" USING btree (
  "push_status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_notification_related" ON "sys_notification" USING btree (
  "related_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "related_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE related_id IS NOT NULL;
CREATE INDEX "idx_notification_user_read" ON "sys_notification" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_notification
-- ----------------------------
ALTER TABLE "sys_notification" ADD CONSTRAINT "sys_notification_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_user
-- ----------------------------
CREATE INDEX "idx_user_client_type" ON "sys_user" USING btree (
  "client_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_community" ON "sys_user" USING btree (
  "community_id" "pg_catalog"."int8_ops" ASC NULLS LAST
) WHERE community_id IS NOT NULL;
CREATE INDEX "idx_user_location_gist" ON "sys_user" USING gist (
  "current_location" "public"."gist_geometry_ops_2d"
);
CREATE INDEX "idx_user_phone" ON "sys_user" USING btree (
  "phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_role" ON "sys_user" USING btree (
  "role" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_status" ON "sys_user" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table sys_user
-- ----------------------------
ALTER TABLE "sys_user" ADD CONSTRAINT "sys_user_username_key" UNIQUE ("username");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_user_address
-- ----------------------------
CREATE INDEX "idx_address_default" ON "sys_user_address" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "is_default" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_addr" ON "sys_user_address" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_user_address
-- ----------------------------
ALTER TABLE "sys_user_address" ADD CONSTRAINT "sys_user_address_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_user_patient
-- ----------------------------
ALTER TABLE "sys_user_patient" ADD CONSTRAINT "sys_user_patient_user_id_patient_id_key" UNIQUE ("user_id", "patient_id");

-- ----------------------------
-- Primary Key structure for table sys_user_patient
-- ----------------------------
ALTER TABLE "sys_user_patient" ADD CONSTRAINT "sys_user_patient_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table volunteer_task
-- ----------------------------
CREATE INDEX "idx_task_patient_location_gist" ON "volunteer_task" USING gist (
  "patient_location" "public"."gist_geometry_ops_2d"
);
CREATE INDEX "idx_volunteer_task_assigned_at" ON "volunteer_task" USING btree (
  "assigned_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_volunteer_task_community" ON "volunteer_task" USING btree (
  "community_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_volunteer_task_patient" ON "volunteer_task" USING btree (
  "patient_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_volunteer_task_status" ON "volunteer_task" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_volunteer_task_type" ON "volunteer_task" USING btree (
  "task_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_volunteer_task_volunteer" ON "volunteer_task" USING btree (
  "volunteer_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table volunteer_task
-- ----------------------------
ALTER TABLE "volunteer_task" ADD CONSTRAINT "volunteer_task_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table clue_record
-- ----------------------------
ALTER TABLE "clue_record" ADD CONSTRAINT "clue_record_patient_id_fkey" FOREIGN KEY ("patient_id") REFERENCES "patient_profile" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table factory_order
-- ----------------------------
ALTER TABLE "factory_order" ADD CONSTRAINT "factory_order_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "sys_user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table patient_profile
-- ----------------------------
ALTER TABLE "patient_profile" ADD CONSTRAINT "patient_profile_community_id_fkey" FOREIGN KEY ("community_id") REFERENCES "sys_community" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "patient_profile" ADD CONSTRAINT "patient_profile_guardian_id_fkey" FOREIGN KEY ("guardian_id") REFERENCES "sys_user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_user
-- ----------------------------
ALTER TABLE "sys_user" ADD CONSTRAINT "sys_user_community_id_fkey" FOREIGN KEY ("community_id") REFERENCES "sys_community" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_user_address
-- ----------------------------
ALTER TABLE "sys_user_address" ADD CONSTRAINT "sys_user_address_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "sys_user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_user_patient
-- ----------------------------
ALTER TABLE "sys_user_patient" ADD CONSTRAINT "sys_user_patient_patient_id_fkey" FOREIGN KEY ("patient_id") REFERENCES "patient_profile" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "sys_user_patient" ADD CONSTRAINT "sys_user_patient_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "sys_user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
