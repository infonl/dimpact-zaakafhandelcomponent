/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import {
  FormioSetupService,
  KNOWN_ZAC_FIELDS,
  ZAC_FIELD_ATTRIBUTE,
} from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  taak,
} from "./formio-setup-service.test-fixtures";

const vertrouwelijkheidaanduidingComponent: ExtendedComponentSchema = {
  type: "select",
  key: "vertrouwelijkheidaanduiding",
  input: true,
  attributes: {
    [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.VERTROUWELIJKHEIDAANDUIDING,
  },
};

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;

  beforeEach(() => {
    ({ formioSetupService } = configureFormioSetupServiceTestBed());
  });

  afterEach(() => jest.restoreAllMocks());

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeVertrouwelijkheidaanduidingField"
    ].name,
    () => {
      it("should set valueProperty, template and offer all 8 confidentiality levels with translated labels", async () => {
        const translateService = formioSetupService["translateService"] as TranslateService;
        jest
          .spyOn(translateService, "instant")
          .mockImplementation((key) => `translated:${key}`);

        const component: ExtendedComponentSchema = {
          ...vertrouwelijkheidaanduidingComponent,
        };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.valueProperty).toBe("value");
        expect(component.template).toBe("{{ item.label }}");
        expect(component.data.custom()).toEqual([
          {
            value: "OPENBAAR",
            label: "translated:vertrouwelijkheidaanduiding.OPENBAAR",
          },
          {
            value: "BEPERKT_OPENBAAR",
            label: "translated:vertrouwelijkheidaanduiding.BEPERKT_OPENBAAR",
          },
          {
            value: "INTERN",
            label: "translated:vertrouwelijkheidaanduiding.INTERN",
          },
          {
            value: "ZAAKVERTROUWELIJK",
            label: "translated:vertrouwelijkheidaanduiding.ZAAKVERTROUWELIJK",
          },
          {
            value: "VERTROUWELIJK",
            label: "translated:vertrouwelijkheidaanduiding.VERTROUWELIJK",
          },
          {
            value: "CONFIDENTIEEL",
            label: "translated:vertrouwelijkheidaanduiding.CONFIDENTIEEL",
          },
          {
            value: "GEHEIM",
            label: "translated:vertrouwelijkheidaanduiding.GEHEIM",
          },
          {
            value: "ZEER_GEHEIM",
            label: "translated:vertrouwelijkheidaanduiding.ZEER_GEHEIM",
          },
        ]);
      });
    },
  );
});
