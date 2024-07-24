-- create API Autorisatie - Applicatie
INSERT INTO authorizations_applicatie (id, uuid, client_ids, label, heeft_alle_autorisaties) VALUES (1, 'beb4876d-56ac-448c-aa88-38228eb6d053', '{zac_client}', 'zaakafhandelcomponent', true);

-- create API Autorisatie - Autorisatiegegegevens
INSERT INTO vng_api_common_jwtsecret (id, identifier, secret) VALUES (1, 'zac_client', 'openklantZaakafhandelcomponentClientSecret');

-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');

-- add customer contact data
INSERT INTO public.klanten_klant (uuid,bronorganisatie,klantnummer,bedrijfsnaam,website_url,voornaam,voorvoegsel_achternaam,achternaam,functie,telefoonnummer,emailadres,subject,subject_type,aanmaakkanaal,geverifieerd) VALUES
    ('6eae1573-0f78-48f1-b6dd-a6df1cb9f7a3','123443210','12345678','Dummy Bedrijf','https://info.nl','Héndrika','','Janse','Test Klant','0612345678','hendrika.janse@example.com','','natuurlijk_persoon','beide',true);
INSERT INTO public.klanten_klantadres (huisnummer,huisletter,huisnummertoevoeging,postcode,woonplaats_naam,straatnaam,landcode,klant_id) VALUES
    (1,'','','1122AA','Amsterdam','StraatNaam','NL',1);
-- link this customer data to a natuurlijk persoon with BSN number '999993896'
-- so it can be linked to the citizen data in the BRP Proxy container based on this BSN number
INSERT INTO public.klanten_natuurlijkpersoon (inp_bsn,anp_identificatie,inp_a_nummer,geslachtsnaam,voorvoegsel_geslachtsnaam,voorletters,voornamen,geslachtsaanduiding,geboortedatum,klant_id) VALUES
    ('999993896','','','Janse','','H','Héndrika','v','20-11-1986',1);
-- also link this customer data to a vestiging wiht vestigings number '000012345678`
-- so it can be linked to the company data in the KKK Wiremock container based on this vestigings number
INSERT INTO public.klanten_vestiging (id, vestigings_nummer, handelsnaam, klant_id) VALUES(1, '000012345678', '{dummyVestingsHandelsnaam1}', 1);

-- insert a few contactmomenten
INSERT INTO public.contactmomenten_contactmoment (id, uuid, bronorganisatie, registratiedatum, tekst, voorkeurskanaal, voorkeurstaal, kanaal, initiatiefnemer, medewerker, onderwerp_links, vorig_contactmoment_id) VALUES(1, '38fb9b69-1064-457c-8abc-6a898b86d1bb'::uuid, '123443210', '2000-01-01 12:00:00.000', 'dummyContactMoment1', 'dummyContactMomentPreferredCommunicationChannel1', 'nld', 'dummyContactMomentCommunicationChannel1', 'klant', '', '{}', NULL);
INSERT INTO public.contactmomenten_contactmoment (id, uuid, bronorganisatie, registratiedatum, tekst, voorkeurskanaal, voorkeurstaal, kanaal, initiatiefnemer, medewerker, onderwerp_links, vorig_contactmoment_id) VALUES(2, '6a295904-9fd1-4ee8-8f41-a1b14f4f0c55'::uuid, '123443210', '2010-01-01 12:00:00.000', 'dummyContactMoment2', 'dummyContactMomentPreferredCommunicationChannel2', 'nld', 'dummyContactMomentCommunicationChannel2', 'klant', '', '{}', 1);

-- insert a klant contactmomenten for the klant created earlier
-- note that the URL reference to the klant needs to be the 'external' URL and not the Docker network internal one (localhost:8002 instead of openklant:8000)
INSERT INTO public.contactmomenten_klantcontactmoment (id, uuid, klant, rol, contactmoment_id, gelezen) VALUES(1, 'c59a980f-e3bd-4567-808b-4789a3f6516e'::uuid, 'http://localhost:8002/klanten/api/v1/klanten/6eae1573-0f78-48f1-b6dd-a6df1cb9f7a3', 'belanghebbende', 1, false);
INSERT INTO public.contactmomenten_klantcontactmoment (id, uuid, klant, rol, contactmoment_id, gelezen) VALUES(2, '26cff460-7390-4be3-b370-31436eb04ae0'::uuid, 'http://localhost:8002/klanten/api/v1/klanten/6eae1573-0f78-48f1-b6dd-a6df1cb9f7a3', 'gesprekspartner', 1, true);

