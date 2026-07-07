/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of, throwError } from "rxjs";
import { ZacInput } from "../../form/input/input";
import { ZacTextarea } from "../../form/textarea/textarea";
import {
  RedenDialogComponent,
  RedenDialogData,
} from "./reden-dialog.component";

const setup = (data: Partial<RedenDialogData> = {}) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };

  const dialogData: RedenDialogData = {
    titleKey: "actie.zaak.heropenen.reden",
    ...data,
  };

  TestBed.configureTestingModule({
    imports: [
      RedenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      { provide: MAT_DIALOG_DATA, useValue: dialogData },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });

  const fixture: ComponentFixture<RedenDialogComponent> =
    TestBed.createComponent(RedenDialogComponent);
  fixture.detectChanges();

  return { fixture, component: fixture.componentInstance, dialogRefMock };
};

const submitButton = (fixture: ComponentFixture<RedenDialogComponent>) =>
  fixture.debugElement.query(By.css('button[type="submit"]'))
    .nativeElement as HTMLButtonElement;

// Enters a valid, dirty reden so the submit guard lets the callback run.
const enterReden = (
  component: RedenDialogComponent,
  reden = "een geldige reden",
) => {
  const control = component["form"].controls.reden;
  control.setValue(reden);
  control.markAsDirty();
};

describe(RedenDialogComponent.name, () => {
  it("renders a single-line input by default", () => {
    const { fixture } = setup({ multiline: false });
    expect(fixture.debugElement.query(By.directive(ZacInput))).toBeTruthy();
    expect(fixture.debugElement.query(By.directive(ZacTextarea))).toBeNull();
  });

  it("renders a textarea when multiline is true", () => {
    const { fixture } = setup({ multiline: true });
    expect(fixture.debugElement.query(By.directive(ZacTextarea))).toBeTruthy();
    expect(fixture.debugElement.query(By.directive(ZacInput))).toBeNull();
  });

  it("disables submit until a valid, dirty reden is entered", () => {
    const { fixture, component } = setup();
    expect(submitButton(fixture).disabled).toBe(true);

    enterReden(component);
    fixture.detectChanges();

    expect(submitButton(fixture).disabled).toBe(false);
  });

  it("does not run the callback when submitted while pristine", () => {
    const callback = jest.fn().mockReturnValue(of(true));
    const { component } = setup({ callback });

    component["form"].controls.reden.setValue("reden"); // valid but not dirty
    component["submit"]();

    expect(callback).not.toHaveBeenCalled();
  });

  it("invokes the callback with the entered reden and closes with its result", () => {
    const callback = jest.fn().mockReturnValue(of("service-result"));
    const { component, dialogRefMock } = setup({ callback });

    enterReden(component, "mijn reden");
    component["submit"]();

    expect(callback).toHaveBeenCalledWith("mijn reden");
    expect(dialogRefMock.close).toHaveBeenCalledWith("service-result");
  });

  it("closes with true when the callback returns a nullish result", () => {
    const callback = jest.fn().mockReturnValue(of(null));
    const { component, dialogRefMock } = setup({ callback });

    enterReden(component);
    component["submit"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it("keeps the dialog open and shows an inline error when the callback errors", () => {
    const callback = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("x")));
    const { fixture, component, dialogRefMock } = setup({ callback });

    enterReden(component);
    component["submit"]();
    fixture.detectChanges();

    expect(dialogRefMock.close).not.toHaveBeenCalled();
    expect(component["loading"]).toBe(false);
    expect(dialogRefMock.disableClose).toBe(false);
    expect(fixture.debugElement.query(By.css(".dialog-error"))).toBeTruthy();
  });

  it("closes with true when confirming without a callback", () => {
    const { component, dialogRefMock } = setup();

    enterReden(component);
    component["submit"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it("close() dismisses the dialog with false", () => {
    const { component, dialogRefMock } = setup();
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it("close() does nothing while loading", () => {
    const { component, dialogRefMock } = setup();
    component["loading"] = true;

    component["close"]();

    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it("applies the provided label, button labels and maxlength", () => {
    const { component } = setup({
      label: "actie.zaak.heropenen.reden",
      confirmButtonActionKey: "actie.zaak.heropenen",
      cancelButtonActionKey: "actie.annuleren.anders",
      maxlength: 100,
    });

    expect(component["label"]).toBe("actie.zaak.heropenen.reden");
    expect(component["submitLabel"]).toBe("actie.zaak.heropenen");
    expect(component["cancelLabel"]).toBe("actie.annuleren.anders");

    component["form"].controls.reden.setValue("x".repeat(101));
    expect(component["form"].controls.reden.hasError("maxlength")).toBe(true);
  });
});
