/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { Subject } from "rxjs";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "./confirm-dialog.component";

function setup(data: ConfirmDialogData) {
  const mockDialogRef: Pick<
    MatDialogRef<ConfirmDialogComponent>,
    "close" | "disableClose"
  > = { close: jest.fn(), disableClose: false };

  TestBed.configureTestingModule({
    imports: [ConfirmDialogComponent, NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });

  const fixture = TestBed.createComponent(ConfirmDialogComponent);
  fixture.detectChanges();
  return { fixture, mockDialogRef };
}

describe("ConfirmDialogComponent", () => {
  describe("message and uitleg", () => {
    it("renders the message key as HTML in the content area", () => {
      const { fixture } = setup(new ConfirmDialogData("my.message.key"));
      const p = fixture.nativeElement.querySelector(
        "[mat-dialog-content] p",
      ) as HTMLElement;
      expect(p.innerHTML).toContain("my.message.key");
    });

    it("shows uitleg when provided", () => {
      const { fixture } = setup(
        new ConfirmDialogData("msg.key", undefined, "Some extra explanation"),
      );
      const uitleg = fixture.nativeElement.querySelector(
        "p.uitleg",
      ) as HTMLElement;
      expect(uitleg).not.toBeNull();
      expect(uitleg.innerHTML).toContain("Some extra explanation");
    });

    it("hides uitleg when not provided", () => {
      const { fixture } = setup(new ConfirmDialogData("msg.key"));
      expect(fixture.nativeElement.querySelector("p.uitleg")).toBeNull();
    });
  });

  describe("without observable", () => {
    let fixture: ComponentFixture<ConfirmDialogComponent>;
    let mockDialogRef: Pick<
      MatDialogRef<ConfirmDialogComponent>,
      "close" | "disableClose"
    >;

    beforeEach(() => {
      ({ fixture, mockDialogRef } = setup(new ConfirmDialogData("msg.key")));
    });

    it("X button closes the dialog with false", () => {
      fixture.nativeElement
        .querySelector("mat-toolbar button[mat-icon-button]")
        .click();
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it("cancel button closes the dialog with false", () => {
      fixture.nativeElement.querySelector("#cancelButton").click();
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it("confirm button closes the dialog with true", () => {
      fixture.nativeElement.querySelector("#confirmButton").click();
      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });
  });

  describe("with observable", () => {
    let fixture: ComponentFixture<ConfirmDialogComponent>;
    let mockDialogRef: Pick<
      MatDialogRef<ConfirmDialogComponent>,
      "close" | "disableClose"
    >;
    let subject: Subject<void>;

    beforeEach(() => {
      subject = new Subject<void>();
      ({ fixture, mockDialogRef } = setup(
        new ConfirmDialogData("msg.key", subject.asObservable()),
      ));
    });

    it("disables dialog close and shows spinner while the observable is pending", () => {
      fixture.nativeElement.querySelector("#confirmButton").click();
      fixture.detectChanges();

      expect(mockDialogRef.disableClose).toBe(true);
      expect(
        fixture.nativeElement.querySelector("mat-spinner"),
      ).not.toBeNull();
    });

    it("closes with true when the observable succeeds", () => {
      fixture.nativeElement.querySelector("#confirmButton").click();
      subject.next();
      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });

    it("closes with false when the observable errors", () => {
      fixture.nativeElement.querySelector("#confirmButton").click();
      subject.error(new Error("fail"));
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it("disables confirm and cancel buttons while loading", () => {
      fixture.nativeElement.querySelector("#confirmButton").click();
      fixture.detectChanges();

      const confirmBtn = fixture.nativeElement.querySelector(
        "#confirmButton",
      ) as HTMLButtonElement;
      const cancelBtn = fixture.nativeElement.querySelector(
        "#cancelButton",
      ) as HTMLButtonElement;
      expect(confirmBtn.disabled).toBe(true);
      expect(cancelBtn.disabled).toBe(true);
    });
  });
});
