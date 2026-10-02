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
        const translateService = formioSetupService[
          "translateService"
        ] as TranslateService;
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
            label: "translated:vertrouwelijkheidaanduiding.openbaar",
          },
          {
            value: "BEPERKT_OPENBAAR",
            label: "translated:vertrouwelijkheidaanduiding.beperkt-openbaar",
          },
          {
            value: "INTERN",
            label: "translated:vertrouwelijkheidaanduiding.intern",
          },
          {
            value: "ZAAKVERTROUWELIJK",
            label: "translated:vertrouwelijkheidaanduiding.zaakvertrouwelijk",
          },
          {
            value: "VERTROUWELIJK",
            label: "translated:vertrouwelijkheidaanduiding.vertrouwelijk",
          },
          {
            value: "CONFIDENTIEEL",
            label: "translated:vertrouwelijkheidaanduiding.confidentieel",
          },
          {
            value: "GEHEIM",
            label: "translated:vertrouwelijkheidaanduiding.geheim",
          },
          {
            value: "ZEER_GEHEIM",
            label: "translated:vertrouwelijkheidaanduiding.zeer-geheim",
          },
        ]);
      });
    },
  );
});
