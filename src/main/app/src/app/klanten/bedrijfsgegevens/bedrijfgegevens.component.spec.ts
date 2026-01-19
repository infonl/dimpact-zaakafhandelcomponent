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
import { MatIconHarness } from "@angular/material/icon/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { sleep } from "../../../../setupJest";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
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
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MaterialModule,
        PipesModule,
      ],
      providers: [
        KlantenService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTanStackQuery(queryClient),
      ],
      declarations: [BedrijfsgegevensComponent],
    });

    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(klantenService, "readBedrijf").mockReturnValue({
      ...klantenService.readBedrijf(betrokkeneIdentificatie),
      retry: false,
    });

    fixture = TestBed.createComponent(BedrijfsgegevensComponent);

    componentRef = fixture.componentRef;
    componentRef.setInput("initiatorIdentificatie", betrokkeneIdentificatie);

    loader = TestbedHarnessEnvironment.loader(fixture);
    httpController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    queryClient.clear();
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
        const request = httpController.expectOne(
          `/rest/klanten/vestiging/${betrokkeneIdentificatie.vestigingsnummer}/${betrokkeneIdentificatie.kvkNummer}`,
        );
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
