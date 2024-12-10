-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');

-- Set up the Autorisatiecomponentconfiguratie
INSERT INTO authorizations_authorizationsconfig (api_root, component) VALUES('http://openzaak.local:8000/api/v1/', 'ac');

-- Set up the OpenNotificaties 'self' service
-- TODO: it seems we need to use 'host.docker.internal' here
INSERT INTO zgw_consumers_service (label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id, uuid, timeout, api_connection_check_path) VALUES('notificaties-self', 'nrc', 'http://host.docker.internal:8003/api/v1/', 'opennotificaties', 'openNotificatiesApiSecretKey', 'zgw', '', '', 'http://host.docker.internal:8003/api/v1/schema/openapi.yaml', '', '', '', '', NULL, NULL, 'd7d82c34-f6f2-4cf7-9376-bd23e7633a1f', 10, '');

-- Set up the Notificatiescomponentconfiguratie
-- We assume here that a record already exists with id=1 (this is provisioned by OpenNotificaties on startup)
UPDATE notifications_api_common_notificationsconfig SET notifications_api_service_id=(SELECT id FROM zgw_consumers_service WHERE label = 'notificaties-self'), notification_delivery_max_retries=5, notification_delivery_retry_backoff=3, notification_delivery_retry_backoff_max=48 WHERE id=1;

-- Set up the External API credentials
INSERT INTO vng_api_common_apicredential (api_root, client_id, secret, label, user_id, user_representation) VALUES('http://openzaak.local:8000/autorisaties/api/v1/', 'open-zaak-autorisaties', 'openZaakAutorisatiesApiSecretKey', 'Open Zaak - Autorisaties', 'open-zaak-autorisaties', 'Open Zaak - Autorisaties');

-- Set up the Autorisatiegegevens
INSERT INTO vng_api_common_jwtsecret (identifier, secret) VALUES(1, 'open-zaak-autorisaties', 'openZaakAutorisatiesApiSecretKey');
