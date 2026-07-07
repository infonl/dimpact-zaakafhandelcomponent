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
import { fromPartial } from "src/test-helpers";
import { ZacSelect } from "../../shared/form/select/select";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  ZaakAfbrekenDialogComponent,
  ZaakAfbrekenDialogData,
} from "./zaak-afbreken-dialog.component";

const reden = fromPartial<GeneratedType<"RestZaakbeeindigReden">>({
  id: "42",
  naam: "Ingetrokken",
});

const setup = (data: Partial<ZaakAfbrekenDialogData> = {}) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  const callback = jest.fn().mockReturnValue(of("afgebroken"));

  const dialogData: ZaakAfbrekenDialogData = {
    options: of([reden]),
    callback,
    ...data,
  };

  TestBed.configureTestingModule({
    imports: [
      ZaakAfbrekenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      { provide: MAT_DIALOG_DATA, useValue: dialogData },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });

  const fixture: ComponentFixture<ZaakAfbrekenDialogComponent> =
    TestBed.createComponent(ZaakAfbrekenDialogComponent);
  fixture.detectChanges();

  return {
    fixture,
    component: fixture.componentInstance,
    dialogRefMock,
    callback,
  };
};

const submitButton = (fixture: ComponentFixture<ZaakAfbrekenDialogComponent>) =>
  fixture.debugElement.query(By.css('button[type="submit"]'))
    .nativeElement as HTMLButtonElement;

describe(ZaakAfbrekenDialogComponent.name, () => {
  it("renders a select for the reden", () => {
    const { fixture } = setup();
    expect(fixture.debugElement.query(By.directive(ZacSelect))).toBeTruthy();
  });

  it("disables submit until a reden is selected", () => {
    const { fixture, component } = setup();
    expect(submitButton(fixture).disabled).toBe(true);

    component["form"].controls.reden.setValue(reden);
    component["form"].controls.reden.markAsDirty();
    fixture.detectChanges();

    expect(submitButton(fixture).disabled).toBe(false);
  });

  it("invokes the callback with the selected reden and closes with its result", () => {
    const { component, dialogRefMock, callback } = setup();

    component["form"].controls.reden.setValue(reden);
    component["form"].controls.reden.markAsDirty();
    component["submit"]();

    expect(callback).toHaveBeenCalledWith(reden);
    expect(dialogRefMock.close).toHaveBeenCalledWith("afgebroken");
  });

  it("keeps the dialog open and shows an inline error when the callback errors", () => {
    const callback = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("x")));
    const { fixture, component, dialogRefMock } = setup({ callback });

    component["form"].controls.reden.setValue(reden);
    component["form"].controls.reden.markAsDirty();
    component["submit"]();
    fixture.detectChanges();

    expect(dialogRefMock.close).not.toHaveBeenCalled();
    expect(component["loading"]).toBe(false);
    expect(dialogRefMock.disableClose).toBe(false);
    expect(fixture.debugElement.query(By.css(".dialog-error"))).toBeTruthy();
  });

  it("close() dismisses the dialog with false", () => {
    const { component, dialogRefMock } = setup();
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });
});
