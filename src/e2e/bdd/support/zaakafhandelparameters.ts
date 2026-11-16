/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { dataOf, type Schemas, type ZacApi } from "./zac-api";

const CMMN_CASE_DEFINITION_KEY = "generiek-zaakafhandelmodel";
const NIET_ONTVANKELIJK_RESULTAATTYPE = "Geweigerd";
const STATUS_MAIL_OFF: Schemas["ZaakafhandelparametersStatusMailOption"] =
  "BESCHIKBAAR_UIT";
const REFERENTIETABELLEN_OF_HUMAN_TASK: Record<string, string[]> = {
  ADVIES_INTERN: ["ADVIES"],
};

type Zaaktype = Schemas["RestZaaktypeOverzicht"];
type ZaaktypeConfiguration = Schemas["RestZaaktypeConfiguration"];

async function groepNamed(api: ZacApi, naam: string) {
  const groepen = await dataOf(api.GET("/rest/identity/groups"));
  const groep = groepen.find((candidate) => candidate.naam === naam);
  if (!groep) {
    throw new Error(
      `ZAC has no groep "${naam}". It has: ${groepen.map((candidate) => candidate.naam).join(", ")}.`,
    );
  }
  return groep;
}

async function currentZaaktypeNamed(api: ZacApi, omschrijving: string) {
  const configurations = await dataOf(api.GET("/rest/zaakafhandelparameters"));
  const zaaktypes = configurations.map(
    (configuration) => configuration.zaaktype,
  );
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
  api: ZacApi,
  zaaktype: Zaaktype,
): Promise<Partial<ZaaktypeConfiguration>> {
  const processDefinitions = await dataOf(
    api.GET("/rest/zaakafhandelparameters/process-definitions"),
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

  const resultaattypes = await dataOf(
    api.GET("/rest/zaakafhandelparameters/resultaattypes/{zaaktypeUUID}", {
      params: { path: { zaaktypeUUID: zaaktype.uuid } },
    }),
  );
  const nietOntvankelijk = resultaattypes.find(
    (resultaattype) => resultaattype.naam === NIET_ONTVANKELIJK_RESULTAATTYPE,
  );
  if (!nietOntvankelijk) {
    throw new Error(
      `The zaaktype "${zaaktype.omschrijving}" has no resultaattype "${NIET_ONTVANKELIJK_RESULTAATTYPE}".`,
    );
  }

  const humanTaskParameters: Schemas["RestHumanTaskParameters"][] = [];
  for (const humanTask of caseDefinition.humanTaskDefinitions ?? []) {
    const referentieTabellen: Schemas["RestHumanTaskReferenceTable"][] = [];
    for (const code of REFERENTIETABELLEN_OF_HUMAN_TASK[humanTask.id] ?? []) {
      const tabel = await dataOf(
        api.GET("/rest/referentietabellen/code/{code}", {
          params: { path: { code } },
        }),
      );
      referentieTabellen.push({ veld: code, tabel });
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
    userEventListenerParameters: (
      caseDefinition.userEventListenerDefinitions ?? []
    ).map(({ id, naam }) => ({ id, naam })),
  };
}

export async function setUpZaaktype(
  api: ZacApi,
  zaaktypeOmschrijving: string,
  groepNaam: string,
) {
  const groep = await groepNamed(api, groepNaam);
  const zaaktype = await currentZaaktypeNamed(api, zaaktypeOmschrijving);
  const path = { zaaktypeUUID: zaaktype.uuid };
  const current = await dataOf(
    api.GET("/rest/zaakafhandelparameters/{zaaktypeUUID}", {
      params: { path },
    }),
  );

  const isSetUp =
    current.isValide &&
    current.defaultGroepId === groep.id &&
    current.betrokkeneKoppelingen?.isBrpKoppelenEnabled &&
    current.betrokkeneKoppelingen?.isKvkKoppelenEnabled;
  if (isSetUp) return;

  await dataOf(
    api.PUT("/rest/zaakafhandelparameters/{zaaktypeUUID}", {
      params: { path },
      body: {
        ...current,
        ...(current.isValide ? {} : await cmmnParametersFor(api, zaaktype)),
        defaultGroepId: groep.id,
        intakeMail: current.intakeMail ?? STATUS_MAIL_OFF,
        afrondenMail: current.afrondenMail ?? STATUS_MAIL_OFF,
        zaakAfzenders: (current.zaakAfzenders ?? []).map((afzender) =>
          afzender.mail === "GEMEENTE"
            ? { ...afzender, isDefaultMail: true, replyTo: "GEMEENTE" }
            : afzender,
        ),
        betrokkeneKoppelingen: {
          ...current.betrokkeneKoppelingen,
          isBrpKoppelenEnabled: true,
          isKvkKoppelenEnabled: true,
        },
      },
    }),
  );
}
