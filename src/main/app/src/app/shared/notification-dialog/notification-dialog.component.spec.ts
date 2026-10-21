/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import {
  NotificationDialogComponent,
  NotificationDialogData,
} from "./notification-dialog.component";

describe("NotificationDialogComponent", () => {
  let fixture: ComponentFixture<NotificationDialogComponent>;
  let mockDialogRef: Pick<MatDialogRef<NotificationDialogComponent>, "close">;
  let dialogData: NotificationDialogData;

  beforeEach(async () => {
    mockDialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [NotificationDialogComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        { provide: MatDialogRef, useValue: mockDialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: new NotificationDialogData("Test <b>melding</b>"),
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationDialogComponent);
    dialogData = TestBed.inject(MAT_DIALOG_DATA);
    fixture.detectChanges();
  });

  it("displays the message as HTML", () => {
    const paragraph = screen.getByRole("paragraph");
    expect(paragraph.innerHTML).toBe(dialogData.melding);
  });

  it("closes the dialog with true when the user confirms", async () => {
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "actie.ok" }));
    expect(mockDialogRef.close).toHaveBeenCalledWith(true);
  });
});
