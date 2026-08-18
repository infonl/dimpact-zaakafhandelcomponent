/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { FormioSetupService } from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  document1,
  document2,
  documentsFieldset,
  regelLinkColumn,
  selectedUnsignedDocumentsFieldset,
  signedDocument,
  taak,
  unsignedDocumentsFieldset,
} from "./formio-setup-service.test-fixtures";

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;

  beforeEach(() => {
    ({ formioSetupService } = configureFormioSetupServiceTestBed());
  });

  // `testQueryClient` is shared by every test here, and the global `clearAllMocks` leaves
  // spy implementations in place: without this, one test's mocked documents feed the next.
  afterEach(() => jest.restoreAllMocks());

  describe("initializing two tasks", () => {
    const otherTaak: GeneratedType<"RestTask"> = {
      ...taak,
      id: "other-id",
      zaakUuid: "other-zaakUuid",
      taakdata: {},
    };

    it("should give each form the documents and links of its own task, whichever finishes first", async () => {
      const documentsPerZaak: Record<string, (typeof document1)[]> = {
        [taak.zaakUuid]: [document1],
        [otherTaak.zaakUuid]: [document2],
      };
      const resolvers: (() => void)[] = [];
      jest.spyOn(testQueryClient, "fetchQuery").mockImplementation(
        ((options: { queryKey: [string, { zaakUUID: string }] }) =>
          new Promise<(typeof document1)[]>((resolve) =>
            // held back so both setups are in flight at once, then released in reverse order
            resolvers.push(() =>
              resolve(documentsPerZaak[options.queryKey[1].zaakUUID]),
            ),
          )) as typeof testQueryClient.fetchQuery,
      );

      const column: ExtendedComponentSchema = { ...regelLinkColumn };
      const otherColumn: ExtendedComponentSchema = { ...regelLinkColumn };
      const grid: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
        components: [column],
      };
      const otherGrid: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
        components: [otherColumn],
      };

      const initializations = Promise.all([
        formioSetupService.createFormioForm(
          { components: [grid] } as FormioForm,
          taak,
        ),
        formioSetupService.createFormioForm(
          { components: [otherGrid] } as FormioForm,
          otherTaak,
        ),
      ]);
      while (resolvers.length < 2) await Promise.resolve();
      resolvers.reverse().forEach((resolve) => resolve());
      await initializations;

      expect(grid.defaultValue).toEqual([
        { selected: false, titel: document1.titel, uuid: document1.uuid },
      ]);
      expect(otherGrid.defaultValue).toEqual([
        { selected: false, titel: document2.titel, uuid: document2.uuid },
      ]);
      expect(column.attrs).toEqual(
        expect.arrayContaining([
          {
            attr: "href",
            value: `/informatie-objecten/{{ row.uuid }}`,
          },
        ]),
      );
      expect(otherColumn.attrs).toEqual(
        expect.arrayContaining([
          {
            attr: "href",
            value: `/informatie-objecten/{{ row.uuid }}`,
          },
        ]),
      );
    });

    it("should fetch for its own task from a data source called after another task was initialized", async () => {
      const fetchQuerySpy = jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([document1]);
      const component: ExtendedComponentSchema = { ...documentsFieldset };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        taak,
      );
      await formioSetupService.createFormioForm(
        { components: [{ ...documentsFieldset }] } as FormioForm,
        otherTaak,
      );

      // Form.io calls the data source on render and refresh, long after setup
      fetchQuerySpy.mockClear();
      await component.data.custom();

      expect(fetchQuerySpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: [
            "/rest/informatieobjecten/informatieobjectenList",
            { zaakUUID: taak.zaakUuid, informatieobjectUUIDs: undefined },
          ],
        }),
      );
    });
  });

  describe("fetching the zaak documents", () => {
    it("should always refetch, so a cached list cannot offer documents that have since been signed or unlinked", async () => {
      const fetchQuerySpy = jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([document1]);
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        taak,
      );

      expect(fetchQuerySpy).toHaveBeenCalledWith(
        expect.objectContaining({ staleTime: 0 }),
      );
    });
  });

  describe("a finished task", () => {
    const storedRows = [
      {
        selected: true,
        titel: signedDocument.titel,
        uuid: signedDocument.uuid,
      },
      { selected: false, titel: document1.titel, uuid: document1.uuid },
    ];

    const afgerondTaak = (
      taakdata: GeneratedType<"RestTask">["taakdata"],
    ): GeneratedType<"RestTask"> => ({
      ...taak,
      status: "AFGEROND",
      taakdata,
    });

    it("should show the stored rows of the selection grid, ticks and signed documents included", async () => {
      const fetchQuerySpy = jest.spyOn(testQueryClient, "fetchQuery");
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({ ZAAK_Documenten_Ondertekenen_Selectie: storedRows }),
      );

      expect(component.defaultValue).toEqual(storedRows);
      expect(fetchQuerySpy).not.toHaveBeenCalled();
    });

    it("should tick a document this task submitted that carries a signature now", async () => {
      jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([signedDocument, document1]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };
      const finishedTaak = afgerondTaak({
        ZAAK_Documenten_Te_Ondertekenen: [
          {
            selected: true,
            titel: signedDocument.titel,
            uuid: signedDocument.uuid,
          },
          { selected: true, titel: document1.titel, uuid: document1.uuid },
        ],
      });

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        finishedTaak,
      );

      const rows = [
        {
          selected: true,
          titel: signedDocument.titel,
          uuid: signedDocument.uuid,
        },
        { selected: false, titel: document1.titel, uuid: document1.uuid },
      ];
      expect(component.defaultValue).toEqual(rows);
      expect(finishedTaak.taakdata?.ZAAK_Documenten_Te_Ondertekenen).toEqual(
        rows,
      );
    });

    it("should leave a document this task did not submit unticked, however it was signed since", async () => {
      jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([signedDocument]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({
          ZAAK_Documenten_Te_Ondertekenen: [
            {
              selected: false,
              titel: signedDocument.titel,
              uuid: signedDocument.uuid,
            },
          ],
        }),
      );

      expect(component.defaultValue).toEqual([
        {
          selected: false,
          titel: signedDocument.titel,
          uuid: signedDocument.uuid,
        },
      ]);
    });

    it("should show the signing grid with the current titles", async () => {
      jest
        .spyOn(testQueryClient, "fetchQuery")
        .mockResolvedValue([{ ...signedDocument, titel: "Renamed Document" }]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({
          ZAAK_Documenten_Te_Ondertekenen: [
            {
              selected: true,
              titel: "Stale Title",
              uuid: signedDocument.uuid,
            },
          ],
        }),
      );

      expect(component.defaultValue).toEqual([
        {
          selected: true,
          titel: "Renamed Document",
          uuid: signedDocument.uuid,
        },
      ]);
    });

    it("should keep a document that was removed since, under the title it was submitted with", async () => {
      jest.spyOn(testQueryClient, "fetchQuery").mockResolvedValue([]);
      const component: ExtendedComponentSchema = {
        ...selectedUnsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({
          ZAAK_Documenten_Te_Ondertekenen: [
            { selected: true, titel: document1.titel, uuid: document1.uuid },
          ],
        }),
      );

      expect(component.defaultValue).toEqual([
        { selected: false, titel: document1.titel, uuid: document1.uuid },
      ]);
    });

    it("should leave the stored task data untouched", async () => {
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };
      const finishedTaak = afgerondTaak({
        ZAAK_Documenten_Ondertekenen_Selectie: storedRows,
      });

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        finishedTaak,
      );

      expect(
        finishedTaak.taakdata?.ZAAK_Documenten_Ondertekenen_Selectie,
      ).toEqual(storedRows);
    });

    it("should not validate a grid that can no longer be filled in", async () => {
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({ ZAAK_Documenten_Ondertekenen_Selectie: storedRows }),
      );

      expect(component.validate?.custom).toBeUndefined();
    });

    it("should hide an empty grid and explain why", async () => {
      const component: ExtendedComponentSchema = {
        ...unsignedDocumentsFieldset,
      };

      await formioSetupService.createFormioForm(
        { components: [component] } as FormioForm,
        afgerondTaak({ ZAAK_Documenten_Ondertekenen_Selectie: [] }),
      );

      expect(component.description).toBe("msg.geen-documenten-te-ondertekenen");
    });
  });
});
