/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

-- SQL script that adds a zaaktype in the PABC database

INSERT INTO entity_type (
    id,
    entity_type_id,
    "type",
    "name",
    uri
) VALUES (
    '[[ENTITY_TYPE_UUID]]',
    'Load test zaaktype [[ZAAKTYPE_NUMBER]]',
    'ZAAKTYPE',
    'Load test zaaktype [[ZAAKTYPE_NUMBER]]',
    NULL
);

INSERT INTO domain_entity_type (
    domain_id,
    entity_types_id
) VALUES (
    'd1e2f3a4-b5c6-7d8e-9f0a-b1c2d3e4f5a6',
    '[[ENTITY_TYPE_UUID]]'
);
