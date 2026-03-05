-- 0) 可选：用 enum 约束 stage/status（比 varchar 更安全）
do $$ begin
  create type task_stage as enum ('STRUCTURE','UNDERSTANDING','TRAINING','REFLECTION');
exception when duplicate_object then null;
end $$;

do $$ begin
  create type run_status as enum ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED');
exception when duplicate_object then null;
end $$;


-- 1) 会话
create table if not exists learning_session (
  id bigserial primary key,
  user_id varchar(64) not null,
  course_id varchar(64) not null,
  chapter_id varchar(64) not null,

  goal_text text,

  current_node_id bigint,
  current_stage task_stage,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  -- 同一用户同一章节同时只允许一个“进行中会话”（MVP 常见诉求）
  -- 如果你允许多个并行会话，可删掉这个约束
  unique (user_id, chapter_id)
);

-- current_node_id 需要引用 concept_node，所以要先建 concept_node，再补 FK
create index if not exists idx_session_user on learning_session(user_id);
create index if not exists idx_session_course_chapter on learning_session(course_id, chapter_id);


-- 2) 概念节点
create table if not exists concept_node (
  id bigserial primary key,
  chapter_id varchar(64) not null,
  name varchar(128) not null,
  outline text,

  -- 先修依赖建议用关系表替代 jsonb（可查询、可约束、可排序）
  order_no int not null default 0,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  -- 同一章节 concept 名称不重复（避免 UI 上出现两个“栈”）
  unique (chapter_id, name)
);

create index if not exists idx_concept_node_chapter_order on concept_node(chapter_id, order_no);


-- 2.1) 节点先修依赖（替代 prerequisite_ids jsonb）
create table if not exists concept_prerequisite (
  node_id bigint not null references concept_node(id) on delete cascade,
  prereq_node_id bigint not null references concept_node(id) on delete restrict,
  primary key (node_id, prereq_node_id),
  check (node_id <> prereq_node_id)
);

create index if not exists idx_prereq_prereq on concept_prerequisite(prereq_node_id);


-- 补 session.current_node_id 外键（必须在 concept_node 后）
alter table learning_session
  add constraint fk_session_current_node
  foreign key (current_node_id) references concept_node(id)
  deferrable initially deferred;


-- 3) 任务（任务定义/队列项）
create table if not exists task (
  id bigserial primary key,
  session_id bigint not null references learning_session(id) on delete cascade,
  stage task_stage not null,
  node_id bigint not null references concept_node(id) on delete restrict,

  objective text not null,
  input_json jsonb,

  -- 任务定义层面可维护“期望产出结构”等（可选）
  expected_output_schema jsonb,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 常用查询：session 内按 stage 拉任务
create index if not exists idx_task_session_stage_created on task(session_id, stage, created_at desc);
create index if not exists idx_task_node on task(node_id);


-- 4) 任务运行（一次 attempt = 你原来的 status/output/score/evidence）
create table if not exists task_attempt (
  id bigserial primary key,
  task_id bigint not null references task(id) on delete cascade,

  status run_status not null default 'PENDING',

  -- 运行参数（可覆盖 task.input_json），输出产物
  run_input_json jsonb,
  output_json jsonb,

  score int,
  error_tags jsonb not null default '[]'::jsonb,
  feedback_json jsonb,

  started_at timestamptz,
  finished_at timestamptz,

  created_at timestamptz not null default now()
);

-- 常用：查最新一次 attempt
create index if not exists idx_attempt_task_created on task_attempt(task_id, created_at desc);
-- 常用：拉取待运行队列
create index if not exists idx_attempt_status_created on task_attempt(status, created_at);


-- 5) 掌握度
create table if not exists mastery (
  user_id varchar(64) not null,
  node_id bigint not null references concept_node(id) on delete cascade,

  -- numeric(4,3) OK；如果追求性能/空间可改 smallint 0~1000
  mastery_value numeric(4,3) not null default 0.000,

  updated_at timestamptz not null default now(),
  primary key (user_id, node_id)
);

create index if not exists idx_mastery_user_updated on mastery(user_id, updated_at desc);
create index if not exists idx_mastery_node on mastery(node_id);


-- 6) 可选：GIN 索引（如果你会按 error_tags/feedback_json 里字段检索）
-- create index idx_attempt_error_tags_gin on task_attempt using gin (error_tags);
-- create index idx_task_input_gin on task using gin (input_json);