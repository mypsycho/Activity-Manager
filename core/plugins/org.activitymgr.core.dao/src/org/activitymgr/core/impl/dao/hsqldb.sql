drop table if exists REPORT_CONFIG;
drop table if exists CONTRIBUTION;
drop table if exists DURATION;
drop table if exists TASK;
drop table if exists COLLABORATOR;

-- ------------------------------------------------------------
-- Collaborateurs
-- ------------------------------------------------------------
create table COLLABORATOR (
	CLB_ID         integer generated by default as identity(start with 1) not null,
	CLB_LOGIN      varchar(255) not null,
	CLB_FIRST_NAME varchar( 50) not null,
	CLB_LAST_NAME  varchar( 50) not null,
	CLB_IS_ACTIVE  boolean not null,
    constraint CLB_PK primary key (CLB_ID) 
);
create unique index CLB_LOGIN_IDX on COLLABORATOR (CLB_LOGIN);

-- ------------------------------------------------------------
-- Taches
-- ------------------------------------------------------------
create table TASK (
	TSK_ID           integer     generated by default as identity(start with 1) not null,
	TSK_PATH         varchar(255) not null,
	TSK_NUMBER       varchar(  2) not null,
	TSK_CODE         varchar( 20) not null,
	TSK_NAME         varchar(150) not null,
	TSK_BUDGET       integer      not null,
	TSK_INITIAL_CONS integer      not null,
	TSK_TODO         integer      not null,
	TSK_COMMENT      varchar(255),
	TSK_CLOSED       boolean      not null,
    constraint TSK_PK primary key (TSK_ID)
);
create index TSK_PATH_IDX on TASK (TSK_PATH);
create unique index TSK_PATH_NUMBER_IDX on TASK (TSK_PATH, TSK_NUMBER);
create unique index TSK_PATH_CODE_IDX on TASK (TSK_PATH, TSK_CODE);

-- ------------------------------------------------------------
-- Durees
-- ------------------------------------------------------------
create table DURATION (
	DUR_ID         integer not null,
	DUR_IS_ACTIVE  boolean not null,
    constraint DUR_PK primary key (DUR_ID)
);

-- ------------------------------------------------------------
-- Contributions
-- ------------------------------------------------------------
create table CONTRIBUTION (
	CTB_YEAR          integer not null,
	CTB_MONTH         integer not null,
	CTB_DAY           integer not null,
	CTB_CONTRIBUTOR   integer not null,
	CTB_TASK          integer not null,
	CTB_DURATION      integer not null,
    constraint CTB_PK primary key (CTB_YEAR, CTB_MONTH, CTB_DAY, CTB_CONTRIBUTOR, CTB_TASK),
    constraint CTB_CONTRIBUTOR_FK foreign key (CTB_CONTRIBUTOR) references COLLABORATOR (CLB_ID),
    constraint CTB_TASK_FK foreign key (CTB_TASK) references TASK (TSK_ID),
    constraint CTB_DURATION_FK foreign key (CTB_DURATION) references DURATION (DUR_ID)
);
create index CTB_CONTRIBUTOR_IDX on CONTRIBUTION (CTB_CONTRIBUTOR);
create index CTB_TASK_IDX on CONTRIBUTION (CTB_TASK);
create index CTB_DURATION_IDX on CONTRIBUTION (CTB_DURATION);

-- ------------------------------------------------------------
-- Report configurations
-- ------------------------------------------------------------
create table REPORT_CONFIG (
	REP_ID            integer generated by default as identity(start with 1) not null,
	REP_CATEGORY      varchar(  50) not null,
	REP_OWNER         integer,
	REP_NAME          varchar( 100) not null,
	REP_CONFIGURATION varchar(1024),
    constraint REP_PK primary key (REP_ID),
    constraint REP_OWNER_FK foreign key (REP_OWNER) references COLLABORATOR (CLB_ID)
);
create index REP_OWNER_IDX on REPORT_CONFIG (REP_OWNER);
