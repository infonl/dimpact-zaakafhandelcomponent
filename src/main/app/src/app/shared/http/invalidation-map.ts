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

const opschorting = (uuid: string) =>
  read("/rest/zaken/zaak/{uuid}/opschorting", { path: { uuid } });

const takenVanZaak = (zaakUUID: OptionalId) =>
  zaakUUID ? [read("/rest/taken/zaak/{zaakUUID}", { path: { zaakUUID } })] : [];

const planItems = (uuid: OptionalId) =>
  uuid
    ? [
        read("/rest/planitems/zaak/{uuid}/humanTaskPlanItems", {
          path: { uuid },
        }),
        read("/rest/planitems/zaak/{uuid}/userEventListenerPlanItems", {
          path: { uuid },
        }),
      ]
    : [];

const zaakProces = (uuid: OptionalId) => [
  ...zaak(uuid),
  ...takenVanZaak(uuid),
  ...planItems(uuid),
];

const documentenVanZaak = (zaakUUID: OptionalId) =>
  zaakUUID
    ? [
        search("/rest/informatieobjecten/informatieobjectenList", { zaakUUID }),
        search("/rest/informatieobjecten/informatieobjectenList", {
          shouldIncludeGekoppeldeZaakDocumenten: true,
        }),
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

const bagObjecten = (zaakUuid: OptionalId) =>
  zaakUuid
    ? [
        read("/rest/bag/zaak/{zaakUuid}", { path: { zaakUuid } }),
        read("/rest/zaken/zaak/{uuid}/historie", { path: { uuid: zaakUuid } }),
      ]
    : [];

const mailtemplates = () => [
  read("/rest/beheer/mailtemplates"),
  read("/rest/beheer/mailtemplates/koppelbaar"),
  read("/rest/mailtemplates/{mailtemplateEnum}/{zaakUUID}"),
];

const mailtemplateKoppelingen = () => [
  read("/rest/beheer/mailtemplatekoppeling"),
  read("/rest/mailtemplates/{mailtemplateEnum}/{zaakUUID}"),
];

const processDefinitions = () => [
  read("/rest/bpmn-process-definitions"),
  read("/rest/zaakafhandelparameters/process-definitions"),
];

const SYSTEM_REFERENTIETABELLEN: Partial<Record<string, () => ReadTarget[]>> = {
  AFZENDER: () => [
    read("/rest/referentietabellen/afzender"),
    read("/rest/zaakafhandelparameters/replyTo"),
  ],
  COMMUNICATIEKANAAL: () => [
    read("/rest/referentietabellen/communicatiekanaal/{inclusiefEFormulier}"),
  ],
  SERVER_ERROR_ERROR_PAGINA_TEKST: () => [
    read("/rest/referentietabellen/server-error-text"),
  ],
  BRP_DOELBINDING_ZOEK_WAARDE: () => [
    read("/rest/referentietabellen/brp-doelbinding-zoek-waarde"),
  ],
  BRP_DOELBINDING_RAADPLEEG_WAARDE: () => [
    read("/rest/referentietabellen/brp-doelbinding-raadpleeg-waarde"),
  ],
  BRP_VERWERKINGSREGISTER_WAARDE: () => [
    read("/rest/referentietabellen/brp-verwerkingregister-waarde"),
  ],
};

const besluitHistorie = (uuid: OptionalId) =>
  uuid ? [read("/rest/zaken/besluit/{uuid}/historie", { path: { uuid } })] : [];

const geautoriseerdeMedewerkers = (uuid: string) => [
  read("/rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers", {
    path: { uuid },
  }),
  read(
    "/rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten",
  ),
  read("/rest/zaken/zaak/{uuid}/historie", { path: { uuid } }),
];

const zaakByPath = {
  refresh: ({ parameters }: { parameters: { path: { uuid: string } } }) =>
    zaak(parameters.path.uuid),
  markStale: zaakLijsten,
};

const zaakProcesByPath = {
  refresh: ({ parameters }: { parameters: { path: { uuid: string } } }) =>
    zaakProces(parameters.path.uuid),
  markStale: zaakLijsten,
};

const opschortingByPath = {
  refresh: ({ parameters }: { parameters: { path: { uuid: string } } }) => [
    ...zaakProces(parameters.path.uuid),
    opschorting(parameters.path.uuid),
  ],
  markStale: zaakLijsten,
};

const betrokkenenByResponse = {
  refresh: ({ response }: { response?: { uuid: string } }) =>
    response
      ? [
          ...zaak(response.uuid),
          read("/rest/zaken/zaak/{uuid}/betrokkene", {
            path: { uuid: response.uuid },
          }),
        ]
      : [],
};

const documentActionByPath = {
  refresh: ({
    parameters,
  }: {
    parameters: { path: { uuid: string }; query?: { zaak?: string | null } };
  }) => documentAction(parameters.path.uuid, parameters.query?.zaak),
  markStale: () => [search("/rest/zoeken/list")],
};

const mailByPath = {
  refresh: ({ parameters }: { parameters: { path: { zaakUuid: string } } }) => [
    read("/rest/zaken/zaak/{uuid}", {
      path: { uuid: parameters.path.zaakUuid },
    }),
    ...documentenVanZaak(parameters.path.zaakUuid),
  ],
};

export const INVALIDATION_MAP: InvalidationMap = {
  "POST /rest/bag": { refresh: ({ body }) => bagObjecten(body?.zaakUuid) },
  "DELETE /rest/bag": { refresh: ({ body }) => bagObjecten(body?.zaakUuid) },
  "PUT /rest/bag/adres": { none: READ_OVER_WRITE_VERB },

  "PUT /rest/beheer/mailtemplatekoppeling": {
    refresh: mailtemplateKoppelingen,
  },
  "DELETE /rest/beheer/mailtemplatekoppeling/{id}": {
    refresh: mailtemplateKoppelingen,
  },
  "POST /rest/beheer/mailtemplates": { refresh: mailtemplates },
  "PUT /rest/beheer/mailtemplates/{id}": {
    refresh: ({ parameters }) => [
      ...mailtemplates(),
      read("/rest/beheer/mailtemplates/{id}", {
        path: { id: parameters.path.id },
      }),
    ],
  },
  "DELETE /rest/beheer/mailtemplates/{id}": { refresh: mailtemplates },

  "POST /rest/bpmn-process-definitions": { refresh: processDefinitions },
  "DELETE /rest/bpmn-process-definitions/{key}": {
    refresh: processDefinitions,
  },
  "POST /rest/bpmn-process-definitions/{key}/forms": {
    refresh: () => [read("/rest/bpmn-process-definitions")],
  },
  "DELETE /rest/bpmn-process-definitions/{key}/forms/{name}": {
    refresh: () => [read("/rest/bpmn-process-definitions")],
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
    refresh: ({ parameters }) => [
      read("/rest/gebruikersvoorkeuren/tabel-gegevens/{werklijst}", {
        path: { werklijst: parameters.path.werklijst },
      }),
    ],
  },
  "PUT /rest/gebruikersvoorkeuren/dasboardcard": { none: RESPONSE_IS_CACHED },
  "DELETE /rest/gebruikersvoorkeuren/dasboardcard": {
    none: RESPONSE_IS_CACHED,
  },
  "PUT /rest/gebruikersvoorkeuren/dasboardcard/actief": {
    none: RESPONSE_IS_CACHED,
  },
  "POST /rest/gebruikersvoorkeuren/zoekopdracht": {
    refresh: () => [read("/rest/gebruikersvoorkeuren/zoekopdracht/{lijstID}")],
  },
  "DELETE /rest/gebruikersvoorkeuren/zoekopdracht/{id}": {
    refresh: () => [read("/rest/gebruikersvoorkeuren/zoekopdracht/{lijstID}")],
  },
  "PUT /rest/gebruikersvoorkeuren/zoekopdracht/actief": {
    none: "the list on screen already shows the choice, and is read on every visit",
  },
  "DELETE /rest/gebruikersvoorkeuren/zoekopdracht/{werklijst}/actief": {
    none: "the list on screen already shows the choice, and is read on every visit",
  },

  "POST /rest/identity/behandelaar-groups": { none: READ_OVER_WRITE_VERB },

  "PUT /rest/inbox-productaanvragen": { none: READ_OVER_WRITE_VERB },
  "DELETE /rest/inbox-productaanvragen/{id}": {
    refresh: () => [search("/rest/inbox-productaanvragen")],
  },
  "PUT /rest/inboxdocumenten": { none: READ_OVER_WRITE_VERB },
  "DELETE /rest/inboxdocumenten/{id}": {
    refresh: () => [search("/rest/inboxdocumenten")],
  },

  "POST /rest/indexeren/commit-pending-changes-to-search-index": {
    markStale: zaakLijsten,
  },

  "POST /rest/informatieobjecten/download/zip": {
    none: "a download changes nothing on the server",
  },
  "POST /rest/informatieobjecten/informatieobject/verplaats": {
    refresh: () => [
      search("/rest/inboxdocumenten"),
      search("/rest/ontkoppeldedocumenten"),
      search("/rest/informatieobjecten/informatieobjectenList"),
      read("/rest/zaken/zaak/{uuid}/historie"),
    ],
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
  "POST /rest/informatieobjecten/informatieobject/{uuid}/lock":
    documentActionByPath,
  "POST /rest/informatieobjecten/informatieobject/{uuid}/onderteken":
    documentActionByPath,
  "POST /rest/informatieobjecten/informatieobject/{uuid}/unlock":
    documentActionByPath,
  "POST /rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}":
    {
      refresh: ({ parameters }) => documentenVanZaak(parameters.path.zaakUuid),
      markStale: () => [search("/rest/zoeken/list")],
    },
  "POST /rest/informatieobjecten/informatieobjecten/verzenden": {
    refresh: ({ body }) => documentenVanZaak(body?.zaakUuid),
  },
  "PUT /rest/informatieobjecten/informatieobjectenList": {
    none: READ_OVER_WRITE_VERB,
  },

  "DELETE /rest/internal/signaleringen/delete-old": { none: NOT_CALLED_HERE },

  "PUT /rest/klanten/bedrijven": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/klanten/contactmomenten": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/klanten/personen": { none: READ_OVER_WRITE_VERB },

  "POST /rest/mail/acknowledge/{zaakUuid}": mailByPath,
  "POST /rest/mail/send/{zaakUuid}": mailByPath,

  "POST /rest/notificaties": { none: NOT_CALLED_HERE },

  "POST /rest/notities": { none: RESPONSE_IS_CACHED },
  "PATCH /rest/notities": { none: RESPONSE_IS_CACHED },
  "DELETE /rest/notities/{id}": {
    none: "the caller removes the note from the cache",
  },

  "PUT /rest/ontkoppeldedocumenten": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/ontkoppeldedocumenten/lijst/koppelen": {
    afterFailureToo: true,
    refresh: () => [
      search("/rest/ontkoppeldedocumenten"),
      search("/rest/informatieobjecten/informatieobjectenList"),
      read("/rest/zaken/zaak/{uuid}/historie"),
    ],
  },
  "PUT /rest/ontkoppeldedocumenten/lijst/verwijderen": {
    afterFailureToo: true,
    refresh: () => [search("/rest/ontkoppeldedocumenten")],
  },
  "DELETE /rest/ontkoppeldedocumenten/{id}": {
    refresh: () => [search("/rest/ontkoppeldedocumenten")],
  },

  "POST /rest/planitems/doHumanTaskPlanItem": {
    refresh: () => [
      read("/rest/zaken/zaak/{uuid}"),
      read("/rest/taken/zaak/{zaakUUID}"),
      read("/rest/planitems/zaak/{uuid}/humanTaskPlanItems"),
      read("/rest/planitems/zaak/{uuid}/userEventListenerPlanItems"),
      read("/rest/zaken/zaak/{uuid}/historie"),
    ],
    markStale: taakLijsten,
  },
  "POST /rest/planitems/doUserEventListenerPlanItem": {
    refresh: ({ body }) => zaakProces(body?.zaakUuid),
    markStale: zaakLijsten,
  },

  "POST /rest/referentietabellen": {
    refresh: () => [read("/rest/referentietabellen")],
  },
  "PUT /rest/referentietabellen/{id}": {
    refresh: ({ parameters, body }) => [
      read("/rest/referentietabellen"),
      read("/rest/referentietabellen/{id}", {
        path: { id: parameters.path.id },
      }),
      read("/rest/referentietabellen/code/{code}"),
      ...(SYSTEM_REFERENTIETABELLEN[body?.code ?? ""]?.() ?? []),
    ],
  },
  "DELETE /rest/referentietabellen/{id}": {
    refresh: () => [read("/rest/referentietabellen")],
    markStale: ({ parameters }) => [
      read("/rest/referentietabellen/{id}", {
        path: { id: parameters.path.id },
      }),
      read("/rest/referentietabellen/code/{code}"),
    ],
  },

  "PUT /rest/signaleringen/group/{groupId}/instellingen": {
    afterFailureToo: true,
    awaitRefresh: true,
    refresh: ({ parameters }) => [
      read("/rest/signaleringen/group/{groupId}/instellingen", {
        path: { groupId: parameters.path.groupId },
      }),
    ],
  },
  "PUT /rest/signaleringen/instellingen": {
    afterFailureToo: true,
    awaitRefresh: true,
    refresh: () => [
      read("/rest/signaleringen/instellingen"),
      read("/rest/signaleringen/typen/dashboard"),
    ],
  },
  "PUT /rest/signaleringen/zaken/{type}": { none: READ_OVER_WRITE_VERB },

  "PATCH /rest/taken/complete": {
    refresh: ({ body }) => [
      ...zaakProces(body?.zaakUuid),
      ...documentenVanZaak(body?.zaakUuid),
    ],
    markStale: taakLijsten,
  },
  "PUT /rest/taken/taakdata": {
    refresh: ({ body }) => takenVanZaak(body?.zaakUuid),
    markStale: taakLijsten,
  },
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
    {
      refresh: ({ parameters }) => [
        read(
          "/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping",
          { path: { zaakafhandelUUID: parameters.path.zaakafhandelUUID } },
        ),
      ],
    },
  "PUT /rest/zaakafhandelparameters/{zaaktypeUUID}": {
    refresh: ({ parameters: { path } }) => [
      read("/rest/zaakafhandelparameters/{zaaktypeUUID}", { path }),
      read("/rest/zaakafhandelparameters"),
      read("/rest/zaakafhandelparameters/zaakbeeindigredenen/{zaaktypeUUID}", {
        path,
      }),
      read("/rest/beheer/mailtemplatekoppeling"),
      read("/rest/mailtemplates/{mailtemplateEnum}/{zaakUUID}"),
      read("/rest/zaken/zaak/{uuid}/afzender"),
      read("/rest/zaaktypen"),
      read("/rest/zaaktypen/{zaaktypeUuid}", {
        path: { zaaktypeUuid: path.zaaktypeUUID },
      }),
    ],
  },

  "POST /rest/zaken/besluit": {
    refresh: ({ body }) => [
      ...zaak(body?.zaakUuid),
      ...documentenVanZaak(body?.zaakUuid),
    ],
  },
  "PUT /rest/zaken/besluit": {
    refresh: ({ body, response }) => [
      ...besluitHistorie(body?.besluitUuid),
      ...zaak(response?.zaakUuid),
    ],
  },
  "PUT /rest/zaken/besluit/intrekken": {
    refresh: ({ body, response }) => [
      ...besluitHistorie(body?.besluitUuid),
      ...zaak(response?.zaakUuid),
    ],
  },
  "POST /rest/zaken/betrokkene": betrokkenenByResponse,
  "DELETE /rest/zaken/betrokkene/{uuid}": betrokkenenByResponse,
  "PUT /rest/zaken/gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken": {
    none: READ_OVER_WRITE_VERB,
  },
  "PATCH /rest/zaken/initiator": {
    refresh: ({ body }) => zaak(body?.zaakUUID),
    markStale: zaakLijsten,
  },
  "DELETE /rest/zaken/{uuid}/initiator": zaakByPath,
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
  "PATCH /rest/zaken/zaak/koppel": {
    refresh: ({ body }) => [
      ...zaak(body?.zaakUuid),
      ...zaak(body?.teKoppelenZaakUuid),
    ],
  },
  "PATCH /rest/zaken/zaak/ontkoppel": {
    refresh: ({ body }) => zaak(body?.zaakUuid),
  },
  "PATCH /rest/zaken/zaak/{uuid}": zaakByPath,
  "PATCH /rest/zaken/zaak/{uuid}/afbreken": zaakProcesByPath,
  "PATCH /rest/zaken/zaak/{uuid}/afsluiten": zaakProcesByPath,
  "PUT /rest/zaken/zaak/{uuid}/brondatum": zaakProcesByPath,
  "PATCH /rest/zaken/zaak/{uuid}/heropenen": zaakProcesByPath,
  "PATCH /rest/zaken/zaak/{uuid}/resume": opschortingByPath,
  "PATCH /rest/zaken/zaak/{uuid}/suspend": opschortingByPath,
  "PATCH /rest/zaken/zaak/{uuid}/verlenging": zaakProcesByPath,
  "POST /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers": {
    refresh: ({ parameters }) =>
      geautoriseerdeMedewerkers(parameters.path.uuid),
  },
  "DELETE /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}":
    {
      refresh: ({ parameters }) =>
        geautoriseerdeMedewerkers(parameters.path.uuid),
    },
  "PUT /rest/zaken/zaakinformatieobjecten/ontkoppel": {
    refresh: ({ body }) => [
      search("/rest/ontkoppeldedocumenten"),
      ...documentenVanZaak(body?.zaakUUID),
    ],
  },
  "PATCH /rest/zaken/{uuid}/zaaklocatie": zaakByPath,

  "PUT /rest/zoeken/list": { none: READ_OVER_WRITE_VERB },
  "PUT /rest/zoeken/zaken": { none: READ_OVER_WRITE_VERB },

  "POST /rest/{job-id}": { none: NOT_CALLED_HERE },
  "DELETE /rest/{job-id}": { none: NOT_CALLED_HERE },
};
