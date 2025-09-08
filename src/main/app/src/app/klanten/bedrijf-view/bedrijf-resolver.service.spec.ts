/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { ActivatedRouteSnapshot, convertToParamMap } from "@angular/router";
import {
  KVK_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "src/app/shared/utils/constants";
import { KlantenService } from "../klanten.service";
import { BedrijfResolverService } from "./bedrijf-resolver.service";

describe(BedrijfResolverService.name, () => {
  let bedrijfResolverService: BedrijfResolverService;
  let klantenServiceMock: jest.Mocked<KlantenService>;

  beforeEach(() => {
    klantenServiceMock = {
      readBedrijf: jest.fn(),
    } as unknown as jest.Mocked<KlantenService>;

    TestBed.configureTestingModule({
      providers: [
        BedrijfResolverService,
        { provide: KlantenService, useValue: klantenServiceMock },
      ],
    });

    bedrijfResolverService = TestBed.inject(BedrijfResolverService);
  });

  const makeRoute = (id: string | null): ActivatedRouteSnapshot =>
    ({
      paramMap: convertToParamMap(id ? { id } : {}),
    }) as ActivatedRouteSnapshot;

  it("should throw an error if no id is provided", () => {
    const route = makeRoute(null);

    expect(() => bedrijfResolverService.resolve(route)).toThrowError(
      "BedrijfResolverService: no 'id' found in route",
    );
  });

  it("should resolve a vestigingsnummer (VN)", () => {
    const vestigingsnummer = "1".repeat(VESTIGINGSNUMMER_LENGTH);
    const route = makeRoute(vestigingsnummer);

    bedrijfResolverService.resolve(route);

    expect(klantenServiceMock.readBedrijf).toHaveBeenCalledWith(
      expect.objectContaining({
        type: "VN",
        bsnNummer: null,
        vestigingsnummer,
        kvkNummer: null,
        rsin: null,
      }),
    );
  });

  it("should resolve a KVK number", () => {
    const kvk = "2".repeat(KVK_LENGTH);
    const route = makeRoute(kvk);

    bedrijfResolverService.resolve(route);

    expect(klantenServiceMock.readBedrijf).toHaveBeenCalledWith(
      expect.objectContaining({
        type: "RSIN",
        bsnNummer: null,
        vestigingsnummer: null,
        kvkNummer: kvk,
        rsin: null,
      }),
    );
  });

  it("should resolve a RSIN", () => {
    const rsin = "3".repeat(KVK_LENGTH + 1); // langer dan KVK_LENGTH
    const route = makeRoute(rsin);

    bedrijfResolverService.resolve(route);

    expect(klantenServiceMock.readBedrijf).toHaveBeenCalledWith(
      expect.objectContaining({
        type: "RSIN",
        bsnNummer: null,
        vestigingsnummer: null,
        kvkNummer: null,
        rsin,
      }),
    );
  });
});
