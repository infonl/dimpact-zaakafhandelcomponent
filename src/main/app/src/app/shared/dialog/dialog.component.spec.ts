/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { Subject, of, throwError } from "rxjs";
import { DialogData } from "./dialog-data";
import { DialogComponent } from "./dialog.component";

const makeDialogData = (
  fields: Partial<ConstructorParameters<typeof DialogData>[0]> = {},
): DialogData =>
  new DialogData({
    formFields: [],
    icon: "",
    ...fields,
  } as Partial<DialogData["options"]> as unknown as DialogData["options"]);

function setup(data: DialogData = makeDialogData()) {
  const afterOpened$ = new Subject<void>();
  const mockDialogRef: Pick<
    MatDialogRef<DialogComponent>,
    "close" | "disableClose" | "afterOpened"
  > = {
    close: jest.fn(),
    disableClose: false,
    afterOpened: jest.fn().mockReturnValue(afterOpened$),
  };

  TestBed.configureTestingModule({
    imports: [NoopAnimationsModule, TranslateModule.forRoot(), DialogComponent],
    providers: [
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });

  const fixture: ComponentFixture<DialogComponent> =
    TestBed.createComponent(DialogComponent);
  const component = fixture.componentInstance;
  fixture.detectChanges();

  return { fixture, component, mockDialogRef, afterOpened$ };
}

describe(DialogComponent.name, () => {
  it("starts in loading state and becomes ready after dialog opens", () => {
    const { component, afterOpened$ } = setup();
    expect(component["loading"]).toBe(true);
    afterOpened$.next();
    expect(component["loading"]).toBe(false);
  });

  it("cancel() closes the dialog with false", () => {
    const { component, mockDialogRef } = setup();
    component["cancel"]();
    expect(mockDialogRef.close).toHaveBeenCalledWith(false);
  });

  it("confirm() without callback closes with true immediately", () => {
    const { component, mockDialogRef } = setup(
      makeDialogData({ formFields: [] }),
    );
    component["confirm"]();
    expect(mockDialogRef.close).toHaveBeenCalledWith(true);
  });

  it("confirm() with callback calls it and closes with returned value", () => {
    const callbackResult = { id: 42 };
    const callback = jest.fn().mockReturnValue(of(callbackResult));
    const data = makeDialogData({ callback });
    const { component, mockDialogRef } = setup(data);

    component["confirm"]();

    expect(callback).toHaveBeenCalled();
    expect(mockDialogRef.close).toHaveBeenCalledWith(callbackResult);
  });

  it("confirm() with callback closes with true when callback returns nullish", () => {
    const callback = jest.fn().mockReturnValue(of(null));
    const { component, mockDialogRef } = setup(makeDialogData({ callback }));

    component["confirm"]();

    expect(mockDialogRef.close).toHaveBeenCalledWith(true);
  });

  it("confirm() with callback that errors closes with false", () => {
    const callback = jest.fn().mockReturnValue(throwError(() => new Error()));
    const { component, mockDialogRef } = setup(makeDialogData({ callback }));

    component["confirm"]();

    expect(mockDialogRef.close).toHaveBeenCalledWith(false);
  });

  it("confirm() sets disableClose and loading=true before callback resolves", () => {
    const pending$ = new Subject<unknown>();
    const callback = jest.fn().mockReturnValue(pending$);
    const { component, mockDialogRef } = setup(makeDialogData({ callback }));

    component["confirm"]();

    expect(mockDialogRef.disableClose).toBe(true);
    expect(component["loading"]).toBe(true);
  });

  it("disabled() returns true while loading", () => {
    const { component } = setup();
    component["loading"] = true;
    expect(component["disabled"]()).toBe(true);
  });

  it("disabled() returns true when form fields are invalid", () => {
    const { component, afterOpened$ } = setup();
    afterOpened$.next();
    jest.spyOn(component["data"], "formFieldsInvalid").mockReturnValue(true);
    expect(component["disabled"]()).toBe(true);
  });

  it("disabled() returns false when not loading and fields are valid", () => {
    const { component, afterOpened$ } = setup();
    afterOpened$.next();
    jest.spyOn(component["data"], "formFieldsInvalid").mockReturnValue(false);
    expect(component["disabled"]()).toBe(false);
  });

  it("renders confirm button when confirmButtonActionKey is set", () => {
    const { fixture } = setup(
      makeDialogData({ confirmButtonActionKey: "actie.bevestigen" }),
    );
    const btn = fixture.nativeElement.querySelector("#confirmButton");
    expect(btn).toBeTruthy();
  });

  it("does not render confirm button when confirmButtonActionKey is null", () => {
    const { fixture } = setup(makeDialogData({ confirmButtonActionKey: null }));
    const btn = fixture.nativeElement.querySelector("#confirmButton");
    expect(btn).toBeNull();
  });

  it("renders cancel button when cancelButtonActionKey is set", () => {
    const { fixture } = setup(
      makeDialogData({ cancelButtonActionKey: "actie.annuleren" }),
    );
    const btn = fixture.nativeElement.querySelector("#cancelButton");
    expect(btn).toBeTruthy();
  });

  it("renders melding paragraph when data.options.melding is provided", () => {
    const { fixture } = setup(makeDialogData({ melding: "let op!" }));
    const p = fixture.nativeElement.querySelector(
      "[mat-dialog-content] p",
    ) as HTMLElement;
    expect(p).toBeTruthy();
    expect(p.innerHTML).toContain("let op!");
  });
});
