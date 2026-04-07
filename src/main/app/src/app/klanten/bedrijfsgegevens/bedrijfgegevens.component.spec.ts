/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatExpansionPanelHarness } from "@angular/material/expansion/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatProgressSpinnerHarness } from "@angular/material/progress-spinner/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { KlantenService } from "../klanten.service";
import { BedrijfsgegevensComponent } from "./bedrijfsgegevens.component";

describe(BedrijfsgegevensComponent.name, () => {
  let fixture: ComponentFixture<BedrijfsgegevensComponent>;
  let componentRef: ComponentRef<BedrijfsgegevensComponent>;
  let loader: HarnessLoader;
  let httpController: HttpTestingController;

  let klantenService: KlantenService;
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const betrokkeneIdentificatie = fromPartial<BetrokkeneIdentificatie>({
    type: "VN",
    kvkNummer: "12345678",
    vestigingsnummer: "12345678",
  });

  const vestigingUrl = `/rest/klanten/vestiging/${betrokkeneIdentificatie.vestigingsnummer}/${betrokkeneIdentificatie.kvkNummer}`;

  const testZaak = fromPartial<GeneratedType<"RestZaak">>({
    initiatorIdentificatie: betrokkeneIdentificatie,
    rechten: {
      toevoegenInitiatorBedrijf: false,
      verwijderenInitiator: false,
    },
  });

  const testBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
    naam: "Test BV",
    vestigingsnummer: betrokkeneIdentificatie.vestigingsnummer,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        BedrijfsgegevensComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MaterialModule,
        PipesModule,
      ],
      providers: [
        KlantenService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(queryClient),
      ],
    });

    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(klantenService, "readBedrijf").mockReturnValue({
      ...klantenService.readBedrijf(betrokkeneIdentificatie),
      retry: false,
    });

    fixture = TestBed.createComponent(BedrijfsgegevensComponent);

    componentRef = fixture.componentRef;
    componentRef.setInput("zaak", testZaak);

    loader = TestbedHarnessEnvironment.loader(fixture);
    httpController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    queryClient.clear();
  });

  describe("while loading", () => {
    it("should show a spinner", async () => {
      const spinner = await loader.getHarnessOrNull(MatProgressSpinnerHarness);
      expect(spinner).toBeTruthy();
    });
  });

  describe("rechten", () => {
    it("should show the edit button when toevoegenInitiatorBedrijf is true", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          rechten: { toevoegenInitiatorBedrijf: true, verwijderenInitiator: false },
        }),
      );
      fixture.detectChanges();

      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: '[title="actie.initiator.wijzigen"]' }),
      );
      expect(button).toBeTruthy();
    });

    it("should not show the edit button when toevoegenInitiatorBedrijf is false", async () => {
      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: '[title="actie.initiator.wijzigen"]' }),
      );
      expect(button).toBeNull();
    });

    it("should show the delete button when verwijderenInitiator is true", async () => {
      componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          ...testZaak,
          rechten: { toevoegenInitiatorBedrijf: false, verwijderenInitiator: true },
        }),
      );
      fixture.detectChanges();

      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: '[title="actie.ontkoppelen"]' }),
      );
      expect(button).toBeTruthy();
    });

    it("should not show the delete button when verwijderenInitiator is false", async () => {
      const button = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: '[title="actie.ontkoppelen"]' }),
      );
      expect(button).toBeNull();
    });
  });

  describe("on successful load", () => {
    beforeEach(async () => {
      notifyManager.setScheduler((fn) => fn());
      const request = httpController.expectOne(vestigingUrl);
      request.flush(testBedrijf);
      await sleep();
      fixture.detectChanges();
    });

    afterEach(() => {
      notifyManager.setScheduler(queueMicrotask);
    });

    it("should expand the panel", async () => {
      const panel = await loader.getHarness(MatExpansionPanelHarness);
      expect(await panel.isExpanded()).toBe(true);
    });

    it("should show the visit link", async () => {
      const link = await loader.getHarnessOrNull(
        MatButtonHarness.with({ selector: 'a[title="actie.bedrijf.bekijken"]' }),
      );
      expect(link).toBeTruthy();
    });
  });

  describe.each([
    {
      status: 404,
      iconName: "warning",
      statusText: "Not finding the vestiging",
    },
    {
      status: 500,
      iconName: "error",
      statusText: "Error fetching the vestiging",
    },
  ])(
    "Error handling fetching vestiging",
    ({ status, iconName, statusText }) => {
      beforeEach(() => {
        const request = httpController.expectOne(vestigingUrl);
        request.flush(null, { status, statusText });
      });

      it("should display the icon", async () => {
        await sleep();
        const icon = await loader.getHarnessOrNull(
          MatIconHarness.with({ name: iconName }),
        );
        expect(icon).toBeTruthy();
      });
    },
  );
});
