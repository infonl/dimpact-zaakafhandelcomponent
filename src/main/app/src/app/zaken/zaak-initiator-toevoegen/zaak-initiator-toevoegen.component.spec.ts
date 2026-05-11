/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { ZaakInitiatorToevoegenComponent } from "./zaak-initiator-toevoegen.component";

describe(ZaakInitiatorToevoegenComponent.name, () => {
  let fixture: ComponentFixture<ZaakInitiatorToevoegenComponent>;
  let component: ZaakInitiatorToevoegenComponent;

  async function createComponent(toevoegenToegestaan = false) {
    fixture = TestBed.createComponent(ZaakInitiatorToevoegenComponent);
    component = fixture.componentInstance;
    component.toevoegenToegestaan = toevoegenToegestaan;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakInitiatorToevoegenComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();
  });

  it("renders the expansion panel header title", async () => {
    await createComponent();
    const title = fixture.nativeElement.querySelector("mat-panel-title");
    expect(title).toBeTruthy();
  });

  it("hides the add button when toevoegenToegestaan is false", async () => {
    await createComponent(false);
    const button = fixture.nativeElement.querySelector(
      "button[mat-icon-button]",
    );
    expect(button).toBeNull();
  });

  it("shows the add button when toevoegenToegestaan is true", async () => {
    await createComponent(true);
    const button = fixture.nativeElement.querySelector(
      "button[mat-icon-button]",
    );
    expect(button).toBeTruthy();
  });

  it("emits add event when add button is clicked", async () => {
    await createComponent(true);
    const addSpy = jest.spyOn(component.add, "emit");
    const button = fixture.nativeElement.querySelector(
      "button[mat-icon-button]",
    );
    button.click();
    expect(addSpy).toHaveBeenCalledTimes(1);
  });
});
