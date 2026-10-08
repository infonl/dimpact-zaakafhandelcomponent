/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
} from "@angular/forms";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { ZacSelect } from "./select";

interface TestOption {
  id: number;
  name: string;
  unit?: string;
  suffix?: string;
}

interface TestForm extends Record<string, AbstractControl> {
  option: FormControl<TestOption | null>;
}

describe(ZacSelect.name, () => {
  let component: ZacSelect<
    TestForm,
    keyof TestForm,
    TestOption,
    keyof TestOption | ((option: TestOption) => string)
  >;
  let componentRef: ComponentRef<typeof component>;
  let fixture: ComponentFixture<typeof component>;

  const createTestForm = () => {
    return new FormGroup<TestForm>({
      option: new FormControl<TestOption | null>(null, { nonNullable: true }),
    });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZacSelect,
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [TranslateService],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ZacSelect<
        TestForm,
        keyof TestForm,
        TestOption,
        keyof TestOption | ((option: TestOption) => string)
      >,
    );
    componentRef = fixture.componentRef;
    component = fixture.componentInstance;
  });

  describe("displaySuffix", () => {
    const testOption: TestOption = {
      id: 1,
      name: "Test Option",
      unit: "kg",
    };

    beforeEach(() => {
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "option");
      componentRef.setInput("options", [testOption]);
      fixture.detectChanges();
    });

    it("should return null when no suffix is provided", () => {
      const result = component["displaySuffix"](testOption);
      expect(result).toBeNull();
    });

    it("should return the value of the key when suffix is a key of the option", () => {
      componentRef.setInput("suffix", "unit");
      fixture.detectChanges();

      const result = component["displaySuffix"](testOption);
      expect(result).toBe("kg");
    });

    it("should return the value of the 'id' key when suffix is 'id'", () => {
      componentRef.setInput("suffix", "id");
      fixture.detectChanges();

      const result = component["displaySuffix"](testOption);
      expect(result).toBe(1);
    });

    it("should return the value of the 'name' key when suffix is 'name'", () => {
      componentRef.setInput("suffix", "name");
      fixture.detectChanges();

      const result = component["displaySuffix"](testOption);
      expect(result).toBe("Test Option");
    });

    it("should return undefined when suffix key exists but value is explicitly undefined", () => {
      const optionWithUndefinedUnit: TestOption = {
        id: 3,
        name: "Option with undefined unit",
        unit: undefined,
      };
      componentRef.setInput("suffix", "unit");
      fixture.detectChanges();

      const result = component["displaySuffix"](optionWithUndefinedUnit);
      expect(result).toBeUndefined();
    });

    it("should return empty string when suffix key exists but value is empty string", () => {
      const optionWithEmptyUnit: TestOption = {
        id: 4,
        name: "Option with empty unit",
        unit: "",
      };
      componentRef.setInput("suffix", "unit");
      fixture.detectChanges();

      const result = component["displaySuffix"](optionWithEmptyUnit);
      expect(result).toBe("");
    });

    it("should handle numeric values correctly", () => {
      componentRef.setInput("suffix", "id");
      fixture.detectChanges();

      const result = component["displaySuffix"](testOption);
      expect(result).toBe(1);
      expect(typeof result).toBe("number");
    });
  });
  describe("the suffix of an option", () => {
    it("should translate a suffix that the backend sends as a key with a SNAKE_CASE constant", async () => {
      const user = userEvent.setup();
      componentRef.setInput("form", createTestForm());
      componentRef.setInput("key", "option");
      componentRef.setInput("optionDisplayValue", "name");
      componentRef.setInput("suffix", "suffix");
      componentRef.setInput("options", [
        {
          id: 1,
          name: "gemeente@example.com",
          suffix: "gegevens.mail.afzender.GEMEENTE",
        },
      ]);
      const translateService = TestBed.inject(TranslateService);
      translateService.setTranslation("nl", {
        "gegevens.mail.afzender.gemeente": "E-mailadres van de gemeente",
      });
      translateService.use("nl");
      fixture.detectChanges();
      await fixture.whenStable();

      await user.click(screen.getByRole("combobox"));

      expect(
        screen.getByRole("option", { name: /gemeente@example.com/ }),
      ).toHaveTextContent("E-mailadres van de gemeente");
    });
  });
});
