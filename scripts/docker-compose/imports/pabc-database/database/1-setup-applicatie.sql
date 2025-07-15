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
    ('15e01325-d883-4e42-9152-9137be5f443b','f0c1b2d3-4e5f-6789-0a1b-2c3d4e5f6789','d1e2f3a4-b5c6-7d8e-9f0a-b1c2d3e4f5a6', '003591ad-c6c8-4107-8877-9579c5e27982');

-- create zaaktype entity types

