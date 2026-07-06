/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Component, provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { NEVER, Observable, of, throwError } from "rxjs";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { ZacTextarea } from "../../form/textarea/textarea";
import { ZacDialogBody } from "./dialog-body.component";

@Component({
  standalone: true,
  imports: [ZacDialogBody, ZacTextarea, ReactiveFormsModule],
  template: `
    <zac-dialog-body
      [form]="form"
      [callback]="callback"
      [melding]="melding"
      confirmLabel="actie.document.ontkoppelen"
    >
      <zac-textarea [form]="form" key="reden" />
    </zac-dialog-body>
  `,
})
class HostComponent {
  readonly form = new FormGroup({
    reden: new FormControl<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
    ]),
  });
  callback: () => Observable<unknown> = () => of(true);
  melding = "Weet u zeker dat u dit document wilt ontkoppelen?";
}

const setup = (overrides: { callback?: () => Observable<unknown> } = {}) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };

  TestBed.configureTestingModule({
    imports: [HostComponent, NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [
      provideZonelessChangeDetection(),
      provideTanStackQuery(testQueryClient),
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });

  const fixture: ComponentFixture<HostComponent> =
    TestBed.createComponent(HostComponent);
  const host = fixture.componentInstance;
  if (overrides.callback) host.callback = overrides.callback;
  fixture.detectChanges();

  const body = fixture.debugElement.query(By.directive(ZacDialogBody))
    .componentInstance as ZacDialogBody;

  const fillReden = (value: string) => {
    host.form.controls.reden.setValue(value);
    host.form.controls.reden.markAsDirty();
  };

  return { fixture, host, body, dialogRefMock, fillReden };
};

describe(ZacDialogBody.name, () => {
  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  it("renders the confirm label, the melding and the projected field", () => {
    const { fixture } = setup();

    const title = fixture.nativeElement.querySelector("mat-toolbar span");
    expect(title.textContent).toContain("actie.document.ontkoppelen");
    expect(fixture.nativeElement.textContent).toContain(
      "Weet u zeker dat u dit document wilt ontkoppelen?",
    );
    expect(fixture.nativeElement.querySelector("textarea")).not.toBeNull();
  });

  it("runs the callback and closes with the result on success", async () => {
    const { body, dialogRefMock, fillReden } = setup({
      callback: () => of("ontkoppeld"),
    });
    fillReden("Reden");

    body["confirm"]();
    await sleep();

    expect(dialogRefMock.close).toHaveBeenCalledWith("ontkoppeld");
  });

  it("does not run the callback while the form is invalid", async () => {
    const callback = jest.fn(() => of(true));
    const { body, dialogRefMock } = setup({ callback });

    body["confirm"]();
    await sleep();

    expect(callback).not.toHaveBeenCalled();
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it("disables closing (dialog and X button) while the callback is running", async () => {
    const { fixture, body, dialogRefMock, fillReden } = setup({
      callback: () => NEVER,
    });
    fillReden("Reden");

    body["confirm"]();
    await sleep();
    fixture.detectChanges();

    expect(dialogRefMock.disableClose).toBe(true);
    const closeButton = fixture.nativeElement.querySelector(
      "mat-toolbar button[mat-icon-button]",
    );
    expect(closeButton.disabled).toBe(true);
  });

  it("does not start a second action while one is already running", async () => {
    const callback = jest.fn(() => NEVER);
    const { body, fillReden } = setup({ callback });
    fillReden("Reden");

    body["confirm"]();
    await sleep();
    body["confirm"]();
    await sleep();

    expect(callback).toHaveBeenCalledTimes(1);
  });

  it("keeps the dialog open, shows the error inline and re-enables closing on failure", async () => {
    const httpErrorResponse = new HttpErrorResponse({
      status: 500,
      error: { message: "Ontkoppelen is mislukt" },
    });
    const { fixture, body, dialogRefMock, fillReden } = setup({
      callback: () => throwError(() => httpErrorResponse),
    });
    fillReden("Reden");

    body["confirm"]();
    await sleep();
    fixture.detectChanges();

    expect(body["errorMessage"]()).toBe("Ontkoppelen is mislukt");
    expect(fixture.nativeElement.querySelector(".dialog-error")).not.toBeNull();
    expect(dialogRefMock.close).not.toHaveBeenCalled();
    expect(dialogRefMock.disableClose).toBe(false);
  });

  it("closes with false on cancel", () => {
    const { body, dialogRefMock } = setup();

    body["cancel"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });
});
