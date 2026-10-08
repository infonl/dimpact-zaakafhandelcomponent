/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFileSync } from "node:fs";
import { join } from "node:path";
import { GeneratedType } from "../shared/utils/generated-types";
import { toI18nKey } from "../shared/utils/i18n-key";
import { ObjectType } from "./websocket/model/object-type";
import { Opcode } from "./websocket/model/opcode";

function readDutchKeys() {
  const file = join(__dirname, "../../assets/i18n/nl.json");
  return new Set(Object.keys(JSON.parse(readFileSync(file, "utf-8"))));
}

function valuesOf<Value extends string>(values: Record<Value, true>) {
  return Object.keys(values) as Value[];
}

const signaleringTypes = valuesOf<GeneratedType<"Type">>({
  ZAAK_DOCUMENT_TOEGEVOEGD: true,
  ZAAK_OP_NAAM: true,
  ZAAK_VERLOPEND: true,
  TAAK_OP_NAAM: true,
  TAAK_VERLOPEN: true,
});

// Only ZAAK_OP_NAAM targets groups (SignaleringType.java), so only it appears in the group signaleringen table.
const groupSignaleringTypes = valuesOf<
  Extract<GeneratedType<"Type">, "ZAAK_OP_NAAM">
>({ ZAAK_OP_NAAM: true });

// No signaleringtype row in the database has subject DOCUMENT, so no signalering setting shows it.
const signaleringSubjects = valuesOf<
  Exclude<GeneratedType<"SignaleringSubject">, "DOCUMENT">
>({ ZAAK: true, TAAK: true });

const mails = valuesOf<GeneratedType<"Mail">>({
  ZAAK_ALGEMEEN: true,
  ZAAK_ONTVANKELIJK: true,
  ZAAK_NIET_ONTVANKELIJK: true,
  ZAAK_AFGEHANDELD: true,
  TAAK_AANVULLENDE_INFORMATIE: true,
  TAAK_ONTVANGSTBEVESTIGING: true,
  TAAK_ADVIES_EXTERN: true,
  SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD: true,
  SIGNALERING_ZAAK_OP_NAAM: true,
  SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM: true,
  SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM: true,
  SIGNALERING_TAAK_OP_NAAM: true,
  SIGNALERING_TAAK_VERLOPEN: true,
});

const mailTemplateVariables = valuesOf<GeneratedType<"MailTemplateVariables">>({
  DOCUMENT_TITEL: true,
  DOCUMENT_LINK: true,
  DOCUMENT_URL: true,
  GEMEENTE: true,
  TAAK_BEHANDELAAR_GROEP: true,
  TAAK_BEHANDELAAR_MEDEWERKER: true,
  TAAK_FATALEDATUM: true,
  TAAK_LINK: true,
  TAAK_URL: true,
  ZAAK_BEHANDELAAR_GROEP: true,
  ZAAK_BEHANDELAAR_MEDEWERKER: true,
  ZAAK_FATALEDATUM: true,
  ZAAK_INITIATOR: true,
  ZAAK_INITIATOR_ADRES: true,
  ZAAK_LINK: true,
  ZAAK_NUMMER: true,
  ZAAK_OMSCHRIJVING: true,
  ZAAK_REGISTRATIEDATUM: true,
  ZAAK_STARTDATUM: true,
  ZAAK_STATUS: true,
  ZAAK_STREEFDATUM: true,
  ZAAK_TOELICHTING: true,
  ZAAK_URL: true,
  ZAAKTYPE_OMSCHRIJVING: true,
  ZAAKTYPE_OMSCHRIJVING_GENERIEK: true,
});

const vertrouwelijkheidaanduidingen = valuesOf<
  GeneratedType<"RestVertrouwelijkheidaanduiding">
>({
  OPENBAAR: true,
  BEPERKT_OPENBAAR: true,
  INTERN: true,
  ZAAKVERTROUWELIJK: true,
  VERTROUWELIJK: true,
  CONFIDENTIEEL: true,
  GEHEIM: true,
  ZEER_GEHEIM: true,
});

const formulierDefinities = valuesOf<GeneratedType<"FormulierDefinitie">>({
  DEFAULT_TAAKFORMULIER: true,
  AANVULLENDE_INFORMATIE: true,
  ADVIES: true,
  EXTERN_ADVIES_VASTLEGGEN: true,
  EXTERN_ADVIES_MAIL: true,
  GOEDKEUREN: true,
  DOCUMENT_VERZENDEN_POST: true,
});

const bagObjectStatuses = valuesOf<
  GeneratedType<"StatusNaamgeving"> | GeneratedType<"StatusWoonplaats">
>({
  NAAMGEVING_UITGEGEVEN: true,
  NAAMGEVING_INGETROKKEN: true,
  WOONPLAATS_AANGEWEZEN: true,
  WOONPLAATS_INGETROKKEN: true,
});

const pandStatuses = valuesOf<GeneratedType<"StatusPand">>({
  BOUWVERGUNNING_VERLEEND: true,
  NIET_GEREALISEERD_PAND: true,
  BOUW_GESTART: true,
  PAND_IN_GEBRUIK_NIET_INGEMETEN_: true,
  PAND_IN_GEBRUIK: true,
  VERBOUWING_PAND: true,
  SLOOPVERGUNNING_VERLEEND: true,
  PAND_GESLOOPT: true,
  PAND_BUITEN_GEBRUIK: true,
  PAND_TEN_ONRECHTE_OPGEVOERD: true,
});

const adresseerbaarObjectTypes = valuesOf<
  GeneratedType<"TypeAdresseerbaarObject">
>({ VERBLIJFSOBJECT: true, STANDPLAATS: true, LIGPLAATS: true });

const bagObjectTypes = valuesOf<GeneratedType<"BagObjectType">>({
  ADRES: true,
  WOONPLAATS: true,
  PAND: true,
  OPENBARE_RUIMTE: true,
  NUMMERAANDUIDING: true,
  ADRESSEERBAAR_OBJECT: true,
});

const dashboardCardIds = valuesOf<GeneratedType<"DashboardCardId">>({
  MIJN_DOCUMENTEN_NIEUW: true,
  MIJN_TAKEN: true,
  MIJN_TAKEN_NIEUW: true,
  MIJN_ZAKEN: true,
  MIJN_ZAKEN_NIEUW: true,
  MIJN_ZAKEN_WAARSCHUWING: true,
});

// The ZGW client serialises EMPTY as "", which the templates skip as falsy.
const informatieobjectStatuses = valuesOf<
  Exclude<GeneratedType<"StatusEnum">, "EMPTY">
>({
  IN_BEWERKING: true,
  TER_VASTSTELLING: true,
  DEFINITIEF: true,
  GEARCHIVEERD: true,
});

// The ZGW client serialises EMPTY as "", which the templates skip as falsy.
const vervalredenen = valuesOf<
  Exclude<GeneratedType<"VervalredenEnum">, "EMPTY">
>({
  TIJDELIJK: true,
  INGETROKKEN_OVERHEID: true,
  INGETROKKEN_BELANGHEBBENDE: true,
});

const historyActions = valuesOf<GeneratedType<"HistoryAction">>({
  AANGEMAAKT: true,
  GEWIJZIGD: true,
  GEKOPPELD: true,
  ONTKOPPELD: true,
});

const zaakIndicaties = valuesOf<GeneratedType<"ZaakIndicatie">>({
  OPSCHORTING: true,
  HEROPEND: true,
  HOOFDZAAK: true,
  DEELZAAK: true,
  VERLENGD: true,
  ONTVANGSTBEVESTIGING_NIET_VERSTUURD: true,
});

const documentIndicaties = valuesOf<GeneratedType<"DocumentIndicatie">>({
  VERGRENDELD: true,
  ONDERTEKEND: true,
  BESLUIT: true,
  GEBRUIKSRECHT: true,
  VERZONDEN: true,
});

const persoonIndicaties = valuesOf<GeneratedType<"RestPersoonIndicaties">>({
  IN_ONDERZOEK: true,
  GEHEIMHOUDING_OP_PERSOONSGEGEVENS: true,
  OVERLEDEN: true,
  MINISTERIELE_REGELING: true,
  EMIGRATIE: true,
  OPSCHORTING_BIJHOUDING: true,
  NIET_INGEZETENE: true,
  ONDER_CURATELE: true,
  BLOKKERING_VANWEGE_VERHUIZING: true,
});

// The CMMN model only defines user event listeners for these; BRONDATUM_ZETTEN is never a plan item.
const userEventListenerActies = valuesOf<
  Exclude<GeneratedType<"UserEventListenerActie">, "BRONDATUM_ZETTEN">
>({ INTAKE_AFRONDEN: true, ZAAK_AFHANDELEN: true });

const zaakLinkRelatieTypes = valuesOf<
  Extract<
    GeneratedType<"RelatieType">,
    "HOOFDZAAK" | "DEELZAAK" | "GERELATEERD"
  >
>({ HOOFDZAAK: true, DEELZAAK: true, GERELATEERD: true });

// Only the document link search sends ZAAKTYPE_DOES_NOT_ALLOW_INFORMATIEOBJECTTYPE, and that screen shows no reason.
const zaakNotLinkableReasons = valuesOf<
  Exclude<
    GeneratedType<"ZaakNotLinkableReason">,
    "ZAAKTYPE_DOES_NOT_ALLOW_INFORMATIEOBJECTTYPE"
  >
>({
  ALREADY_GERELATEERD: true,
  AFGEHANDELD: true,
  OPEN: true,
  IS_DEELZAAK: true,
  ALREADY_DEELZAAK: true,
  HAS_DEELZAKEN: true,
  ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK: true,
  NOT_AUTHORISED_TO_KOPPELEN: true,
});

const afleidingswijzen = valuesOf<GeneratedType<"AfleidingswijzeEnum">>({
  AFGEHANDELD: true,
  ANDER_DATUMKENMERK: true,
  EIGENSCHAP: true,
  GERELATEERDE_ZAAK: true,
  HOOFDZAAK: true,
  INGANGSDATUM_BESLUIT: true,
  TERMIJN: true,
  VERVALDATUM_BESLUIT: true,
  ZAAKOBJECT: true,
});

const taakStatuses = valuesOf<GeneratedType<"TaakStatus">>({
  NIET_TOEGEKEND: true,
  TOEGEKEND: true,
  AFGEROND: true,
});

const zoekObjectTypes = valuesOf<GeneratedType<"ZoekObjectType">>({
  ZAAK: true,
  TAAK: true,
  DOCUMENT: true,
});

const bedrijfTypes = valuesOf<GeneratedType<"BedrijfType">>({
  HOOFDVESTIGING: true,
  NEVENVESTIGING: true,
  RECHTSPERSOON: true,
});

const snackbarScreenEventObjectTypes = [
  ObjectType.ZAAK,
  ObjectType.TAAK,
  ObjectType.ZAAK_ROLLEN,
  ObjectType.ZAAK_BESLUITEN,
];

const snackbarScreenEventOpcodes = [Opcode.UPDATED, Opcode.DELETED];

const DYNAMIC_KEYS = [
  {
    description: "signalering type",
    keys: signaleringTypes.map((type) => toI18nKey(`signalering.type.${type}`)),
  },
  {
    description: "group signalering type",
    keys: groupSignaleringTypes.map((type) =>
      toI18nKey(`signalering.type.${type}.group`),
    ),
  },
  {
    description: "signalering subject type",
    keys: signaleringSubjects.map((subject) =>
      toI18nKey(`signalering.subjecttype.${subject}`),
    ),
  },
  {
    description: "mail",
    keys: mails.map((mail) => toI18nKey(`mail.${mail}`)),
  },
  {
    description: "mail template variable",
    keys: mailTemplateVariables.map((variable) =>
      toI18nKey(`mailtemplate.variabele.${variable}`),
    ),
  },
  {
    description: "vertrouwelijkheidaanduiding",
    keys: vertrouwelijkheidaanduidingen.map((vertrouwelijkheidaanduiding) =>
      toI18nKey(`vertrouwelijkheidaanduiding.${vertrouwelijkheidaanduiding}`),
    ),
  },
  {
    description: "formulier definitie",
    keys: formulierDefinities.map((formulierDefinitie) =>
      toI18nKey(`formulier-definitie.${formulierDefinitie}`),
    ),
  },
  {
    description: "BAG object status",
    keys: bagObjectStatuses.map((status) =>
      toI18nKey(`bag-object.status.${status}`),
    ),
  },
  {
    description: "pand status",
    keys: pandStatuses.map((status) =>
      toI18nKey(`bag-object.pand.status.${status}`),
    ),
  },
  {
    description: "adresseerbaar object type",
    keys: adresseerbaarObjectTypes.map((type) =>
      toI18nKey(`adresseerbaar-object.${type}`),
    ),
  },
  {
    description: "BAG object type",
    keys: bagObjectTypes.map((type) => toI18nKey(`objecttype.${type}`)),
  },
  {
    description: "dashboard card",
    keys: dashboardCardIds.map((id) => toI18nKey(`dashboard.card.${id}`)),
  },
  {
    description: "informatieobject status",
    keys: informatieobjectStatuses.map((status) =>
      toI18nKey(`informatieobject.status.${status}`),
    ),
  },
  {
    description: "besluit vervalreden",
    keys: vervalredenen.map((vervalreden) =>
      toI18nKey(`besluit.vervalreden.${vervalreden}`),
    ),
  },
  {
    description: "history action on a zaak or document",
    keys: historyActions.flatMap((action) => [
      toI18nKey(`action.case.${action}`),
      toI18nKey(`action.document.${action}`),
    ]),
  },
  {
    description: "zaak, document or persoon indicatie",
    keys: [...zaakIndicaties, ...documentIndicaties, ...persoonIndicaties].map(
      (indicatie) => toI18nKey(`indicatie.${indicatie}`),
    ),
  },
  {
    description: "user event listener plan item and its confirmation",
    keys: userEventListenerActies.flatMap((actie) => [
      toI18nKey(`planitem.${actie}`),
      toI18nKey(`msg.planitem.uitgevoerd.${actie}`),
    ]),
  },
  {
    description: "zaak link relation type",
    keys: zaakLinkRelatieTypes.map((relatieType) =>
      toI18nKey(`zaak.koppelen.link.type.${relatieType}`),
    ),
  },
  {
    description: "reason a zaak cannot be linked",
    keys: zaakNotLinkableReasons.map((reason) =>
      toI18nKey(`zaak.koppelen.niet-koppelbaar.${reason}`),
    ),
  },
  {
    description: "afleidingswijze brondatum",
    keys: afleidingswijzen.map((afleidingswijze) =>
      toI18nKey(`afleidingswijze-brondatum.${afleidingswijze}`),
    ),
  },
  {
    description: "taak status",
    keys: taakStatuses.map((status) => toI18nKey(`taak.status.${status}`)),
  },
  {
    description: "zoek object type and its view action",
    keys: zoekObjectTypes.flatMap((type) => [
      toI18nKey(`type.${type}`),
      `actie.${type.toLowerCase()}.bekijken`,
    ]),
  },
  {
    description: "bedrijf type",
    keys: bedrijfTypes.map((type) => toI18nKey(type)),
  },
  {
    description: "changed object type and operation in a screen event",
    keys: [
      ...snackbarScreenEventObjectTypes.map((objectType) =>
        toI18nKey(`msg.gewijzigd.objecttype.${objectType}`),
      ),
      ...snackbarScreenEventOpcodes.map((opcode) =>
        toI18nKey(`msg.gewijzigd.operatie.${opcode}`),
      ),
    ],
  },
];

describe("the translations of values the backend sends", () => {
  const dutchKeys = readDutchKeys();

  it.each(DYNAMIC_KEYS)(
    "should have a translation for every $description the backend can send",
    ({ keys }) => {
      const missingKeys = keys.filter((key) => !dutchKeys.has(key));

      expect(missingKeys).toEqual([]);
    },
  );
});
