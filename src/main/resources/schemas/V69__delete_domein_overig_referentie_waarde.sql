/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Delete the obsolete 'domein_overig' row from the 'referentie_waarde' table because it is not used
-- by ZAC and therefore is not needed. Only do this for clean environments on which no database migrations have
-- been run and not for existing ones because then there is a risk that this value is already in use.
DO
$$
BEGIN
    IF (
        SELECT COUNT(*) = (SELECT MAX(installed_rank) FROM flyway_schema_history)
        FROM flyway_schema_history
    ) THEN
        -- Clean environment detected: safe to delete the obsolete 'domein_overig' row.
        DELETE FROM ${schema}.referentie_waarde
        WHERE id_referentie_tabel = (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'DOMEIN')
        AND naam = 'domein_overig';
    ELSE
        -- Not a clean environment, just change the 'domein_overig' row to a non-system value.
        UPDATE ${schema}.referentie_waarde
        SET is_systeem_waarde = FALSE
        WHERE id_referentie_tabel = (SELECT id_referentie_tabel FROM ${schema}.referentie_tabel WHERE code = 'DOMEIN')
        AND naam = 'domein_overig';
    END IF;
END;
$$;
