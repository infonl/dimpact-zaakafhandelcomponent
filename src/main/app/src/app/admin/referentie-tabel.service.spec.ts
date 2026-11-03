/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ReferentieTabelService } from "./referentie-tabel.service";

describe(ReferentieTabelService.name, () => {
  let service: ReferentieTabelService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(ReferentieTabelService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => {});
  });

  const referenceTable = fromPartial<GeneratedType<"RestReferenceTable">>({
    id: 42,
    code: "fakeTableCode",
    name: "fakeTableName",
    values: [
      { id: 1, name: "fakeValueName1" },
      { id: 2, name: "fakeValueName2" },
    ],
  });

  describe("renameReferentieTabel", () => {
    it("puts the typed name and keeps the values", () => {
      const { body } = service.renameReferentieTabel(referenceTable);

      expect(body("fakeNewTableName")).toEqual({
        code: "fakeTableCode",
        name: "fakeNewTableName",
        values: referenceTable.values,
      });
    });

    it("names the renamed table in the confirmation", async () => {
      const { mutationOptions, body } =
        service.renameReferentieTabel(referenceTable);

      await runMutationOnSuccess(mutationOptions, body("fakeNewTableName"));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.referentietabel.gewijzigd",
        { tabel: "fakeTableCode" },
      );
    });
  });

  describe("addReferentieTabelValue", () => {
    it("appends the typed value to the existing ones", () => {
      const { body } = service.addReferentieTabelValue(referenceTable);

      expect(body("fakeValueName3")).toEqual({
        code: "fakeTableCode",
        name: "fakeTableName",
        values: [...referenceTable.values!, { name: "fakeValueName3" }],
      });
    });

    it("names the added value in the confirmation", async () => {
      const { mutationOptions, body } =
        service.addReferentieTabelValue(referenceTable);

      await runMutationOnSuccess(mutationOptions, body("fakeValueName3"));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.referentietabel.waarde-toegevoegd",
        { value: "fakeValueName3" },
      );
    });
  });

  describe("updateReferentieTabelValue", () => {
    it("renames only the value that was edited", () => {
      const { body } = service.updateReferentieTabelValue(
        referenceTable,
        referenceTable.values![1],
      );

      expect(body("fakeRenamedValue")).toEqual({
        code: "fakeTableCode",
        name: "fakeTableName",
        values: [
          { id: 1, name: "fakeValueName1" },
          { id: 2, name: "fakeRenamedValue" },
        ],
      });
    });

    it("names the edited value in the confirmation", async () => {
      const { mutationOptions, body } = service.updateReferentieTabelValue(
        referenceTable,
        referenceTable.values![1],
      );

      await runMutationOnSuccess(mutationOptions, body("fakeRenamedValue"));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.referentietabel.waarde-gewijzigd",
        { value: "fakeRenamedValue" },
      );
    });
  });

  describe("deleteReferentieTabelValue", () => {
    it("puts the table back without the deleted value", () => {
      const { body } = service.deleteReferentieTabelValue(
        referenceTable,
        referenceTable.values![0],
      );

      expect(body).toEqual({
        code: "fakeTableCode",
        name: "fakeTableName",
        values: [{ id: 2, name: "fakeValueName2" }],
      });
    });

    it("names the deleted value in the confirmation", async () => {
      const { mutationOptions, body } = service.deleteReferentieTabelValue(
        referenceTable,
        referenceTable.values![0],
      );

      await runMutationOnSuccess(mutationOptions, body);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.referentietabel.waarde-verwijderd",
        { value: "fakeValueName1" },
      );
    });
  });

  describe("listCommunicatiekanalen", () => {
    it("reads the communicatiekanalen without the e-formulier by default", async () => {
      const read = testQueryClient.fetchQuery(
        service.listCommunicatiekanalen(),
      );
      httpTestingController
        .expectOne("/rest/referentietabellen/communicatiekanaal/false")
        .flush(["fakeChannel"]);

      expect(await read).toEqual(["fakeChannel"]);
    });

    it("reads the communicatiekanalen with the e-formulier when asked", async () => {
      const read = testQueryClient.fetchQuery(
        service.listCommunicatiekanalen(true),
      );
      httpTestingController
        .expectOne("/rest/referentietabellen/communicatiekanaal/true")
        .flush(["fakeChannel", "E-formulier"]);

      expect(await read).toEqual(["fakeChannel", "E-formulier"]);
    });
  });

  describe("system reference tables", () => {
    const systemReferentieTabelQueryKeys = (): Record<
      string,
      readonly (readonly unknown[])[]
    > => ({
      AFZENDER: [service.listAfzenders().queryKey],
      COMMUNICATIEKANAAL: [
        service.listCommunicatiekanalen(false).queryKey,
        service.listCommunicatiekanalen(true).queryKey,
      ],
      SERVER_ERROR_ERROR_PAGINA_TEKST: [
        service.listServerErrorTexts().queryKey,
      ],
      BRP_DOELBINDING_ZOEK_WAARDE: [service.listBrpSearchValues().queryKey],
      BRP_DOELBINDING_RAADPLEEG_WAARDE: [service.listBrpViewValues().queryKey],
      BRP_VERWERKINGSREGISTER_WAARDE: [
        service.listBrpProcessingValues().queryKey,
      ],
    });

    const seedAllSystemReferentieTabelQueries = () => {
      for (const queryKey of Object.values(
        systemReferentieTabelQueryKeys(),
      ).flat()) {
        testQueryClient.setQueryData(queryKey, ["fakeValueName"]);
      }
    };

    const invalidatedCodes = () =>
      Object.entries(systemReferentieTabelQueryKeys())
        .filter(([, queryKeys]) =>
          queryKeys.some(
            (queryKey) =>
              testQueryClient.getQueryState(queryKey)?.isInvalidated,
          ),
        )
        .map(([code]) => code);

    const renameTable = async (code: string) => {
      const { mutationOptions, body } = service.renameReferentieTabel(
        fromPartial<GeneratedType<"RestReferenceTable">>({
          ...referenceTable,
          code,
        }),
      );
      await runMutationOnSuccess(mutationOptions, body("fakeNewTableName"));
    };

    it.each([
      "AFZENDER",
      "COMMUNICATIEKANAAL",
      "SERVER_ERROR_ERROR_PAGINA_TEKST",
      "BRP_DOELBINDING_ZOEK_WAARDE",
      "BRP_DOELBINDING_RAADPLEEG_WAARDE",
      "BRP_VERWERKINGSREGISTER_WAARDE",
    ])(
      "invalidates only the values of the %s table when that table changes",
      async (code) => {
        seedAllSystemReferentieTabelQueries();

        await renameTable(code);

        expect(invalidatedCodes()).toEqual([code]);
      },
    );

    it("invalidates both communicatiekanalen reads when the COMMUNICATIEKANAAL table changes", async () => {
      seedAllSystemReferentieTabelQueries();

      await renameTable("COMMUNICATIEKANAAL");

      for (const queryKey of systemReferentieTabelQueryKeys()
        .COMMUNICATIEKANAAL) {
        expect(testQueryClient.getQueryState(queryKey)?.isInvalidated).toBe(
          true,
        );
      }
    });

    it("keeps the system reference table values when another table changes", async () => {
      seedAllSystemReferentieTabelQueries();

      await renameTable("fakeTableCode");

      expect(invalidatedCodes()).toEqual([]);
    });
  });

  describe("deleteReferentieTabel", () => {
    it("addresses the table by its id", async () => {
      const referenceTable = fromPartial<GeneratedType<"RestReferenceTable">>({
        id: 42,
      });

      const request = service.deleteReferentieTabel().mutationFn!(
        referenceTable,
        fromPartial<MutationFunctionContext>({}),
      );
      httpTestingController
        .expectOne("/rest/referentietabellen/42")
        .flush(null);

      await request;
    });

    it("names the deleted table in the confirmation and invalidates its queries", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);
      const referenceTable = fromPartial<GeneratedType<"RestReferenceTable">>({
        id: 42,
        code: "fakeTableCode",
      });

      await runMutationOnSuccess(
        service.deleteReferentieTabel(),
        referenceTable,
      );

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.tabel.verwijderen.uitgevoerd",
        { tabel: "fakeTableCode" },
      );
      expect(invalidateQueries).toHaveBeenCalled();
    });
  });
});
