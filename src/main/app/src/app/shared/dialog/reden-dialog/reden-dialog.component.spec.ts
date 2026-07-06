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

  it("disables submit until a valid reden is entered", () => {
    const { fixture, component } = setup();
    expect(submitButton(fixture).disabled).toBe(true);

    component["form"].controls.reden.setValue("een geldige reden");
    component["form"].controls.reden.markAsDirty();
    fixture.detectChanges();

    expect(submitButton(fixture).disabled).toBe(false);
  });

  it("invokes the callback with the entered reden and closes with its result", () => {
    const callback = jest.fn().mockReturnValue(of("service-result"));
    const { component, dialogRefMock } = setup({ callback });

    component["form"].controls.reden.setValue("mijn reden");
    component["submit"]();

    expect(callback).toHaveBeenCalledWith("mijn reden");
    expect(dialogRefMock.close).toHaveBeenCalledWith("service-result");
  });

  it("closes with true when the callback returns a nullish result", () => {
    const callback = jest.fn().mockReturnValue(of(null));
    const { component, dialogRefMock } = setup({ callback });

    component["form"].controls.reden.setValue("reden");
    component["submit"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it("closes with false when the callback errors", () => {
    const callback = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("x")));
    const { component, dialogRefMock } = setup({ callback });

    component["form"].controls.reden.setValue("reden");
    component["submit"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it("closes with true when confirming without a callback", () => {
    const { component, dialogRefMock } = setup();

    component["form"].controls.reden.setValue("reden");
    component["submit"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it("close() dismisses the dialog with false", () => {
    const { component, dialogRefMock } = setup();
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });
});
