-- create ZAC application roles
INSERT INTO "ApplicationRoles" ("Id", "Name", "Application") VALUES
    ('003591ad-c6c8-4107-8877-9579c5e27982','raadpleger','zaakafhandelcomponent'),
    ('a43e878d-a08f-4102-9d6c-9aa58299581b','behandelaar','zaakafhandelcomponent'),
    ('03dc5977-e99d-4b39-a7c1-6a5ceb16a574','coordinator','zaakafhandelcomponent'),
    ('2a7cfd79-bf3c-4f86-8a45-2aedd2b2ba0e','recordmanager','zaakafhandelcomponent'),
    ('c54a72b5-053b-4c3e-ac9f-c6034a6e23ea','beheerder','zaakafhandelcomponent');

-- create ZAC example functional roles
-- note that for now the functional roles map 1-on-1 to the application roles (this will change in future)
INSERT INTO "FunctionalRoles" ("Id", "Name") VALUES
    ('f0c1b2d3-4e5f-6789-0a1b-2c3d4e5f6789','raadpleger'),
    ('12345678-90ab-cdef-1234-567890abcdef','behandelaar'),
    ('23456789-0abc-def1-2345-67890abcdef1','coordinator'),
    ('34567890-abcd-ef12-3456-7890abcdef12','recordmanager'),
    ('45678901-bcde-f123-4567-890abcdef123','beheerder');

-- create ZAC example domains
INSERT INTO "Domains" ("Id", "Name", "Description") VALUES
    ('d1e2f3a4-b5c6-7d8e-9f0a-b1c2d3e4f5a6','domein_test_1', 'Test domein 1'),
    ('d3e4f5a6-b7c8-9d0e-1f2a-b3c4d5e6f7a8','domein_test_2', 'Test domein 2');

-- create mappings between domains, functional roles and application roles
INSERT INTO "Mappings" ("Id", "FunctionalRoleId", "DomainId", "ApplicationRoleId") VALUES
    -- create mappings for all functional roles to the corresponding application role in 'domein_test_1' where a functional role also contains all 'underlying' application roles
    ('15e01325-d883-4e42-9152-9137be5f443b', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'raadpleger'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('4e4a88ae-b582-452e-af81-1cbc1b955e5e', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'behandelaar'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('898d49e5-b899-4e7b-b99c-895e5bed8294', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'behandelaar'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('eb1ba239-948e-4577-9655-8cc81a4a1526', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'coordinator'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('1135fffe-d160-439a-b3fa-75e30ea76ce0', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'coordinator'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('0b197823-4676-4c19-ad09-f7488f235f50', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'coordinator'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('eb6ab6fd-925e-45a2-a01b-64634090ba2f', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('d99bcdfd-4727-4cc4-bc1d-6530ca766309', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('c76c0c86-f974-434e-a21c-8380a8fbdb10', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('0c502a34-7b48-4437-a223-842d4d048bab', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'recordmanager')),
    ('e468514b-f4cf-4b1e-bc5d-653bdbcc4587', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('b15ab065-126d-4311-8170-18b047173903', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('ac0f0745-8e74-4966-a706-b8ef0e806bdc', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('5fcb04d7-731e-4b87-98c0-3afd8692d064', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'beheerder')),
    ('33c36417-b0df-45db-8c11-36d18cf63425', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'recordmanager')),
    -- create mappings for recordmanager and beheerder to the corresponding application role in 'domein_test_2'
    ('699daaf5-9978-4739-9a18-2cfb26958e77', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('db7e0427-68ec-4bd9-ad3b-a5a945230b24', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('0e5dc382-c26b-491f-8a45-6025f150c3a9', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('a87ed914-096f-4d73-82de-ef87c033adb8', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'recordmanager')),
    ('6ff6a97c-ad32-4822-b23b-ec2a99c3a87a', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('8695329f-c258-4420-ba98-851611e6f2ec', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('4c299601-a0e5-4987-883c-47c65c26fe98', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('db8073b8-7d23-4d81-b8db-7edc690bf046', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'recordmanager')),
    ('e6990d64-55af-40b8-af54-b1f4b1083bce', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'beheerder'));

-- create zaaktype entity types
INSERT INTO "EntityTypes" ("Id", "EntityTypeId", "Type", "Name", "Uri") VALUES
    ('e2fd7b9f-f104-4ac8-9293-2086661d36e8','zaaktype_test_1', 'zaaktype', 'Test zaaktype 1', 'https://example.com/zaaktype/test-1'),
    ('d8ae5c97-1288-4d3a-8f90-8cd6d98717be','zaaktype_test_2', 'zaaktype', 'Test zaaktype 2', 'https://example.com/zaaktype/test-2'),
    ('5b5d4f41-4c9c-4ff3-b59e-2f6a0f7f1d6c','BPMN Evenementen Vooroverleg', 'zaaktype', 'BPMN Evenementen Vooroverleg', 'https://example.com/zaaktype/8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a'),
    ('a6c7f182-0e7d-4f0f-9067-fd8a4ec7f3f1','BPMN test zaaktype', 'zaaktype', 'BPMN test zaaktype', 'https://example.com/zaaktype/26076928-ce07-4d5d-8638-c2d276f6caca'),
    ('0db3c76a-2911-48c7-a24c-f6e4f3b18c48','Indienen aansprakelijkstelling door derden behandelen', 'zaaktype', 'Indienen aansprakelijkstelling door derden behandelen', 'https://example.com/zaaktype/fd2bf643-c98a-4b00-b2b3-9ae0c41ed425'),
    ('e9dbe8f7-7b30-41d2-bb0d-187d4a4d6822','Melding evenement organiseren behandelen', 'zaaktype', 'Melding evenement organiseren behandelen', 'https://example.com/zaaktype/448356ff-dcfb-4504-9501-7fe929077c4f');

--create mappings between entity types and domains
INSERT INTO "DomainEntityType" ("DomainId", "EntityTypesId") VALUES
    ((SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "EntityTypes" WHERE "EntityTypeId" = 'zaaktype_test_1')),
    ((SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "EntityTypes" WHERE "EntityTypeId" = 'zaaktype_test_2')),
    ((SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "EntityTypes" WHERE "Name" = 'BPMN Evenementen Vooroverleg')),
    ((SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "EntityTypes" WHERE "Name" = 'BPMN test zaaktype')),
    ((SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "EntityTypes" WHERE "Name" = 'Indienen aansprakelijkstelling door derden behandelen')),
    ((SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "EntityTypes" WHERE "Name" = 'Melding evenement organiseren behandelen'));

