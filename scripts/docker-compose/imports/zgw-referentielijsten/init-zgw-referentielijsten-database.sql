-- VNG ZGW referentielijsten database initialization script. To be run only once.

-- Insert communication channels required by ZAC.
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('9f86d995-e2f9-486e-abe2-e58ae18edaea', 'E-formulier', 'E-formulier');
INSERT INTO datamodel_communicatiekanaal (uuid, naam, omschrijving) VALUES ('f5de7d7f-8440-4ce7-8f27-f934ad0c2ea6', 'E-mail', 'E-mail');

