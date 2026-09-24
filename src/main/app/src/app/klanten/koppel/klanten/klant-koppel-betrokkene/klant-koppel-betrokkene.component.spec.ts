/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, input, output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { KlantenService } from "src/app/klanten/klanten.service";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { KlantKoppelBetrokkeneComponent } from "./klant-koppel-betrokkene.component";

const fakePersoon = fromPartial<GeneratedType<"RestPersoon">>({
  bsn: "999990408",
});

const fakeBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
  kvkNummer: "12345678",
});

const fakeRoltype = fromPartial<GeneratedType<"RestRoltype">>({
  uuid: "fakeRoltypeUuid",
  naam: "fakeRoltype",
});

@Component({
  selector: "zac-persoon-zoek",
  template: `
    <p>persoon-zoek zaaktypeUUID: {{ zaaktypeUUID() }}</p>
    <p>persoon-zoek syncEnabled: {{ syncEnabled() }}</p>
    <p>persoon-zoek blockSearch: {{ blockSearch() }}</p>
    <button type="button" (click)="persoon.emit(fakePersoon)">
      select fake persoon
    </button>
  `,
  standalone: true,
})
class PersoonZoekStubComponent {
  readonly syncEnabled = input<boolean>();
  readonly blockSearch = input<boolean>();
  readonly zaaktypeUUID = input<string | null>();
  readonly persoon = output<GeneratedType<"RestPersoon">>();
  protected readonly fakePersoon = fakePersoon;
}

@Component({
  selector: "zac-bedrijf-zoek",
  template: `
    <p>bedrijf-zoek syncEnabled: {{ syncEnabled() }}</p>
    <p>bedrijf-zoek blockSearch: {{ blockSearch() }}</p>
    <button type="button" (click)="bedrijf.emit(fakeBedrijf)">
      select fake bedrijf
    </button>
  `,
  standalone: true,
})
class BedrijfZoekStubComponent {
  readonly syncEnabled = input<boolean>();
  readonly blockSearch = input<boolean>();
  readonly bedrijf = output<GeneratedType<"RestBedrijf">>();
  protected readonly fakeBedrijf = fakeBedrijf;
}

describe(KlantKoppelBetrokkeneComponent.name, () => {
  const user = userEvent.setup();

  let fixture: ComponentFixture<KlantKoppelBetrokkeneComponent>;
  let listBetrokkeneRoltypen: jest.Mock;
  let onKlantGegevens: jest.Mock<void, [KlantGegevens]>;

  beforeEach(async () => {
    listBetrokkeneRoltypen = jest.fn().mockReturnValue(of([fakeRoltype]));

    await TestBed.configureTestingModule({
      imports: [
        KlantKoppelBetrokkeneComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: KlantenService,
          useValue: fromPartial<KlantenService>({ listBetrokkeneRoltypen }),
        },
      ],
    })
      .overrideComponent(KlantKoppelBetrokkeneComponent, {
        set: {
          imports: [
            NgIf,
            ReactiveFormsModule,
            TranslateModule,
            ZacSelect,
            ZacInput,
            PersoonZoekStubComponent,
            BedrijfZoekStubComponent,
          ],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(KlantKoppelBetrokkeneComponent);
    onKlantGegevens = jest.fn();
    fixture.componentInstance.klantGegevens.subscribe(onKlantGegevens);
  });

  function initialiseWith(inputs: {
    type: "persoon" | "bedrijf";
    zaaktypeUUID?: string | null;
  }) {
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    fixture.detectChanges();
  }

  function roltypeField() {
    return screen.getByRole("combobox", { name: "BetrokkeneRoltype" });
  }

  async function chooseRoltype() {
    await user.click(roltypeField());
    await user.click(screen.getByRole("option", { name: "fakeRoltype" }));
    fixture.detectChanges();
  }

  function queryPersoonZoek() {
    return screen.queryByText(/^persoon-zoek syncEnabled/);
  }

  function queryBedrijfZoek() {
    return screen.queryByText(/^bedrijf-zoek syncEnabled/);
  }

  describe("the search for the klant", () => {
    it('shows only the persoon search when type is "persoon"', () => {
      initialiseWith({ type: "persoon" });

      expect(queryPersoonZoek()).toBeInTheDocument();
      expect(queryBedrijfZoek()).not.toBeInTheDocument();
    });

    it('shows only the bedrijf search when type is "bedrijf"', () => {
      initialiseWith({ type: "bedrijf" });

      expect(queryBedrijfZoek()).toBeInTheDocument();
      expect(queryPersoonZoek()).not.toBeInTheDocument();
    });

    it("switches from the persoon search to the bedrijf search when the type changes", () => {
      initialiseWith({ type: "persoon" });

      fixture.componentRef.setInput("type", "bedrijf");
      fixture.detectChanges();

      expect(queryBedrijfZoek()).toBeInTheDocument();
      expect(queryPersoonZoek()).not.toBeInTheDocument();
    });

    it("enables the synchronisation of the persoon search", () => {
      initialiseWith({ type: "persoon" });

      expect(
        screen.getByText("persoon-zoek syncEnabled: true"),
      ).toBeInTheDocument();
    });

    it("enables the synchronisation of the bedrijf search", () => {
      initialiseWith({ type: "bedrijf" });

      expect(
        screen.getByText("bedrijf-zoek syncEnabled: true"),
      ).toBeInTheDocument();
    });

    it("passes the zaaktypeUUID to the persoon search", () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });

      expect(
        screen.getByText("persoon-zoek zaaktypeUUID: fakeZaaktypeUuid"),
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
  });

  describe("the betrokkene roltypen", () => {
    it("offers the betrokkene roltypen of the zaaktype", async () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });

      await user.click(roltypeField());

      expect(listBetrokkeneRoltypen).toHaveBeenCalledWith("fakeZaaktypeUuid");
      expect(
        screen.getByRole("option", { name: "fakeRoltype" }),
      ).toBeInTheDocument();
    });

    it("does not load any roltypen when no zaaktypeUUID is given", () => {
      initialiseWith({ type: "persoon" });

      expect(listBetrokkeneRoltypen).not.toHaveBeenCalled();
    });

    it("does not reload the roltypen when the zaaktypeUUID changes", () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });

      fixture.componentRef.setInput("zaaktypeUUID", "fakeOtherZaaktypeUuid");
      fixture.detectChanges();

      expect(listBetrokkeneRoltypen).toHaveBeenCalledTimes(1);
      expect(listBetrokkeneRoltypen).toHaveBeenCalledWith("fakeZaaktypeUuid");
    });
  });

  describe("blocking the search", () => {
    it("blocks the persoon search until a roltype is chosen", async () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });

      expect(
        screen.getByText("persoon-zoek blockSearch: true"),
      ).toBeInTheDocument();

      await chooseRoltype();

      expect(
        screen.getByText("persoon-zoek blockSearch: false"),
      ).toBeInTheDocument();
    });

    it("blocks the bedrijf search until a roltype is chosen", async () => {
      initialiseWith({ type: "bedrijf", zaaktypeUUID: "fakeZaaktypeUuid" });

      expect(
        screen.getByText("bedrijf-zoek blockSearch: true"),
      ).toBeInTheDocument();

      await chooseRoltype();

      expect(
        screen.getByText("bedrijf-zoek blockSearch: false"),
      ).toBeInTheDocument();
    });
  });

  describe("selecting a klant", () => {
    it("emits the persoon with the chosen roltype and the toelichting", async () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });
      await chooseRoltype();
      await user.type(
        screen.getByRole("textbox", { name: "Toelichting" }),
        "fakeToelichting",
      );

      await user.click(
        screen.getByRole("button", { name: "select fake persoon" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledTimes(1);
      expect(onKlantGegevens).toHaveBeenCalledWith({
        klant: fakePersoon,
        betrokkeneRoltype: fakeRoltype,
        betrokkeneToelichting: "fakeToelichting",
      });
    });

    it("emits an empty toelichting when none is filled in", async () => {
      initialiseWith({ type: "persoon", zaaktypeUUID: "fakeZaaktypeUuid" });
      await chooseRoltype();

      await user.click(
        screen.getByRole("button", { name: "select fake persoon" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledWith(
        expect.objectContaining({ betrokkeneToelichting: "" }),
      );
    });

    it("emits the bedrijf with the chosen roltype", async () => {
      initialiseWith({ type: "bedrijf", zaaktypeUUID: "fakeZaaktypeUuid" });
      await chooseRoltype();

      await user.click(
        screen.getByRole("button", { name: "select fake bedrijf" }),
      );

      expect(onKlantGegevens).toHaveBeenCalledWith({
        klant: fakeBedrijf,
        betrokkeneRoltype: fakeRoltype,
        betrokkeneToelichting: "",
      });
    });
  });
});
