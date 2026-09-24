/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { ZaakInitiatorToevoegenComponent } from "./zaak-initiator-toevoegen.component";

describe(ZaakInitiatorToevoegenComponent.name, () => {
  let fixture: ComponentFixture<ZaakInitiatorToevoegenComponent>;

  const user = userEvent.setup();

  const setup = async (toevoegenToegestaan: boolean) => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakInitiatorToevoegenComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakInitiatorToevoegenComponent);
    fixture.componentRef.setInput("toevoegenToegestaan", toevoegenToegestaan);
    fixture.detectChanges();
  };

  const addButton = () =>
    screen.queryByRole("button", { name: "actie.initiator.koppelen" });

  it("tells that the zaak has no initiator", async () => {
    await setup(false);

    expect(screen.getByText("msg.zaak.geen.initiator")).toBeInTheDocument();
  });

  it("hides the add button when adding an initiator is not allowed", async () => {
    await setup(false);

    expect(addButton()).not.toBeInTheDocument();
  });

  it("shows the add button when adding an initiator is allowed", async () => {
    await setup(true);

    expect(addButton()).toBeInTheDocument();
  });

  it("shows the add button once adding an initiator becomes allowed", async () => {
    await setup(false);

    fixture.componentRef.setInput("toevoegenToegestaan", true);
    fixture.detectChanges();

    expect(addButton()).toBeInTheDocument();
  });

  it("hides the add button once adding an initiator is no longer allowed", async () => {
    await setup(true);

    fixture.componentRef.setInput("toevoegenToegestaan", false);
    fixture.detectChanges();

    expect(addButton()).not.toBeInTheDocument();
  });

  it("emits add when the add button is clicked", async () => {
    await setup(true);
    const add = jest.fn();
    fixture.componentInstance.add.subscribe(add);

    await user.click(
      screen.getByRole("button", { name: "actie.initiator.koppelen" }),
    );

    expect(add).toHaveBeenCalledTimes(1);
  });
});
