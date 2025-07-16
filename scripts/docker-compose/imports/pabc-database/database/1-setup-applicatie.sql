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
    -- create one-on-one mappings for all functional roles to the corresponding application role in 'domein_test_1'
    ('15e01325-d883-4e42-9152-9137be5f443b', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'raadpleger'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'raadpleger')),
    ('4e4a88ae-b582-452e-af81-1cbc1b955e5e', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'behandelaar'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('eb1ba239-948e-4577-9655-8cc81a4a1526', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'coordinator'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('eb6ab6fd-925e-45a2-a01b-64634090ba2f', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'recordmanager')),
    ('e468514b-f4cf-4b1e-bc5d-653bdbcc4587', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_1'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'beheerder')),
    -- create one-on-one mappings for all functional roles except raadpleger to the corresponding application role in 'domein_test_2'
    ('68760634-e29b-4af5-a03a-36fa2ac61811', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'behandelaar'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'behandelaar')),
    ('c39f6947-e580-41ab-b685-aa72603250da', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'coordinator'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'coordinator')),
    ('699daaf5-9978-4739-9a18-2cfb26958e77', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'recordmanager'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'recordmanager')),
    ('6ff6a97c-ad32-4822-b23b-ec2a99c3a87a', (SELECT "Id" FROM "FunctionalRoles" WHERE "Name" = 'beheerder'), (SELECT "Id" FROM "Domains" WHERE "Name" = 'domein_test_2'), (SELECT "Id" FROM "ApplicationRoles" WHERE "Name" = 'beheerder'));

-- create zaaktype entity types
INSERT INTO "EntityTypes" ("Id", "EntityTypeId", "Type", "Name", "Uri") VALUES
    ('e2fd7b9f-f104-4ac8-9293-2086661d36e8','zaaktype_test_1', 'zaaktype', 'Test zaaktype 1', 'https://example.com/zaaktype/test-1'),
    ('d8ae5c97-1288-4d3a-8f90-8cd6d98717be','zaaktype_test_2', 'zaaktype', 'Test zaaktype 2', 'https://example.com/zaaktype/test-2');

--create mappings between entity types and domains
INSERT INTO "DomainEntityType" ("DomainId", "EntityTypesId") VALUES
    ('d1e2f3a4-b5c6-7d8e-9f0a-b1c2d3e4f5a6', 'e2fd7b9f-f104-4ac8-9293-2086661d36e8'),
    ('d3e4f5a6-b7c8-9d0e-1f2a-b3c4d5e6f7a8', 'd8ae5c97-1288-4d3a-8f90-8cd6d98717be');
