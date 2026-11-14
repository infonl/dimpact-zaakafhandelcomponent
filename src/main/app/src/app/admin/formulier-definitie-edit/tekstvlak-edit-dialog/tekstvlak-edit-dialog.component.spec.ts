/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { TekstvlakEditDialogComponent } from "./tekstvlak-edit-dialog.component";

describe(TekstvlakEditDialogComponent.name, () => {
  let component: TekstvlakEditDialogComponent;
  let fixture: ComponentFixture<TekstvlakEditDialogComponent>;
  const dialogData = { value: "Initial value" };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TekstvlakEditDialogComponent],
      imports: [
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatDialogModule,
        BrowserAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TekstvlakEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should instantiate with the value passed", () => {
    expect(component).toBeTruthy();
    expect(component.formControl.value).toBe(dialogData.value);
  });

  it("should save the value from the form control to the data object", () => {
    const newValue = "New value";
    component.formControl.setValue(newValue);
    component.updateData();

    expect(dialogData.value).toBe(newValue);
  });
});
