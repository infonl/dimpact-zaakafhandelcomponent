
-- add a parent-child (hoofdzaak-deelzaaktype) relation between the two zaaktypes created previously
-- so that we can test the functionality in ZAC to manage hoofdzaak - deelzaak relations
INSERT INTO catalogi_zaaktype_deelzaaktypen (id, from_zaaktype_id, to_zaaktype_id)
VALUES
(
1,
   (SELECT id FROM catalogi_zaaktype WHERE identificatie = 'indienen-aansprakelijkstelling-behandelen'),
   (SELECT id FROM catalogi_zaaktype WHERE identificatie = 'melding-evenement-organiseren-behandelen')
);

-- Insert productaanvraag PDF document as enkelvoudig informatieobject.
-- This assumes that the PDF in question is available in the OpenZaak container in: '/app/private-media/uploads/2023/10/dummy-test-document.pdf'
-- Note that we use an id of '999' for both the canonical and the enkelvoudig informatieobject records to avoid duplicate key conflicts
-- on subsequent new documents created via the Open Zaak REST API.
-- It appears that Open Zaak uses its cache for this data and does not see this data inserted via the database at startup..
INSERT INTO documenten_enkelvoudiginformatieobjectcanonical (id, lock) VALUES (999, '');
INSERT INTO documenten_enkelvoudiginformatieobject (id, identificatie, bronorganisatie, creatiedatum, titel, vertrouwelijkheidaanduiding, auteur, status, beschrijving, ontvangstdatum, verzenddatum, indicatie_gebruiksrecht, ondertekening_soort, ondertekening_datum, uuid, formaat, taal, bestandsnaam, inhoud, link, integriteit_algoritme, integriteit_waarde, integriteit_datum, versie, begin_registratie, _informatieobjecttype_id, canonical_id, bestandsomvang, _informatieobjecttype_base_url_id, _informatieobjecttype_relative_url, verschijningsvorm, trefwoorden) VALUES (999, 'DOCUMENT-2023-0000000001', '002564440', '2023-10-30', 'Dummy test document', 'zaakvertrouwelijk', 'Aanvrager', 'definitief', 'Ingezonden formulier', NULL, NULL, false, '', NULL, '37e16ddc-7992-418c-b5a0-54cf6680329e', 'application/pdf', 'nld', 'dummy-test-document.pdf', 'uploads/2023/10/dummy-test-document.pdf', '', '', '', NULL, 1, '2023-10-30 12:54:01.849', 2, 999, 1234, NULL, NULL, '', '{}');

-- Set up the BAG service configuration. This requires that the corresponding variables have been passed on to this script.
INSERT INTO zgw_consumers_service (label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id, uuid, timeout, api_connection_check_path, slug) VALUES ('BAG', 'orc', :'BAG_API_CLIENT_MP_REST_URL', '', '', 'api_key', 'X-Api-Key', :'BAG_API_KEY', :'BAG_API_CLIENT_MP_REST_URL', '', '', '', '', NULL, NULL, 'd986d53f-68a9-45af-a3ff-194b1b8ec089', 10,'', 'bag');
-- Set up the Objecten service configuration.
INSERT INTO zgw_consumers_service (label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id, uuid, timeout, api_connection_check_path, slug) VALUES ('Objects API', 'orc', 'http://objecten-api.local:8000/api/v2/', '', '', 'api_key', 'Authorization', 'Token cd63e158f3aca276ef284e3033d020a22899c728', 'http://objecten-api.local:8000/api/v2/schema/openapi.yaml', '', '', '', '', NULL, NULL, '9fe5f3fb-a1f5-4ea5-a2fe-c5b1294da0f4', 10, '', 'objects-api');
INSERT INTO zgw_consumers_service (label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id, uuid, timeout, api_connection_check_path, slug) VALUES ('Objects API IntelliJ', 'orc', 'http://host.docker.internal:8010/api/v2/', '', '', 'api_key', 'Authorization', 'Token cd63e158f3aca276ef284e3033d020a22899c728', 'http://host.docker.internal:8010/api/v2/schema/openapi.yaml', '', '', '', '', NULL, NULL, '91a1cc84-7677-47b5-a6e2-f33aae89ff8f', 10,'', 'objects-api-intellij');
-- Set up the OpenNotificaties service configuration.
-- Unfortunately it seems that we need to use 'host.docker.internal' here to connect to Open Notificaties. Not sure why.
-- Please see our 'testing.md' document on how to set this up.
INSERT INTO zgw_consumers_service (label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id, uuid, timeout, api_connection_check_path, slug) VALUES('Open Notificaties', 'nrc', 'http://host.docker.internal:8003/api/v1/', 'open-zaak-autorisaties', 'openZaakAutorisatiesApiSecretKey', 'zgw', '', '', 'http://host.docker.internal:8003/api/v1/schema/openapi.yaml', '', 'open-zaak-notificaties', 'Open Zaak - Notificaties', '', NULL, NULL, '031fe099-095b-4091-9f99-f81ef30561be', 10,'', 'open-notificaties');
-- Set up the OpenArchiefbeheer service configuration.
INSERT INTO zgw_consumers_service (label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id, uuid, timeout, api_connection_check_path, slug) VALUES('Open Archiefbeheer', 'nrc', 'http://host.docker.internal:8004/api/v1/', 'open-archiefbeheer', 'openArchiefbeheerApiSecretKey', 'zgw', '', '', 'http://host.docker.internal:8004/api/v1/schema', '', 'open-archiefbeheer', 'Open Archiefbeheer', '', NULL, NULL, 'f21a5a4d-36c8-44a4-bf72-44da1cd30a26', 10, '', 'open-archiefbeheer');

-- TODO: do we need to update the existing 'Authorization API service' record in the zgw_consumers_service table here?

-- Set up the Notificatiescomponentconfiguratie
-- we assume here that a record already exists with id=1 (this is provisioned by OpenNotificaties on startup)
UPDATE notifications_api_common_notificationsconfig SET notifications_api_service_id=(SELECT id FROM zgw_consumers_service WHERE label = 'Open Notificaties'), notification_delivery_max_retries=5, notification_delivery_retry_backoff=3, notification_delivery_retry_backoff_max=48 WHERE id=1;

-- Update the primary key sequences for the tables in which we previously inserted data using fixed primary key values
-- so that new records inserted manually via the OpenZaak UI do not conflict with the records we inserted.
ALTER SEQUENCE catalogi_zaaktype_id_seq RESTART WITH 100;
ALTER SEQUENCE catalogi_resultaattype_id_seq RESTART WITH 100;
ALTER SEQUENCE catalogi_roltype_id_seq RESTART WITH 100;
ALTER SEQUENCE catalogi_statustype_id_seq RESTART WITH 100;
ALTER SEQUENCE catalogi_besluittype_zaaktypen_id_seq RESTART WITH 100;
ALTER SEQUENCE catalogi_zaaktypeinformatieobjecttype_id_seq RESTART WITH 100;
