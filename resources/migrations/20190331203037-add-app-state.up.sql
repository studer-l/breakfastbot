insert into app_state values (0, CURRENT_DATE) ON CONFLICT(dummy) DO UPDATE SET attendance_primed_until = CURRENT_DATE;
