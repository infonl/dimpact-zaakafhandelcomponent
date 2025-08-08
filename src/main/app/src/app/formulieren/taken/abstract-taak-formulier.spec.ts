import { TestBed } from "@angular/core/testing";
import { FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

class TestTaakFormulier extends AbstractTaakFormulier {
  taakinformatieMapping = {
    uitkomst: "uitkomst",
    bijlagen: "bijlagen",
    opmerking: "opmerking",
  };

  constructor(
    translate: TranslateService,
    informatieObjectenService: InformatieObjectenService,
  ) {
    super(translate, informatieObjectenService);
  }

  protected _initStartForm(): void {
    // Not needed for this test
  }

  protected _initBehandelForm(): void {
    // Not needed for this test
  }

  // Expose protected field for testing
  get toelichtingFieldName(): string {
    return AbstractTaakFormulier.TOELICHTING_FIELD;
  }
}

describe(AbstractTaakFormulier.name, () => {
  let component: TestTaakFormulier;
  let translateService: TranslateService;
  let informatieObjectenService: InformatieObjectenService;
  let formGroup: FormGroup;
  let toelichtingField: AbstractFormField;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: TranslateService, useValue: { instant: jest.fn() } },
        {
          provide: InformatieObjectenService,
          useValue: { listEnkelvoudigInformatieobjecten: jest.fn() },
        },
      ],
    });

    translateService = TestBed.inject(TranslateService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);

    component = new TestTaakFormulier(
      translateService,
      informatieObjectenService,
    );

    component.taak = {
      id: "test-taak-id",
      zaakUuid: "test-zaak-uuid",
      naam: "Test Taak",
      status: "TOEGEKEND",
      taakdata: {},
      taakinformatie: {},
      toelichting: undefined,
    } as GeneratedType<"RestTask">;

    toelichtingField = new TextareaFormFieldBuilder(null)
      .id(component.toelichtingFieldName)
      .label("Toelichting")
      .build();

    component.form = [[toelichtingField]];

    formGroup = new FormGroup({
      [component.toelichtingFieldName]: toelichtingField.formControl,
    });
  });

  describe(AbstractTaakFormulier.prototype.getTaak.name, () => {
    it.each([
      { value: null, expected: null },
      { value: undefined, expected: null },
      { value: "", expected: null },
      { value: "test string", expected: "test string" },
    ])(
      "should return '$expected' for toelichting when form control value is '$value'",
      ({ value, expected }) => {
        toelichtingField.formControl.setValue(value);

        const result = component.getTaak(formGroup);

        expect(result.toelichting).toBe(expected);
      },
    );
  });
});
