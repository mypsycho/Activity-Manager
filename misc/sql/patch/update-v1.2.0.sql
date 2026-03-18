-- alter table Task drop TSK_CLOSED; -- [IF EXISTS] is not a thing in MySQL
alter table TASK add TSK_CLOSED integer(1) not null; -- Set 0 to existing values
