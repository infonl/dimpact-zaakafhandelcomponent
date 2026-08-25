/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- SQL script that updates a zaaktype in the Open Zaak database

UPDATE catalogi_zaaktype
SET datum_einde_geldigheid = datum_begin_geldigheid,
    zaaktype_omschrijving = 'Load test zaaktype [[ZAAKTYPE_NUMBER]] (Superseded version [[VERSION_NUMBER]])',
    zaaktype_omschrijving_generiek = 'Load test zaaktype [[ZAAKTYPE_NUMBER]] (Superseded version [[VERSION_NUMBER]])'
WHERE uuid = '[[ZAAKTYPE_UUID]]';
