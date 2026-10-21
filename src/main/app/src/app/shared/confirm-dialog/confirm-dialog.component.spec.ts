/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
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
    imports: [ConfirmDialogComponent, NoopAnimationsModule],
    providers: [
      provideTranslateService(),
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });

  const fixture = TestBed.createComponent(ConfirmDialogComponent);
  fixture.detectChanges();
  return { fixture, mockDialogRef };
}

describe(ConfirmDialogComponent.name, () => {
  describe("message and uitleg", () => {
    it("renders the message key as HTML in the content area", () => {
      setup(new ConfirmDialogData("my.message.key"));
      expect(screen.getByText("my.message.key")).toBeInTheDocument();
    });

    it("shows uitleg when provided", () => {
      setup(
        new ConfirmDialogData(
          "msg.key",
          undefined,
          "Some <strong>extra</strong> explanation",
        ),
      );
      expect(screen.getByRole("strong")).toHaveTextContent("extra");
      expect(screen.getByText(/explanation/)).toHaveTextContent(
        "Some extra explanation",
      );
    });

    it("hides uitleg when not provided", () => {
      setup(new ConfirmDialogData("msg.key"));
      expect(screen.getByRole("paragraph")).toHaveTextContent("msg.key");
    });
  });

  describe("without observable", () => {
    let mockDialogRef: Pick<
      MatDialogRef<ConfirmDialogComponent>,
      "close" | "disableClose"
    >;

    beforeEach(() => {
      ({ mockDialogRef } = setup(new ConfirmDialogData("msg.key")));
    });

    it("X button closes the dialog with false", async () => {
      const user = userEvent.setup();
      await user.click(within(screen.getByRole("heading")).getByRole("button"));
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it("cancel button closes the dialog with false", async () => {
      const user = userEvent.setup();
      await user.click(screen.getByRole("button", { name: "actie.nee" }));
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it("confirm button closes the dialog with true", async () => {
      const user = userEvent.setup();
      await user.click(screen.getByRole("button", { name: "actie.ja" }));
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

    const clickConfirm = async () => {
      const user = userEvent.setup();
      await user.click(screen.getByRole("button", { name: "actie.ja" }));
    };

    it("disables dialog close and shows spinner while the observable is pending", async () => {
      await clickConfirm();
      fixture.detectChanges();

      expect(mockDialogRef.disableClose).toBe(true);
      expect(
        within(screen.getByRole("button", { name: /actie\.ja/ })).getByRole(
          "progressbar",
          // the spinner sits inside an aria-hidden mat-icon
          { hidden: true },
        ),
      ).toBeInTheDocument();
    });

    it("closes with true when the observable succeeds", async () => {
      await clickConfirm();
      subject.next();
      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });

    it("closes with false when the observable errors", async () => {
      await clickConfirm();
      subject.error(new Error("fail"));
      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it("disables confirm and cancel buttons while loading", async () => {
      await clickConfirm();
      fixture.detectChanges();

      expect(screen.getByRole("button", { name: /actie\.ja/ })).toBeDisabled();
      expect(screen.getByRole("button", { name: "actie.nee" })).toBeDisabled();
    });
  });
});
