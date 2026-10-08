-- Postgres oturum / varsayilan timezone: Europe/Istanbul
-- timestamptz degerleri mutlak an olarak saklanir; SELECT'te +03 gosterilir.

ALTER DATABASE aislam SET TIME ZONE 'Europe/Istanbul';
ALTER DATABASE aislam_test SET TIME ZONE 'Europe/Istanbul';
ALTER DATABASE aislam_prod SET TIME ZONE 'Europe/Istanbul';
ALTER DATABASE postgres SET TIME ZONE 'Europe/Istanbul';

ALTER ROLE aislam_dev SET TIME ZONE 'Europe/Istanbul';

SHOW TIME ZONE;
