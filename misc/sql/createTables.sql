-- SEE /org.activitymgr.core.dao/src/org/activitymgr/core/impl/dao/mysqldb.sql

-- ------------------------------------------------------------
-- Collaborateurs
-- ------------------------------------------------------------
create table COLLABORATOR (
	CLB_ID         integer(  3) not null auto_increment,
	CLB_LOGIN      varchar(255) unique not null,
	CLB_FIRST_NAME varchar( 50) not null,
	CLB_LAST_NAME  varchar( 50) not null,
 	CLB_IS_ACTIVE  integer(  1) not null,
    index CLB_LOGIN_IDX (CLB_LOGIN),
    constraint CLB_PK primary key (CLB_ID) 
);

-- ------------------------------------------------------------
-- Taches
-- ------------------------------------------------------------
create table TASK (
	TSK_ID           integer(   8) not null auto_increment,
	TSK_PATH         varchar( 255) not null,
	TSK_NUMBER       varchar(   2) not null,
	TSK_CODE         varchar(  20) not null,
	TSK_NAME         varchar( 150) not null,
	TSK_BUDGET       integer(   8) not null,
	TSK_INITIAL_CONS integer(   8) not null,
	TSK_TODO         integer(   8) not null,
	TSK_COMMENT      text,
    index TSK_PATH_IDX (TSK_PATH),
    index TSK_FULLPATH_IDX (TSK_PATH, TSK_NUMBER),
    index TSK_PATH_CODE_IDX (TSK_PATH, TSK_CODE),
    constraint TSK_PK primary key (TSK_ID),
    constraint TSK_UNIQUE_FULLPATH 
    	unique (TSK_PATH, TSK_NUMBER),
    constraint TSK_UNIQUE_PATH_CODE
    	unique (TSK_PATH, TSK_CODE) 
);

-- ------------------------------------------------------------
-- Dur�es
-- ------------------------------------------------------------
create table DURATION (
	DUR_ID         integer(3) not null,
	DUR_IS_ACTIVE  integer(1) not null,
    constraint DUR_PK primary key (DUR_ID)
);

-- ------------------------------------------------------------
-- Taches
-- ------------------------------------------------------------
create table CONTRIBUTION (
	CTB_YEAR          integer(4) not null,
	CTB_MONTH         integer(2) not null,
	CTB_DAY           integer(2) not null,
	CTB_CONTRIBUTOR   integer(3) not null,
	CTB_TASK          integer(8) not null,
	CTB_DURATION      integer(3) not null,
    index CTB_CONTRIBUTOR_IDX (CTB_CONTRIBUTOR),
    index CTB_TASK_IDX (CTB_TASK),
    index CTB_DURATION_IDX (CTB_DURATION),
    constraint CTB_PK primary key (CTB_YEAR, CTB_MONTH, CTB_DAY, CTB_CONTRIBUTOR, CTB_TASK),
    constraint CTB_CONTRIBUTOR_FK foreign key (CTB_CONTRIBUTOR) references COLLABORATOR (CLB_ID),
    constraint CTB_TASK_FK foreign key (CTB_TASK) references TASK (TSK_ID),
    constraint CTB_DURATION_FK foreign key (CTB_DURATION) references DURATION (DUR_ID)
);

--------------------------------------------------------------
-- Report configurations
--------------------------------------------------------------
create table REPORT_CONFIG (
	REP_ID            integer(   3) not null auto_increment,
	REP_CATEGORY      varchar(  50) not null,
	REP_OWNER         integer(   3),
	REP_NAME          varchar( 100) not null,
	REP_CONFIGURATION varchar(1024),
    index REP_OWNER_IDX (REP_OWNER),
    constraint REP_PK primary key (REP_ID),
    constraint REP_CONTRIBUTOR_FK foreign key (REP_OWNER) references COLLABORATOR (CLB_ID)
);

--------------------------------------------------------------
-- View
--------------------------------------------------------------
create view CONTRIBUTION_VIEW as
select 
	ctb_year, ctb_month, ctb_day, 
	clb_login, clb_first_name, clb_last_name, 
	tsk_path, tsk_code, tsk_name,
	ctb_duration
from collaborator, contribution, task
where 
	ctb_contributor=clb_id
	and ctb_task=tsk_id
order by
	ctb_year, ctb_month, ctb_day, clb_id, tsk_path;

-- See: org.activitymgr.ui.rcp.DatabaseUI.reinstallDatabase()
insert into DURATION (DUR_ID, DUR_IS_ACTIVE) values ( 25, 1);
insert into DURATION (DUR_ID, DUR_IS_ACTIVE) values ( 50, 1);
insert into DURATION (DUR_ID, DUR_IS_ACTIVE) values ( 75, 1);
insert into DURATION (DUR_ID, DUR_IS_ACTIVE) values (100, 1);

-- delete from Duration where dur_id = 25
