/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { FoutDialogComponent } from "./fout-dialog.component";

function setup(data: string) {
  const mockDialogRef: Pick<MatDialogRef<FoutDialogComponent>, "close"> = {
    close: jest.fn(),
  };

  TestBed.configureTestingModule({
    imports: [FoutDialogComponent, NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });

  const fixture = TestBed.createComponent(FoutDialogComponent);
  fixture.detectChanges();
  return { fixture, mockDialogRef };
}

describe(FoutDialogComponent.name, () => {
  it("renders the injected data string in the dialog content", () => {
    const { fixture } = setup("some.error.translation.key");
    const content = fixture.nativeElement.querySelector(
      "[mat-dialog-content]",
    ) as HTMLElement;
    expect(content.textContent).toContain("some.error.translation.key");
  });

  it("toolbar close button calls dialogRef.close()", () => {
    const { fixture, mockDialogRef } = setup("error.key");
    fixture.nativeElement
      .querySelector("mat-toolbar button[mat-icon-button]")
      .click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it("actions close button calls dialogRef.close()", () => {
    const { fixture, mockDialogRef } = setup("error.key");
    fixture.nativeElement
      .querySelector("[mat-dialog-actions] button[mat-raised-button]")
      .click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });
});
