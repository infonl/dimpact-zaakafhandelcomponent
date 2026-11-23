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

export type RememberedZaak = { identificatie: string };

export type NewZaak = {
  zaaktype: string;
  groep: string;
  omschrijving: string;
  initiator?: { bsn: string };
};

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
  /** Remembers a zaak that the scenario created, for example through the UI, so later steps can find it. */
  rememberZaak(zaak: RememberedZaak): void;
  /** The zaak that the scenario created last. Fails the step when the scenario created no zaak yet. */
  readonly zaak: RememberedZaak;
  /** Waits until the search index of ZAC has the zaak, so lists and werkvoorraden can show it. */
  untilZaakIsIndexed(identificatie: string): Promise<void>;
};

async function isZaakInSearchIndex(api: ZacApi, identificatie: string) {
  const { resultaten } = await dataOf(
    api.PUT("/rest/zoeken/list", {
      body: {
        type: "ZAAK",
        page: 0,
        rows: 10,
        zoeken: { ZAAK_IDENTIFICATIE: identificatie },
      },
    }),
  );
  return (resultaten ?? []).some(
    (zaak) => zaak.identificatie === identificatie,
  );
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
    const zaak = await dataOf(
      api.GET("/rest/zaken/zaak/id/{identificatie}", {
        params: { path: { identificatie } },
      }),
    );
    if (zaak.status?.naam === STATUS_IN_BEHANDELING) return;
    const listeners = await dataOf(
      api.GET("/rest/planitems/zaak/{uuid}/userEventListenerPlanItems", {
        params: { path: { uuid: zaak.uuid } },
      }),
    );
    const intakeAfronden = listeners.find(
      ({ userEventListenerActie }) =>
        userEventListenerActie === "INTAKE_AFRONDEN",
    );
    if (!intakeAfronden) {
      throw new Error(`The zaak ${identificatie} cannot finish its intake.`);
    }
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
      const zaak = { identificatie };
      this.rememberZaak(zaak);
      return zaak;
    },

    async finishIntake({ identificatie }) {
      await finishIntakeOf(await apiAs("behandelaar"), identificatie);
    },

    rememberZaak(zaak) {
      zaken.push(zaak);
    },

    get zaak() {
      const zaak = zaken.at(-1);
      if (!zaak) throw new Error("This scenario has not created a zaak yet.");
      return zaak;
    },

    async untilZaakIsIndexed(identificatie) {
      const api = await apiAs("behandelaar");
      await expect
        .poll(() => isZaakInSearchIndex(api, identificatie), {
          message: `The search index of ZAC has no zaak ${identificatie}.`,
          timeout: SEARCH_INDEX_TIMEOUT_MILLISECONDS,
        })
        .toBe(true);
    },
  };
}
