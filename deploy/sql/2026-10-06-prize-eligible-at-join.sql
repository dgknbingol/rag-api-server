-- quiz ödül uygunluğu: entity alanları DB'de yoksa 42703 verir.
-- Postgres'te bir kez çalıştır:

ALTER TABLE quiz_attempts
    ADD COLUMN IF NOT EXISTS prize_eligible_at_join boolean NOT NULL DEFAULT true;

ALTER TABLE quiz_participations
    ADD COLUMN IF NOT EXISTS prize_eligible_at_join boolean NOT NULL DEFAULT true;
