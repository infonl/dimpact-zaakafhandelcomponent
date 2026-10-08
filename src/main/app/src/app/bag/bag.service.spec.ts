/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { firstValueFrom } from "rxjs";
import { sleep, testQueryClient } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { runMutation } from "../shared/http/run-mutation";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "../zaken/zaken.service";
import { BAGService } from "./bag.service";

describe(BAGService.name, () => {
  let service: BAGService;
  let zakenService: ZakenService;
  let httpTestingController: HttpTestingController;

  const flushDelete = async () => {
    await new Promise(requestAnimationFrame);
    httpTestingController.expectOne("/rest/bag").flush(null);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
      ],
    });

    service = TestBed.inject(BAGService);
    zakenService = TestBed.inject(ZakenService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("create", () => {
    const zaakobject = fromPartial<GeneratedType<"RestBagObject">>({
      omschrijving: "fakeOmschrijving",
    });

    it("posts the bag object to couple to the zaak", async () => {
      runMutation(testQueryClient, service.create(), {
        zaakUuid: "fakeZaakUuid",
        zaakobject,
      }).subscribe();
      await sleep();

      const request = httpTestingController.expectOne("/rest/bag");
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual({
        zaakUuid: "fakeZaakUuid",
        zaakobject,
      });
      request.flush(null);
    });

    it("invalidates the historie of the zaak the bag object is coupled to, so every caller gets a fresh one", async () => {
      const invalidateHistorie = jest.spyOn(zakenService, "invalidateHistorie");

      const created = firstValueFrom(
        runMutation(testQueryClient, service.create(), {
          zaakUuid: "fakeZaakUuid",
          zaakobject,
        }),
      );
      await sleep();
      httpTestingController.expectOne("/rest/bag").flush(null);
      await created;

      expect(invalidateHistorie).toHaveBeenCalledWith("fakeZaakUuid");
    });

    it("invalidates no historie when the request carries no zaak uuid, which the endpoint types allow", async () => {
      const invalidateHistorie = jest.spyOn(zakenService, "invalidateHistorie");

      const created = firstValueFrom(
        runMutation(testQueryClient, service.create(), { zaakobject }),
      );
      await sleep();
      httpTestingController.expectOne("/rest/bag").flush(null);
      await created;

      expect(invalidateHistorie).not.toHaveBeenCalled();
    });
  });

  describe("delete", () => {
    it("invalidates the historie of the zaak the bag object was coupled to, so every caller gets a fresh one", async () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      const deleted = firstValueFrom(
        runMutation(testQueryClient, service.delete(), {
          uuid: "fakeBagObjectGegevensUuid",
          zaakUuid: "fakeZaakUuid",
          redenWijzigen: "fakeReden",
        }),
      );
      await flushDelete();
      await deleted;

      expect(invalidateQueries).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });

    it("invalidates no historie when the request carries no zaak uuid, which the endpoint types allow", async () => {
      const invalidateHistorie = jest.spyOn(zakenService, "invalidateHistorie");

      const deleted = firstValueFrom(
        runMutation(testQueryClient, service.delete(), {
          uuid: "fakeBagObjectGegevensUuid",
          redenWijzigen: "fakeReden",
        }),
      );
      await flushDelete();
      await deleted;

      expect(invalidateHistorie).not.toHaveBeenCalled();
    });
  });
});
