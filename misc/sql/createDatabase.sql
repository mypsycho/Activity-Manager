-- Suppression et cr�ation de la base
create database taskmgr_db;

-- Suppression et cr�ation de l'utilisateur
grant all privileges on taskmgr_db.* to taskmgr@'%'
identified by 'taskmgr';
flush privileges;
