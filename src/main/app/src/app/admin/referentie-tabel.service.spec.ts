/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { fromPartial, runMutationOnSuccess } from "../../test-helpers";
import { UtilService } from "../core/service/util.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { ReferentieTabelService } from "./referentie-tabel.service";

describe(ReferentieTabelService.name, () => {
  let service: ReferentieTabelService;
  let utilService: UtilService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideQueryClient(testQueryClient)],
    });

    service = TestBed.inject(ReferentieTabelService);
    utilService = TestBed.inject(UtilService);
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
      const { mutationOptions } = service.renameReferentieTabel(referenceTable);

      await runMutationOnSuccess(mutationOptions());

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
      const { mutationOptions } =
        service.addReferentieTabelValue(referenceTable);

      await runMutationOnSuccess(mutationOptions("fakeValueName3"));

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
      const { mutationOptions } = service.updateReferentieTabelValue(
        referenceTable,
        referenceTable.values![1],
      );

      await runMutationOnSuccess(mutationOptions("fakeRenamedValue"));

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
      const { mutationOptions } = service.deleteReferentieTabelValue(
        referenceTable,
        referenceTable.values![0],
      );

      await runMutationOnSuccess(mutationOptions);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.referentietabel.waarde-verwijderd",
        { value: "fakeValueName1" },
      );
    });
  });

  describe("deleteReferentieTabel", () => {
    it("names the deleted table in the confirmation and invalidates its queries", async () => {
      const invalidateQueries = jest
        .spyOn(testQueryClient, "invalidateQueries")
        .mockResolvedValue(undefined);
      const referenceTable = fromPartial<GeneratedType<"RestReferenceTable">>({
        id: 42,
        code: "fakeTableCode",
      });

      await runMutationOnSuccess(service.deleteReferentieTabel(referenceTable));

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.tabel.verwijderen.uitgevoerd",
        { tabel: "fakeTableCode" },
      );
      expect(invalidateQueries).toHaveBeenCalled();
    });
  });
});
