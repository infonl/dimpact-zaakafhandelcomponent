CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO catalogi_catalogus (naam, uuid, domein, rsin, contactpersoon_beheer_naam, contactpersoon_beheer_telefoonnummer, contactpersoon_beheer_emailadres, _etag, begindatum_versie, versie) VALUES
    ('zac', '8225508a-6840-413e-acc9-6422af120db1', 'ALG', '002564440', 'ZAC Test Catalogus', '06-12345678', 'noreply@example.com', '_etag', NULL, '');

INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{zac_client}', 'ZAC', true);
INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{open-zaak}', 'Open Zaak', true);
INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{open-archiefbeheer}', 'Open Archiefbeheer', true);
INSERT INTO authorizations_applicatie (uuid, client_ids, label, heeft_alle_autorisaties) VALUES (uuid_generate_v4(), '{open-notificaties}', 'Open notificaties', true);

INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('zac_client', 'openzaakZaakafhandelcomponentClientSecret');
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('open-zaak', 'opennotificatiesOpenzaakSecret');
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('open-archiefbeheer', 'openArchiefbeheerApiSecretKey');
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES ('open-notificaties', 'opennotificatiesAutorisatieApiSecret');
