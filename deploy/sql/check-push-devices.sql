SELECT device_id,
       enabled,
       left(coalesce(last_error, ''), 50) AS err,
       prayer_prefs->'aksam'->'atTime'->>'enabled' AS aksam_at,
       prayer_prefs->'ogle'->'atTime'->>'enabled' AS ogle_at,
       updated_at
FROM device_push_tokens
ORDER BY updated_at DESC
LIMIT 5;
