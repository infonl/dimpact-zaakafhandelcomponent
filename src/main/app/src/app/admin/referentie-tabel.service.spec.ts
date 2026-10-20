/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse, provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import {
  type MutationFunctionContext,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { QUERY_CLIENT } from "../shared/http/query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { ReferentieTabelService } from "./referentie-tabel.service";

describe(ReferentieTabelService.name, () => {
  let service: ReferentieTabelService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
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

  describe("listServerErrorTextsQuery", () => {
    it("neither retries nor opens an error dialog when the server fails, because the texts are shown inside that dialog", async () => {
      const foutAfhandelen = jest.spyOn(
        TestBed.inject(FoutAfhandelingService),
        "foutAfhandelen",
      );

      const result = TestBed.inject(QUERY_CLIENT).fetchQuery(
        service.listServerErrorTextsQuery(),
      );
      await Promise.resolve();
      httpTestingController
        .expectOne("/rest/referentietabellen/server-error-text")
        .flush(null, { status: 500, statusText: "Internal Server Error" });

      await expect(result).rejects.toBeInstanceOf(HttpErrorResponse);
      httpTestingController.verify();
      expect(foutAfhandelen).not.toHaveBeenCalled();
    });
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
