/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { APIRequestContext } from "@playwright/test";
import { getJson, putJson } from "./zac-api";

const CMMN_CASE_DEFINITION_KEY = "generiek-zaakafhandelmodel";
const NIET_ONTVANKELIJK_RESULTAATTYPE = "Geweigerd";
const STATUS_MAIL_OFF = "BESCHIKBAAR_UIT";
const REFERENTIETABELLEN_OF_HUMAN_TASK: Record<string, string[]> = {
  ADVIES_INTERN: ["ADVIES"],
};

type Zaaktype = { uuid: string; omschrijving: string; isNuGeldig: boolean };
type Groep = { id: string; naam: string };
type ZaakAfzender = { mail: string; isDefaultMail: boolean; replyTo?: string };
type HumanTaskDefinition = { id: string; defaultFormulierDefinitie: string };
type UserEventListenerDefinition = { id: string; naam: string };
type ProcessDefinition = {
  key: string;
  processEngine: string;
  humanTaskDefinitions: HumanTaskDefinition[];
  userEventListenerDefinitions: UserEventListenerDefinition[];
};
type Zaakafhandelparameters = {
  zaaktype: Zaaktype;
  isValide: boolean;
  defaultGroepId?: string;
  betrokkeneKoppelingen: {
    isBrpKoppelenEnabled: boolean;
    isKvkKoppelenEnabled: boolean;
  };
  zaakAfzenders: ZaakAfzender[];
  intakeMail?: string;
  afrondenMail?: string;
  [setting: string]: unknown;
};

async function groepNamed(api: APIRequestContext, naam: string) {
  const groepen = await getJson<Groep[]>(api, "/rest/identity/groups");
  const groep = groepen.find((candidate) => candidate.naam === naam);
  if (!groep) {
    throw new Error(
      `ZAC has no groep "${naam}". It has: ${groepen.map((candidate) => candidate.naam).join(", ")}.`,
    );
  }
  return groep;
}

async function currentZaaktypeNamed(
  api: APIRequestContext,
  omschrijving: string,
) {
  const all = await getJson<Zaakafhandelparameters[]>(
    api,
    "/rest/zaakafhandelparameters",
  );
  const zaaktypes = all.map((parameters) => parameters.zaaktype);
  const zaaktype = zaaktypes.find(
    (candidate) =>
      candidate.omschrijving === omschrijving && candidate.isNuGeldig,
  );
  if (!zaaktype) {
    const known = [
      ...new Set(zaaktypes.map((candidate) => candidate.omschrijving)),
    ];
    throw new Error(
      `ZAC has no zaaktype "${omschrijving}" that is valid today. It has: ${known.join(", ")}.`,
    );
  }
  return zaaktype;
}

async function cmmnParametersFor(
  api: APIRequestContext,
  zaaktype: Zaaktype,
): Promise<Record<string, unknown>> {
  const processDefinitions = await getJson<ProcessDefinition[]>(
    api,
    "/rest/zaakafhandelparameters/process-definitions",
  );
  const caseDefinition = processDefinitions.find(
    (definition) =>
      definition.processEngine === "CMMN" &&
      definition.key === CMMN_CASE_DEFINITION_KEY,
  );
  if (!caseDefinition) {
    throw new Error(
      `ZAC has no CMMN case definition "${CMMN_CASE_DEFINITION_KEY}".`,
    );
  }

  const resultaattypes = await getJson<{ naam: string }[]>(
    api,
    `/rest/zaakafhandelparameters/resultaattypes/${zaaktype.uuid}`,
  );
  const nietOntvankelijk = resultaattypes.find(
    (resultaattype) => resultaattype.naam === NIET_ONTVANKELIJK_RESULTAATTYPE,
  );
  if (!nietOntvankelijk) {
    throw new Error(
      `The zaaktype "${zaaktype.omschrijving}" has no resultaattype "${NIET_ONTVANKELIJK_RESULTAATTYPE}".`,
    );
  }

  const humanTaskParameters = [];
  for (const humanTask of caseDefinition.humanTaskDefinitions) {
    const referentieTabellen = [];
    for (const code of REFERENTIETABELLEN_OF_HUMAN_TASK[humanTask.id] ?? []) {
      const tabel = await getJson<{ id: number }>(
        api,
        `/rest/referentietabellen/code/${code}`,
      );
      referentieTabellen.push({ veld: code, tabel: { id: tabel.id } });
    }
    humanTaskParameters.push({
      planItemDefinition: humanTask,
      formulierDefinitieId: humanTask.defaultFormulierDefinitie,
      referentieTabellen,
      actief: true,
    });
  }

  return {
    processDefinition: caseDefinition,
    zaakNietOntvankelijkResultaattype: nietOntvankelijk,
    humanTaskParameters,
    userEventListenerParameters: caseDefinition.userEventListenerDefinitions,
  };
}

export async function setUpZaaktype(
  api: APIRequestContext,
  zaaktypeOmschrijving: string,
  groepNaam: string,
) {
  const groep = await groepNamed(api, groepNaam);
  const zaaktype = await currentZaaktypeNamed(api, zaaktypeOmschrijving);
  const path = `/rest/zaakafhandelparameters/${zaaktype.uuid}`;
  const current = await getJson<Zaakafhandelparameters>(api, path);

  const isSetUp =
    current.isValide &&
    current.defaultGroepId === groep.id &&
    current.betrokkeneKoppelingen.isBrpKoppelenEnabled &&
    current.betrokkeneKoppelingen.isKvkKoppelenEnabled;
  if (isSetUp) return;

  await putJson(api, path, {
    ...current,
    ...(current.isValide ? {} : await cmmnParametersFor(api, zaaktype)),
    defaultGroepId: groep.id,
    intakeMail: current.intakeMail ?? STATUS_MAIL_OFF,
    afrondenMail: current.afrondenMail ?? STATUS_MAIL_OFF,
    zaakAfzenders: current.zaakAfzenders.map((afzender) =>
      afzender.mail === "GEMEENTE"
        ? { ...afzender, isDefaultMail: true, replyTo: "GEMEENTE" }
        : afzender,
    ),
    betrokkeneKoppelingen: {
      ...current.betrokkeneKoppelingen,
      isBrpKoppelenEnabled: true,
      isKvkKoppelenEnabled: true,
    },
  });
}
