/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { ExtendedComponentSchema } from "@formio/angular";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { testQueryClient } from "../../../../../setupJest";
import { ZaakafhandelParametersService } from "../../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../../core/service/util.service";
import { IdentityService } from "../../../identity/identity.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  FormioSetupService,
  KNOWN_ZAC_FIELDS,
  ZAC_FIELD_ATTRIBUTE,
} from "./formio-setup-service";

export const groepComponent: ExtendedComponentSchema = {
  type: "select",
  key: "AM_TeamBehandelaar_Groep",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.GROEP,
  },
};

export const medewerkerComponent: ExtendedComponentSchema = {
  type: "select",
  key: "AM_TeamBehandelaar_Medewerker",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.MEDEWERKER,
  },
};

export const smartDocumentsTemplateGroupsComponent: ExtendedComponentSchema = {
  type: "select",
  key: "Fake_Smart_Documents_Template_Groups",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUPS,
  },
};

export const smartDocumentsTemplateGroupTemplatesComponent: ExtendedComponentSchema =
  {
    type: "select",
    key: "SD_SmartDocuments_TemplateGroupTemplates",
    input: true,
    refreshOn: smartDocumentsTemplateGroupsComponent.key,
    attributes: {
      [ZAC_FIELD_ATTRIBUTE]:
        KNOWN_ZAC_FIELDS.SMART_DOCUMENTS_TEMPLATE_GROUP_TEMPLATES,
    },
  };

export const documentsFieldset: ExtendedComponentSchema = {
  type: "select",
  key: "ZAAK_Documents_Select",
  input: true,
  multiple: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN,
  },
};

export const unsignedDocumentsFieldset: ExtendedComponentSchema = {
  type: "datagrid",
  key: "ZAAK_Documenten_Ondertekenen_Selectie",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN_NIET_ONDERTEKEND,
  },
};

export const selectedUnsignedDocumentsFieldset: ExtendedComponentSchema = {
  type: "datagrid",
  key: "ZAAK_Documenten_Te_Ondertekenen",
  input: true,
  refreshOn: "ZAAK_Documenten_Ondertekenen_Selectie",
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.GEKOZEN_DOCUMENTEN_NIET_ONDERTEKEND,
  },
};

export const regelLinkColumn: ExtendedComponentSchema = {
  type: "htmlelement",
  key: "openen",
  input: false,
  tableView: false,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REGEL_LINK,
  },
};

export const regelLinkViewIconColumn: ExtendedComponentSchema = {
  type: "htmlelement",
  key: "openen",
  input: false,
  tableView: false,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON,
  },
};

export const referenceTableFieldset: ExtendedComponentSchema = {
  type: "select",
  key: "RT_ReferenceTable_Values",
  input: true,
  properties: {
    ReferenceTable_Code: "COMMUNICATIEKANAAL",
  },
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REFERENTIE_TABEL,
  },
};

export function gegevensInputComponent(
  veld: string,
  {
    zacType = KNOWN_ZAC_FIELDS.ZAAK_GEGEVENS,
    formaat,
    key = "IN_Seeded",
  }: { zacType?: KNOWN_ZAC_FIELDS; formaat?: string; key?: string } = {},
): ExtendedComponentSchema {
  return {
    type: "textfield",
    key,
    label: "Seeded",
    input: true,
    properties: {
      ZAC_VELD: veld,
      ZAC_INVOER: "true",
      ...(formaat ? { ZAC_FORMAAT: formaat } : {}),
    },
    attributes: { [ZAC_FIELD_ATTRIBUTE]: zacType },
  };
}

export function taakGegevensComponent(
  veld: string,
  options?: { label?: string; formaat?: string },
): ExtendedComponentSchema {
  return {
    ...zaakGegevensComponent(veld, options),
    key: "TG_Taakveld",
    attributes: { [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.TAAK_GEGEVENS },
  };
}

export function zaakGegevensComponent(
  veld: string,
  { label = "Zaakveld", formaat }: { label?: string; formaat?: string } = {},
): ExtendedComponentSchema {
  return {
    type: "textfield",
    key: "ZO_Zaakveld",
    label,
    input: true,
    properties: formaat
      ? { ZAC_VELD: veld, ZAC_FORMAAT: formaat }
      : { ZAC_VELD: veld },
    attributes: {
      [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.ZAAK_GEGEVENS,
    },
  };
}

export const document1 = { uuid: "doc-1", titel: "Document One" };
export const document2 = { uuid: "doc-2", titel: "Document Two" };
export const signedDocument = {
  uuid: "doc-3",
  titel: "Document Three",
  ondertekening: { soort: "Digitaal", datum: "2026-01-01" },
};

export const zaak = {
  uuid: "test-zaakUuid",
  identificatie: "ZAAK-2026-0000000835",
  omschrijving: "test-zaak-omschrijving",
  startdatum: "2026-08-24",
  communicatiekanaal: "Medewerkersportaal",
  isOpgeschort: false,
  isOpen: true,
  kenmerken: [
    { kenmerk: "fakeKenmerk1", bron: "fakeBron1" },
    { kenmerk: "fakeKenmerk2", bron: "fakeBron2" },
  ],
  indicaties: ["OPSCHORTING", "VERLENGD"],
  besluiten: [],
  groep: { id: "fakeGroupId", naam: "fakeGroupName" },
  behandelaar: { id: "fakeUserId", naam: "fakeUserName" },
  status: { naam: "In behandeling" },
  zaaktype: {
    uuid: "test-zaaktype-uuid",
    omschrijving: "test-zaaktypeOmschrijving",
  },
} as unknown as GeneratedType<"RestZaak">;

export const taak: GeneratedType<"RestTask"> = {
  id: "test-id",
  zaakUuid: "test-zaakUuid",
  zaaktypeUUID: "test-zaaktype-uuid",
  behandelaar: undefined,
  groep: undefined,
  naam: "test-taak",
  fataledatum: new Date().toISOString(),
  creatiedatumTijd: new Date().toISOString(),
  formioFormulier: {},
  rechten: {
    lezen: true,
    toekennen: true,
    wijzigen: true,
    toevoegenDocument: true,
  },
  status: "TOEGEKEND",
  taakdata: {},
  formulierDefinitieId: "test-formulierDefinitieId",
  tabellen: {},
  taakdocumenten: [],
  taakinformatie: {},
  toelichting: undefined,
  toekenningsdatumTijd: new Date().toISOString(),
  zaaktypeOmschrijving: "test-zaaktypeOmschrijving",
  zaakIdentificatie: "test-zaakIdentificatie",
};

export function configureFormioSetupServiceTestBed() {
  TestBed.configureTestingModule({
    imports: [
      MatSidenav,
      RouterModule.forRoot([]),
      TranslateModule.forRoot(),
      NoopAnimationsModule,
    ],
    providers: [
      UtilService,
      IdentityService,
      ZaakafhandelParametersService,
      FormioSetupService,
      QueryClient,
      {
        provide: ActivatedRoute,
        useValue: { data: of({ taak }) },
      },
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideQueryClient(testQueryClient),
    ],
  }).compileComponents();

  return {
    formioSetupService: TestBed.inject(FormioSetupService),
    utilService: TestBed.inject(UtilService),
  };
}
