/*
 * SPDX-FileCopyrightText: 2025 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Migration script to fix mail template sequence management
-- This ensures the sequence starts from the correct next value after existing records
-- to avoid ID conflicts when creating new mail templates

DO $$
DECLARE
    max_id BIGINT;
    record_count INTEGER;
    invalid_id_count INTEGER;
    next_sequence_value BIGINT;
BEGIN
    -- Validate that all existing records have valid IDs (not null and > 0)
    SELECT COUNT(*) INTO invalid_id_count 
    FROM ${schema}.mail_template 
    WHERE id_mail_template IS NULL OR id_mail_template <= 0;
    
    IF invalid_id_count > 0 THEN
        RAISE EXCEPTION 'Found % mail template records with invalid IDs (NULL or <= 0). Migration cannot proceed.', invalid_id_count;
    END IF;
    
    -- Get the count of existing records for validation
    SELECT COUNT(*) INTO record_count FROM ${schema}.mail_template;
    
    -- Get the maximum existing ID
    SELECT COALESCE(MAX(id_mail_template), 0) INTO max_id FROM ${schema}.mail_template;
    
    -- Calculate the next sequence value (max_id + 1)
    next_sequence_value := max_id + 1;
    
    -- Log the current state for debugging
    RAISE NOTICE 'Mail template migration: Found % existing records, max ID: %, setting sequence to: %', 
        record_count, max_id, next_sequence_value;
    
    -- Set the sequence to start from the next available ID
    -- The 'false' parameter means the next nextval() will return next_sequence_value
    PERFORM setval('${schema}.sq_mail_template', next_sequence_value, false);
    
    -- Verify the sequence was set correctly by checking the last_value
    -- We can't use currval() here because it requires nextval() to be called first in this session
    IF (SELECT last_value FROM ${schema}.sq_mail_template) != next_sequence_value THEN
        RAISE EXCEPTION 'Failed to set sequence value correctly. Expected: %, Actual: %', 
            next_sequence_value, (SELECT last_value FROM ${schema}.sq_mail_template);
    END IF;
    
    -- Final validation: ensure no duplicate IDs exist
    SELECT COUNT(*) - COUNT(DISTINCT id_mail_template) INTO invalid_id_count 
    FROM ${schema}.mail_template;
    
    IF invalid_id_count > 0 THEN
        RAISE EXCEPTION 'Found duplicate IDs in mail_template table. Migration cannot proceed.';
    END IF;
    
    RAISE NOTICE 'Mail template sequence migration completed successfully. Next ID will be: %', next_sequence_value;
    
END $$;