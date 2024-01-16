-- create API Autorisatie - Applicatie
INSERT INTO authorizations_applicatie (id, uuid, client_ids, label, heeft_alle_autorisaties) VALUES (1, 'beb4876d-56ac-448c-aa88-38228eb6d053', '{zac_client}', 'zaakafhandelcomponent', true);

-- create API Autorisatie - Autorisatiegegegevens
INSERT INTO vng_api_common_jwtsecret (id, identifier, secret) VALUES (1, 'zac_client', 'openklantZaakafhandelcomponentClientSecret');

-- create superuser to be able to log in to the UI with username admin and password admin
INSERT INTO accounts_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) VALUES (1, 'pbkdf2_sha256$260000$gtIe19cI1vW9RzIsRDpriC$o8G6cItI5vXqbGFcXuu0pbullajpvMDc6Hze70mf+jE=', null, true, 'admin', '', '', 'admin@example.com', true, true, '2023-08-08 15:14:56.735552 +00:00');

-- add customer contact data which is linked to the test citizen data in the BRP Proxy container based on BSN number '999993896'
INSERT INTO public.klanten_klant (uuid,bronorganisatie,klantnummer,bedrijfsnaam,website_url,voornaam,voorvoegsel_achternaam,achternaam,functie,telefoonnummer,emailadres,subject,subject_type,aanmaakkanaal,geverifieerd) VALUES
    ('6eae1573-0f78-48f1-b6dd-a6df1cb9f7a3','123443210','12345678','Dummy Bedrijf','https://info.nl','Héndrika','','Janse','Test Klant','0612345678','hendrika.janse@example.com','','natuurlijk_persoon','beide',true);
INSERT INTO public.klanten_klantadres (huisnummer,huisletter,huisnummertoevoeging,postcode,woonplaats_naam,straatnaam,landcode,klant_id) VALUES
    (1,'','','1122AA','Amsterdam','StraatNaam','NL',1);
INSERT INTO public.klanten_natuurlijkpersoon (inp_bsn,anp_identificatie,inp_a_nummer,geslachtsnaam,voorvoegsel_geslachtsnaam,voorletters,voornamen,geslachtsaanduiding,geboortedatum,klant_id) VALUES
    ('999993896','','','Janse','','H','Héndrika','v','20-11-1986',1);


