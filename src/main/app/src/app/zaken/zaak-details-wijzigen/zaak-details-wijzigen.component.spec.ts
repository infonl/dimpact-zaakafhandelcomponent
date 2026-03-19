/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatDatepickerInputHarness } from "@angular/material/datepicker/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import moment from "moment";
import { of } from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { FormHelper } from "../../shared/form/helpers";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CaseDetailsEditComponent } from "./zaak-details-wijzigen.component";

describe(CaseDetailsEditComponent.name, () => {
  let fixture: ComponentFixture<CaseDetailsEditComponent>;
  let loader: HarnessLoader;
  let component: CaseDetailsEditComponent;

  let referentieTabelService: ReferentieTabelService;
  let utilService: UtilService;
  let identityService: IdentityService;

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
      declarations: [CaseDetailsEditComponent],
      imports: [
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        MaterialFormBuilderModule,
        MaterialModule,
        NoopAnimationsModule,
      ],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDetailsEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;

    referentieTabelService = TestBed.inject(ReferentieTabelService);
    utilService = TestBed.inject(UtilService);
    identityService = TestBed.inject(IdentityService);

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

  const renderComponent = (zaakOverrides = {}) => {
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
      ${0}             | ${true}          | ${"servicenorm is 0"}
      ${30}            | ${false}         | ${"servicenorm is 30"}
      ${1}             | ${false}         | ${"servicenorm is 1"}
    `(
      "should disable einddatumGepland ($expectedDisabled) control when $description",
      async ({ servicenormValue, expectedDisabled }) => {
        // Arrange
        renderComponent({
          zaaktype: {
            uuid: "zaaktype-123",
            omschrijving: "Test zaaktype",
            servicenorm: servicenormValue,
          },
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
        },
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
        },
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
        },
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
    const zaakWithAllDates = (startdatum: string) => ({
      startdatum,
      einddatumGepland: "2024-01-20",
      uiterlijkeEinddatumAfdoening: "2024-01-30",
      zaaktype: {
        uuid: "zaaktype-123",
        omschrijving: "Test zaaktype",
        servicenorm: 30,
      },
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
            servicenorm: 30,
          },
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
          servicenorm: 30,
        },
      });

      component["form"].controls.startdatum.setValue(moment("2024-01-25"));

      expect(component["form"].valid).toBe(false);
    });
  });
});
