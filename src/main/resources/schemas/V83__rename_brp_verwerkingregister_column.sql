/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- rename verwerkingsregisterwaarde to verwerkingregisterwaarde
ALTER TABLE ${schema}.zaaktype_cmmn_brp_parameters
    RENAME verwerkingsregisterwaarde TO verwerkingregisterwaarde;
