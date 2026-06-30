/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ZaakInitiatorToevoegenComponent } from "./zaak-initiator-toevoegen.component";

describe(ZaakInitiatorToevoegenComponent.name, () => {
  let fixture: ComponentFixture<ZaakInitiatorToevoegenComponent>;
  let component: ZaakInitiatorToevoegenComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakInitiatorToevoegenComponent,
        NoopAnimationsModule,
        MatExpansionModule,
        MatIconModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakInitiatorToevoegenComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it("hides the add button when toevoegenToegestaan is false", async () => {
    component.toevoegenToegestaan = false;
    fixture.detectChanges();
    const buttons = await loader.getAllHarnesses(MatButtonHarness);
    expect(buttons).toHaveLength(0);
  });

  it("shows the add button when toevoegenToegestaan is true", async () => {
    component.toevoegenToegestaan = true;
    fixture.detectChanges();
    const buttons = await loader.getAllHarnesses(MatButtonHarness);
    expect(buttons).toHaveLength(1);
  });

  it("emits add event when the button is clicked", async () => {
    component.toevoegenToegestaan = true;
    fixture.detectChanges();
    jest.spyOn(component.add, "emit");
    const button = await loader.getHarness(MatButtonHarness);
    await button.click();
    expect(component.add.emit).toHaveBeenCalledTimes(1);
  });
});
