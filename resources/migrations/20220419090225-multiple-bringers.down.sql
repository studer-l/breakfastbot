ALTER TABLE bringer ADD CONSTRAINT day UNIQUE(day);
--;;
ALTER TABLE bringer DROP CONSTRAINT bringer_day_fkey;
--;;
ALTER TABLE bringer ADD FOREIGN KEY (day, id) REFERENCES attendances (day, id);
