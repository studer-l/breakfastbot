-- Table containing some persistent state across restarts

-- :name create-app-state-table :!
create table app_state (
  dummy integer primary key,
  attendance_primed_until date not null
);

-- :name drop-app-state-table :!
drop table if exists app_state;

-- :name insert-initial-app-state :! :1
insert into app_state values (0, CURRENT_DATE);

-- :name set-last-primed :! :1
update app_state set attendance_primed_until = :date where dummy = 0;

-- :name get-last-primed :? :1
select attendance_primed_until from app_state where dummy = 0;
