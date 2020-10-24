-- :name create-attendances-table :!
create table attendances (
  day date,
  id integer references members(id),
  primary key (day, id)
);

-- :name drop-attendances-table :!
drop table if exists attendances;

-- :name insert-attendance-by-id :! :n
-- CAREFUL this may re-add people that already signed off
insert into attendances (day, id) values (:day, :id) on conflict do nothing;

-- :name insert-attendance-by-email :! :n
-- CAREFUL this may re-add people that already signed off
insert into attendances (day, id)
values (:day, (select id
                 from members
                where email = :email))
on conflict do nothing;

-- :name get-all-attendees :? :*
select members.fullname, members.email
  from members, attendances
 where attendances.day = :day
   and attendances.id = members.id;

-- :name get-all-events :? :*
select distinct day from attendances;

-- :name get-person-with-latest-bring-date-on :? :1
-- This determines who should bring the next breakfast:
-- from all attendees, it should be the one that brought it the least recent. 
select email, fullname
  from members, attendances
 where day = :date
   and members.id = attendances.id
 order by day desc
 limit 1;

-- :name remove-attendance-by-email-at :! :n
delete from attendances
 where id = (select id
               from members
              where email = :email)
       and day = :day;

-- :name remove-attendances-from :! :n
delete from attendances
 where id = (select id from members where email = :email)
       and day > :date

-- :name any-attendance-on-date :? :1
select exists (select 1 from attendances where day = :day);

-- :name get-attendance-counts-since-bringing :?
-- For a given date, gives the number of events attended by each attendee since
-- they last brought breakfast themselves.
select id,
       (count(*)-1) as count
  from attendances
 where day > (select coalesce((
   /* get either the latest date this `id` brought breakfast */
   select day from bringer
    where bringer.id = attendances.id
    order by day desc
    limit 1),
    /* or from the start */
    (select cast( 'epoch' as date))))
   and
       day <= :day
   and
       id in (select id from attendances where day =  :day)
 group by id;


-- :name cancel-event :! :n
-- removes all attendances from event on given day
delete from attendances where day = :day;

