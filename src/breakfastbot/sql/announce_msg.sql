-- Matches breakfast dates to message id where it was announced

-- :name create-announce-msg-table :!
create table announce_msg (
  day date primary key,
  id integer unique
);

-- :name drop-announce-msg-table :!
drop table if exists announce_msg;

-- :name insert-announce-msg-id :! :1
insert into announce_msg values (:day, :id);

-- :name get-announce-msg-id :? :1
select id from announce_msg where day = :day;
