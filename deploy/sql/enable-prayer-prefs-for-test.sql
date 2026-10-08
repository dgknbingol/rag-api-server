-- Ezan testi: aktif cihazda vaktinde bildirimleri ac
UPDATE device_push_tokens
SET last_error = NULL,
    enabled = true,
    prayer_prefs = jsonb_set(
      jsonb_set(
        jsonb_set(
          jsonb_set(
            jsonb_set(
              jsonb_set(prayer_prefs, '{imsak,atTime,enabled}', 'true'),
              '{ogle,atTime,enabled}', 'true'
            ),
            '{ikindi,atTime,enabled}', 'true'
          ),
          '{aksam,atTime,enabled}', 'true'
        ),
        '{yatsi,atTime,enabled}', 'true'
      ),
      '{gunes,atTime,enabled}', 'false'
    )
WHERE device_id = '80ca2c41-c6b9-4cc4-9de8-6dae02a66d7d';

SELECT device_id,
       prayer_prefs->'aksam'->'atTime'->>'enabled' AS aksam_at,
       prayer_prefs->'yatsi'->'atTime'->>'enabled' AS yatsi_at
FROM device_push_tokens
WHERE device_id = '80ca2c41-c6b9-4cc4-9de8-6dae02a66d7d';
