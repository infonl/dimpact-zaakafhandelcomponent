import { DebugElement, EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { FieldType } from "../../model/field-type.enum";
import { AutocompleteFormField } from "./autocomplete-form-field";
import { AutocompleteComponent } from "./autocomplete.component";

describe(AutocompleteComponent.name, () => {
  let component: AutocompleteComponent;
  let fixture: ComponentFixture<AutocompleteComponent>;
  let debugElement: DebugElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AutocompleteComponent],
      imports: [
        ReactiveFormsModule,
        MatAutocompleteModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AutocompleteComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;

    component.data = {
      label: "Test Label",
      formControl: new FormControl(),
      id: "test",
      optionLabel: "name",
      options: of([{ name: "Option 1" }, { name: "Option 2" }]),
      optionsChanged$: new EventEmitter(),
    } as AutocompleteFormField;

    fixture.detectChanges();
  });

  describe("Suffix action", () => {
    it("should show the search action when initiating", () => {
      const searchIcon = debugElement.query(By.css("mat-icon[matSuffix]"));
      expect(searchIcon.nativeElement.textContent).toContain("search");
    });

    it("should show the clear action when a value has been selected", () => {
      component.data.formControl.setValue("Option 1");
      fixture.detectChanges();

      const clearButton = debugElement.query(By.css("button[mat-icon-button]"));
      expect(clearButton).toBeTruthy();
    });

    it("should revert back to the search action when the clear action has been pressed", () => {
      component.data.formControl.setValue("Option 1");
      fixture.detectChanges();

      const clearButton = debugElement.query(By.css("button[mat-icon-button]"));
      clearButton.triggerEventHandler("click", null);
      fixture.detectChanges();

      const searchIcon = debugElement.query(By.css("mat-icon[matSuffix]"));
      expect(searchIcon.nativeElement.textContent).toContain("search");
    });
  });
});
