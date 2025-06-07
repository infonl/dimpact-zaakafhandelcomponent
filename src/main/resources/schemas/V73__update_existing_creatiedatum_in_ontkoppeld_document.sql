/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

UPDATE ${schema}.ontkoppeld_document
SET creatiedatum = creatiedatum + INTERVAL '1 day'
WHERE creatiedatum IS NOT NULL;
