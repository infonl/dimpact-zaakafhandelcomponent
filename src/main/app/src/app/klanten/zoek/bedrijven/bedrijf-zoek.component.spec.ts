/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of, Subject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantenService } from "../../klanten.service";
import { FormCommunicatieService } from "../form-communicatie-service";
import { BedrijfZoekComponent } from "./bedrijf-zoek.component";

const fakeBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
  naam: "fakeBedrijfsnaam",
  kvkNummer: "12345678",
  vestigingsnummer: "000012345678",
  identificatieType: "VN",
});

describe(BedrijfZoekComponent.name, () => {
  const user = userEvent.setup();

  let fixture: ComponentFixture<BedrijfZoekComponent>;
  let listBedrijven: jest.Mock;
  let itemSelected: Subject<{ selected: boolean; uuid: string | null }>;
  let notifyItemSelected: jest.Mock;
  let navigate: jest.SpyInstance;

  async function setup({
    syncEnabled,
    sideNav,
    onBedrijf,
  }: {
    syncEnabled?: boolean;
    sideNav?: MatSidenav;
    onBedrijf?: (bedrijf: GeneratedType<"RestBedrijf">) => void;
  } = {}) {
    listBedrijven = jest.fn().mockReturnValue(
      of(
        fromPartial<GeneratedType<"RESTResultaatRestBedrijf">>({
          resultaten: [fakeBedrijf],
        }),
      ),
    );
    itemSelected = new Subject();
    notifyItemSelected = jest.fn();

    const rendered = await render(BedrijfZoekComponent, {
      inputs: {
        ...(syncEnabled !== undefined && { syncEnabled }),
        ...(sideNav && { sideNav }),
      },
      on: { ...(onBedrijf && { bedrijf: onBedrijf }) },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        {
          provide: KlantenService,
          useValue: fromPartial<KlantenService>({ listBedrijven }),
        },
        {
          provide: UtilService,
          useValue: fromPartial<UtilService>({ setLoading: jest.fn() }),
        },
        {
          provide: FormCommunicatieService,
          useValue: fromPartial<FormCommunicatieService>({
            itemSelected$: itemSelected.asObservable(),
            notifyItemSelected,
          }),
        },
      ],
    });

    fixture = rendered.fixture;
    navigate = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);
  }

  function kvkNummerField() {
    return screen.getByRole("textbox", { name: "Kvknummer" });
  }

  function searchButton() {
    return screen.getByRole("button", { name: "actie.zoeken" });
  }

  async function searchByKvkNummer(kvkNummer = "12345678") {
    await user.type(kvkNummerField(), kvkNummer);
    fixture.detectChanges();
    await user.click(searchButton());
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function resultRow() {
    return screen.getByRole("row", { name: /fakeBedrijfsnaam/ });
  }

  function queryResultRow() {
    return screen.queryByRole("row", { name: /fakeBedrijfsnaam/ });
  }

  describe("searching for a bedrijf", () => {
    it("searches for bedrijven with the entered kvk-nummer and lists them", async () => {
      await setup();

      await searchByKvkNummer();

      expect(listBedrijven).toHaveBeenCalledWith(
        expect.objectContaining({ kvkNummer: "12345678" }),
      );
      expect(resultRow()).toBeInTheDocument();
    });

    it("disables the search button while blockSearch is set, even when the form is valid", async () => {
      await setup();
      fixture.componentRef.setInput("blockSearch", true);
      fixture.detectChanges();

      await user.type(kvkNummerField(), "12345678");
      fixture.detectChanges();

      expect(searchButton()).toBeDisabled();
    });
  });

  describe("when a parent listens to the selected bedrijf", () => {
    const onBedrijf = jest.fn();

    it("emits the bedrijf and clears the results when the user selects a bedrijf", async () => {
      await setup({ onBedrijf });
      await searchByKvkNummer();

      await user.click(
        within(resultRow()).getByRole("button", { name: "actie.selecteren" }),
      );
      fixture.detectChanges();

      expect(onBedrijf).toHaveBeenCalledWith(fakeBedrijf);
      expect(queryResultRow()).not.toBeInTheDocument();
      expect(kvkNummerField()).toHaveValue("");
    });

    describe("without syncEnabled", () => {
      it("does not notify the other search forms when the user selects a bedrijf", async () => {
        await setup({ onBedrijf });
        await searchByKvkNummer();

        await user.click(
          within(resultRow()).getByRole("button", {
            name: "actie.selecteren",
          }),
        );

        expect(notifyItemSelected).not.toHaveBeenCalled();
      });

      it("keeps its results when another search form selects an item", async () => {
        await setup({ onBedrijf });
        await searchByKvkNummer();

        itemSelected.next({ selected: true, uuid: "fakeOtherFormUuid" });
        fixture.detectChanges();

        expect(resultRow()).toBeInTheDocument();
      });
    });

    describe("with syncEnabled", () => {
      it("notifies the other search forms when the user selects a bedrijf", async () => {
        await setup({ syncEnabled: true, onBedrijf });
        await searchByKvkNummer();

        await user.click(
          within(resultRow()).getByRole("button", {
            name: "actie.selecteren",
          }),
        );

        expect(notifyItemSelected).toHaveBeenCalledWith(expect.any(String));
      });

      it("clears its results and form when another search form selects an item", async () => {
        await setup({ syncEnabled: true, onBedrijf });
        await searchByKvkNummer();

        itemSelected.next({ selected: true, uuid: "fakeOtherFormUuid" });
        fixture.detectChanges();

        expect(queryResultRow()).not.toBeInTheDocument();
        expect(kvkNummerField()).toHaveValue("");
      });

      it("keeps its results when it was itself the search form that selected an item", async () => {
        await setup({ syncEnabled: true, onBedrijf });
        await searchByKvkNummer();

        await user.click(
          within(resultRow()).getByRole("button", {
            name: "actie.selecteren",
          }),
        );
        const [ownUuid] = notifyItemSelected.mock.lastCall!;
        await searchByKvkNummer();

        itemSelected.next({ selected: true, uuid: ownUuid });
        fixture.detectChanges();

        expect(resultRow()).toBeInTheDocument();
      });

      it("keeps its results when another search form clears its selection", async () => {
        await setup({ syncEnabled: true, onBedrijf });
        await searchByKvkNummer();

        itemSelected.next({ selected: false, uuid: "fakeOtherFormUuid" });
        fixture.detectChanges();

        expect(resultRow()).toBeInTheDocument();
      });
    });
  });

  describe("when no parent listens to the selected bedrijf", () => {
    it("offers to view the bedrijf instead of selecting it", async () => {
      await setup();
      await searchByKvkNummer();

      expect(
        within(resultRow()).getByRole("button", {
          name: "actie.bedrijf.bekijken",
        }),
      ).toBeInTheDocument();
      expect(
        within(resultRow()).queryByRole("button", {
          name: "actie.selecteren",
        }),
      ).not.toBeInTheDocument();
    });

    it("closes the side navigation and opens the vestiging page when the user views a bedrijf", async () => {
      const sideNav = fromPartial<MatSidenav>({ close: jest.fn() });
      await setup({ sideNav });
      await searchByKvkNummer();

      await user.click(
        within(resultRow()).getByRole("button", {
          name: "actie.bedrijf.bekijken",
        }),
      );

      expect(sideNav.close).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith([
        "/bedrijf",
        "12345678",
        "vestiging",
        "000012345678",
      ]);
    });

    it("opens the vestiging page when the user views a bedrijf without a side navigation", async () => {
      await setup();
      await searchByKvkNummer();

      await user.click(
        within(resultRow()).getByRole("button", {
          name: "actie.bedrijf.bekijken",
        }),
      );

      expect(navigate).toHaveBeenCalledWith([
        "/bedrijf",
        "12345678",
        "vestiging",
        "000012345678",
      ]);
    });
  });
});
