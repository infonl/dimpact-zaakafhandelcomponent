/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { dataOf, type Schemas, type ZacApi } from "./zac-api";

const CMMN_CASE_DEFINITION_KEY = "generiek-zaakafhandelmodel";
const NIET_ONTVANKELIJK_RESULTAATTYPE = "Geweigerd";
// The kind stack and the test environments have far fewer zaaktypes, so one page holds them all.
const ALL_ZAAKTYPES = 1000;
const STATUS_MAIL_OFF: Schemas["ZaakafhandelparametersStatusMailOption"] =
  "BESCHIKBAAR_UIT";
const REFERENTIETABELLEN_OF_HUMAN_TASK: Record<string, string[]> = {
  ADVIES_INTERN: ["ADVIES"],
};

type Zaaktype = Schemas["RestZaaktypeBeheerOverzicht"];
type ZaaktypeConfiguration = Schemas["RestZaaktypeConfiguration"];

export async function groepNamed(api: ZacApi, naam: string) {
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
  const { resultaten = [] } = await dataOf(
    api.GET("/rest/zaakafhandelparameters", {
      params: { query: { isNuGeldig: true, rows: ALL_ZAAKTYPES } },
    }),
  );
  const zaaktypes = resultaten.map((configuration) => configuration.zaaktype);
  const zaaktype = zaaktypes.find(
    (candidate) => candidate.omschrijving === omschrijving,
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

// No journey finishes an intake as niet ontvankelijk, so any resultaattype of a zaaktype without it will do.
async function nietOntvankelijkResultaattypeOf(
  api: ZacApi,
  zaaktype: Zaaktype,
) {
  const resultaattypes = await dataOf(
    api.GET("/rest/zaakafhandelparameters/resultaattypes/{zaaktypeUUID}", {
      params: { path: { zaaktypeUUID: zaaktype.uuid } },
    }),
  );
  const nietOntvankelijk =
    resultaattypes.find(
      ({ naam }) => naam === NIET_ONTVANKELIJK_RESULTAATTYPE,
    ) ?? resultaattypes.at(0);
  if (!nietOntvankelijk) {
    throw new Error(
      `The zaaktype "${zaaktype.omschrijving}" has no resultaattype.`,
    );
  }
  return nietOntvankelijk;
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
    zaakNietOntvankelijkResultaattype: await nietOntvankelijkResultaattypeOf(
      api,
      zaaktype,
    ),
    humanTaskParameters,
    userEventListenerParameters: (
      caseDefinition.userEventListenerDefinitions ?? []
    ).map(({ id, naam }) => ({ id, naam })),
  };
}

type ZaakAfzender = Schemas["RestZaakAfzender"];
type Groep = Schemas["RestGroup"];

function withGemeenteAsDefaultAfzender(afzenders: ZaakAfzender[]) {
  return afzenders.map((afzender) =>
    afzender.mail === "GEMEENTE"
      ? { ...afzender, isDefaultMail: true, replyTo: "GEMEENTE" }
      : afzender,
  );
}

function hasBrpDoelbindingen({ brpDoelbindingen }: ZaaktypeConfiguration) {
  return Boolean(
    brpDoelbindingen?.zoekWaarde &&
      brpDoelbindingen.raadpleegWaarde &&
      brpDoelbindingen.verwerkingregisterWaarde,
  );
}

/** The first value that ZAC offers for each BRP doelbinding. The BRP of the journeys accepts every value. */
async function brpDoelbindingenOf(api: ZacApi) {
  const [zoekWaarde] = await dataOf(
    api.GET("/rest/referentietabellen/brp-doelbinding-zoek-waarde"),
  );
  const [raadpleegWaarde] = await dataOf(
    api.GET("/rest/referentietabellen/brp-doelbinding-raadpleeg-waarde"),
  );
  const [verwerkingregisterWaarde] = await dataOf(
    api.GET("/rest/referentietabellen/brp-verwerkingregister-waarde"),
  );
  if (!zoekWaarde || !raadpleegWaarde || !verwerkingregisterWaarde) {
    throw new Error(
      "ZAC has no value for a BRP doelbinding in its referentietabellen.",
    );
  }
  return { zoekWaarde, raadpleegWaarde, verwerkingregisterWaarde };
}

/**
 * Whether the zaakafhandelparameters already are what the journeys expect: valid, routing new zaken to the groep,
 * with the BRP and KVK koppelingen on, every human task on, no automatic mail at the intake and at the end, and the gemeente as default
 * afzender. When ZAC sets the BRP doelbindingen per zaaktype, they also have these, because a beheerder cannot save
 * the zaakafhandelparameters without them.
 */
export function isSetUpFor(
  current: ZaaktypeConfiguration,
  groep: Pick<Groep, "id">,
  { isBrpDoelbindingPerZaaktype }: { isBrpDoelbindingPerZaaktype: boolean },
) {
  const gemeente = current.zaakAfzenders?.find(
    (afzender) => afzender.mail === "GEMEENTE",
  );
  return (
    (!isBrpDoelbindingPerZaaktype || hasBrpDoelbindingen(current)) &&
    current.isValide === true &&
    current.defaultGroepId === groep.id &&
    current.betrokkeneKoppelingen?.isBrpKoppelenEnabled === true &&
    current.betrokkeneKoppelingen?.isKvkKoppelenEnabled === true &&
    (current.humanTaskParameters ?? []).every(({ actief }) => actief) &&
    current.intakeMail === STATUS_MAIL_OFF &&
    current.afrondenMail === STATUS_MAIL_OFF &&
    (!gemeente ||
      (gemeente.isDefaultMail === true && gemeente.replyTo === "GEMEENTE"))
  );
}

/**
 * Sets up the zaakafhandelparameters of the zaaktype so that it routes new zaken to the groep, as `isSetUpFor`
 * describes. It changes nothing when they already are, so every scenario can call it. The caller keeps other workers
 * from setting up the same zaaktype at the same time.
 */
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
  const isBrpDoelbindingPerZaaktype = await dataOf(
    api.GET("/rest/configuratie/brp/doelbinding-setup-enabled"),
  );
  if (isSetUpFor(current, groep, { isBrpDoelbindingPerZaaktype })) {
    return zaaktype;
  }

  const { humanTaskParameters, ...otherParameters } = {
    ...current,
    ...(current.isValide ? {} : await cmmnParametersFor(api, zaaktype)),
  };
  await dataOf(
    api.PUT("/rest/zaakafhandelparameters/{zaaktypeUUID}", {
      params: { path },
      body: {
        ...otherParameters,
        humanTaskParameters: humanTaskParameters?.map((parameters) => ({
          ...parameters,
          actief: true,
        })),
        defaultGroepId: groep.id,
        intakeMail: STATUS_MAIL_OFF,
        afrondenMail: STATUS_MAIL_OFF,
        zaakAfzenders: withGemeenteAsDefaultAfzender(
          current.zaakAfzenders ?? [],
        ),
        betrokkeneKoppelingen: {
          ...current.betrokkeneKoppelingen,
          isBrpKoppelenEnabled: true,
          isKvkKoppelenEnabled: true,
        },
        ...(isBrpDoelbindingPerZaaktype && !hasBrpDoelbindingen(current)
          ? { brpDoelbindingen: await brpDoelbindingenOf(api) }
          : {}),
      },
    }),
  );
  return zaaktype;
}

/** Makes the BPMN process handle the zaaktype, and routes its new zaken to the groep. Changes nothing when it does. */
export async function setUpBpmnZaaktype(
  api: ZacApi,
  zaaktypeOmschrijving: string,
  processKey: string,
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
  if (
    current.processDefinition?.key === processKey &&
    current.defaultGroepId === groep.id
  ) {
    return;
  }

  await dataOf(
    api.PUT("/rest/zaakafhandelparameters/{zaaktypeUUID}", {
      params: { path },
      body: {
        ...current,
        processDefinition: { processEngine: "BPMN", key: processKey },
        defaultGroepId: groep.id,
        zaakNietOntvankelijkResultaattype:
          current.zaakNietOntvankelijkResultaattype ??
          (await nietOntvankelijkResultaattypeOf(api, zaaktype)),
      },
    }),
  );
}
