-- Script to initialize kanalen and abonnement data only in Open Notificaties database
-- Temporary workaround until Open Notificaties setup configuration files support setting the 'client_id' for abonnementen.
-- Once that is supported in Open Notificaties, we can remove this script and use extend (../../opennotificaties/setup-configuration/data.yaml) to set up the kanalen and abonnement.
-- We need to set up the kanalen here as well because the abonnement references the kanalen.

-- Set up the kanalen
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('493002ad-e5d5-4747-93b2-1853e78889f5', 'zaaktypen', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#zaaktypen', '{catalogus}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('3ad6676c-98cc-4664-babb-02bda0c886d8', 'zaken', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#zaken', '{bronorganisatie,zaaktype,vertrouwelijkheidaanduiding}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('97d7d649-0979-422a-8880-a0aee37cc6ea', 'documenten', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#documenten', '{bronorganisatie,informatieobjecttype,vertrouwelijkheidaanduiding}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('93179f23-965e-4720-964f-d09be3bc2790', 'besluittypen', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#besluittypen', '{catalogus}');
INSERT INTO datamodel_kanaal (uuid, naam, documentatie_link, filters) VALUES('c4f52cb5-07e7-44cb-b4b7-2539bce684f9', 'besluiten', 'http://open-zaak-zac-dev.westeurope.cloudapp.azure.com/ref/kanalen/#besluiten', '{verantwoordelijke_organisatie,besluittype}');

-- Set up the abonnement to ZAC
-- This assumes ZAC is running and available on 'host.docker.internal' and port 8080.
-- Please see our 'testing.md' document on how to set this up.
INSERT INTO datamodel_abonnement (uuid, callback_url, auth, client_id) VALUES('fb4e3474-18c8-474b-94ae-980850ea4a7f', 'http://host.docker.internal:8080/rest/notificaties', 'openNotificatiesApiSecretKey', 'opennotificaties');
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'zaaktypen'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'zaken'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'documenten'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'besluittypen'));
INSERT INTO datamodel_filtergroup (abonnement_id, kanaal_id) VALUES((SELECT ID FROM datamodel_abonnement where client_id = 'opennotificaties'), (SELECT ID FROM datamodel_kanaal where naam = 'besluiten'));
