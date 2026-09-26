/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import {
  ComponentFixture,
  fakeAsync,
  TestBed,
  tick,
} from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of, Subject } from "rxjs";
import { PolicyService } from "src/app/policy/policy.service";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "src/app/shared/material/material.module";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantenService } from "../../klanten.service";
import { FormCommunicatieService } from "../form-communicatie-service";
import { PersoonZoekComponent } from "./persoon-zoek.component";

const fakePersoon = fromPartial<GeneratedType<"RestPersoon">>({
  bsn: "999990408",
  naam: "fakeNaam",
  temporaryPersonId: "fakeTemporaryPersonId",
});

describe(PersoonZoekComponent.name, () => {
  const user = userEvent.setup();

  let component: PersoonZoekComponent;
  let fixture: ComponentFixture<typeof component>;
  let klantenService: KlantenService;
  let loader: HarnessLoader;
  let itemSelected: Subject<{ selected: boolean; uuid: string | null }>;
  let notifyItemSelected: jest.Mock;

  beforeEach(async () => {
    itemSelected = new Subject();
    notifyItemSelected = jest.fn();

    await TestBed.configureTestingModule({
      imports: [
        PersoonZoekComponent,
        FormsModule,
        ReactiveFormsModule,
        MaterialFormBuilderModule,
        MatIconModule,
        MaterialModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: UtilService,
          useValue: {
            setLoading: jest.fn(),
          },
        },
        {
          provide: FormCommunicatieService,
          useValue: {
            itemSelected$: itemSelected.asObservable(),
            notifyItemSelected,
          },
        },
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(klantenService, "getPersonenParameters").mockReturnValue(
      of([
        {
          bsn: "REQ",
          geboortedatum: "OPT",
          gemeenteVanInschrijving: "NON",
          geslachtsnaam: "NON",
          huisnummer: "NON",
          postcode: "NON",
          straat: "NON",
          voornamen: "NON",
          voorvoegsel: "NON",
        },
      ]),
    );
    jest
      .spyOn(klantenService, "listPersonen")
      .mockReturnValue(
        of(fromPartial<GeneratedType<"RESTResultaatRestPersoon">>({})),
      );

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest
      .spyOn(configuratieService, "readGemeenteCode")
      .mockReturnValue(of("1234"));

    TestBed.inject(PolicyService);
  });

  function createComponent(
    inputs: {
      zaaktypeUUID?: string | null;
      sideNav?: MatSidenav;
      syncEnabled?: boolean;
    } = {},
    onPersoon?: (persoon: GeneratedType<"RestPersoon">) => void,
  ) {
    fixture = TestBed.createComponent(PersoonZoekComponent);
    component = fixture.componentInstance;
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    if (onPersoon) component.persoon.subscribe(onPersoon);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  async function searchByBsn(bsn = "999990408") {
    await user.type(screen.getByRole("textbox", { name: "Bsn" }), bsn);
    fixture.detectChanges();
    await user.click(screen.getByRole("button", { name: "actie.zoeken" }));
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function resultRow() {
    return screen.getByRole("row", { name: /999990408/ });
  }

  function queryResultRow() {
    return screen.queryByRole("row", { name: /999990408/ });
  }

  describe(PersoonZoekComponent.prototype.zoekPersonen.name, () => {
    beforeEach(() => createComponent({ zaaktypeUUID: "test-zaaktype-uuid" }));

    it(`should call the ${KlantenService.prototype.listPersonen.name}`, () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.any(Object),
        "test-zaaktype-uuid",
      );
    });

    it("should pass the fields in the request when the form is valid", async () => {
      const spy = jest.spyOn(klantenService, "listPersonen");

      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn] = inputs;
      await bsn.setValue("999990408");

      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          bsn: "999990408",
        }),
        "test-zaaktype-uuid",
      );
    });

    it("should disabled all 'NON' fields when a 'REQ' field is filled", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn, , ...rest] = inputs;

      await bsn.setValue("999990408");

      for (const input of rest) {
        expect(await input.isDisabled()).toBe(true);
      }
    });

    it("should not disable all 'OPT' fields when a 'REQ' field is filled", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn, geboortedatum] = inputs;

      await bsn.setValue("999990408");

      expect(await geboortedatum.isDisabled()).toBe(false);
    });

    it("should extract gemeenteVanInschrijving code when it is an object", () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.formGroup.controls.gemeenteVanInschrijving.setValue({
        code: "0344",
      });

      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          gemeenteVanInschrijving: "0344",
        }),
        "test-zaaktype-uuid",
      );
    });

    it("should pass gemeenteVanInschrijving as string when it is a string", () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.formGroup.controls.gemeenteVanInschrijving.setValue("1234");

      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          gemeenteVanInschrijving: "1234",
        }),
        "test-zaaktype-uuid",
      );
    });
  });

  describe("the zaaktype of the search", () => {
    it("searches with the given zaaktypeUUID when the user searches", async () => {
      createComponent({ zaaktypeUUID: "fakeZaaktypeUuid" });

      await searchByBsn();

      expect(klantenService.listPersonen).toHaveBeenCalledWith(
        expect.objectContaining({ bsn: "999990408" }),
        "fakeZaaktypeUuid",
      );
    });

    it("searches with an empty zaaktype UUID when no zaaktypeUUID is given", async () => {
      createComponent();

      await searchByBsn();

      expect(klantenService.listPersonen).toHaveBeenCalledWith(
        expect.objectContaining({ bsn: "999990408" }),
        "",
      );
    });

    it("searches with the new zaaktypeUUID after the zaaktypeUUID changes", async () => {
      createComponent({ zaaktypeUUID: "fakeZaaktypeUuid" });
      fixture.componentRef.setInput("zaaktypeUUID", "fakeOtherZaaktypeUuid");
      fixture.detectChanges();

      await searchByBsn();

      expect(klantenService.listPersonen).toHaveBeenCalledWith(
        expect.objectContaining({ bsn: "999990408" }),
        "fakeOtherZaaktypeUuid",
      );
    });
  });

  describe("the search button", () => {
    it("is disabled while blockSearch is set, even when the form is valid", async () => {
      createComponent();
      fixture.componentRef.setInput("blockSearch", true);
      fixture.detectChanges();

      await user.type(
        screen.getByRole("textbox", { name: "Bsn" }),
        "999990408",
      );
      fixture.detectChanges();

      expect(
        screen.getByRole("button", { name: "actie.zoeken" }),
      ).toBeDisabled();
    });
  });

  describe("when a parent listens to the selected persoon", () => {
    const onPersoon = jest.fn();

    beforeEach(() => {
      jest.spyOn(klantenService, "listPersonen").mockReturnValue(
        of(
          fromPartial<GeneratedType<"RESTResultaatRestPersoon">>({
            resultaten: [fakePersoon],
          }),
        ),
      );
    });

    it("emits the persoon and clears the results when the user selects a persoon", async () => {
      createComponent({}, onPersoon);
      await searchByBsn();

      await user.click(
        within(resultRow()).getByRole("button", { name: "actie.selecteren" }),
      );
      fixture.detectChanges();

      expect(onPersoon).toHaveBeenCalledWith(fakePersoon);
      expect(queryResultRow()).not.toBeInTheDocument();
      expect(screen.getByRole("textbox", { name: "Bsn" })).toHaveValue("");
    });

    describe("without syncEnabled", () => {
      beforeEach(async () => {
        createComponent({}, onPersoon);
        await searchByBsn();
      });

      it("does not notify the other search forms when the user selects a persoon", async () => {
        await user.click(
          within(resultRow()).getByRole("button", {
            name: "actie.selecteren",
          }),
        );

        expect(notifyItemSelected).not.toHaveBeenCalled();
      });

      it("keeps its results when another search form selects an item", () => {
        itemSelected.next({ selected: true, uuid: "fakeOtherFormUuid" });
        fixture.detectChanges();

        expect(resultRow()).toBeInTheDocument();
      });
    });

    describe("with syncEnabled", () => {
      beforeEach(async () => {
        createComponent({ syncEnabled: true }, onPersoon);
        await searchByBsn();
      });

      it("notifies the other search forms when the user selects a persoon", async () => {
        await user.click(
          within(resultRow()).getByRole("button", {
            name: "actie.selecteren",
          }),
        );

        expect(notifyItemSelected).toHaveBeenCalledWith(expect.any(String));
      });

      it("clears its results and form when another search form selects an item", () => {
        itemSelected.next({ selected: true, uuid: "fakeOtherFormUuid" });
        fixture.detectChanges();

        expect(queryResultRow()).not.toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Bsn" })).toHaveValue("");
      });

      it("keeps its results when it was itself the search form that selected an item", async () => {
        await user.click(
          within(resultRow()).getByRole("button", {
            name: "actie.selecteren",
          }),
        );
        const [ownUuid] = notifyItemSelected.mock.lastCall!;
        await searchByBsn();

        itemSelected.next({ selected: true, uuid: ownUuid });
        fixture.detectChanges();

        expect(resultRow()).toBeInTheDocument();
      });

      it("keeps its results when another search form clears its selection", () => {
        itemSelected.next({ selected: false, uuid: "fakeOtherFormUuid" });
        fixture.detectChanges();

        expect(resultRow()).toBeInTheDocument();
      });
    });
  });

  describe("when no parent listens to the selected persoon", () => {
    let navigate: jest.SpyInstance;

    beforeEach(() => {
      jest.spyOn(klantenService, "listPersonen").mockReturnValue(
        of(
          fromPartial<GeneratedType<"RESTResultaatRestPersoon">>({
            resultaten: [fakePersoon],
          }),
        ),
      );
      navigate = jest
        .spyOn(TestBed.inject(Router), "navigate")
        .mockResolvedValue(true);
    });

    it("offers to view the persoon instead of selecting it", async () => {
      createComponent();
      await searchByBsn();

      expect(
        within(resultRow()).getByRole("button", {
          name: "actie.persoon.bekijken",
        }),
      ).toBeInTheDocument();
      expect(
        within(resultRow()).queryByRole("button", {
          name: "actie.selecteren",
        }),
      ).not.toBeInTheDocument();
    });

    it("closes the side navigation and opens the persoon page when the user views a persoon", async () => {
      const sideNav = fromPartial<MatSidenav>({ close: jest.fn() });
      createComponent({ sideNav });
      await searchByBsn();

      await user.click(
        within(resultRow()).getByRole("button", {
          name: "actie.persoon.bekijken",
        }),
      );

      expect(sideNav.close).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith([
        "/persoon/",
        "fakeTemporaryPersonId",
      ]);
    });

    it("opens the persoon page when the user views a persoon without a side navigation", async () => {
      createComponent();
      await searchByBsn();

      await user.click(
        within(resultRow()).getByRole("button", {
          name: "actie.persoon.bekijken",
        }),
      );

      expect(navigate).toHaveBeenCalledWith([
        "/persoon/",
        "fakeTemporaryPersonId",
      ]);
    });
  });

  describe("brpGemeenten effect", () => {
    beforeEach(() => createComponent({ zaaktypeUUID: "test-zaaktype-uuid" }));

    it("should auto-set gemeenteVanInschrijving when exactly one gemeente is returned", fakeAsync(() => {
      testQueryClient.setQueryData(
        klantenService.listAuthorisedBrpGemeenten().queryKey,
        [{ code: "0344", naam: "Utrecht" }],
      );

      tick();
      fixture.detectChanges();

      expect(
        component.formGroup.controls.gemeenteVanInschrijving.value,
      ).toEqual({ code: "0344", naam: "Utrecht" });
    }));
  });
});
