/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatExpansionPanelHarness } from "@angular/material/expansion/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { QueryClient } from "@tanstack/query-core";
import { fromPartial } from "@total-typescript/shoehorn";
import { testQueryClient } from "../../../../setupJest";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZaakAfhandelenDialogComponent } from "./zaak-afhandelen-dialog.component";

describe(ZaakAfhandelenDialogComponent.name, () => {
  let fixture: ComponentFixture<ZaakAfhandelenDialogComponent>;
  let loader: HarnessLoader;

  let dialogRef: MatDialogRef<ZaakAfhandelenDialogComponent>;
  let queryClient: QueryClient;
  let httpTestingController: HttpTestingController;

  const mockDialogRef = {
    close: jest.fn(),
    disableClose: false,
  };

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "test-zaak-uuid",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "test-zaaktype-uuid",
      omschrijving: "Test Zaaktype",
      zaakafhandelparameters: {
        afrondenMail: "BESCHIKBAAR_UIT",
      },
    }),
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "BSN",
      bsnNummer: "123456789",
    }),
    resultaat: null,
    besluiten: [],
  });

  const mockPlanItem = fromPartial<GeneratedType<"RESTPlanItem">>({
    id: "test-plan-item-id",
    userEventListenerActie: "ZAAK_AFHANDELEN",
    toelichting: "Test toelichting",
  });

  const mockResultaattypes = [
    fromPartial<GeneratedType<"RestResultaattype">>({
      id: "resultaat-1",
      naam: "Test Resultaat 1",
      besluitVerplicht: false,
      datumKenmerkVerplicht: true,
    }),
    fromPartial<GeneratedType<"RestResultaattype">>({
      id: "resultaat-2",
      naam: "Test Resultaat 2",
      besluitVerplicht: true,
      datumKenmerkVerplicht: false,
    }),
    fromPartial<GeneratedType<"RestResultaattype">>({
      id: "resultaat-3",
      naam: "Test Resultaat 3",
      besluitVerplicht: false,
      datumKenmerkVerplicht: false,
    }),
  ];

  const mockAfzenders = [
    fromPartial<GeneratedType<"RestZaakAfzender">>({
      mail: "test@example.com",
      suffix: "Test Afzender",
      replyTo: "reply@example.com",
    }),
  ];

  const mockMailtemplate = fromPartial<GeneratedType<"RESTMailtemplate">>({
    onderwerp: "Test Onderwerp",
    body: "Test Body",
  });

  const createTestBed = async (
    zaakMock: GeneratedType<"RestZaak">,
    planItemMock?: GeneratedType<"RESTPlanItem"> | null,
  ) => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      declarations: [ZaakAfhandelenDialogComponent, StaticTextComponent],
      imports: [
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        {
          provide: MatDialogRef,
          useValue: mockDialogRef,
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            zaak: zaakMock,
            planItem: planItemMock,
          },
        },
        CustomValidators,
      ],
    }).compileComponents();

    dialogRef = TestBed.inject(MatDialogRef);
    queryClient = TestBed.inject(QueryClient);
    httpTestingController = TestBed.inject(HttpTestingController);

    queryClient.setQueryData(
      ["resultaattypes", zaakMock.zaaktype.uuid],
      mockResultaattypes,
    );
    queryClient.setQueryData(["afzenders", zaakMock.uuid], mockAfzenders);
    queryClient.setQueryData(["mailtemplate", zaakMock.uuid], mockMailtemplate);
    queryClient.setQueryData(
      ["initiatorEmail", zaakMock.initiatorIdentificatie?.bsnNummer],
      { emailadres: "initiator@example.com" },
    );

    fixture = TestBed.createComponent(ZaakAfhandelenDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    await createTestBed(mockZaak, mockPlanItem);
  });

  describe("sendMail checkbox", () => {
    it("should show mail fields when sendMail is checked", async () => {
      const sendMailCheckbox = await loader.getHarness(MatCheckboxHarness);
      await sendMailCheckbox.check();

      const fields = await loader.getAllHarnesses(MatSelectHarness);
      const verzenderField = fields[1];

      expect(verzenderField).toBeTruthy();
    });

    it("should hide mail fields when sendMail is unchecked", async () => {
      const sendMailCheckbox = await loader.getHarness(MatCheckboxHarness);
      await sendMailCheckbox.uncheck();

      const fields = await loader.getAllHarnesses(MatSelectHarness);
      const verzenderField = fields[1];

      expect(verzenderField).toBeFalsy();
    });
  });

  describe("resultaattype selection", () => {
    it("should show besluitVastleggen button when resultaattype requires besluit", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[2]?.click();

      const besluitButton = await loader.getHarnessOrNull(
        MatButtonHarness.with({ text: /actie\.besluit\.vastleggen/ }),
      );
      const isDisabled = await besluitButton?.isDisabled();
      expect(isDisabled).toBeFalsy();
    });

    it("should open besluit vastleggen when besluit button is clicked", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();

      await options[1]?.click();

      const button = await loader.getHarness(
        MatButtonHarness.with({
          text: /actie\.besluit\.vastleggen/,
        }),
      );
      await button.click();

      expect(dialogRef.close).toHaveBeenCalledWith("openBesluitVastleggen");
    });
  });

  describe("actions", () => {
    it("should close dialog when close button is clicked", async () => {
      const closeButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.annuleren/ }),
      );
      await closeButton.click();

      expect(dialogRef.close).toHaveBeenCalled();
    });
  });

  describe("form validation", () => {
    it("should disable submit button when form is invalid", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );
      const isDisabled = await submitButton.isDisabled();

      expect(isDisabled).toBe(true);
    });

    it("should enable submit button when form is valid", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[2]?.click();

      await fixture.whenStable();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );

      const isDisabled = await submitButton.isDisabled();

      expect(isDisabled).toBe(false);
    });
  });

  describe("form submission", () => {
    it("should call planItem mutation on submit", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[2]?.click();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );

      await submitButton.click();
      await fixture.whenStable();

      const req = httpTestingController.expectOne(
        `/rest/planitems/doUserEventListenerPlanItem`,
      );
      expect(req.request.method).toEqual("POST");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          actie: "ZAAK_AFHANDELEN",
          planItemInstanceId: "test-plan-item-id",
          zaakUuid: "test-zaak-uuid",
          resultaattypeUuid: "resultaat-3",
        }),
      );
    });

    it("should send over a 'brondatumEigenschap' when a brondatumEigenschap is required", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[0]?.click(); // Select a type that requires brondatumEigenschap

      const inputs = await loader.getAllHarnesses(MatInputHarness);

      await inputs[0].setValue("test toelichting");
      await inputs[1].setValue("2022-01-01");

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );

      await submitButton.click();
      await fixture.whenStable();

      const req = httpTestingController.expectOne(
        `/rest/planitems/doUserEventListenerPlanItem`,
      );
      expect(req.request.method).toEqual("POST");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          actie: "ZAAK_AFHANDELEN",
          planItemInstanceId: "test-plan-item-id",
          zaakUuid: "test-zaak-uuid",
          resultaattypeUuid: "resultaat-1",
          resultaatToelichting: "test toelichting",
          brondatumEigenschap: "2021-12-31T23:00:00.000Z",
        }),
      );
    });

    it("should not allow the form to be submitted when a brondatumEigenschap is required", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[0]?.click();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );
      const isDisabled = await submitButton.isDisabled();

      expect(isDisabled).toBe(true);
    });
  });

  describe("mail expansion panel", () => {
    it("should show mail body in expansion panel when sendMail is checked", async () => {
      const sendMailCheckbox = await loader.getHarness(MatCheckboxHarness);
      await sendMailCheckbox.check();
      fixture.detectChanges();

      const expansionPanel = await loader.getHarness(MatExpansionPanelHarness);
      expect(expansionPanel).toBeTruthy();

      const panelText = await expansionPanel.getTextContent();
      expect(panelText).toContain("Test Body");
    });
  });

  describe("mail suffix logic", () => {
    it("should show suffix in the dropdown options before selection", async () => {
      const sendMailCheckbox = await loader.getHarness(MatCheckboxHarness);
      await sendMailCheckbox.check();
      fixture.detectChanges();

      const selects = await loader.getAllHarnesses(MatSelectHarness);
      const verzenderSelect = selects[1];

      await verzenderSelect.open();
      const options = await verzenderSelect.getOptions();
      const optionText = await options[0].getText();

      expect(optionText).toContain("test@example.com  Test Afzender");
    });

    it("should not show suffix in the select box when an afzender is selected", async () => {
      const sendMailCheckbox = await loader.getHarness(MatCheckboxHarness);
      await sendMailCheckbox.check();
      fixture.detectChanges();

      const selects = await loader.getAllHarnesses(MatSelectHarness);
      const verzenderSelect = selects[1];

      await verzenderSelect.open();
      const options = await verzenderSelect.getOptions();
      await options[0].click();
      fixture.detectChanges();

      const valueText = await verzenderSelect.getValueText();

      expect(valueText).toBe("test@example.com");
      expect(valueText).not.toContain("Test Afzender");
    });
  });

  describe("zaak afhandelen button visibility", () => {
    test.each([
      [
        {
          resultaatType: { id: "test-id-1", besluitVerplicht: true },
          besluiten: [],
        },
        false,
      ],
      [
        {
          resultaatType: { id: "test-id-2", besluitVerplicht: true },
          besluiten: [
            fromPartial<GeneratedType<"RestDecision">>({
              uuid: "mock-besluit-uuid",
              url: "http://example.com/besluit",
            }),
          ],
        },
        true,
      ],
      [
        {
          resultaatType: { id: "test-id-3", besluitVerplicht: false },
          besluiten: [
            fromPartial<GeneratedType<"RestDecision">>({
              uuid: "mock-besluit-uuid",
              url: "http://example.com/besluit",
            }),
          ],
        },
        true,
      ],
      [
        {
          resultaatType: { id: "test-id-4", besluitVerplicht: false },
          besluiten: [],
        },
        true,
      ],
    ])(
      "should show submit button correctly for besluitVerplicht=%s and besluiten=%s",
      async (state, showSubmitButton) => {
        const component = fixture.componentInstance;

        component.data.zaak.besluiten = state.besluiten;

        component.formGroup.patchValue({ resultaattype: state.resultaatType });

        fixture.detectChanges();
        await fixture.whenStable();

        const submitButton = await loader.getHarnessOrNull(
          MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
        );

        if (showSubmitButton) {
          expect(submitButton).toBeTruthy();
        } else {
          expect(submitButton).toBeNull();
        }
      },
    );
  });

  describe("Open dialog with zaakafhandelparameters afrondenMail BESCHIKBAAR_AAN", () => {
    beforeEach(async () => {
      const mockZaakWithAfrondenMailAan = fromPartial<
        GeneratedType<"RestZaak">
      >({
        ...mockZaak,
        uuid: "test-zaak-uuid-afronden-aan",
        zaaktype: {
          ...mockZaak.zaaktype,
          zaakafhandelparameters: {
            afrondenMail: "BESCHIKBAAR_AAN",
          },
        },
      });

      await createTestBed(mockZaakWithAfrondenMailAan, mockPlanItem);
    });

    it("should show sendMail checkbox checked", async () => {
      const sendMailCheckbox = await loader.getHarness(MatCheckboxHarness);
      expect(await sendMailCheckbox.isChecked()).toBe(true);
    });

    it("should show verzender field", async () => {
      const fields = await loader.getAllHarnesses(MatSelectHarness);
      const verzenderField = fields[1];
      expect(verzenderField).toBeTruthy();
    });
  });

  describe("Open dialog with zaakafhandelparameters afrondenMail NIET_BESCHIKBAAR", () => {
    beforeEach(async () => {
      const mockZaakWithAfrondenMailNietBeschikbaar = fromPartial<
        GeneratedType<"RestZaak">
      >({
        ...mockZaak,
        uuid: "test-zaak-uuid-afronden-niet-beschikbaar",
        zaaktype: {
          ...mockZaak.zaaktype,
          zaakafhandelparameters: {
            afrondenMail: "NIET_BESCHIKBAAR",
          },
        },
      });

      await createTestBed(
        mockZaakWithAfrondenMailNietBeschikbaar,
        mockPlanItem,
      );
    });

    it("should not show sendMail checkbox", async () => {
      const sendMailCheckbox =
        await loader.getHarnessOrNull(MatCheckboxHarness);
      expect(sendMailCheckbox).toBeNull();
    });

    it("should not show verzender field", async () => {
      const fields = await loader.getAllHarnesses(MatSelectHarness);
      const verzenderField = fields[1];
      expect(verzenderField).toBeFalsy();
    });
  });

  describe("Open dialog with planItem null (reopened case)", () => {
    beforeEach(async () => {
      const mockZaakWithNoPlanItem = fromPartial<GeneratedType<"RestZaak">>({
        ...mockZaak,
        uuid: "test-zaak-uuid-no-planitem",
      });

      await createTestBed(mockZaakWithNoPlanItem, null);
    });

    it("should call afsluiten mutation on submit", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[2]?.click();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );

      await submitButton.click();
      await fixture.whenStable();

      const req = httpTestingController.expectOne(
        `/rest/zaken/zaak/test-zaak-uuid-no-planitem/afsluiten`,
      );
      expect(req.request.method).toEqual("PATCH");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          resultaattypeUuid: "resultaat-3",
        }),
      );
    });
  });
});
