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
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatExpansionPanelHarness } from "@angular/material/expansion/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { EMPTY, of } from "rxjs";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";
import { ZaakAfhandelenDialogComponent } from "./zaak-afhandelen-dialog.component";

describe(ZaakAfhandelenDialogComponent.name, () => {
  let fixture: ComponentFixture<ZaakAfhandelenDialogComponent>;
  let loader: HarnessLoader;

  let dialogRef: MatDialogRef<ZaakAfhandelenDialogComponent>;
  let zakenService: ZakenService;
  let planItemsService: PlanItemsService;
  let mailtemplateService: MailtemplateService;
  let klantenService: KlantenService;

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
        afrondenMail: "BESCHIKBAAR_AAN",
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
      id: "resultaat-2",
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

  beforeEach(async () => {
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
        {
          provide: MatDialogRef,
          useValue: mockDialogRef,
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            zaak: mockZaak,
            planItem: mockPlanItem,
          },
        },
        CustomValidators,
      ],
    }).compileComponents();

    dialogRef = TestBed.inject(MatDialogRef);
    zakenService = TestBed.inject(ZakenService);
    planItemsService = TestBed.inject(PlanItemsService);
    mailtemplateService = TestBed.inject(MailtemplateService);
    klantenService = TestBed.inject(KlantenService);

    jest
      .spyOn(zakenService, "listResultaattypes")
      .mockReturnValue(of(mockResultaattypes));
    jest
      .spyOn(zakenService, "listAfzendersVoorZaak")
      .mockReturnValue(of(mockAfzenders));
    jest
      .spyOn(zakenService, "readDefaultAfzenderVoorZaak")
      .mockReturnValue(of(mockAfzenders[0]));
    jest
      .spyOn(mailtemplateService, "findMailtemplate")
      .mockReturnValue(of(mockMailtemplate));
    jest
      .spyOn(klantenService, "ophalenContactGegevens")
      .mockReturnValue(of({ emailadres: "initiator@example.com" }));
    jest
      .spyOn(planItemsService, "doUserEventListenerPlanItem")
      .mockReturnValue(EMPTY);

    fixture = TestBed.createComponent(ZaakAfhandelenDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
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
    it(`should call ${PlanItemsService.prototype.doUserEventListenerPlanItem.name} on submit`, async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[2]?.click();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );

      await submitButton.click();

      expect(planItemsService.doUserEventListenerPlanItem).toHaveBeenCalled();
    });

    it("should send over a 'brondatumEigenschap' when a brondatumEigenschap is required", async () => {
      const resultaattypeSelect = await loader.getHarness(MatSelectHarness);
      await resultaattypeSelect.open();

      const options = await resultaattypeSelect.getOptions();
      await options[0]?.click(); // Select a type that requires brondatumEigenschap

      const inputs = await loader.getAllHarnesses(MatInputHarness);

      await inputs[1].setValue("2022-01-01");

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie\.zaak\.afhandelen/ }),
      );

      await submitButton.click();

      expect(planItemsService.doUserEventListenerPlanItem).toHaveBeenCalledWith(
        expect.objectContaining({
          brondatumEigenschap: "2021-12-31T23:00:00.000Z", // ISO 8601 format
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
});
