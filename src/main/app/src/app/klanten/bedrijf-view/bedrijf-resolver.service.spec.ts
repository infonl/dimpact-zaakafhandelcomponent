/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { convertToParamMap } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import {
  KVK_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "src/app/shared/utils/constants";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";
import { BedrijfResolverService } from "./bedrijf-resolver.service";

describe(BedrijfResolverService.name, () => {
  let bedrijfResolverService: BedrijfResolverService;
  let foutAfhandelingService: FoutAfhandelingService;
  let queryClient: QueryClient;

  const vestigingsnummer = "1".repeat(VESTIGINGSNUMMER_LENGTH);
  const kvkNummer = "2".repeat(KVK_LENGTH);
  const rsin = "3".repeat(KVK_LENGTH + 1); // langer dan KVK_LENGTH

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BedrijfResolverService,
        FoutAfhandelingService,
        KlantenService,
        QueryClient,
        provideHttpClient(withInterceptorsFromDi()),
      ],
      imports: [TranslateModule.forRoot()],
    });

    bedrijfResolverService = TestBed.inject(BedrijfResolverService);

    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest.spyOn(foutAfhandelingService, "openFoutDialog").mockImplementation();

    TestBed.inject(KlantenService);
    queryClient = TestBed.inject(QueryClient);
    jest
      .spyOn(queryClient, "ensureQueryData")
      .mockResolvedValue(fromPartial<GeneratedType<"RestBedrijf">>({}));
  });

  it("should throw an error if no id is provided", async () => {
    await expect(async () =>
      bedrijfResolverService.resolve(
        fromPartial({
          get paramMap() {
            return convertToParamMap({ id: null });
          },
        }),
      ),
    ).rejects.toThrowError("BedrijfResolverService: no 'id' found in route");
  });

  describe(BedrijfResolverService.prototype.resolve.name, () => {
    it.each([
      { params: { id: kvkNummer }, type: "RSIN" },
      {
        params: { id: kvkNummer, vestigingsnummer: vestigingsnummer },
        type: "VN",
      },
      { params: { id: rsin }, type: "RSIN" },
    ])(
      "should determine the correct type based on the passed parameters",
      async ({ params }) => {
        await bedrijfResolverService.resolve(
          fromPartial({
            get paramMap() {
              return convertToParamMap(params);
            },
          }),
        );

        expect(queryClient.ensureQueryData).toHaveBeenCalledWith(
          expect.objectContaining({
            queryKey: expect.arrayContaining([
              expect.stringContaining("/rest/klanten/"),
            ]),
          }),
        );
      },
    );

    it("shouldn't call the error handling when trying to call just a vestigingsnummer", async () => {
      const spy = jest.spyOn(foutAfhandelingService, "openFoutDialog");
      await bedrijfResolverService.resolve(
        fromPartial({
          get paramMap() {
            return convertToParamMap({ id: vestigingsnummer });
          },
        }),
      );

      expect(spy).not.toHaveBeenCalled();
    });
  });
});
