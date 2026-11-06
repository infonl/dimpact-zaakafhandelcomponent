/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- un_signalering_instellingen treats the always NULL other owner column as distinct, so it allowed duplicates.
DO $$
DECLARE
    deleted_row_count BIGINT;
BEGIN
    DELETE FROM ${schema}.signalering_instellingen older
    USING ${schema}.signalering_instellingen newer
    WHERE older.signaleringtype_enum = newer.signaleringtype_enum
      AND older.id_groep IS NOT DISTINCT FROM newer.id_groep
      AND older.id_medewerker IS NOT DISTINCT FROM newer.id_medewerker
      AND older.id_signalering_instellingen < newer.id_signalering_instellingen;
    GET DIAGNOSTICS deleted_row_count = ROW_COUNT;
    IF deleted_row_count > 0 THEN
        RAISE WARNING 'V108: deleted % duplicate row(s) of signalering_instellingen, kept the newest', deleted_row_count;
    END IF;
END $$;

CREATE UNIQUE INDEX un_signalering_instellingen_medewerker
    ON ${schema}.signalering_instellingen (signaleringtype_enum, id_medewerker)
    WHERE id_medewerker IS NOT NULL;

CREATE UNIQUE INDEX un_signalering_instellingen_groep
    ON ${schema}.signalering_instellingen (signaleringtype_enum, id_groep)
    WHERE id_groep IS NOT NULL;
