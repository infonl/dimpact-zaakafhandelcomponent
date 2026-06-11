/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTabGroupHarness } from "@angular/material/tabs/testing";
import { MatTooltip } from "@angular/material/tooltip";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { PolicyService } from "src/app/policy/policy.service";
import { SharedModule } from "src/app/shared/shared.module";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";
import { KlantKoppelComponent } from "./klant-koppel.component";

@Component({
  selector: "zac-klant-koppel-initiator-persoon",
  template: "",
  standalone: true,
})
class KlantKoppelInitiatorStubComponent {
  @Input() type!: string;
  @Input() zaaktypeUUID?: string | null;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
}

@Component({
  selector: "zac-klant-koppel-betrokkene-persoon",
  template: "",
  standalone: true,
})
class KlantKoppelBetrokkeneStubComponent {
  @Input() type!: string;
  @Input() zaaktypeUUID?: string | null;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
}

const mockSideNav = { close: jest.fn() } as unknown as MatDrawer;

describe(KlantKoppelComponent.name, () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        KlantKoppelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    });
  });

  describe("tab groups", () => {
    let fixture: ComponentFixture<KlantKoppelComponent>;
    let loader: HarnessLoader;
    let policyService: PolicyService;

    function createFixture(initiator = false) {
      fixture = TestBed.createComponent(KlantKoppelComponent);
      fixture.componentRef.setInput("sideNav", mockSideNav);
      fixture.componentRef.setInput("initiator", initiator);
      fixture.componentRef.setInput("allowPersoon", true);
      fixture.componentRef.setInput("allowBedrijf", true);
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    }

    beforeEach(async () => {
      await TestBed.overrideComponent(KlantKoppelComponent, {
        set: {
          imports: [
            SharedModule,
            MatTooltip,
            TranslateModule,
            KlantKoppelInitiatorStubComponent,
            KlantKoppelBetrokkeneStubComponent,
          ],
        },
      }).compileComponents();

      policyService = TestBed.inject(PolicyService);

      testQueryClient.setQueryData(
        policyService.readOverigeRechten().queryKey,
        fromPartial<GeneratedType<"RestOverigeRechten">>({ brpZoeken: true }),
      );

      createFixture();
    });

    it("should render two tabs when allowPersoon and allowBedrijf are true", async () => {
      const tabGroup = await loader.getHarness(MatTabGroupHarness);
      expect(await (await tabGroup.getTabs()).length).toBe(2);
    });

    describe("betrokkene tab group (initiator=false)", () => {
      describe("when brpZoeken is true", () => {
        it("should have the persoon tab enabled", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isDisabled()).toBe(false);
        });

        it("should select the persoon tab by default", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isSelected()).toBe(true);
        });

        it("should show an empty tooltip on the persoon tab label", () => {
          const tooltip = fixture.debugElement
            .query(By.directive(MatTooltip))
            ?.injector.get(MatTooltip);
          expect(tooltip?.message).toBe("");
        });
      });

      describe("when brpZoeken is false", () => {
        beforeEach(() => {
          testQueryClient.setQueryData(
            policyService.readOverigeRechten().queryKey,
            fromPartial<GeneratedType<"RestOverigeRechten">>({
              brpZoeken: false,
            }),
          );
          createFixture();
        });

        it("should have the persoon tab disabled", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isDisabled()).toBe(true);
        });

        it("should select the bedrijf tab by default", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [, bedrijfTab] = await tabGroup.getTabs();
          expect(await bedrijfTab.isSelected()).toBe(true);
        });

        it("should show a tooltip on the persoon tab label", () => {
          const tooltip = fixture.debugElement
            .query(By.directive(MatTooltip))
            ?.injector.get(MatTooltip);
          expect(tooltip?.message).toBe("msg.rechten.geen.persoon.zoeken");
        });
      });
    });

    describe("initiator tab group (initiator=true)", () => {
      beforeEach(() => {
        createFixture(true);
      });

      describe("when brpZoeken is true", () => {
        it("should have the persoon tab enabled", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isDisabled()).toBe(false);
        });

        it("should select the persoon tab by default", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isSelected()).toBe(true);
        });
      });

      describe("when brpZoeken is false", () => {
        beforeEach(() => {
          testQueryClient.setQueryData(
            policyService.readOverigeRechten().queryKey,
            fromPartial<GeneratedType<"RestOverigeRechten">>({
              brpZoeken: false,
            }),
          );
          createFixture(true);
        });

        it("should have the persoon tab disabled", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [persoonTab] = await tabGroup.getTabs();
          expect(await persoonTab.isDisabled()).toBe(true);
        });

        it("should select the bedrijf tab by default", async () => {
          const tabGroup = await loader.getHarness(MatTabGroupHarness);
          const [, bedrijfTab] = await tabGroup.getTabs();
          expect(await bedrijfTab.isSelected()).toBe(true);
        });
      });
    });
  });
});
