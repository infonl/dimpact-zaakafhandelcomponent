/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import {
  Component,
  provideZonelessChangeDetection,
  TemplateRef,
  ViewChild,
} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { NEVER, Observable, of, throwError } from "rxjs";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { ZacTextarea } from "../../form/textarea/textarea";
import { GenericDialogData } from "./generic-dialog-data";
import { GenericDialogComponent } from "./generic-dialog.component";

type OntkoppelForm = FormGroup<{ reden: FormControl<string | null> }>;

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, ZacTextarea],
  template: `<ng-template #template let-form>
    <zac-textarea [form]="form" key="reden" label="reden" />
  </ng-template>`,
})
class TemplateHostComponent {
  @ViewChild("template", { static: true })
  readonly template!: TemplateRef<{ $implicit: OntkoppelForm }>;
}

const setup = (
  overrides: {
    callback?: (form: OntkoppelForm) => Observable<unknown>;
    data?: Partial<GenericDialogData<OntkoppelForm>>;
  } = {},
) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  const form: OntkoppelForm = new FormGroup({
    reden: new FormControl<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
    ]),
  });
  const callback = jest.fn(overrides.callback ?? (() => of(true)));

  const templateHolder: {
    current?: TemplateRef<{ $implicit: OntkoppelForm }>;
  } = {};

  TestBed.configureTestingModule({
    imports: [
      GenericDialogComponent,
      TemplateHostComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZonelessChangeDetection(),
      provideTanStackQuery(testQueryClient),
      { provide: MatDialogRef, useValue: dialogRefMock },
      {
        provide: MAT_DIALOG_DATA,
        useFactory: (): GenericDialogData<OntkoppelForm> => ({
          form,
          contentTemplate: templateHolder.current!,
          callback,
          melding: "Weet u zeker dat u dit document wilt ontkoppelen?",
          icon: "link_off",
          confirmButtonActionKey: "actie.document.ontkoppelen",
          ...overrides.data,
        }),
      },
    ],
  });

  const hostFixture = TestBed.createComponent(TemplateHostComponent);
  hostFixture.detectChanges();
  templateHolder.current = hostFixture.componentInstance.template;

  const fixture: ComponentFixture<GenericDialogComponent> =
    TestBed.createComponent(GenericDialogComponent);
  fixture.detectChanges();

  const fillReden = (value: string) => {
    form.controls.reden.setValue(value);
    form.controls.reden.markAsDirty();
  };

  return {
    fixture,
    component: fixture.componentInstance,
    dialogRefMock,
    form,
    callback,
    fillReden,
  };
};

describe(GenericDialogComponent.name, () => {
  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("rendering", () => {
    it("shows the title, the message and the projected field", () => {
      const { fixture } = setup();

      const title = fixture.nativeElement.querySelector("mat-toolbar span");
      expect(title.textContent).toContain("actie.document.ontkoppelen");
      expect(fixture.nativeElement.textContent).toContain(
        "Weet u zeker dat u dit document wilt ontkoppelen?",
      );
      expect(fixture.nativeElement.querySelector("textarea")).not.toBeNull();
    });

    it("does not show an error message initially", () => {
      const { fixture, component } = setup();

      expect(component["errorMessage"]()).toBeNull();
      expect(fixture.nativeElement.querySelector(".dialog-error")).toBeNull();
    });
  });

  describe("confirm()", () => {
    it("calls the callback with the form and closes with the result on success", async () => {
      const { component, form, callback, dialogRefMock, fillReden } = setup({
        callback: () => of("ontkoppeld"),
      });
      fillReden("Document is niet meer relevant");

      component["confirm"]();
      await sleep();

      expect(callback).toHaveBeenCalledWith(form);
      expect(dialogRefMock.close).toHaveBeenCalledWith("ontkoppeld");
    });

    it("does not call the callback while the form is invalid", async () => {
      const { component, callback, dialogRefMock } = setup();

      component["confirm"]();
      await sleep();

      expect(callback).not.toHaveBeenCalled();
      expect(dialogRefMock.close).not.toHaveBeenCalled();
    });

    it("disables closing the dialog while the callback is running", async () => {
      const { component, fixture, dialogRefMock, fillReden } = setup({
        callback: () => NEVER,
      });
      fillReden("Reden");

      component["confirm"]();
      await sleep();
      fixture.detectChanges();

      expect(dialogRefMock.disableClose).toBe(true);
      const closeButton = fixture.nativeElement.querySelector(
        "mat-toolbar button[mat-icon-button]",
      );
      expect(closeButton.disabled).toBe(true);
    });

    it("does not start a second action while one is already running", async () => {
      const { component, callback, fillReden } = setup({
        callback: () => NEVER,
      });
      fillReden("Reden");

      component["confirm"]();
      await sleep();
      component["confirm"]();
      await sleep();

      expect(callback).toHaveBeenCalledTimes(1);
    });

    it("does not call the callback while the form is pristine", async () => {
      const { component, form, callback } = setup();
      form.controls.reden.setValue("Reden");
      form.markAsPristine();

      component["confirm"]();
      await sleep();

      expect(callback).not.toHaveBeenCalled();
    });

    it("keeps the dialog open, shows the error inline and re-enables closing on failure", async () => {
      const httpErrorResponse = new HttpErrorResponse({
        status: 500,
        statusText: "Internal Server Error",
        error: { message: "Ontkoppelen is mislukt" },
      });
      const { component, fixture, dialogRefMock, fillReden } = setup({
        callback: () => throwError(() => httpErrorResponse),
      });
      fillReden("Reden");

      component["confirm"]();
      await sleep();
      fixture.detectChanges();

      expect(component["errorMessage"]()).toBe("Ontkoppelen is mislukt");
      const error = fixture.nativeElement.querySelector(".dialog-error");
      expect(error).not.toBeNull();
      expect(error.textContent).toContain("Ontkoppelen is mislukt");
      expect(dialogRefMock.close).not.toHaveBeenCalled();
      expect(dialogRefMock.disableClose).toBe(false);
    });

    it("clears a previous error when the action is retried", async () => {
      const httpErrorResponse = new HttpErrorResponse({ status: 500 });
      let attempt = 0;
      const { component, dialogRefMock, fillReden } = setup({
        callback: () =>
          ++attempt === 1 ? throwError(() => httpErrorResponse) : of(true),
      });
      fillReden("Reden");

      component["confirm"]();
      await sleep(100);
      expect(component["errorMessage"]()).not.toBeNull();

      component["confirm"]();
      await sleep();

      expect(component["errorMessage"]()).toBeNull();
      expect(dialogRefMock.close).toHaveBeenCalledWith(true);
    });
  });

  describe("cancel()", () => {
    it("closes the dialog with false without calling the callback", () => {
      const { component, callback, dialogRefMock } = setup();

      component["cancel"]();

      expect(dialogRefMock.close).toHaveBeenCalledWith(false);
      expect(callback).not.toHaveBeenCalled();
    });
  });
});
