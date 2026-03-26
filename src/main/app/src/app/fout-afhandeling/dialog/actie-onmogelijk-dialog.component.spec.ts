/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ActieOnmogelijkDialogComponent } from "./actie-onmogelijk-dialog.component";

function setup() {
  const mockDialogRef: Pick<
    MatDialogRef<ActieOnmogelijkDialogComponent>,
    "close"
  > = { close: jest.fn() };

  TestBed.configureTestingModule({
    imports: [
      ActieOnmogelijkDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [{ provide: MatDialogRef, useValue: mockDialogRef }],
  });

  const fixture = TestBed.createComponent(ActieOnmogelijkDialogComponent);
  fixture.detectChanges();
  return { fixture, mockDialogRef };
}

describe(ActieOnmogelijkDialogComponent.name, () => {
  it("renders the onmogelijk body text in the dialog content", () => {
    const { fixture } = setup();
    const content = fixture.nativeElement.querySelector(
      "[mat-dialog-content]",
    ) as HTMLElement;
    expect(content.textContent).toContain(
      "dialoog.error.body.onmogelijk.opgeschort",
    );
  });

  it("toolbar close button calls dialogRef.close()", () => {
    const { fixture, mockDialogRef } = setup();
    fixture.nativeElement
      .querySelector("mat-toolbar button[mat-icon-button]")
      .click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it("actions close button calls dialogRef.close()", () => {
    const { fixture, mockDialogRef } = setup();
    fixture.nativeElement
      .querySelector("[mat-dialog-actions] button[mat-raised-button]")
      .click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });
});
