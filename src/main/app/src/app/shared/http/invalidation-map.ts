/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  type InvalidationMap,
  read,
  type ReadTarget,
  search,
} from "./invalidation";

const READ_OVER_WRITE_VERB = "reads, with its filters in a request body";
const NOT_CLASSIFIED_YET =
  "not classified yet, its service or component invalidates by hand";
const NOT_CALLED_HERE = "called by another system, not by the frontend";
const RESPONSE_IS_CACHED = "the caller puts the response in the cache";

/** The API types an id as optional in some bodies, and a rule must not fail a write that succeeded. */
type OptionalId = string | null | undefined;

const zaak = (uuid: OptionalId) =>
  uuid
    ? [
        read("/rest/zaken/zaak/{uuid}", { path: { uuid } }),
        read("/rest/zaken/zaak/{uuid}/historie", { path: { uuid } }),
      ]
    : [];

const takenVanZaak = (zaakUUID: OptionalId) =>
  zaakUUID ? [read("/rest/taken/zaak/{zaakUUID}", { path: { zaakUUID } })] : [];

const documentenVanZaak = (zaakUUID: OptionalId) =>
  zaakUUID
    ? [
        search("/rest/informatieobjecten/informatieobjectenList", { zaakUUID }),
        read("/rest/zaken/zaak/{uuid}/historie", { path: { uuid: zaakUUID } }),
      ]
    : [];

const document = (uuid: string) => [
  read("/rest/informatieobjecten/informatieobject/{uuid}", { path: { uuid } }),
  read("/rest/informatieobjecten/informatieobject/{uuid}/huidigeversie", {
    path: { uuid },
  }),
  read("/rest/informatieobjecten/informatieobject/{uuid}/historie", {
    path: { uuid },
  }),
  read(
    "/rest/informatieobjecten/informatieobject/{uuid}/zaakinformatieobjecten",
    { path: { uuid } },
  ),
];

const documentAction = (
  uuid: string,
  zaakUUID: OptionalId,
): readonly ReadTarget[] => [
  ...document(uuid),
  ...(zaakUUID
    ? documentenVanZaak(zaakUUID)
    : [search("/rest/informatieobjecten/informatieobjectenList")]),
];

const zaakLijsten = () => [
  search("/rest/zoeken/list"),
  search("/rest/signaleringen/zaken/{type}"),
  read("/rest/zaken/waarschuwing"),
];

const taakLijsten = () => [
  search("/rest/zoeken/list"),
  read("/rest/signaleringen/taken/{type}"),
];

const mailtemplateKoppelingen = () => [
  read("/rest/beheer/mailtemplatekoppeling"),
  read("/rest/mailtemplates/{mailtemplateEnum}/{zaakUUID}"),
];

const documentActionByPath = {
  refresh: ({
    parameters,
  }: {
    parameters: { path: { uuid: string }; query?: { zaak?: string | null } };
  }) => documentAction(parameters.path.uuid, parameters.query?.zaak),
};

export const INVALIDATION_MAP: InvalidationMap = {
  "POST /rest/bag": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/bag": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/bag/adres": { none: READ_OVER_WRITE_VERB },

  "PUT /rest/beheer/mailtemplatekoppeling": {
    refresh: mailtemplateKoppelingen,
  },
  "DELETE /rest/beheer/mailtemplatekoppeling/{id}": {
    refresh: mailtemplateKoppelingen,
  },
  "POST /rest/beheer/mailtemplates": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/beheer/mailtemplates/{id}": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/beheer/mailtemplates/{id}": { none: NOT_CLASSIFIED_YET },

  "POST /rest/bpmn-process-definitions": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/bpmn-process-definitions/{key}": { none: NOT_CLASSIFIED_YET },
  "POST /rest/bpmn-process-definitions/{key}/forms": {
    none: NOT_CLASSIFIED_YET,
  },
  "DELETE /rest/bpmn-process-definitions/{key}/forms/{name}": {
    none: NOT_CLASSIFIED_YET,
  },

  "POST /rest/csv/export": { none: READ_OVER_WRITE_VERB },

  "POST /rest/document-creation/create-document-attended": {
    none: "opens the SmartDocuments wizard, which creates the document later",
  },
  "POST /rest/document-creation/smartdocuments/callback/zaak/{zaakUuid}": {
    none: NOT_CALLED_HERE,
  },
  "POST /rest/document-creation/smartdocuments/callback/zaak/{zaakUuid}/task/{taskId}":
    { none: NOT_CALLED_HERE },

  "PUT /rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}": {
    none: NOT_CLASSIFIED_YET,
  },
  "PUT /rest/gebruikersvoorkeuren/dasboardcard": { none: RESPONSE_IS_CACHED },
  "DELETE /rest/gebruikersvoorkeuren/dasboardcard": {
    none: RESPONSE_IS_CACHED,
  },
  "PUT /rest/gebruikersvoorkeuren/dasboardcard/actief": {
    none: RESPONSE_IS_CACHED,
  },
  "POST /rest/gebruikersvoorkeuren/zoekopdracht": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/gebruikersvoorkeuren/zoekopdracht/{id}": {
    none: NOT_CLASSIFIED_YET,
  },
  "PUT /rest/gebruikersvoorkeuren/zoekopdracht/actief": {
    none: "the list on screen already shows the choice, and is read on every visit",
  },
  "DELETE /rest/gebruikersvoorkeuren/zoekopdracht/{werklijst}/actief": {
    none: "the list on screen already shows the choice, and is read on every visit",
  },

  "POST /rest/identity/behandelaar-groups": { none: READ_OVER_WRITE_VERB },

  "PUT /rest/inbox-productaanvragen": { none: READ_OVER_WRITE_VERB },
  "DELETE /rest/inbox-productaanvragen/{id}": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/inboxdocumenten": { none: READ_OVER_WRITE_VERB },
  "DELETE /rest/inboxdocumenten/{id}": { none: NOT_CLASSIFIED_YET },

  "POST /rest/indexeren/commit-pending-changes-to-search-index": {
    markStale: zaakLijsten,
  },

  "POST /rest/informatieobjecten/download/zip": {
    none: "a download changes nothing on the server",
  },
  "POST /rest/informatieobjecten/informatieobject/verplaats": {
    none: NOT_CLASSIFIED_YET,
  },
  "PUT /rest/informatieobjecten/informatieobject/{uuid}": documentActionByPath,
  "DELETE /rest/informatieobjecten/informatieobject/{uuid}": {
    refresh: ({ body }) => [
      search("/rest/inboxdocumenten"),
      search("/rest/ontkoppeldedocumenten"),
      ...documentenVanZaak(body?.zaakUuid),
    ],
    markStale: () => [search("/rest/zoeken/list")],
  },
  "POST /rest/informatieobjecten/informatieobject/{uuid}/convert":
    documentActionByPath,
  "POST /rest/informatieobjecten/informatieobject/{uuid}/lock": {
    none: NOT_CLASSIFIED_YET,
  },
  "POST /rest/informatieobjecten/informatieobject/{uuid}/onderteken":
    documentActionByPath,
  "POST /rest/informatieobjecten/informatieobject/{uuid}/unlock": {
    none: NOT_CLASSIFIED_YET,
  },
  "POST /rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}":
    {
      refresh: ({ parameters }) => documentenVanZaak(parameters.path.zaakUuid),
      markStale: () => [search("/rest/zoeken/list")],
    },
  "POST /rest/informatieobjecten/informatieobjecten/verzenden": {
    none: NOT_CLASSIFIED_YET,
  },
  "PUT /rest/informatieobjecten/informatieobjectenList": {
    none: READ_OVER_WRITE_VERB,
  },

  "DELETE /rest/internal/signaleringen/delete-old": { none: NOT_CALLED_HERE },

  "PUT /rest/klanten/bedrijven": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/klanten/contactmomenten": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/klanten/personen": { none: READ_OVER_WRITE_VERB },

  "POST /rest/mail/acknowledge/{zaakUuid}": { none: NOT_CLASSIFIED_YET },
  "POST /rest/mail/send/{zaakUuid}": { none: NOT_CLASSIFIED_YET },

  "POST /rest/notificaties": { none: NOT_CALLED_HERE },

  "POST /rest/notities": { none: RESPONSE_IS_CACHED },
  "PATCH /rest/notities": { none: RESPONSE_IS_CACHED },
  "DELETE /rest/notities/{id}": {
    none: "the caller removes the note from the cache",
  },

  "PUT /rest/ontkoppeldedocumenten": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/ontkoppeldedocumenten/lijst/koppelen": {
    none: NOT_CLASSIFIED_YET,
  },
  "PUT /rest/ontkoppeldedocumenten/lijst/verwijderen": {
    none: NOT_CLASSIFIED_YET,
  },
  "DELETE /rest/ontkoppeldedocumenten/{id}": { none: NOT_CLASSIFIED_YET },

  "POST /rest/planitems/doHumanTaskPlanItem": { none: NOT_CLASSIFIED_YET },
  "POST /rest/planitems/doUserEventListenerPlanItem": {
    none: NOT_CLASSIFIED_YET,
  },

  "POST /rest/referentietabellen": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/referentietabellen/{id}": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/referentietabellen/{id}": { none: NOT_CLASSIFIED_YET },

  "PUT /rest/signaleringen/group/{groupId}/instellingen": {
    none: NOT_CLASSIFIED_YET,
  },
  "PUT /rest/signaleringen/instellingen": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/signaleringen/zaken/{type}": { none: READ_OVER_WRITE_VERB },

  "PATCH /rest/taken/complete": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/taken/taakdata": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/taken/toekennen": {
    refresh: ({ body }) => takenVanZaak(body?.zaakUuid),
    markStale: taakLijsten,
  },
  "PATCH /rest/taken/toekennen/mij": {
    markStale: ({ body }) => [
      ...takenVanZaak(body?.zaakUuid),
      ...taakLijsten(),
    ],
  },
  "PATCH /rest/taken/lijst/toekennen/mij": {
    refresh: ({ body }) => takenVanZaak(body?.zaakUuid),
    markStale: taakLijsten,
  },
  "PUT /rest/taken/lijst/verdelen": { markStale: taakLijsten },
  "PUT /rest/taken/lijst/vrijgeven": { markStale: taakLijsten },
  "POST /rest/taken/upload/{uuid}/{field}": {
    none: "keeps the file for the form that is open, until the taak is saved",
  },

  "PUT /rest/zaakafhandelparameters/smartdocuments-group-template-names": {
    none: READ_OVER_WRITE_VERB,
  },
  "PUT /rest/zaakafhandelparameters/smartdocuments-template-group": {
    none: READ_OVER_WRITE_VERB,
  },
  "POST /rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping":
    { none: NOT_CLASSIFIED_YET },
  "PUT /rest/zaakafhandelparameters/{zaaktypeUUID}": {
    none: NOT_CLASSIFIED_YET,
  },

  "POST /rest/zaken/besluit": {
    refresh: ({ body }) => [
      ...zaak(body?.zaakUuid),
      ...documentenVanZaak(body?.zaakUuid),
    ],
  },
  "PUT /rest/zaken/besluit": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/zaken/besluit/intrekken": { none: NOT_CLASSIFIED_YET },
  "POST /rest/zaken/betrokkene": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/zaken/betrokkene/{uuid}": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken": {
    none: READ_OVER_WRITE_VERB,
  },
  "PATCH /rest/zaken/initiator": { none: NOT_CLASSIFIED_YET },
  "DELETE /rest/zaken/{uuid}/initiator": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/zaken/lijst/toekennen/mij": {
    refresh: ({ body }) => zaak(body?.zaakUUID),
    markStale: zaakLijsten,
  },
  "PUT /rest/zaken/lijst/verdelen": { markStale: zaakLijsten },
  "PUT /rest/zaken/lijst/vrijgeven": { markStale: zaakLijsten },
  "PATCH /rest/zaken/toekennen": {
    refresh: ({ body }) => zaak(body?.zaakUUID),
    markStale: zaakLijsten,
  },
  "PUT /rest/zaken/toekennen/mij": {
    refresh: ({ body }) => zaak(body?.zaakUUID),
    markStale: zaakLijsten,
  },
  "POST /rest/zaken/zaak": {
    refresh: ({ body }) => zaak(body?.koppeling?.zaakUuid),
    markStale: () => [...zaakLijsten(), search("/rest/inbox-productaanvragen")],
  },
  "PATCH /rest/zaken/zaak/koppel": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/ontkoppel": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}/afbreken": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}/afsluiten": { none: NOT_CLASSIFIED_YET },
  "PUT /rest/zaken/zaak/{uuid}/brondatum": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}/heropenen": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}/resume": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}/suspend": { none: NOT_CLASSIFIED_YET },
  "PATCH /rest/zaken/zaak/{uuid}/verlenging": { none: NOT_CLASSIFIED_YET },
  "POST /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers": {
    none: NOT_CLASSIFIED_YET,
  },
  "DELETE /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}":
    { none: NOT_CLASSIFIED_YET },
  "PUT /rest/zaken/zaakinformatieobjecten/ontkoppel": {
    none: NOT_CLASSIFIED_YET,
  },
  "PATCH /rest/zaken/{uuid}/zaaklocatie": { none: NOT_CLASSIFIED_YET },

  "PUT /rest/zoeken/list": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/zoeken/zaken": { none: READ_OVER_WRITE_VERB },

  "POST /rest/{job-id}": { none: NOT_CALLED_HERE },
  "DELETE /rest/{job-id}": { none: NOT_CALLED_HERE },
};
