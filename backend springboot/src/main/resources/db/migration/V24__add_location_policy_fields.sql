DELETE ap
FROM attendance_policies ap
         LEFT JOIN class_groups g
                   ON g.id = ap.group_id
WHERE g.id IS NULL;

ALTER TABLE attendance_policies
    ADD COLUMN require_location TINYINT NOT NULL DEFAULT 0 AFTER critical_absent_count,
    ADD COLUMN location_lat DECIMAL(10,7) NULL AFTER require_location,
    ADD COLUMN location_lng DECIMAL(10,7) NULL AFTER location_lat,
    ADD COLUMN allowed_radius_meter INT UNSIGNED NOT NULL DEFAULT 150 AFTER location_lng,
    ADD CONSTRAINT chk_ap_require_location
        CHECK (require_location IN (0, 1)),
    ADD CONSTRAINT chk_ap_location_lat
        CHECK (location_lat IS NULL OR (location_lat >= -90.0000000 AND location_lat <= 90.0000000)),
    ADD CONSTRAINT chk_ap_location_lng
        CHECK (location_lng IS NULL OR (location_lng >= -180.0000000 AND location_lng <= 180.0000000)),
    ADD CONSTRAINT chk_ap_location_pair
        CHECK (
            (location_lat IS NULL AND location_lng IS NULL)
                OR
            (location_lat IS NOT NULL AND location_lng IS NOT NULL)
            ),
    ADD CONSTRAINT chk_ap_location_required_target
        CHECK (
            require_location = 0
                OR
            (location_lat IS NOT NULL AND location_lng IS NOT NULL)
            ),
    ADD CONSTRAINT chk_ap_allowed_radius
        CHECK (allowed_radius_meter BETWEEN 10 AND 10000);