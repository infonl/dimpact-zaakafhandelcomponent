/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { KlantenService } from "../../klanten/klanten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneLinkComponent } from "./betrokkene-link.component";

const makePersoonBetrokkene = (
  fields: Partial<GeneratedType<"RestZaakBetrokkene">> = {},
) =>
  fromPartial<GeneratedType<"RestZaakBetrokkene">>({
    type: "BSN",
    identificatieType: "BSN",
    temporaryPersonId: "temp-person-123",
    ...fields,
  });

const makeBedrijfBetrokkene = (
  fields: Partial<GeneratedType<"RestZaakBetrokkene">> = {},
) =>
  fromPartial<GeneratedType<"RestZaakBetrokkene">>({
    type: "RSIN",
    identificatieType: "RSIN",
    identificatie: "123456789",
    kvkNummer: "12345678",
    ...fields,
  });

const mockPersoon = fromPartial<GeneratedType<"RestPersoon">>({
  temporaryPersonId: "temp-person-123",
});

const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
  kvkNummer: "12345678",
  rsin: "123456789",
});

const persoonQueryKey = ["betrokkene-link-spec-persoon"];
const bedrijfQueryKey = ["betrokkene-link-spec-bedrijf"];

const setup = (
  betrokkene: GeneratedType<"RestZaakBetrokkene">,
  zaaktypeUuid = "zaaktype-uuid",
) => {
  const fixture: ComponentFixture<BetrokkeneLinkComponent> =
    TestBed.createComponent(BetrokkeneLinkComponent);
  fixture.componentRef.setInput("betrokkene", betrokkene);
  fixture.componentRef.setInput("zaaktypeUuid", zaaktypeUuid);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance };
};

describe(BetrokkeneLinkComponent.name, () => {
  let klantenService: KlantenService;

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());

    await TestBed.configureTestingModule({
      imports: [
        BetrokkeneLinkComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(klantenService, "readPersoon").mockReturnValue(
      queryOptions({
        queryKey: persoonQueryKey,
        queryFn: async () => mockPersoon,
      }) as ReturnType<KlantenService["readPersoon"]>,
    );
    jest.spyOn(klantenService, "readBedrijf").mockReturnValue(
      queryOptions({
        queryKey: bedrijfQueryKey,
        queryFn: async () => mockBedrijf,
      }) as ReturnType<KlantenService["readBedrijf"]>,
    );
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => setTimeout(fn, 0));
    testQueryClient.clear();
  });

  describe("when betrokkene is BSN type with temporaryPersonId", () => {
    it("shows persoon link anchor pointing to the persoon route", async () => {
      testQueryClient.setQueryData(persoonQueryKey, mockPersoon);
      const { fixture } = setup(
        makePersoonBetrokkene({ temporaryPersonId: "temp-456" }),
      );
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const anchor = await loader.getHarness(
        MatButtonHarness.with({ selector: "a[mat-icon-button]" }),
      );
      const host = await anchor.host();
      expect(await host.getAttribute("href")).toContain("persoon");
      expect(await host.getAttribute("title")).toBe("actie.persoon.bekijken");
    });
  });

  describe("when betrokkene is RSIN type with kvkNummer", () => {
    it("shows bedrijf link anchor pointing to the bedrijf route", async () => {
      testQueryClient.setQueryData(bedrijfQueryKey, mockBedrijf);
      const { fixture } = setup(
        makeBedrijfBetrokkene({ kvkNummer: "12345678" }),
      );
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const anchor = await loader.getHarness(
        MatButtonHarness.with({ selector: "a[mat-icon-button]" }),
      );
      const host = await anchor.host();
      expect(await host.getAttribute("href")).toContain("12345678");
      expect(await host.getAttribute("title")).toBe("actie.bedrijf.bekijken");
    });

    it("shows warning icon instead of link when betrokkene has no kvkNummer", async () => {
      testQueryClient.setQueryData(bedrijfQueryKey, mockBedrijf);
      const { fixture } = setup(
        makeBedrijfBetrokkene({ kvkNummer: undefined }),
      );
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const warningIcon = await loader.getHarness(
        MatIconHarness.with({ name: "warning" }),
      );
      expect(await warningIcon.getName()).toBe("warning");
      const anchor = fixture.nativeElement.querySelector("a[mat-icon-button]");
      expect(anchor).toBeNull();
    });
  });

  describe("when betrokkene is BSN type without temporaryPersonId", () => {
    it("does not show any link anchor when persoon query is disabled", () => {
      const { fixture } = setup(
        makePersoonBetrokkene({ temporaryPersonId: undefined }),
      );
      const anchor = fixture.nativeElement.querySelector("a[mat-icon-button]");
      expect(anchor).toBeNull();
    });
  });
});
