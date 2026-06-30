/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MomentDateAdapter,
} from "@angular/material-moment-adapter";
import { MatButtonHarness } from "@angular/material/button/testing";
import {
  DateAdapter,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE,
} from "@angular/material/core";
import { MatDatepickerInputHarness } from "@angular/material/datepicker/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import moment from "moment";
import { of } from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { fromPartial } from "src/test-helpers";
import { IdentityService } from "../../identity/identity.service";
import { FormHelper } from "../../shared/form/helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { CaseDetailsEditComponent } from "./zaak-details-wijzigen.component";

describe(CaseDetailsEditComponent.name, () => {
  let fixture: ComponentFixture<CaseDetailsEditComponent>;
  let loader: HarnessLoader;
  let component: CaseDetailsEditComponent;

  let referentieTabelService: ReferentieTabelService;
  let utilService: UtilService;
  let identityService: IdentityService;
  let zakenService: ZakenService;

  const mockSideNav = fromPartial<MatDrawer>({
    close: jest.fn(),
  });

  const mockLoggedInUser = fromPartial<GeneratedType<"RestLoggedInUser">>({
    id: "user-123",
    naam: "testuser",
  });

  const baseZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-123",
    omschrijving: "Test zaak",
    startdatum: "2024-01-01",
    communicatiekanaal: "email",
    vertrouwelijkheidaanduiding: "OPENBAAR",
    rechten: {
      wijzigen: true,
      wijzigenDoorlooptijd: true,
      toekennen: true,
    },
    zaaktype: {
      uuid: "zaaktype-123",
      omschrijving: "Test zaaktype",
    },
    isProcesGestuurd: false,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CaseDetailsEditComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: DateAdapter,
          useClass: MomentDateAdapter,
          deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS],
        },
        {
          provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS,
          useValue: { strict: false },
        },
        {
          provide: MAT_DATE_FORMATS,
          useValue: {
            parse: { dateInput: "yyyy-MM-DD" },
            display: {
              dateInput: "yyyy-MM-DD",
              monthYearLabel: "MMMM YYYY",
              dateA11yLabel: "LL",
              monthYearA11yLabel: "MMMM YYYY",
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDetailsEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;

    referentieTabelService = TestBed.inject(ReferentieTabelService);
    utilService = TestBed.inject(UtilService);
    identityService = TestBed.inject(IdentityService);
    zakenService = TestBed.inject(ZakenService);

    // Mock service methods
    jest
      .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
      .mockReturnValue(of([]));
    jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([]));
    jest
      .spyOn(referentieTabelService, "listCommunicatiekanalen")
      .mockReturnValue(of(["email", "telefoon", "post"]));
    jest.spyOn(utilService, "getEnumAsSelectList").mockReturnValue([
      { label: "Openbaar", value: "openbaar" },
      { label: "Beperkt openbaar", value: "beperkt_openbaar" },
    ]);
  });

  const renderComponent = (
    zaakOverrides: Partial<GeneratedType<"RestZaak">> = {},
  ) => {
    const zaak = fromPartial<GeneratedType<"RestZaak">>({
      ...baseZaak,
      ...zaakOverrides,
    });

    component.zaak = zaak;
    component.loggedInUser = mockLoggedInUser;
    component.sideNav = mockSideNav;

    fixture.detectChanges();
  };

  describe("einddatumGepland control disabling", () => {
    it.each`
      servicenormValue | expectedDisabled | description
      ${undefined}     | ${true}          | ${"servicenorm is undefined"}
      ${null}          | ${true}          | ${"servicenorm is null"}
      ${false}         | ${true}          | ${"servicenorm is false"}
      ${true}          | ${false}         | ${"servicenorm is true"}
    `(
      "should disable einddatumGepland ($expectedDisabled) control when $description",
      async ({
        servicenormValue,
        expectedDisabled,
      }: {
        servicenormValue: boolean | null | undefined;
        expectedDisabled: boolean;
      }) => {
        // Arrange
        renderComponent({
          zaaktype: {
            uuid: "zaaktype-123",
            omschrijving: "Test zaaktype",
            servicenorm: servicenormValue,
          } as Partial<
            GeneratedType<"RestZaaktype">
          > as unknown as GeneratedType<"RestZaaktype">,
        });

        // Act
        await fixture.whenStable();

        // Assert
        const dateInputs = await loader.getAllHarnesses(
          MatDatepickerInputHarness,
        );
        const einddatumGeplandInput = dateInputs[1]; // einddatumGepland
        expect(await einddatumGeplandInput.isDisabled()).toBe(expectedDisabled);
      },
    );
  });

  describe("form control disabling based on permissions", () => {
    it("should disable specific controls when user lacks wijzigen permission", async () => {
      // Arrange
      renderComponent({
        rechten: {
          wijzigen: false,
          wijzigenDoorlooptijd: true,
          toekennen: true,
        } as Partial<
          GeneratedType<"RestZaakRechten">
        > as unknown as GeneratedType<"RestZaakRechten">,
      });

      // Act
      await fixture.whenStable();

      // Assert
      const selects = await loader.getAllHarnesses(MatSelectHarness);

      expect(await selects[2].isDisabled()).toBe(true); // communicatiekanaal
      expect(await selects[3].isDisabled()).toBe(true); // vertrouwelijkheidaanduiding
    });

    it("should disable controls when user lacks toekennen permission", async () => {
      // Arrange
      renderComponent({
        rechten: {
          wijzigen: true,
          wijzigenDoorlooptijd: true,
          toekennen: false,
        } as Partial<
          GeneratedType<"RestZaakRechten">
        > as unknown as GeneratedType<"RestZaakRechten">,
      });

      // Act
      await fixture.whenStable();

      // Assert
      const selects = await loader.getAllHarnesses(MatSelectHarness);
      expect(await selects[0].isDisabled()).toBe(true); // groep
    });

    it("should disable date controls when date changes are not allowed", async () => {
      // Arrange
      renderComponent({
        rechten: {
          wijzigen: true,
          wijzigenDoorlooptijd: false,
          toekennen: true,
        } as Partial<
          GeneratedType<"RestZaakRechten">
        > as unknown as GeneratedType<"RestZaakRechten">,
        isProcesGestuurd: false,
      });

      // Act
      await fixture.whenStable();

      // Assert
      const dateInputs = await loader.getAllHarnesses(
        MatDatepickerInputHarness,
      );
      expect(await dateInputs[0].isDisabled()).toBe(true); // startdatum
      expect(await dateInputs[2].isDisabled()).toBe(true); // uiterlijkeEinddatumAfdoening
    });
  });

  describe("date validation", () => {
    const zaakWithAllDates = (
      startdatum: string,
    ): Partial<GeneratedType<"RestZaak">> => ({
      startdatum,
      einddatumGepland: "2024-01-20",
      uiterlijkeEinddatumAfdoening: "2024-01-30",
      zaaktype: {
        uuid: "zaaktype-123",
        omschrijving: "Test zaaktype",
        servicenorm: true,
      } as Partial<
        GeneratedType<"RestZaaktype">
      > as unknown as GeneratedType<"RestZaaktype">,
    });

    describe("start-na-streef: startdatum after einddatumGepland", () => {
      it("sets error on startdatum when startdatum changes to after einddatumGepland", () => {
        renderComponent(zaakWithAllDates("2024-01-10"));

        component["form"].controls.startdatum.setValue(moment("2024-01-25"));

        expect(component["form"].controls.startdatum.errors).toEqual(
          FormHelper.CustomErrorMessage(
            "msg.error.date.invalid.datum.start-na-streef",
          ),
        );
      });

      it("sets error on einddatumGepland when einddatumGepland changes to before startdatum", () => {
        renderComponent(zaakWithAllDates("2024-01-10"));

        component["form"].controls.einddatumGepland.setValue(
          moment("2024-01-05"),
        );

        expect(component["form"].controls.einddatumGepland.errors).toEqual(
          FormHelper.CustomErrorMessage(
            "msg.error.date.invalid.datum.start-na-streef",
          ),
        );
      });
    });

    describe("start-na-fatale: startdatum after uiterlijkeEinddatumAfdoening", () => {
      it("sets error on startdatum when startdatum changes to after uiterlijkeEinddatumAfdoening", () => {
        renderComponent(zaakWithAllDates("2024-01-10"));

        component["form"].controls.startdatum.setValue(moment("2024-02-01"));

        expect(component["form"].controls.startdatum.errors).toEqual(
          FormHelper.CustomErrorMessage(
            "msg.error.date.invalid.datum.start-na-fatale",
          ),
        );
      });

      it("sets error on uiterlijkeEinddatumAfdoening when it changes to before startdatum", () => {
        renderComponent({
          startdatum: "2024-01-20",
          uiterlijkeEinddatumAfdoening: "2024-02-01",
          zaaktype: {
            uuid: "zaaktype-123",
            omschrijving: "Test zaaktype",
            servicenorm: true,
          } as Partial<
            GeneratedType<"RestZaaktype">
          > as unknown as GeneratedType<"RestZaaktype">,
        });

        component["form"].controls.uiterlijkeEinddatumAfdoening.setValue(
          moment("2024-01-15"),
        );

        expect(
          component["form"].controls.uiterlijkeEinddatumAfdoening.errors,
        ).toEqual(
          FormHelper.CustomErrorMessage(
            "msg.error.date.invalid.datum.start-na-fatale",
          ),
        );
      });
    });

    describe("streef-na-fatale: einddatumGepland after uiterlijkeEinddatumAfdoening", () => {
      it("sets error on einddatumGepland when it changes to after uiterlijkeEinddatumAfdoening", () => {
        renderComponent(zaakWithAllDates("2024-01-01"));

        component["form"].controls.einddatumGepland.setValue(
          moment("2024-02-01"),
        );

        expect(component["form"].controls.einddatumGepland.errors).toEqual(
          FormHelper.CustomErrorMessage(
            "msg.error.date.invalid.datum.streef-na-fatale",
          ),
        );
      });

      it("sets error on uiterlijkeEinddatumAfdoening when it changes to before einddatumGepland", () => {
        renderComponent(zaakWithAllDates("2024-01-01"));

        component["form"].controls.uiterlijkeEinddatumAfdoening.setValue(
          moment("2024-01-15"),
        );

        expect(
          component["form"].controls.uiterlijkeEinddatumAfdoening.errors,
        ).toEqual(
          FormHelper.CustomErrorMessage(
            "msg.error.date.invalid.datum.streef-na-fatale",
          ),
        );
      });
    });
  });

  describe("current group in groep dropdown", () => {
    it("should call listBehandelaarGroupsForZaaktype with the zaaktype omschrijving", async () => {
      renderComponent();
      await fixture.whenStable();

      expect(
        identityService.listBehandelaarGroupsForZaaktype,
      ).toHaveBeenCalledWith("Test zaaktype");
    });

    it("should prepend the current group when it is not in the list", async () => {
      const activeGroup = fromPartial<GeneratedType<"RestGroup">>({
        id: "g1",
        naam: "Active Group",
        active: true,
      });
      jest
        .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
        .mockReturnValue(of([activeGroup]));

      renderComponent({
        groep: { id: "g-inactive", naam: "Inactive Group", active: false },
      });
      await fixture.whenStable();

      let groups: GeneratedType<"RestGroup">[] = [];
      component["groups"].subscribe(
        (fetchedGroups) => (groups = fetchedGroups),
      );

      expect(groups).toHaveLength(2);
      expect(groups[0]).toMatchObject({ id: "g-inactive", active: false });
      expect(groups[1]).toEqual(activeGroup);
    });

    it("should not prepend the group when it is already in the list", async () => {
      const activeGroup = fromPartial<GeneratedType<"RestGroup">>({
        id: "g1",
        naam: "Active Group",
        active: true,
      });
      jest
        .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
        .mockReturnValue(of([activeGroup]));

      renderComponent({
        groep: { id: "g1", naam: "Active Group", active: true },
      });
      await fixture.whenStable();

      let groups: GeneratedType<"RestGroup">[] = [];
      component["groups"].subscribe(
        (fetchedGroups) => (groups = fetchedGroups),
      );

      expect(groups).toHaveLength(1);
      expect(groups[0]).toEqual(activeGroup);
    });

    it("should not prepend when zaak.groep is undefined", async () => {
      const activeGroup = fromPartial<GeneratedType<"RestGroup">>({
        id: "g1",
        naam: "Active Group",
        active: true,
      });
      jest
        .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
        .mockReturnValue(of([activeGroup]));

      renderComponent({ groep: undefined });
      await fixture.whenStable();

      let groups: GeneratedType<"RestGroup">[] = [];
      component["groups"].subscribe(
        (fetchedGroups) => (groups = fetchedGroups),
      );

      expect(groups).toHaveLength(1);
    });
  });

  describe("groupDisplayValue", () => {
    beforeEach(() => renderComponent());

    it("should append (inactief) suffix for an inactive group", () => {
      const group = fromPartial<GeneratedType<"RestGroup">>({
        id: "g1",
        naam: "Test Group",
        active: false,
      });
      const result = component["groupDisplayValue"](group);
      expect(result).toContain("Test Group");
      expect(result).toContain("inactief");
    });

    it("should return just the naam for an active group", () => {
      const group = fromPartial<GeneratedType<"RestGroup">>({
        id: "g1",
        naam: "Test Group",
        active: true,
      });
      expect(component["groupDisplayValue"](group)).toBe("Test Group");
    });
  });

  describe("submit button validity after date changes", () => {
    it("form is invalid when startdatum is cleared", () => {
      renderComponent();

      component["form"].controls.startdatum.reset();

      expect(component["form"].valid).toBe(false);
    });

    it("form is invalid when a date violation exists", () => {
      renderComponent({
        startdatum: "2024-01-10",
        einddatumGepland: "2024-01-20",
        uiterlijkeEinddatumAfdoening: "2024-01-30",
        zaaktype: {
          uuid: "zaaktype-123",
          omschrijving: "Test zaaktype",
          servicenorm: true,
        } as Partial<
          GeneratedType<"RestZaaktype">
        > as unknown as GeneratedType<"RestZaaktype">,
      });

      component["form"].controls.startdatum.setValue(moment("2024-01-25"));

      expect(component["form"].valid).toBe(false);
    });
  });

  describe("initialisation", () => {
    it("maps vertrouwelijkheidaanduiding to the matching select option (case-insensitive)", () => {
      renderComponent({ vertrouwelijkheidaanduiding: "OPENBAAR" });
      expect(
        component["form"].controls.vertrouwelijkheidaanduiding.value?.value,
      ).toBe("openbaar");
    });

    it("patches toelichting from the zaak value", () => {
      renderComponent({ toelichting: "een toelichting" });
      expect(component["form"].controls.toelichting.value).toBe(
        "een toelichting",
      );
    });
  });

  describe("reden field", () => {
    it("is disabled on initialisation", () => {
      renderComponent();
      expect(component["form"].controls.reden.disabled).toBe(true);
    });

    it("becomes enabled when the form is marked dirty via a value change", () => {
      renderComponent();
      component["form"].controls.omschrijving.markAsDirty();
      component["form"].controls.omschrijving.updateValueAndValidity();
      expect(component["form"].controls.reden.enabled).toBe(true);
    });
  });

  describe("groep valueChanges", () => {
    it("loads users and enables behandelaar when a group is selected (toekennen=true)", async () => {
      renderComponent();
      const group = fromPartial<GeneratedType<"RestGroup">>({
        id: "g2",
        naam: "Groep 2",
      });
      jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(
        of([
          fromPartial<GeneratedType<"RestUser">>({
            id: "u1",
            naam: "User 1",
          }),
        ]),
      );
      component["form"].controls.groep.setValue(group);
      await fixture.whenStable();
      expect(identityService.listUsersInGroup).toHaveBeenCalledWith("g2");
      expect(component["form"].controls.behandelaar.enabled).toBe(true);
    });

    it("resets and disables behandelaar when groep is set to null", () => {
      renderComponent();
      component["form"].controls.groep.setValue(null);
      expect(component["form"].controls.behandelaar.disabled).toBe(true);
      expect(component["form"].controls.behandelaar.value).toBeNull();
    });
  });

  describe("close and cancel buttons", () => {
    it("calls sideNav.close() when the close icon-button is clicked", async () => {
      renderComponent();
      const buttons = await loader.getAllHarnesses(MatButtonHarness);
      await buttons[0].click();
      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("calls sideNav.close() when the cancel button is clicked", async () => {
      renderComponent();
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.annuleren/ }),
      );
      await cancelButton.click();
      expect(mockSideNav.close).toHaveBeenCalled();
    });
  });

  describe("submit button", () => {
    it("is disabled when the form is pristine", async () => {
      renderComponent();
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.opslaan/ }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("is enabled when form is valid and dirty", async () => {
      const group = fromPartial<GeneratedType<"RestGroup">>({
        id: "g1",
        naam: "G1",
      });
      jest
        .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
        .mockReturnValue(of([group]));
      renderComponent({ groep: group });
      await fixture.whenStable();
      component["form"].controls.reden.enable();
      component["form"].controls.reden.setValue("een reden");
      component["form"].markAsDirty();
      fixture.detectChanges();
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.opslaan/ }),
      );
      expect(await submitButton.isDisabled()).toBe(false);
    });
  });

  describe("onSubmit()", () => {
    it("calls updateZaak and closes sideNav on success", async () => {
      renderComponent();
      jest
        .spyOn(zakenService, "updateZaak")
        .mockReturnValue(of(undefined) as never);
      jest
        .spyOn(zakenService, "toekennen")
        .mockReturnValue(of(undefined) as never);
      component["form"].controls.reden.enable();
      component["form"].controls.reden.setValue("een reden");

      await component["onSubmit"](component["form"]);

      expect(zakenService.updateZaak).toHaveBeenCalledWith(
        "zaak-123",
        expect.objectContaining({ reden: "een reden" }),
      );
      expect(mockSideNav.close).toHaveBeenCalled();
    });

    it("calls toekennenAanIngelogdeMedewerker when behandelaar equals the logged-in user", async () => {
      // zaak has no behandelaar → isSameBehandelaar will be false; keep groep unchanged to avoid subscription
      renderComponent();
      jest
        .spyOn(zakenService, "updateZaak")
        .mockReturnValue(of(undefined) as never);
      jest
        .spyOn(zakenService, "toekennenAanIngelogdeMedewerker")
        .mockReturnValue(of(undefined) as never);
      component["form"].controls.behandelaar.enable();
      component["form"].controls.behandelaar.setValue(
        fromPartial<GeneratedType<"RestUser">>({
          id: "user-123",
          naam: "User 123",
        }),
      );
      component["form"].controls.reden.enable();
      component["form"].controls.reden.setValue("reden");

      await component["onSubmit"](component["form"]);

      expect(zakenService.toekennenAanIngelogdeMedewerker).toHaveBeenCalled();
    });

    it("calls toekennen when behandelaar differs from the logged-in user", async () => {
      // zaak has no behandelaar → isSameBehandelaar will be false; keep groep unchanged to avoid subscription
      renderComponent();
      jest
        .spyOn(zakenService, "updateZaak")
        .mockReturnValue(of(undefined) as never);
      jest
        .spyOn(zakenService, "toekennen")
        .mockReturnValue(of(undefined) as never);
      component["form"].controls.behandelaar.enable();
      component["form"].controls.behandelaar.setValue(
        fromPartial<GeneratedType<"RestUser">>({
          id: "other-user",
          naam: "Other User",
        }),
      );
      component["form"].controls.reden.enable();
      component["form"].controls.reden.setValue("reden");

      await component["onSubmit"](component["form"]);

      expect(zakenService.toekennen).toHaveBeenCalled();
    });

    it("skips patchBehandelaar when behandelaar and groep are unchanged", async () => {
      renderComponent();
      jest
        .spyOn(zakenService, "updateZaak")
        .mockReturnValue(of(undefined) as never);
      jest.spyOn(zakenService, "toekennen");
      jest.spyOn(zakenService, "toekennenAanIngelogdeMedewerker");
      component["form"].controls.reden.enable();
      component["form"].controls.reden.setValue("reden");

      await component["onSubmit"](component["form"]);

      expect(zakenService.toekennen).not.toHaveBeenCalled();
      expect(
        zakenService.toekennenAanIngelogdeMedewerker,
      ).not.toHaveBeenCalled();
    });
  });
});
