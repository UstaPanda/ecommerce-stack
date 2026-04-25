-- Fix the admin password hash if it was seeded with the incorrect value
UPDATE users 
SET password_hash = '$2a$10$EblZqNptyYvcLm/VwDC9uu6HNH9BKAD8/5OdS5nS6.82T9L8A.E6.'
WHERE email = 'admin@example.com' 
AND password_hash = '$2a$10$8.UnVuG9HHgffUDAlk8qn.R.p4Z1r1qP9R2X8Z2/M48/28/M48/28';
