/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitCreateComponent } from "./besluit-create.component";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  zaaktype: { uuid: "zaaktype-uuid-1" },
});

const fakeBesluittype = fromPartial<GeneratedType<"RestBesluitType">>({
  id: "besluittype-id-1",
  naam: "Besluittype 1",
  publication: { enabled: false },
});

const fakeBesluittypeWithPublication = fromPartial<
  GeneratedType<"RestBesluitType">
>({
  id: "besluittype-id-2",
  naam: "Besluittype 2",
  publication: {
    enabled: true,
    responseTermDays: 6,
    publicationTermDays: 1,
  },
});

describe(BesluitCreateComponent.name, () => {
  let fixture: ComponentFixture<BesluitCreateComponent>;
  let component: BesluitCreateComponent;
  let zakenService: ZakenService;
  let informatieObjectenService: InformatieObjectenService;
  let sideNav: MatDrawer;
  // The create mutation stays pending so onSuccess/onError never fire; we only
  // assert that submit() forwards the built payload to the mutation.
  let createBesluitMutationFn: jest.Mock;

  const user = userEvent.setup();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BesluitCreateComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);

    jest
      .spyOn(zakenService, "listResultaattypes")
      .mockReturnValue(of([]) as never);
    jest
      .spyOn(zakenService, "listBesluittypes")
      .mockReturnValue(of([fakeBesluittype]) as never);
    jest
      .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
      .mockReturnValue(of([]) as never);

    createBesluitMutationFn = jest.fn(() => new Promise<void>(() => {}));
    jest.spyOn(zakenService, "createBesluit").mockReturnValue(
      fromPartial({
        mutationKey: ["/rest/zaken/besluit"],
        mutationFn: createBesluitMutationFn,
      }),
    );

    fixture = TestBed.createComponent(BesluitCreateComponent);
    component = fixture.componentInstance;

    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    fixture.componentRef.setInput("zaak", fakeZaak);
    fixture.componentRef.setInput("sideNav", sideNav);

    fixture.detectChanges();
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  const submitButton = () =>
    screen.getByRole("button", { name: "actie.aanmaken" });

  describe("initialisation", () => {
    it("loads resultaattypes and besluittypes for the zaak's zaaktype", () => {
      expect(zakenService.listResultaattypes).toHaveBeenCalledWith(
        "zaaktype-uuid-1",
      );
      expect(zakenService.listBesluittypes).toHaveBeenCalledWith(
        "zaaktype-uuid-1",
      );
    });
  });

  describe("close button", () => {
    it("closes the side nav when the close button is clicked", async () => {
      await user.click(
        screen.getByRole("button", { name: "actie.paneel.sluiten" }),
      );

      expect(sideNav.close).toHaveBeenCalled();
    });
  });

  describe("submit button", () => {
    it("is disabled when no besluit is selected", () => {
      expect(submitButton()).toBeDisabled();
    });

    it("is enabled when required fields are set", () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);
      component["form"].markAsDirty();
      fixture.detectChanges();

      expect(submitButton()).toBeEnabled();
    });
  });

  describe("publication section", () => {
    it("is hidden when selected besluittype has publication disabled", () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);
      fixture.detectChanges();

      expect(screen.getByLabelText(/Ingangsdatum/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Vervaldatum/)).toBeInTheDocument();
      expect(screen.queryByLabelText(/Publicatiedatum/)).toBeNull();
      expect(screen.queryByLabelText(/Uiterlijkereactiedatum/)).toBeNull();
    });

    it("is shown when selected besluittype has publication enabled", () => {
      component["form"].controls.besluit.setValue(
        fakeBesluittypeWithPublication,
      );
      fixture.detectChanges();

      expect(screen.getByLabelText(/Publicatiedatum/)).toBeInTheDocument();
      expect(
        screen.getByLabelText(/Uiterlijkereactiedatum/),
      ).toBeInTheDocument();
    });
  });

  describe("documents", () => {
    it("looks up the documents of the zaak that fit the chosen besluittype", () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);

      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith({
        zaakUUID: "zaak-uuid-1",
        besluittypeUUID: "besluittype-id-1",
      });
    });

    it("looks up the documents of the zaak it currently shows", () => {
      fixture.componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "zaak-uuid-2",
          zaaktype: { uuid: "zaaktype-uuid-1" },
        }),
      );
      fixture.detectChanges();

      component["form"].controls.besluit.setValue(fakeBesluittype);

      expect(
        informatieObjectenService.listEnkelvoudigInformatieobjecten,
      ).toHaveBeenCalledWith({
        zaakUUID: "zaak-uuid-2",
        besluittypeUUID: "besluittype-id-1",
      });
    });
  });

  describe("cancel button", () => {
    it("closes the side nav when the cancel button is clicked", async () => {
      await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

      expect(sideNav.close).toHaveBeenCalled();
    });
  });

  describe("submit()", () => {
    it("triggers the create-besluit mutation with the form payload", async () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);

      component.submit();
      await sleep();

      expect(createBesluitMutationFn.mock.calls[0][0]).toEqual(
        expect.objectContaining({
          zaakUuid: "zaak-uuid-1",
          besluittypeUuid: "besluittype-id-1",
        }),
      );
    });

    it("creates the besluit for the zaak it currently shows", async () => {
      fixture.componentRef.setInput(
        "zaak",
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "zaak-uuid-2",
          zaaktype: { uuid: "zaaktype-uuid-1" },
        }),
      );
      fixture.detectChanges();
      component["form"].controls.besluit.setValue(fakeBesluittype);

      component.submit();
      await sleep();

      expect(createBesluitMutationFn.mock.calls[0][0]).toEqual(
        expect.objectContaining({ zaakUuid: "zaak-uuid-2" }),
      );
    });
  });
});
