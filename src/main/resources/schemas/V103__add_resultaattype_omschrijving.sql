/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- Resultaattypen get a new UUID in every zaaktype version, but keep their omschrijving. The UUID columns stay until
-- ResultaattypeOmschrijvingBackfill has filled these columns on every environment.
ALTER TABLE ${schema}.zaaktype_configuration ADD COLUMN niet_ontvankelijk_resultaattype_omschrijving VARCHAR;
ALTER TABLE ${schema}.zaaktype_completion_parameters ADD COLUMN resultaattype_omschrijving VARCHAR;
