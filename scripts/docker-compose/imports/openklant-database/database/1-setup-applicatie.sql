-- create API Auth - Token authorizations
INSERT INTO token_tokenauth (id, token, contact_person, email, organization, last_modified, created, application, administration) VALUES (1, 'dummyToken', 'OpenKlant Admin', 'openklant-admin@example.com', 'dummyOrganization', '2024-07-31 10:06:52.146060 +00:00', '2024-07-31 10:06:52.146060 +00:00', 'zaakafhandelcomponent', 'admin');

-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');

-- add model data
INSERT INTO public.contactgegevens_persoon (id, adres_adresregel1, adres_adresregel2, adres_adresregel3, adres_land, uuid, geboortedatum, overlijdensdatum, geslachtsnaam, geslacht, voorvoegsel, voornamen, land, adres_nummeraanduiding_id)
    VALUES (1, '', '', '', '', 'e93fc3bf-0b6d-4447-9e5c-de049ebdff09', '2000-07-31', '2124-07-31', 'Janse', 'f', '', 'Héndrika', '', '');
INSERT INTO public.klantinteracties_actor (id, actoridentificator_object_id, uuid, naam, soort_actor, indicatie_actief, actoridentificator_code_objecttype, actoridentificator_code_register, actoridentificator_code_soort_object_id)
    VALUES (1, '', 'f7340d48-a76e-472b-8866-f8d8512afe5a', 'Actor Name', 'medewerker', true, '', '', '');
INSERT INTO public.klantinteracties_klantcontact (id, uuid, nummer, kanaal, onderwerp, inhoud, indicatie_contact_gelukt, taal, vertrouwelijk, plaatsgevonden_op)
    VALUES (1, '21e55404-226d-42b6-aeb0-f28b7c7d09dc', '0000000001', 'email', 'email contact', 'email', true, 'dut', false, '2000-01-01 12:00:00.000000 +00:00');
INSERT INTO public.klantinteracties_klantcontact (id, uuid, nummer, kanaal, onderwerp, inhoud, indicatie_contact_gelukt, taal, vertrouwelijk, plaatsgevonden_op)
    VALUES (2, '21e54405-246c-44b6-aec2-f25b7c7d02db', '0000000002', 'telefoon', 'phone contact', 'telefoon', true, 'dut', false, '2010-01-01 12:00:00.000000 +00:00');
INSERT INTO public.klantinteracties_actorklantcontact (id, uuid, actor_id, klantcontact_id)
    VALUES (1, '63567628-b257-46d3-95c6-1ed76427c526', 1, 1);
INSERT INTO public.klantinteracties_partij (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, uuid, nummer, interne_notitie, soort_partij, indicatie_geheimhouding, voorkeurstaal, indicatie_actief, voorkeurs_digitaal_adres_id, voorkeurs_rekeningnummer_id)
    VALUES (1, '', '', '', '', '', '', '', '', '', '', 'a5e1cd61-f5a2-43f9-9644-805f9bddf1ad', '0000000001', 'note', 'persoon', false, 'dut', true, null, null);
INSERT INTO public.klantinteracties_partijidentificator (id, uuid, andere_partij_identificator, partij_identificator_code_objecttype, partij_identificator_code_soort_object_id, partij_identificator_object_id, partij_identificator_code_register, partij_id)
    VALUES (1, '4b7f454f-e836-4d4d-9f37-df98e473616b', '', 'INGESCHREVEN NATUURLIJK PERSOON', 'Burgerservicenummer', '999993896', 'BRP', 1);
INSERT INTO public.klantinteracties_partij (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, uuid, nummer, interne_notitie, soort_partij, indicatie_geheimhouding, voorkeurstaal, indicatie_actief, voorkeurs_digitaal_adres_id, voorkeurs_rekeningnummer_id)
    VALUES (2, '', '', '', '', '', '', '', '', '', '', '3e9daeaa-e836-4450-aba7-2072477991ba', '0000000002', 'note', 'organisatie', false, 'dut', true, null, null);
INSERT INTO public.klantinteracties_partijidentificator (id, uuid, andere_partij_identificator, partij_identificator_code_objecttype, partij_identificator_code_soort_object_id, partij_identificator_object_id, partij_identificator_code_register, partij_id)
    VALUES (2, 'ea241812-bcb1-4854-b814-6a32c0f1d2f5', '', 'KVK-NUMMER ONDERNEMING', 'Vestigingsnummer', '000012345678', 'KvK', 1);
INSERT INTO public.klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (1, '', '', '', '', '', '', '', '', '', '', 'FoL', 'First', 'of', 'Last', 'b5f9a612-da09-4ad6-8347-d1752e66f77c', 'klant', '', true, 1, 1);
INSERT INTO public.klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (2, '', '', '', '', '', '', '', '', '', '', 'NiF', 'Name', 'in', 'Family', 'b5f8a612-da29-3ad6-8347-d1552f63f7ac', 'klant', '', true, 2, 1);
INSERT INTO public.klantinteracties_betrokkene (id, bezoekadres_nummeraanduiding_id, bezoekadres_adresregel1, bezoekadres_adresregel2, bezoekadres_adresregel3, bezoekadres_land, correspondentieadres_nummeraanduiding_id, correspondentieadres_adresregel1, correspondentieadres_adresregel2, correspondentieadres_adresregel3, correspondentieadres_land, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, uuid, rol, organisatienaam, initiator, klantcontact_id, partij_id)
    VALUES (3, '', '', '', '', '', '', '', '', '', '', 'HoF', 'Head', 'of', 'Family', 'b5f8a613-da19-3ad6-83c7-d1552e63f6ac', 'klant', '', false, 2, 1);
INSERT INTO public.klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, adres, omschrijving, betrokkene_id, partij_id)
    VALUES (1, 'cda1e8e5-bf0a-49d7-a395-b19e406474b7', 'email', 'hendrika.janse@example.com', 'email address', 1, 1);
INSERT INTO public.klantinteracties_digitaaladres (id, uuid, soort_digitaal_adres, adres, omschrijving, betrokkene_id, partij_id)
    VALUES (2, '61734758-761c-47eb-bc6b-c46eb7ba629d', 'telefoon', '0612345678', 'phone number', 1, 1);
INSERT INTO public.klantinteracties_persoon (id, contactnaam_voorletters, contactnaam_voornaam, contactnaam_voorvoegsel_achternaam, contactnaam_achternaam, partij_id)
    VALUES (1, 'FtL', 'First', 'to', 'Last', 1);
