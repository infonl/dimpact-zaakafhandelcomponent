/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { GenericDialogComponent } from "./generic-dialog.component";

@Component({
  standalone: true,
  imports: [GenericDialogComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <zac-generic-dialog
      [titleKey]="titleKey"
      [icon]="icon"
      [melding]="melding"
      [loading]="loading"
      (cancelled)="cancelled = cancelled + 1"
    >
      <span>projected body</span>
    </zac-generic-dialog>
  `,
})
class HostComponent {
  titleKey = "actie.zaak.opschorten";
  icon = "pause";
  melding?: string;
  loading = false;
  cancelled = 0;
}

const setup = () => {
  TestBed.configureTestingModule({
    imports: [HostComponent, NoopAnimationsModule],
    providers: [provideTranslateService()],
  });
  const fixture: ComponentFixture<HostComponent> =
    TestBed.createComponent(HostComponent);
  fixture.detectChanges();
  return { fixture, host: fixture.componentInstance };
};

describe(GenericDialogComponent.name, () => {
  it("renders the title and the projected content", () => {
    setup();
    expect(screen.getByRole("heading")).toHaveTextContent(
      "actie.zaak.opschorten",
    );
    expect(screen.getByText("projected body")).toBeInTheDocument();
  });

  it("renders the melding when provided", () => {
    const { fixture, host } = setup();
    host.melding = "een melding";
    fixture.detectChanges();
    expect(screen.getByText("een melding")).toBeInTheDocument();
  });

  it("emits cancel when the close (X) button is clicked", async () => {
    const user = userEvent.setup();
    const { host } = setup();
    await user.click(within(screen.getByRole("heading")).getByRole("button"));
    expect(host.cancelled).toBe(1);
  });

  it("disables the close (X) button while loading", () => {
    const { fixture, host } = setup();
    host.loading = true;
    fixture.detectChanges();

    expect(
      within(screen.getByRole("heading")).getByRole("button"),
    ).toBeDisabled();
  });
});
