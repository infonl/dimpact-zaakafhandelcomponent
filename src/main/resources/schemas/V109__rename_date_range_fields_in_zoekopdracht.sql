/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- The REST date range was renamed from { van, tot } to { from, through }. A saved search holds the list parameters of
-- the frontend as JSON text, so its date ranges are renamed too: every JSON object whose only keys are van and tot.
CREATE FUNCTION ${schema}.v109_rename_date_range_fields(value JSONB) RETURNS JSONB
    LANGUAGE plpgsql IMMUTABLE AS
$$
BEGIN
    IF jsonb_typeof(value) = 'object' THEN
        IF value <> '{}'::JSONB AND NOT EXISTS (
            SELECT 1 FROM jsonb_object_keys(value) AS field_name WHERE field_name NOT IN ('van', 'tot')
        ) THEN
            RETURN (
                SELECT jsonb_object_agg(CASE field_name WHEN 'van' THEN 'from' ELSE 'through' END, field_value)
                FROM jsonb_each(value) AS field(field_name, field_value)
            );
        END IF;
        RETURN COALESCE(
            (
                SELECT jsonb_object_agg(field_name, ${schema}.v109_rename_date_range_fields(field_value))
                FROM jsonb_each(value) AS field(field_name, field_value)
            ),
            '{}'::JSONB
        );
    ELSIF jsonb_typeof(value) = 'array' THEN
        RETURN COALESCE(
            (
                SELECT jsonb_agg(${schema}.v109_rename_date_range_fields(element) ORDER BY position)
                FROM jsonb_array_elements(value) WITH ORDINALITY AS item(element, position)
            ),
            '[]'::JSONB
        );
    END IF;
    RETURN value;
END;
$$;

DO $$
DECLARE
    saved_search RECORD;
    renamed_count BIGINT := 0;
BEGIN
    FOR saved_search IN
        SELECT id_zoekopdracht, json_zoekopdracht
        FROM ${schema}.zoekopdracht
        WHERE json_zoekopdracht LIKE '%"van"%' OR json_zoekopdracht LIKE '%"tot"%'
    LOOP
        BEGIN
            UPDATE ${schema}.zoekopdracht
            SET json_zoekopdracht = ${schema}.v109_rename_date_range_fields(saved_search.json_zoekopdracht::JSONB)::TEXT
            WHERE id_zoekopdracht = saved_search.id_zoekopdracht;
            renamed_count := renamed_count + 1;
        EXCEPTION
            WHEN invalid_text_representation THEN
                RAISE WARNING 'V109: zoekopdracht % does not hold valid JSON, left it unchanged',
                    saved_search.id_zoekopdracht;
        END;
    END LOOP;
    RAISE NOTICE 'V109: renamed the date ranges in % zoekopdracht(en) that mention van or tot', renamed_count;
END;
$$;

DROP FUNCTION ${schema}.v109_rename_date_range_fields(JSONB);
