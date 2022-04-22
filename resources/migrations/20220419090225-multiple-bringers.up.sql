ALTER TABLE bringer DROP CONSTRAINT IF EXISTS day_key;
--;;
ALTER TABLE bringer DROP CONSTRAINT IF EXISTS day;
--;;
ALTER TABLE bringer DROP CONSTRAINT bringer_day_fkey;
--;;
ALTER TABLE bringer DROP CONSTRAINT IF EXISTS bringer_day_key;
--;;
ALTER TABLE bringer ADD FOREIGN KEY (day, id) REFERENCES attendances (day, id) ON DELETE CASCADE;
