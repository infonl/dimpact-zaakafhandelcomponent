/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { randomBytes } from "node:crypto";
import type { Role } from "../../config/targets/target";
import { withLock } from "./lock";
import { groepNamed, setUpZaaktype } from "./zaakafhandelparameters";
import { dataOf, type Schemas, type ZacApi } from "./zac-api";

const SEARCH_INDEX_TIMEOUT_MILLISECONDS = 60_000;
const STATUS_IN_BEHANDELING = "In behandeling";

export type RememberedZaak = {
  identificatie: string;
  /** The omschrijving that the scenario gave the zaak, before it was made unique. */
  omschrijving?: string;
};

export type NewZaak = {
  zaaktype: string;
  groep: string;
  omschrijving: string;
  initiator?: { bsn: string };
};

export type NewDocument = {
  titel: string;
  /** The omschrijving of the informatieobjecttype, such as "besluit". */
  informatieobjecttype: string;
};

export type NewTaak = {
  /** The name of the human task, such as "Goedkeuren". */
  naam: string;
  groep: string;
  /** The fields of the start form of the task, such as `vraag` for "Goedkeuren". */
  taakdata: Record<string, string>;
};

/**
 * The state of the zaak in the search index of ZAC that a list of zaken filters on: open, afgesloten, or assigned
 * to the behandelaar, as "Mijn zaken" of the behandelaar shows it.
 */
export type ZaakInSearchIndex = "open" | "afgesloten" | "van de behandelaar";

/**
 * The data of one scenario. `Given` steps make data exist through it, and later steps read back what the scenario
 * made. It hides the REST API of ZAC, the roles that may change the data, the lock that keeps workers apart while
 * they change shared data, and the wait for the search index.
 */
export type TestData = {
  /**
   * A text that no other scenario uses, in this run or another: "E2E", the text and a code of the scenario.
   * Scenarios run in parallel against the same target, so give every description of new data a unique text.
   */
  unique(text: string): string;
  /**
   * The groep of the account of the role, as the identity provider of the target has it. Fails the step when the
   * account is not in exactly one groep, because the journeys then cannot tell which groep to use.
   */
  groepOf(role: Role): Promise<Schemas["RestGroup"]>;
  /** Makes sure the zaaktype routes new zaken to the groep. Changes the zaaktype only when it does not yet. */
  ensureZaaktype(
    omschrijving: string,
    settings: { groep: string },
  ): Promise<void>;
  /**
   * Creates a zaak in intake through the REST API as the behandelaar, and remembers it. Sets up its zaaktype first
   * and makes its omschrijving unique.
   */
  createZaak(zaak: NewZaak): Promise<RememberedZaak>;
  /** Finishes the intake of the zaak as ontvankelijk through the REST API as the behandelaar, so it is in behandeling. */
  finishIntake(zaak: RememberedZaak): Promise<void>;
  /** Closes the zaak in behandeling with the resultaat through the REST API as the behandelaar. */
  closeZaak(zaak: RememberedZaak, resultaat: string): Promise<void>;
  /**
   * Adds a small text document to the zaak through the REST API as the behandelaar, and remembers it. Makes its titel
   * unique.
   */
  addDocument(zaak: RememberedZaak, document: NewDocument): Promise<void>;
  /** The unique titel of the document that the scenario added with the titel. Fails the step when it added none. */
  documentTitled(titel: string): string;
  /** Starts the human task of the zaak for the groep, without a behandelaar, through the REST API as the behandelaar. */
  startTaak(zaak: RememberedZaak, taak: NewTaak): Promise<void>;
  /** Remembers a zaak that the scenario created, for example through the UI, so later steps can find it. */
  rememberZaak(zaak: RememberedZaak): void;
  /** The zaak that the scenario created last. Fails the step when the scenario created no zaak yet. */
  readonly zaak: RememberedZaak;
  /** The zaken that the scenario created, the oldest first. */
  readonly zaken: readonly RememberedZaak[];
  /** The zaak that the scenario created with the omschrijving. Fails the step when it created no such zaak. */
  zaakDescribed(omschrijving: string): RememberedZaak;
  /**
   * Waits until the search index of ZAC has the zaak, so lists and werkvoorraden can show it. With a state, waits
   * until the index has the zaak in that state, so the list of afgehandelde zaken shows it or no longer shows it.
   */
  untilZaakIsIndexed(
    identificatie: string,
    state?: ZaakInSearchIndex,
  ): Promise<void>;
  /** Waits until the search index of ZAC has the number of taken with the name of the zaak, so the takenlijsten can show them. */
  untilTaakIsIndexed(
    zaakIdentificatie: string,
    naam: string,
    aantal?: number,
  ): Promise<void>;
};

async function isZaakInSearchIndex(
  api: ZacApi,
  identificatie: string,
  state?: ZaakInSearchIndex,
) {
  const { resultaten } = await dataOf(
    api.PUT("/rest/zoeken/list", {
      body: {
        type: "ZAAK",
        page: 0,
        rows: 10,
        zoeken: { ZAAK_IDENTIFICATIE: identificatie },
        alleenOpenstaandeZaken: state === "open",
        alleenAfgeslotenZaken: state === "afgesloten",
        alleenMijnZaken: state === "van de behandelaar",
      },
    }),
  );
  return (resultaten ?? []).some(
    (zaak) => zaak.identificatie === identificatie,
  );
}

async function taakCountInSearchIndex(
  api: ZacApi,
  zaakIdentificatie: string,
  naam: string,
) {
  const { totaal } = await dataOf(
    api.PUT("/rest/zoeken/list", {
      body: {
        type: "TAAK",
        page: 0,
        rows: 1,
        zoeken: { TAAK_ZAAK_ID: zaakIdentificatie },
        filters: { TAAK_NAAM: { values: [naam] } },
      },
    }),
  );
  return totaal ?? 0;
}

async function onlyGroepOf(api: ZacApi, role: Role) {
  const groupIds =
    (await dataOf(api.GET("/rest/identity/loggedInUser"))).groupIds ?? [];
  if (groupIds.length !== 1) {
    throw new Error(
      `The account of the ${role} is in ${groupIds.length} groepen (${groupIds.join(", ")}), not in exactly one.`,
    );
  }
  const groepen = await dataOf(api.GET("/rest/identity/groups"));
  const groep = groepen.find(({ id }) => id === groupIds[0]);
  if (!groep) {
    throw new Error(
      `ZAC has no groep "${groupIds[0]}", which the account of the ${role} is in.`,
    );
  }
  return groep;
}

/**
 * Open Zaak refuses a second status of a zaak within the same second as the first, so a zaak that was created a moment
 * ago cannot finish its intake yet. Each try reads the zaak again, so a try that did change the status ends the wait.
 */
async function finishIntakeOf(api: ZacApi, identificatie: string) {
  await expect(async () => {
    const zaak = await readZaak(api, identificatie);
    if (zaak.status?.naam === STATUS_IN_BEHANDELING) return;
    const intakeAfronden = await userEventListenerOf(
      api,
      zaak,
      "INTAKE_AFRONDEN",
    );
    await api.POST("/rest/planitems/doUserEventListenerPlanItem", {
      body: {
        zaakUuid: zaak.uuid,
        planItemInstanceId: intakeAfronden.id,
        actie: "INTAKE_AFRONDEN",
        isZaakOntvankelijk: true,
      },
    });
  }).toPass();
}

/** As `finishIntakeOf`, each try reads the zaak again, because Open Zaak can refuse a status within the same second. */
async function closeZaakOf(
  api: ZacApi,
  identificatie: string,
  resultaat: string,
) {
  await expect(async () => {
    const zaak = await readZaak(api, identificatie);
    if (!zaak.isOpen) return;
    const resultaattypes = await dataOf(
      api.GET("/rest/zaken/resultaattypes/{zaaktypeUUID}", {
        params: { path: { zaaktypeUUID: zaak.zaaktype.uuid } },
      }),
    );
    const resultaattype = resultaattypes.find(({ naam }) => naam === resultaat);
    if (!resultaattype) {
      throw new Error(
        `The zaaktype of zaak ${identificatie} has no resultaattype "${resultaat}".`,
      );
    }
    const zaakAfhandelen = await userEventListenerOf(
      api,
      zaak,
      "ZAAK_AFHANDELEN",
    );
    await api.POST("/rest/planitems/doUserEventListenerPlanItem", {
      body: {
        zaakUuid: zaak.uuid,
        planItemInstanceId: zaakAfhandelen.id,
        actie: "ZAAK_AFHANDELEN",
        resultaattypeUuid: resultaattype.id,
      },
    });
  }).toPass();
}

async function readZaak(api: ZacApi, identificatie: string) {
  return dataOf(
    api.GET("/rest/zaken/zaak/id/{identificatie}", {
      params: { path: { identificatie } },
    }),
  );
}

async function userEventListenerOf(
  api: ZacApi,
  zaak: Schemas["RestZaak"],
  actie: Schemas["UserEventListenerActie"],
) {
  const listeners = await dataOf(
    api.GET("/rest/planitems/zaak/{uuid}/userEventListenerPlanItems", {
      params: { path: { uuid: zaak.uuid } },
    }),
  );
  const listener = listeners.find(
    ({ userEventListenerActie }) => userEventListenerActie === actie,
  );
  if (!listener) {
    throw new Error(`The zaak ${zaak.identificatie} has no action ${actie}.`);
  }
  return listener;
}

async function startTaakOf(
  api: ZacApi,
  identificatie: string,
  { naam, groep, taakdata }: NewTaak,
) {
  const zaak = await readZaak(api, identificatie);
  const planItems = await dataOf(
    api.GET("/rest/planitems/zaak/{uuid}/humanTaskPlanItems", {
      params: { path: { uuid: zaak.uuid } },
    }),
  );
  const planItem = planItems.find((candidate) => candidate.naam === naam);
  if (!planItem) {
    throw new Error(
      `The zaak ${identificatie} cannot start the task "${naam}".`,
    );
  }
  const { id, naam: groepNaam } = await groepNamed(api, groep);
  await api.POST("/rest/planitems/doHumanTaskPlanItem", {
    body: {
      planItemInstanceId: planItem.id,
      groep: { id, naam: groepNaam },
      taakdata,
    },
  });
}

async function addDocumentTo(
  api: ZacApi,
  identificatie: string,
  { titel, informatieobjecttype }: NewDocument,
) {
  const zaak = await readZaak(api, identificatie);
  const informatieobjecttypes = await dataOf(
    api.GET("/rest/informatieobjecten/informatieobjecttypes/zaak/{zaakUuid}", {
      params: { path: { zaakUuid: zaak.uuid } },
    }),
  );
  const type = informatieobjecttypes.find(
    ({ omschrijving }) => omschrijving === informatieobjecttype,
  );
  if (!type) {
    throw new Error(
      `The zaak ${identificatie} allows no informatieobjecttype "${informatieobjecttype}".`,
    );
  }
  const inhoud = `${titel}\n`;
  const bestandsnaam = "besluitbrief.txt";
  await dataOf(
    api.POST(
      "/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}",
      {
        params: {
          path: { zaakUuid: zaak.uuid, documentReferenceId: zaak.uuid },
          query: { taakObject: false },
        },
        body: {
          titel,
          bestandsnaam,
          formaat: "text/plain",
          bestandsomvang: Buffer.byteLength(inhoud),
          informatieobjectTypeUUID: type.uuid,
          vertrouwelijkheidaanduiding: "OPENBAAR",
          status: "DEFINITIEF",
          creatiedatum: new Date().toISOString().slice(0, 10),
          auteur: "E2E",
          taal: "dut",
        },
        bodySerializer: (body) => {
          const form = new FormData();
          for (const [name, value] of Object.entries(body)) {
            if (typeof value === "string" || typeof value === "number") {
              form.append(name, String(value));
            }
          }
          form.append(
            "file",
            new Blob([inhoud], { type: "text/plain" }),
            bestandsnaam,
          );
          return form;
        },
      },
    ),
  );
}

/** ZAC accepts a person only by the temporary id that a search in the BRP gives, not by the BSN. */
async function temporaryPersonId(
  api: ZacApi,
  bsn: string,
  zaaktypeUuid: string,
) {
  const { resultaten } = await dataOf(
    api.PUT("/rest/klanten/personen", {
      params: { header: { "X-ZAAKTYPE-UUID": zaaktypeUuid } },
      body: { bsn },
    }),
  );
  const id = resultaten?.find(
    (persoon) => persoon.bsn === bsn,
  )?.temporaryPersonId;
  if (!id) throw new Error(`The BRP of the target has no person ${bsn}.`);
  return id;
}

export function createTestData({
  apiAs,
  locksDirectory,
  scenarioCode = randomBytes(4).toString("hex"),
}: {
  apiAs: (role: Role) => Promise<ZacApi>;
  locksDirectory: string;
  scenarioCode?: string;
}): TestData {
  const zaken: RememberedZaak[] = [];
  const documenten = new Map<string, string>();
  const groepen = new Map<Role, Promise<Schemas["RestGroup"]>>();

  async function ensureZaaktype(omschrijving: string, groep: string) {
    const api = await apiAs("beheerder");
    return withLock(locksDirectory, `zaaktype ${omschrijving}`, () =>
      setUpZaaktype(api, omschrijving, groep),
    );
  }

  return {
    unique: (text) =>
      `${text.startsWith("E2E") ? text : `E2E ${text}`} [${scenarioCode}]`,

    groepOf(role) {
      let groep = groepen.get(role);
      if (!groep) {
        groep = apiAs(role).then((api) => onlyGroepOf(api, role));
        groepen.set(role, groep);
      }
      return groep;
    },

    async ensureZaaktype(omschrijving, { groep }) {
      await ensureZaaktype(omschrijving, groep);
    },

    async createZaak({ zaaktype, groep, omschrijving, initiator }) {
      const { uuid } = await ensureZaaktype(zaaktype, groep);
      const api = await apiAs("behandelaar");
      const { id, naam } = await groepNamed(api, groep);
      const { identificatie } = await dataOf(
        api.POST("/rest/zaken/zaak", {
          body: {
            zaak: {
              zaaktype: { uuid },
              omschrijving: this.unique(omschrijving),
              groep: { id, naam },
              startdatum: new Date().toISOString().slice(0, 10),
              communicatiekanaal: "E-mail",
              vertrouwelijkheidaanduiding: "OPENBAAR",
              initiatorIdentificatie: initiator && {
                type: "BSN",
                temporaryPersonId: await temporaryPersonId(
                  api,
                  initiator.bsn,
                  uuid,
                ),
              },
            },
            bagObjecten: [],
          },
        }),
      );
      const zaak = { identificatie, omschrijving };
      this.rememberZaak(zaak);
      return zaak;
    },

    async finishIntake({ identificatie }) {
      await finishIntakeOf(await apiAs("behandelaar"), identificatie);
    },

    async closeZaak({ identificatie }, resultaat) {
      await closeZaakOf(await apiAs("behandelaar"), identificatie, resultaat);
    },

    async addDocument({ identificatie }, { titel, informatieobjecttype }) {
      const uniqueTitel = this.unique(titel);
      await addDocumentTo(await apiAs("behandelaar"), identificatie, {
        titel: uniqueTitel,
        informatieobjecttype,
      });
      documenten.set(titel, uniqueTitel);
    },

    async startTaak({ identificatie }, taak) {
      await startTaakOf(await apiAs("behandelaar"), identificatie, taak);
    },

    documentTitled(titel) {
      const uniqueTitel = documenten.get(titel);
      if (!uniqueTitel) {
        throw new Error(`This scenario has not added a document "${titel}".`);
      }
      return uniqueTitel;
    },

    rememberZaak(zaak) {
      zaken.push(zaak);
    },

    get zaken() {
      return [...zaken];
    },

    get zaak() {
      const zaak = zaken.at(-1);
      if (!zaak) throw new Error("This scenario has not created a zaak yet.");
      return zaak;
    },

    zaakDescribed(omschrijving) {
      const zaak = zaken.find(
        (candidate) => candidate.omschrijving === omschrijving,
      );
      if (!zaak) {
        throw new Error(
          `This scenario has not created a zaak "${omschrijving}".`,
        );
      }
      return zaak;
    },

    async untilZaakIsIndexed(identificatie, state) {
      const api = await apiAs("behandelaar");
      await expect
        .poll(() => isZaakInSearchIndex(api, identificatie, state), {
          message: `The search index of ZAC has no ${state ? `${state} ` : ""}zaak ${identificatie}.`,
          timeout: SEARCH_INDEX_TIMEOUT_MILLISECONDS,
        })
        .toBe(true);
    },

    async untilTaakIsIndexed(zaakIdentificatie, naam, aantal = 1) {
      const api = await apiAs("behandelaar");
      await expect
        .poll(() => taakCountInSearchIndex(api, zaakIdentificatie, naam), {
          message: `The search index of ZAC has fewer than ${aantal} taken "${naam}" of zaak ${zaakIdentificatie}.`,
          timeout: SEARCH_INDEX_TIMEOUT_MILLISECONDS,
        })
        .toBeGreaterThanOrEqual(aantal);
    },
  };
}
