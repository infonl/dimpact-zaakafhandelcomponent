/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Injector, runInInjectionContext } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatIconModule } from "@angular/material/icon";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { mockMutationFn, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { ToolbarComponent } from "./toolbar.component";

describe(ToolbarComponent.name, () => {
  let fixture: ComponentFixture<ToolbarComponent>;
  let loader: HarnessLoader;
  let identityService: IdentityService;
  let policyService: PolicyService;
  let zakenService: ZakenService;
  let injector: Injector;
  let createZaakMutation: ReturnType<typeof injectMutation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ToolbarComponent],
      imports: [
        MaterialModule,
        MatIconModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    policyService = TestBed.inject(PolicyService);
    jest.spyOn(policyService, "readOverigeRechten").mockReturnValue(
      of({
        startenZaak: true,
        beheren: false,
        zoeken: false,
      }),
    );

    zakenService = TestBed.inject(ZakenService);
    injector = TestBed.inject(Injector);
    createZaakMutation = runInInjectionContext(injector, () =>
      injectMutation(() => ({
        mutationKey: zakenService.createZaak().mutationKey,
        mutationFn: () => mockMutationFn(),
      })),
    );

    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestUser">>({ id: "user-id", naam: "Test" }),
    );

    fixture = TestBed.createComponent(ToolbarComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  describe("Zaak create button disabled state", () => {
    it("should enable the create zaak button when createZaak mutation is not in progress", async () => {
      const button = await loader.getHarness(
        MatButtonHarness.with({ text: "create_new_folder" }),
      );

      expect(await button.isDisabled()).toBe(false);
    });

    it("should disable the create zaak button when createZaak mutation is in progress", async () => {
      createZaakMutation.mutate({});
      fixture.detectChanges();

      const button = await loader.getHarness(
        MatButtonHarness.with({ text: "create_new_folder" }),
      );

      expect(await button.isDisabled()).toBe(true);
    });
  });
});
