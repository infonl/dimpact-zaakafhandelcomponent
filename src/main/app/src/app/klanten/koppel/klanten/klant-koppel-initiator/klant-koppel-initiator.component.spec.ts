/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { KlantKoppelInitiator } from "./klant-koppel-initiator.component";

const fakePersoon = fromPartial<GeneratedType<"RestPersoon">>({
  bsn: "999990408",
});

const fakeBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
  kvkNummer: "12345678",
});

@Component({
  selector: "zac-persoon-zoek",
  template: `
    <p>persoon-zoek zaaktypeUUID: {{ zaaktypeUUID() }}</p>
    <p>persoon-zoek syncEnabled: {{ syncEnabled() }}</p>
    <button type="button" (click)="persoon.emit(fakePersoon)">
      select fake persoon
    </button>
  `,
  standalone: true,
})
class PersoonZoekStubComponent {
  readonly syncEnabled = input<boolean>();
  readonly zaaktypeUUID = input<string | null>();
  readonly persoon = output<GeneratedType<"RestPersoon">>();
  protected readonly fakePersoon = fakePersoon;
}

@Component({
  selector: "zac-bedrijf-zoek",
  template: `
    <p>bedrijf-zoek syncEnabled: {{ syncEnabled() }}</p>
    <button type="button" (click)="bedrijf.emit(fakeBedrijf)">
      select fake bedrijf
    </button>
  `,
  standalone: true,
})
class BedrijfZoekStubComponent {
  readonly syncEnabled = input<boolean>();
  readonly bedrijf = output<GeneratedType<"RestBedrijf">>();
  protected readonly fakeBedrijf = fakeBedrijf;
}

describe(KlantKoppelInitiator.name, () => {
  const user = userEvent.setup();

  let fixture: ComponentFixture<KlantKoppelInitiator>;
  let onKlantGegevens: jest.Mock<void, [KlantGegevens]>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantKoppelInitiator,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    })
      .overrideComponent(KlantKoppelInitiator, {
        set: {
          imports: [
            TranslateModule,
            PersoonZoekStubComponent,
            BedrijfZoekStubComponent,
          ],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(KlantKoppelInitiator);
    onKlantGegevens = jest.fn();
    fixture.componentInstance.klantGegevens.subscribe(onKlantGegevens);
  });

  function initialiseWith(inputs: {
    type?: "persoon" | "bedrijf";
    zaaktypeUUID?: string | null;
  }) {
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    fixture.detectChanges();
  }

  function queryPersoonZoek() {
    return screen.queryByText(/^persoon-zoek syncEnabled/);
  }

  function queryBedrijfZoek() {
    return screen.queryByText(/^bedrijf-zoek syncEnabled/);
  }

  it("shows the persoon search when no type is given", () => {
    initialiseWith({});

    expect(queryPersoonZoek()).toBeInTheDocument();
    expect(queryBedrijfZoek()).not.toBeInTheDocument();
  });

  describe('when type is "persoon"', () => {
    it("shows only the persoon search", () => {
      initialiseWith({ type: "persoon" });

      expect(queryPersoonZoek()).toBeInTheDocument();
      expect(queryBedrijfZoek()).not.toBeInTheDocument();
    });

    it("enables the synchronisation of the persoon search", () => {
      initialiseWith({ type: "persoon" });

      expect(
        screen.getByText("persoon-zoek syncEnabled: true"),
      ).toBeInTheDocument();
    });

    it("passes the zaaktypeUUID to the persoon search", () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });

      expect(
        screen.getByText("persoon-zoek zaaktypeUUID: fakeZaaktypeUuid"),
      ).toBeInTheDocument();
    });

    it("passes no zaaktypeUUID to the persoon search when none is given", () => {
      initialiseWith({ type: "persoon" });

      expect(
        screen.getByText("persoon-zoek zaaktypeUUID:"),
      ).toBeInTheDocument();
    });

    it("passes the new zaaktypeUUID to the persoon search when the zaaktypeUUID changes", () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });

      fixture.componentRef.setInput("zaaktypeUUID", "fakeOtherZaaktypeUuid");
      fixture.detectChanges();

      expect(
        screen.getByText("persoon-zoek zaaktypeUUID: fakeOtherZaaktypeUuid"),
      ).toBeInTheDocument();
    });

    it("emits klantGegevens wrapping the persoon when a persoon is selected", async () => {
      initialiseWith({ type: "persoon" });

      await user.click(
        screen.getByRole("button", { name: "select fake persoon" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledTimes(1);
      expect(onKlantGegevens).toHaveBeenCalledWith(
        new KlantGegevens(fakePersoon),
      );
    });
  });

  describe('when type is "bedrijf"', () => {
    it("shows only the bedrijf search", () => {
      initialiseWith({ type: "bedrijf" });

      expect(queryBedrijfZoek()).toBeInTheDocument();
      expect(queryPersoonZoek()).not.toBeInTheDocument();
    });

    it("enables the synchronisation of the bedrijf search", () => {
      initialiseWith({ type: "bedrijf" });

      expect(
        screen.getByText("bedrijf-zoek syncEnabled: true"),
      ).toBeInTheDocument();
    });

    it("emits klantGegevens wrapping the bedrijf when a bedrijf is selected", async () => {
      initialiseWith({ type: "bedrijf" });

      await user.click(
        screen.getByRole("button", { name: "select fake bedrijf" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledTimes(1);
      expect(onKlantGegevens).toHaveBeenCalledWith(
        new KlantGegevens(fakeBedrijf),
      );
    });
  });

  it("switches from the persoon search to the bedrijf search when the type changes", () => {
    initialiseWith({ type: "persoon" });

    fixture.componentRef.setInput("type", "bedrijf");
    fixture.detectChanges();

    expect(queryBedrijfZoek()).toBeInTheDocument();
    expect(queryPersoonZoek()).not.toBeInTheDocument();
  });
});
