-- VNG ZGW referentielijsten database initialization script. To be run only once.

-- Insert communication channels required by ZAC.
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('9f86d995-e2f9-486e-abe2-e58ae18edaea', 'E-formulier', 'E-formulier');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('f5de7d7f-8440-4ce7-8f27-f934ad0c2ea6', 'E-mail', 'E-mail');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('99c59739-a0f6-414e-a286-ac0ae736a6dc', 'Intern', 'Intern');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('acf74c78-6e93-4df8-8fa9-f6ba0281fa4f', 'Balie', 'Balie');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('721ec15e-18c5-464f-a519-f5b96bf0d88a', 'Telefoon', 'Telefoon');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('d79021ff-9286-45be-a8c4-e6a81f1f0c11', 'Internet', 'Internet');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('659a34bd-515b-4570-8d80-65a0f11ac740', 'Medewerkersportaal', 'Medewerkersportaal');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('51e33b70-6ffd-4aa5-95b9-aba12c5c3102', 'Post', 'Post');

