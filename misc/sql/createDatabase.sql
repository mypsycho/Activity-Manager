-- Creation de la base
create database taskmgr_db;

-- Creation de l'utilisateur
grant all privileges on taskmgr_db.* to taskmgr@'%'
identified by 'taskmgr';
flush privileges;
