/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

ALTER TABLE ${schema}.betrokkene_koppelingen
ALTER COLUMN brpKoppelen SET DEFAULT TRUE;

ALTER TABLE ${schema}.betrokkene_koppelingen
ALTER COLUMN kvkKoppelen SET DEFAULT TRUE;
