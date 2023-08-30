-- create API Autorisatie - Applicatie
INSERT INTO authorizations_applicatie (id, uuid, client_ids, label, heeft_alle_autorisaties) VALUES (1, 'beb4876d-56ac-448c-aa88-38228eb6d053', '{zac_client}', 'zaakafhandelcomponent', true);

-- create API Autorisatie - Autorisatiegegegevens
INSERT INTO vng_api_common_jwtsecret (id, identifier, secret) VALUES (1, 'zac_client', 'openklantZaakafhandelcomponentClientSecret');

-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');
