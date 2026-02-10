-- ============================================
-- Add password column and set default passwords
-- for existing seed users (development only)
-- ============================================

-- Add password column (nullable initially to avoid breaking existing rows)
ALTER TABLE user_service.users ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Set default password for all existing seed users
-- Password: BusConnect2026!  (BCrypt $2a$ hash, compatible with Spring Security)
UPDATE user_service.users
SET password = '$2a$10$h3mngrXmn7cr9dN8g5YWDeoXZe1noGQ2I/Rf33xJtA.39K4SuWXCC'
WHERE password IS NULL;

-- Make password NOT NULL for future inserts
ALTER TABLE user_service.users ALTER COLUMN password SET NOT NULL;
