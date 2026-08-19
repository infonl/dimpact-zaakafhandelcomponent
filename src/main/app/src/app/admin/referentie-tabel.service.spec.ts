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

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
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
