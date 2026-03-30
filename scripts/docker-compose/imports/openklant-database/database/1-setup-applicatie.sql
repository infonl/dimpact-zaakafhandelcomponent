-- create API Auth - Token authorizations
INSERT INTO token_tokenauth (id, token, contact_person, email, organization, last_modified, created, application, administration, identifier) VALUES (1, 'fakeToken', 'OpenKlant Admin', 'openklant-admin@example.com', 'fakeOrganization', '2024-07-31 10:06:52.146060 +00:00', '2024-07-31 10:06:52.146060 +00:00', 'zaakafhandelcomponent', 'admin', 'zaakafhandelcomponent');

-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');

-- add model data
INSERT INTO contactgegevens_persoon (id, adres_adresregel1, adres_adresregel2, adres_adresregel3, adres_straatnaam, adres_stad, adres_land, adres_nummeraanduiding_id, adres_huisnummertoevoeging, adres_postcode, uuid, geboortedatum, overlijdensdatum, geslachtsnaam, geslacht, voorvoegsel, voornamen)
    VALUES (1, '', '', '', '', '', '', '', '', '', 'e93fc3bf-0b6d-4447-9e5c-de049ebdff09', '2000-07-31', '2124-07-31', 'Janse', 'f', '', 'Héndrika');

INSERT INTO klantinteracties_actor (id, actoridentificator_object_id, uuid, naam, soort_actor, indicatie_actief, actoridentificator_code_objecttype, actoridentificator_code_register, actoridentificator_code_soort_object_id)
    VALUES (1, '', 'f7340d48-a76e-472b-8866-f8d8512afe5a', 'Actor Name', 'medewerker', true, '', '', '');

INSERT INTO klantinteracties_klantcontact (id, uuid, nummer, kanaal, onderwerp, inhoud, indicatie_contact_gelukt, taal, vertrouwelijk, plaatsgevonden_op)
    VALUES (1, '21e55404-226d-42b6-aeb0-f28b7c7d09dc', '0000000001', 'email', 'email contact', 'email', true, 'dut', false, '2000-01-01 12:00:00.000000 +00:00');
INSERT INTO klantinteracties_klantcontact (id, uuid, nummer, kanaal, onderwerp, inhoud, indicatie_contact_gelukt, taal, vertrouwelijk, plaatsgevonden_op)
    VALUES (2, '21e54405-246c-44b6-aec2-f25b7c7d02db', '0000000002', 'telefoon', 'phone contact', 'telefoonnummer', true, 'dut', false, '2010-01-01 12:00:00.000000 +00:00');
-- klantcontact to test product request with request specific email address
INSERT INTO klantinteracties_klantcontact (id, "uuid", nummer, kanaal, onderwerp, inhoud, indicatie_contact_gelukt, taal, vertrouwelijk, plaatsgevonden_op)
    VALUES(3, 'b74a2540-edb3-45ec-a7e5-9d09e2b79718', '0000000003', 'Webformulier', 'Productaanvraag-Dimpact test formulier - met DigiD en communicatievoorkeuren', '', NULL, 'nld', true, '2026-03-24 09:49:02.000');
-- klantcontact to test product request with request specific email address
INSERT INTO klantinteracties_klantcontact (id, "uuid", nummer, kanaal, onderwerp, inhoud, indicatie_contact_gelukt, taal, vertrouwelijk, plaatsgevonden_op)
    VALUES(4, 'f453481d-4385-4ab8-8c31-58b649eb763e', '0000000004', 'Webformulier', 'Productaanvraag-Dimpact test formulier - met DigiD en communicatievoorkeuren', '', NULL, 'nld', true, '2026-03-25 09:49:02.000');

INSERT INTO klantinteracties_actorklantcontact (id, uuid, actor_id, klantcontact_id)
    VALUES (1, '63567628-b257-46d3-95c6-1ed76427c526', 1, 1);

-- partij of type persoon
INSERT INTO klantinteracties_partij (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, uuid, nummer, interne_notitie, soort_partij, indicatie_geheimhouding, voorkeurstaal, indicatie_actief, voorkeurs_digitaal_adres_id, voorkeurs_rekeningnummer_id)
    VALUES (1, '', '', '', '', '','', '', '','', '', '', '', '', '','','','', '','a5e1cd61-f5a2-43f9-9644-805f9bddf1ad', '0000000001', 'note', 'persoon', false, 'dut', true, null, null);
-- partij identificatie of type BSN
INSERT INTO klantinteracties_partijidentificator (id, uuid, andere_partij_identificator, partij_identificator_code_objecttype, partij_identificator_code_soort_object_id, partij_identificator_object_id, partij_identificator_code_register, partij_id)
    VALUES (1, '4b7f454f-e836-4d4d-9f37-df98e473616b', '', 'natuurlijk_persoon', 'bsn', '999993896', 'brp', 1);

-- partij of type organisatie - bedrijf
INSERT INTO klantinteracties_partij (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, uuid, nummer, interne_notitie, soort_partij, indicatie_geheimhouding, voorkeurstaal, indicatie_actief, voorkeurs_digitaal_adres_id, voorkeurs_rekeningnummer_id)
    VALUES (2, '', '', '', '', '','', '', '','','', '', '', '', '','','', '','','3e9daeaa-e836-4450-aba7-2072477991ba', '0000000002', 'note', 'organisatie', false, 'dut', true, null, null);
-- partij identificatie of type KVK nummer - note that the objecttype needs to be 'niet natuurlijk persoon'
INSERT INTO klantinteracties_partijidentificator (id, uuid, andere_partij_identificator, partij_identificator_code_objecttype, partij_identificator_code_soort_object_id, partij_identificator_object_id, partij_identificator_code_register, partij_id, sub_identificator_van_id)
    VALUES (2, 'c370a54b-6a96-41ef-8cd2-00e4120abb22', '', 'niet_natuurlijk_persoon', 'kvk_nummer', '12345678', '', 2, null);
-- partij of type organisatie - vestiging
INSERT INTO klantinteracties_partij (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, uuid, nummer, interne_notitie, soort_partij, indicatie_geheimhouding, voorkeurstaal, indicatie_actief, voorkeurs_digitaal_adres_id, voorkeurs_rekeningnummer_id)
    VALUES (3, '', '', '', '', '','', '', '','','', '', '', '', '','','', '','','495da47a-8fe6-4a24-9d1c-46262f009a4b', '0000000003', 'note', 'organisatie', false, 'dut', true, null, null);
-- partij identificatie of type vestigingsnummer - note that the objecttype needs to be 'vestiging' and is a subtype of the KVK nummer partij identificator
INSERT INTO klantinteracties_partijidentificator (id, uuid, andere_partij_identificator, partij_identificator_code_objecttype, partij_identificator_code_soort_object_id, partij_identificator_object_id, partij_identificator_code_register, partij_id, sub_identificator_van_id)
    VALUES (3, 'd549774c-82e0-4a85-8e34-fd0bb868aa42', '', 'vestiging', 'vestigingsnummer', '000012345678', '', 3, 2);

-- add betrokkene (= link to klantcontact) to the persoon partij
INSERT INTO klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (1, '', '', '', '', '','', '','','','', '', '', '', '', '','','','','FoL', 'First', 'of', 'Last', 'b5f9a612-da09-4ad6-8347-d1752e66f77c', 'klant', '', true, 1, 1);
INSERT INTO klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (2, '', '', '', '', '','', '','','','', '', '', '', '', '','','','','NiF', 'Name', 'in', 'Family', 'b5f8a612-da29-3ad6-8347-d1552f63f7ac', 'klant', '', true, 2, 1);
INSERT INTO klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (3, '', '', '', '', '','', '','','','', '', '', '', '','', '','','','HoF', 'Head', 'of', 'Family', 'b5f8a613-da19-3ad6-83c7-d1552e63f6ac', 'klant', '', false, 2, 1);

-- add betrokkene (= link to klantcontact) to the vestiging partij
INSERT INTO klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_straatnaam, bezoekadres_stad, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_straatnaam, correspondentieadres_stad, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (4, '', '', '', '', '','', '','','','', '', '', '', '', '','','','','FoL', 'First', 'of', 'Last', '0ceac9d8-cacc-48ee-a05b-53d97c1a7285', 'klant', '', true, 1, 2);

-- add betrokkene to klantcontact to test product request with request specific email address
INSERT INTO klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, "uuid", rol, organisatienaam, initiator, klantcontact_id, partij_id, bezoekadres_huisnummer, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_stad, bezoekadres_straatnaam, correspondentieadres_huisnummer, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_stad, correspondentieadres_straatnaam)
    VALUES(5, '', '', '', '', '', '', '', '', '', '', '', '', '', '', 'dc4206b8-c789-43b2-97c8-0a0001248fae', 'klant', '', false, 3, NULL, NULL, '', '', '', '', NULL, '', '', '', '');

-- add betrokkene to klantcontact to test product request with request specific email address
INSERT INTO klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, "uuid", rol, organisatienaam, initiator, klantcontact_id, partij_id, bezoekadres_huisnummer, bezoekadres_huisnummertoevoeging, bezoekadres_postcode, bezoekadres_stad, bezoekadres_straatnaam, correspondentieadres_huisnummer, correspondentieadres_huisnummertoevoeging, correspondentieadres_postcode, correspondentieadres_stad, correspondentieadres_straatnaam)
    VALUES(6, '', '', '', '', '', '', '', '', '', '', '', '', '', '', '3e8bc4cc-26a4-4bef-8b85-45f6a94c29a0', 'klant', '', false, 4, NULL, NULL, '', '', '', '', NULL, '', '', '', '');

-- add onderwerpobject to klantcontact to test product request with request specific email address
INSERT INTO klantinteracties_onderwerpobject (id, onderwerpobjectidentificator_object_id, "uuid", klantcontact_id, was_klantcontact_id, onderwerpobjectidentificator_code_objecttype, onderwerpobjectidentificator_code_register, onderwerpobjectidentificator_code_soort_object_id)
    VALUES(1, 'testKenmerkAlternativeEmailAddress1', '6e374d6d-e54d-4c29-ab02-e9da8fed122a', 3, NULL, 'formulierinzending', 'Open Formulieren', 'public_registration_reference');

-- add onderwerpobject to klantcontact to test product request with request specific email address
INSERT INTO klantinteracties_onderwerpobject (id, onderwerpobjectidentificator_object_id, "uuid", klantcontact_id, was_klantcontact_id, onderwerpobjectidentificator_code_objecttype, onderwerpobjectidentificator_code_register, onderwerpobjectidentificator_code_soort_object_id)
    VALUES(2, 'testKenmerkAlternativeEmailAddress2', '4c63e96a-feb6-4df6-922b-9a8585a9cce7', 4, NULL, 'formulierinzending', 'Open Formulieren', 'public_registration_reference');

-- add email and telephone number digital addresses to the persoon partij
INSERT INTO klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, is_standaard_adres, adres, omschrijving, betrokkene_id, partij_id, referentie)
    VALUES (1, 'cda1e8e5-bf0a-49d7-a395-b19e406474b7', 'email', 'true','hendrika.janse@example.com', '', 1, 1, 'referentie1');
INSERT INTO klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, is_standaard_adres, adres, omschrijving, betrokkene_id, partij_id, referentie)
    VALUES (2, '61734758-761c-47eb-bc6b-c46eb7ba629d', 'telefoonnummer', 'true','0612345678', '', 1, 1, 'referentie2');

-- add email and telephone number digital addresses to the vestiging partij
INSERT INTO klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, is_standaard_adres, adres, omschrijving, betrokkene_id, partij_id, referentie)
    VALUES (3, '83d549d8-babb-43aa-a5e6-9bbd1b969440', 'email', 'true','fake.kvk@example.com', '', 2, 2, 'referentie1');
INSERT INTO klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, is_standaard_adres, adres, omschrijving, betrokkene_id, partij_id, referentie)
    VALUES (4, '22a2b1be-238d-4c47-aff4-db9165f43f5f', 'email', 'true','fake.vestiging@example.com', '', 4, 3, 'referentie1');
INSERT INTO klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, is_standaard_adres, adres, omschrijving, betrokkene_id, partij_id, referentie)
    VALUES (5, '0b582f1b-e2c9-4897-8117-744a0e6806ae', 'telefoonnummer', 'true','0201234567', '', 4, 3, 'referentie2');

-- add email digital addresses to betrokkene test product request with request specific email address
INSERT INTO klantinteracties_digitaaladres (id, "uuid", soort_digitaal_adres, adres, omschrijving, betrokkene_id, partij_id, is_standaard_adres, referentie, verificatie_datum)
    VALUES(6, '5dc35a99-11f6-4142-b6ef-8e661b33b704', 'email', 'test-alternative-1@example.com', '', 5, NULL, false, '', NULL);
INSERT INTO klantinteracties_digitaaladres (id, "uuid", soort_digitaal_adres, adres, omschrijving, betrokkene_id, partij_id, is_standaard_adres, referentie, verificatie_datum)
    VALUES(7, '228b39dc-374a-4147-bac1-9ce4abea712e', 'email', 'test-alternative-2@example.com', '', 6, NULL, false, '', NULL);

-- Update the primary key sequences for the tables in which we previously inserted data using fixed primary key values
-- so that new records inserted manually via the Open Klant UI do not conflict with the records we inserted.
ALTER SEQUENCE token_tokenauth_id_seq RESTART WITH 100;
ALTER SEQUENCE accounts_user_id_seq RESTART WITH 100;
ALTER SEQUENCE contactgegevens_persoon_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_actor_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_klantcontact_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_actorklantcontact_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_partijidentificator_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_partij_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_partijidentificator_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_betrokkene_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_onderwerpobject_id_seq RESTART WITH 100;
ALTER SEQUENCE klantinteracties_digitaaladres_id_seq RESTART WITH 100;
