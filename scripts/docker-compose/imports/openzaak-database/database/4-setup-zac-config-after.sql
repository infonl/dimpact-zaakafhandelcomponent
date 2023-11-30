
-- Insert productaanvraag PDF document as enkelvoudig informatieobject.
-- This assumes that the PDF in question is available in the OpenZaak container in: '/app/private-media/uploads/2023/10/dummy-test-document.pdf'
-- Note that we use an id of '999' for both the canonical and the enkelvoudig informatieobject records to avoid duplicate key conflicts
-- on subsequent new documents created via the Open Zaak REST API.
-- It appears that Open Zaak uses its cache for this data and does not see this data inserted via the database at startup..
INSERT INTO documenten_enkelvoudiginformatieobjectcanonical (id, lock) VALUES (999, '');
INSERT INTO documenten_enkelvoudiginformatieobject (id, identificatie, bronorganisatie, creatiedatum, titel, vertrouwelijkheidaanduiding, auteur, status, beschrijving, ontvangstdatum, verzenddatum, indicatie_gebruiksrecht, ondertekening_soort, ondertekening_datum, uuid, formaat, taal, bestandsnaam, inhoud, link, integriteit_algoritme, integriteit_waarde, integriteit_datum, versie, begin_registratie, _informatieobjecttype_id, canonical_id, bestandsomvang, _informatieobjecttype_base_url_id, _informatieobjecttype_relative_url) VALUES (999, 'DOCUMENT-2023-0000000001', '002564440', '2023-10-30', 'Dummy test document', 'zaakvertrouwelijk', 'Aanvrager', 'definitief', 'Ingezonden formulier', null, null, false, '', null, '37e16ddc-7992-418c-b5a0-54cf6680329e', 'application/pdf', 'nld', 'dummy-test-document.pdf', 'uploads/2023/10/dummy-test-document.pdf', '', '', '', null, 1, '2023-10-30 11:54:01.849795 +00:00', 2, 999, 1234, null, null);

-- Open Notificaties is not used yet in our Docker Compose set-up
-- UPDATE notifications_notificationsconfig SET api_root = 'http://opennotificaties:8000/api/v1/';

-- Open Formulieren is not used yet in our Docker Compose set-up
-- INSERT INTO zgw_consumers_service(label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file) VALUES ('Open formulieren', 'nrc', 'http://host.docker.internal:8002/api/v1/', 'openzaak', 'openzaak', 'zgw', '', '', 'http://host.docker.internal:8002/api/v1/schema/openapi.yaml', '', '', '', '');
-- Set up the BAG service configuration. This requires that the corresponding variables have been passed on to this script.
INSERT INTO zgw_consumers_service(label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id) VALUES ('BAG', 'orc', :'BAG_API_CLIENT_MP_REST_URL', '', '', 'api_key', 'X-Api-Key', :'BAG_API_KEY', :'BAG_API_CLIENT_MP_REST_URL', '', '', '', '', null, null);
INSERT INTO zgw_consumers_service(label, api_type, api_root, client_id, secret, auth_type, header_key, header_value, oas, nlx, user_id, user_representation, oas_file, client_certificate_id, server_certificate_id) VALUES ('Objects API', 'orc', 'http://objecten-api.local:8000/api/v2/', '', '', 'api_key', 'Authorization', 'Token cd63e158f3aca276ef284e3033d020a22899c728', 'http://objecten-api.local:8000/api/v2/schema/openapi.yaml', '', '', '', '', null, null);
