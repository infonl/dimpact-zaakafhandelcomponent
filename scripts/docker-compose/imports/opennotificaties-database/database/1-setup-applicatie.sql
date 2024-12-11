-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');

-- Set up the Autorisatiecomponentconfiguratie
INSERT INTO authorizations_authorizationsconfig (api_root, component) VALUES('http://host.docker.internal:8001/autorisaties/api/v1/', 'ac');

-- Set up the Notificatiescomponentconfiguratie
-- We assume here that a record already exists with id=1 (this is provisioned by OpenNotificaties on startup)
UPDATE notifications_api_common_notificationsconfig SET notifications_api_service_id=(SELECT id FROM zgw_consumers_service WHERE label = 'notificaties-self'), notification_delivery_max_retries=5, notification_delivery_retry_backoff=3, notification_delivery_retry_backoff_max=48 WHERE id=1;

-- Set up the External API credentials
-- TODO: it seems we need to use 'host.docker.internal' here
INSERT INTO vng_api_common_apicredential (api_root, client_id, secret, label, user_id, user_representation) VALUES('http://host.docker.internal:8001/autorisaties/api/v1/', 'open-zaak-autorisaties', 'openZaakAutorisatiesApiSecretKey', 'Open Zaak - Autorisaties', 'open-zaak-autorisaties', 'Open Zaak - Autorisaties');

-- Set up the Autorisatiegegevens
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES('open-zaak-autorisaties', 'openZaakAutorisatiesApiSecretKey');

-- Set up the kanalen
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('493002ad-e5d5-4747-93b2-1853e78889f5', 'zaaktypen', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#zaaktypen', '{catalogus}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('3ad6676c-98cc-4664-babb-02bda0c886d8', 'zaken', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#zaken', '{bronorganisatie,zaaktype,vertrouwelijkheidaanduiding}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('97d7d649-0979-422a-8880-a0aee37cc6ea', 'documenten', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#documenten', '{bronorganisatie,informatieobjecttype,vertrouwelijkheidaanduiding}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('93179f23-965e-4720-964f-d09be3bc2790', 'besluittypen', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#besluittypen', '{catalogus}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('c4f52cb5-07e7-44cb-b4b7-2539bce684f9', 'besluiten', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#besluiten', '{verantwoordelijke_organisatie,besluittype}');

-- Set up the abonnement to ZAC
-- This assumes ZAC is running on localhost:8080
INSERT INTO datamodel_abonnement (uuid, callback_url, auth, client_id) VALUES('fb4e3474-18c8-474b-94ae-980850ea4a7f', 'http://localhost:8080/rest/notificaties', 'openNotificatiesApiSecretKey', 'opennotificaties');
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'zaaktypen'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'zaken'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'documenten'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'besluittypen'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'besluiten'));
