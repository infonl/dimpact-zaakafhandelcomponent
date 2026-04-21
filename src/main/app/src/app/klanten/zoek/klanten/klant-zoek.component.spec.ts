/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  Component,
  EventEmitter,
  Output,
} from "@angular/core";
import { By } from "@angular/platform-browser";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatIconModule } from "@angular/material/icon";
import { MatTabGroupHarness } from "@angular/material/tabs/testing";
import { MatTabsModule } from "@angular/material/tabs";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
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
  fromPartial<GeneratedType<"RestBedrijf">>({ kvkNummer: "12345678", ...fields });

describe(KlantZoekComponent.name, () => {
  let component: KlantZoekComponent;
  let fixture: ComponentFixture<KlantZoekComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantZoekComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    })
      .overrideComponent(KlantZoekComponent, {
        set: {
          imports: [
            MatTabsModule,
            MatIconModule,
            TranslateModule,
            PersoonZoekStubComponent,
            BedrijfZoekStubComponent,
          ],
        },
      })
      .compileComponents();

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

      const bedrijf = makeBedrijf({ kvkNummer: "87654321" });
      component["klantGeselecteerd"](bedrijf);

      expect(emittedValues).toHaveLength(1);
      expect(emittedValues[0]).toBe(bedrijf);
    });
  });
});
