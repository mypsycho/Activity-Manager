alter table Task drop TSK_CLOSED;
alter table Task add TSK_CLOSED integer(1) not null; -- Set 0 to existing values
