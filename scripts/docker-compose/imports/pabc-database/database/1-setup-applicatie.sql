-- create ZAC application roles
INSERT INTO "application_role" ("id", "name", "application") VALUES
    ('003591ad-c6c8-4107-8877-9579c5e27982','raadpleger','zaakafhandelcomponent'),
    ('a43e878d-a08f-4102-9d6c-9aa58299581b','behandelaar','zaakafhandelcomponent'),
    ('03dc5977-e99d-4b39-a7c1-6a5ceb16a574','coordinator','zaakafhandelcomponent'),
    ('2a7cfd79-bf3c-4f86-8a45-2aedd2b2ba0e','recordmanager','zaakafhandelcomponent'),
    ('c54a72b5-053b-4c3e-ac9f-c6034a6e23ea','beheerder','zaakafhandelcomponent');

-- create ZAC example functional roles
INSERT INTO "functional_role" ("id", "name") VALUES
    ('f0c1b2d3-4e5f-6789-0a1b-2c3d4e5f6789','raadpleger_domein_test_1'),
    ('12345678-90ab-cdef-1234-567890abcdef','behandelaar_domein_test_1'),
    ('f4fdde87-71c1-467d-9241-421d59484ad8','behandelaar_domein_test_2'),
    ('23456789-0abc-def1-2345-67890abcdef1','coordinator_domein_test_1'),
    ('34567890-abcd-ef12-3456-7890abcdef12','recordmanager_domein_test_1_en_domein_test_2'),
    ('45678901-bcde-f123-4567-890abcdef123','beheerder_elk_domein');

-- create ZAC example domains
INSERT INTO "domain" ("id", "name", "description") VALUES
    ('d1e2f3a4-b5c6-7d8e-9f0a-b1c2d3e4f5a6','domein_test_1', 'Test domein 1'),
    ('d3e4f5a6-b7c8-9d0e-1f2a-b3c4d5e6f7a8','domein_test_2', 'Test domein 2');

-- create mappings between domains, functional roles and application roles
INSERT INTO "mapping" ("id", "functional_role_id", "domain_id", "application_role_id", "is_all_entity_types") VALUES
    -- create mappings for all functional roles to the corresponding application role in 'domein_test_1' where a functional role also contains all 'underlying' application roles
    ('15e01325-d883-4e42-9152-9137be5f443b', (SELECT "id" FROM "functional_role" WHERE "name" = 'raadpleger_domein_test_1'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), false),
    ('4e4a88ae-b582-452e-af81-1cbc1b955e5e', (SELECT "id" FROM "functional_role" WHERE "name" = 'behandelaar_domein_test_1'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), false),
    ('898d49e5-b899-4e7b-b99c-895e5bed8294', (SELECT "id" FROM "functional_role" WHERE "name" = 'behandelaar_domein_test_1'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'behandelaar'), false),
    ('eb1ba239-948e-4577-9655-8cc81a4a1526', (SELECT "id" FROM "functional_role" WHERE "name" = 'coordinator_domein_test_1'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), false),
    ('1135fffe-d160-439a-b3fa-75e30ea76ce0', (SELECT "id" FROM "functional_role" WHERE "name" = 'coordinator_domein_test_1'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'behandelaar'), false),
    ('0b197823-4676-4c19-ad09-f7488f235f50', (SELECT "id" FROM "functional_role" WHERE "name" = 'coordinator_domein_test_1'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'coordinator'), false),
    ('eb6ab6fd-925e-45a2-a01b-64634090ba2f', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), false),
    ('d99bcdfd-4727-4cc4-bc1d-6530ca766309', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'behandelaar'), false),
    ('c76c0c86-f974-434e-a21c-8380a8fbdb10', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'coordinator'), false),
    ('0c502a34-7b48-4437-a223-842d4d048bab', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "application_role" WHERE "name" = 'recordmanager'), false),
    -- create mappings for application roles in 'domein_test_2'
    ('e83ed1fd-ca96-488d-b2d0-2d52c364f487', (SELECT "id" FROM "functional_role" WHERE "name" = 'behandelaar_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), false),
    ('7a37e060-8039-4528-8f55-f32b129b6ef1', (SELECT "id" FROM "functional_role" WHERE "name" = 'behandelaar_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "application_role" WHERE "name" = 'behandelaar'), false),
    ('699daaf5-9978-4739-9a18-2cfb26958e77', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), false),
    ('db7e0427-68ec-4bd9-ad3b-a5a945230b24', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "application_role" WHERE "name" = 'behandelaar'), false),
    ('0e5dc382-c26b-491f-8a45-6025f150c3a9', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "application_role" WHERE "name" = 'coordinator'), false),
    ('a87ed914-096f-4d73-82de-ef87c033adb8', (SELECT "id" FROM "functional_role" WHERE "name" = 'recordmanager_domein_test_1_en_domein_test_2'), (SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "application_role" WHERE "name" = 'recordmanager'), false),
    -- create mappings for 'beheerder_elk_domein' functional role to all application roles in all domains
    ('e468514b-f4cf-4b1e-bc5d-653bdbcc4587', (SELECT "id" FROM "functional_role" WHERE "name" = 'beheerder_elk_domein'), null, (SELECT "id" FROM "application_role" WHERE "name" = 'raadpleger'), true),
    ('b15ab065-126d-4311-8170-18b047173903', (SELECT "id" FROM "functional_role" WHERE "name" = 'beheerder_elk_domein'), null, (SELECT "id" FROM "application_role" WHERE "name" = 'behandelaar'), true),
    ('ac0f0745-8e74-4966-a706-b8ef0e806bdc', (SELECT "id" FROM "functional_role" WHERE "name" = 'beheerder_elk_domein'), null, (SELECT "id" FROM "application_role" WHERE "name" = 'coordinator'), true),
    ('33c36417-b0df-45db-8c11-36d18cf63425', (SELECT "id" FROM "functional_role" WHERE "name" = 'beheerder_elk_domein'), null, (SELECT "id" FROM "application_role" WHERE "name" = 'recordmanager'), true),
    ('e6990d64-55af-40b8-af54-b1f4b1083bce', (SELECT "id" FROM "functional_role" WHERE "name" = 'beheerder_elk_domein'), null, (SELECT "id" FROM "application_role" WHERE "name" = 'beheerder'), true);

-- create zaaktype entity types
INSERT INTO "entity_type" ("id", "entity_type_id", "type", "name", "uri") VALUES
    ('5b5d4f41-4c9c-4ff3-b59e-2f6a0f7f1d6c','Test zaaktype 1', 'zaaktype', 'Test zaaktype 1', 'https://example.com/zaaktype/8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'),
    ('a6c7f182-0e7d-4f0f-9067-fd8a4ec7f3f1','BPMN test zaaktype', 'zaaktype', 'BPMN test zaaktype', 'https://example.com/zaaktype/26076928-ce07-4d5d-8638-c2d276f6caca'),
    ('0db3c76a-2911-48c7-a24c-f6e4f3b18c48','Test zaaktype 2', 'zaaktype', 'Test zaaktype 2', 'https://example.com/zaaktype/fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'),
    ('e9dbe8f7-7b30-41d2-bb0d-187d4a4d6822','Test zaaktype 3', 'zaaktype', 'Test zaaktype 3', 'https://example.com/zaaktype/448356ff-dcfb-4504-9501-7fe929077c4f');

-- create mappings between entity types and domains
INSERT INTO "domain_entity_type" ("domain_id", "entity_types_id") VALUES
    ((SELECT "id" FROM "domain" WHERE "name" = 'domein_test_2'), (SELECT "id" FROM "entity_type" WHERE "entity_type_id" = 'Test zaaktype 1')),
    ((SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "entity_type" WHERE "name" = 'BPMN test zaaktype')),
    ((SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "entity_type" WHERE "name" = 'Test zaaktype 2')),
    ((SELECT "id" FROM "domain" WHERE "name" = 'domein_test_1'), (SELECT "id" FROM "entity_type" WHERE "name" = 'Test zaaktype 3'));

