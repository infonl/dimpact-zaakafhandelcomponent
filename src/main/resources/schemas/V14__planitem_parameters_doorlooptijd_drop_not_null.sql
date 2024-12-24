/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
ALTER TABLE
    ${schema}.humantask_parameters ALTER COLUMN doorlooptijd DROP
        NOT NULL;