/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { FormioSetupService } from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  document1,
  document2,
  documentsFieldset,
  signedDocument,
  taak,
} from "./formio-setup-service.test-fixtures";

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;

  beforeEach(() => {
    ({ formioSetupService } = configureFormioSetupServiceTestBed());
  });

  // `testQueryClient` is shared by every test here, and the global `clearAllMocks` leaves
  // spy implementations in place: without this, one test's mocked documents feed the next.
  afterEach(() => jest.restoreAllMocks());

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeDocumentsField"
    ].name,
    () => {
      it("should set valueProperty, template and a custom data source for a select component", async () => {
        const fetchQuerySpy = jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1, document2]);

        const component: ExtendedComponentSchema = { ...documentsFieldset };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        expect(component.valueProperty).toBe("uuid");
        expect(component.template).toBe("{{ item.titel }}");
        await expect(component.data.custom()).resolves.toEqual([
          document1,
          document2,
        ]);
        expect(fetchQuerySpy).toHaveBeenCalledWith(
          expect.objectContaining({
            queryKey: ["availableDocumentsQuery", taak.zaakUuid, undefined],
          }),
        );
      });

      it("should not filter out already-signed documents", async () => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1, signedDocument]);

        const component: ExtendedComponentSchema = { ...documentsFieldset };
        await formioSetupService.createFormioForm(
          { components: [component] } as FormioForm,
          taak,
        );

        await expect(component.data.custom()).resolves.toEqual([
          document1,
          signedDocument,
        ]);
      });
    },
  );
});
