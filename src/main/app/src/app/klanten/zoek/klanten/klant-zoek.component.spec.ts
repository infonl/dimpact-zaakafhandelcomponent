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
import { Component, EventEmitter, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTabGroupHarness } from "@angular/material/tabs/testing";
import { MatTooltip } from "@angular/material/tooltip";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { PolicyService } from "../../../policy/policy.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantZoekComponent } from "./klant-zoek.component";

@Component({
  selector: "zac-persoon-zoek",
  template: "",
  standalone: true,
})
class PersoonZoekStubComponent {
  @Output() persoon = new EventEmitter<GeneratedType<"RestPersoon">>();
}

@Component({
  selector: "zac-bedrijf-zoek",
  template: "",
  standalone: true,
})
class BedrijfZoekStubComponent {
  @Output() bedrijf = new EventEmitter<GeneratedType<"RestBedrijf">>();
}

const makePersoon = (
  fields: Partial<GeneratedType<"RestPersoon">> = {},
): GeneratedType<"RestPersoon"> =>
  fromPartial<GeneratedType<"RestPersoon">>({ bsn: "999990408", ...fields });

const makeBedrijf = (
  fields: Partial<GeneratedType<"RestBedrijf">> = {},
): GeneratedType<"RestBedrijf"> =>
  fromPartial<GeneratedType<"RestBedrijf">>({
    kvkNummer: "12345678",
    ...fields,
  });

describe(KlantZoekComponent.name, () => {
  let component: KlantZoekComponent;
  let fixture: ComponentFixture<KlantZoekComponent>;
  let loader: HarnessLoader;
  let policyService: PolicyService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantZoekComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    })
      .overrideComponent(KlantZoekComponent, {
        set: {
          imports: [
            MatTabsModule,
            MatIconModule,
            MatTooltip,
            TranslateModule,
            PersoonZoekStubComponent,
            BedrijfZoekStubComponent,
          ],
        },
      })
      .compileComponents();

    policyService = TestBed.inject(PolicyService);

    testQueryClient.setQueryData(
      policyService.readOverigeRechten().queryKey,
      fromPartial<GeneratedType<"RestOverigeRechten">>({ brpZoeken: true }),
    );

    fixture = TestBed.createComponent(KlantZoekComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it("should render a tab group with two tabs", async () => {
    const tabGroup = await loader.getHarness(MatTabGroupHarness);
    const tabs = await tabGroup.getTabs();
    expect(tabs.length).toBe(2);
  });

  it("should display the persoon tab as the first tab", async () => {
    const tabGroup = await loader.getHarness(MatTabGroupHarness);
    const [persoonTab] = await tabGroup.getTabs();
    const label = await persoonTab.getLabel();
    expect(label).toContain("actie.zoeken.persoon");
  });

  it("should display the bedrijf tab as the second tab", async () => {
    const tabGroup = await loader.getHarness(MatTabGroupHarness);
    const [, bedrijfTab] = await tabGroup.getTabs();
    const label = await bedrijfTab.getLabel();
    expect(label).toContain("actie.zoeken.bedrijf");
  });

  it("should select the persoon tab by default", async () => {
    const tabGroup = await loader.getHarness(MatTabGroupHarness);
    const [persoonTab] = await tabGroup.getTabs();
    expect(await persoonTab.isSelected()).toBe(true);
  });

  describe("klantGeselecteerd", () => {
    it("should emit via @Output klant when a persoon is selected", () => {
      const emittedValues: GeneratedType<"RestBedrijf" | "RestPersoon">[] = [];
      component.klant.subscribe((value) => emittedValues.push(value));

      const persoon = makePersoon();
      component["klantGeselecteerd"](persoon);

      expect(emittedValues).toHaveLength(1);
      expect(emittedValues[0]).toBe(persoon);
    });

    it("should emit via @Output klant when a bedrijf is selected", () => {
      const emittedValues: GeneratedType<"RestBedrijf" | "RestPersoon">[] = [];
      component.klant.subscribe((value) => emittedValues.push(value));

      const bedrijf = makeBedrijf();
      component["klantGeselecteerd"](bedrijf);

      expect(emittedValues).toHaveLength(1);
      expect(emittedValues[0]).toBe(bedrijf);
    });

    it("should forward persoon event from zac-persoon-zoek child to @Output klant", () => {
      const emittedValues: GeneratedType<"RestBedrijf" | "RestPersoon">[] = [];
      component.klant.subscribe((value) => emittedValues.push(value));

      const persoonStub = fixture.debugElement.query(
        By.directive(PersoonZoekStubComponent),
      ).componentInstance as PersoonZoekStubComponent;
      const persoon = makePersoon({ bsn: "111111110" });
      persoonStub.persoon.emit(persoon);

      expect(emittedValues).toHaveLength(1);
      expect(emittedValues[0]).toBe(persoon);
    });

    it("should forward bedrijf event from zac-bedrijf-zoek child to @Output klant after switching tabs", async () => {
      const emittedValues: GeneratedType<"RestBedrijf" | "RestPersoon">[] = [];
      component.klant.subscribe((value) => emittedValues.push(value));

      const tabGroup = await loader.getHarness(MatTabGroupHarness);
      const [, bedrijfTab] = await tabGroup.getTabs();
      await bedrijfTab.select();
      fixture.detectChanges();

      const bedrijfStub = fixture.debugElement.query(
        By.directive(BedrijfZoekStubComponent),
      ).componentInstance as BedrijfZoekStubComponent;
      const bedrijf = makeBedrijf({ kvkNummer: "87654321" });
      bedrijfStub.bedrijf.emit(bedrijf);

      expect(emittedValues).toHaveLength(1);
      expect(emittedValues[0]).toBe(bedrijf);
    });
  });

  describe("brpZoeken recht", () => {
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
        fixture = TestBed.createComponent(KlantZoekComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        fixture.detectChanges();
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
});
