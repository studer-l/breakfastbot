-- :name create-bringer-table :!
create table bringer (
  day date NOT NULL,
  id integer references members(id),
  primary key (day, id),
  foreign key (day, id) references attendances (day, id) ON DELETE CASCADE
);

-- :name drop-bringer-table :!
drop table if exists bringer;

-- :name set-bringer-by-email :! :1
insert into bringer (day, id)
values (:day, (select id from members where email = :email));

-- :name change-bringer-on :! :1
update bringer
   set id = (select id from members where email = :email)
 where day = :day;

-- :name set-bringer-on :! :1
insert into bringer values (:day, :id);

-- :name get-bringers-on :? :?
select email, fullname
  from members, bringer
 where bringer.day = :day and members.id = bringer.id;


-- :name have-bringer-for-day :? :1
select exists (select 1 from bringer where day = :day);

-- :name reset-bringer-for-day :! :1
delete from bringer where day = :day;

-- :name is-supposed-to-bring-on :? :1
SELECT true
  FROM members, bringer
 WHERE bringer.day = :day AND members.id = bringer.id
   AND members.email = :email;
