/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { GenericDialogComponent } from "./generic-dialog.component";

@Component({
  standalone: true,
  imports: [GenericDialogComponent],
  template: `
    <zac-generic-dialog
      [titleKey]="titleKey"
      [icon]="icon"
      [melding]="melding"
      [loading]="loading"
      (cancelled)="cancelled = cancelled + 1"
    >
      <span data-testid="projected">projected body</span>
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
    imports: [HostComponent, NoopAnimationsModule, TranslateModule.forRoot()],
  });
  const fixture: ComponentFixture<HostComponent> =
    TestBed.createComponent(HostComponent);
  fixture.detectChanges();
  return { fixture, host: fixture.componentInstance };
};

describe(GenericDialogComponent.name, () => {
  it("renders the title and the projected content", () => {
    const { fixture } = setup();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain("actie.zaak.opschorten");
    expect(
      element.querySelector("[data-testid='projected']")?.textContent,
    ).toContain("projected body");
  });

  it("renders the melding when provided", () => {
    const { fixture, host } = setup();
    host.melding = "een melding";
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      "een melding",
    );
  });

  it("emits cancel when the close (X) button is clicked", () => {
    const { fixture, host } = setup();
    const closeButton = fixture.debugElement.query(By.css("mat-toolbar button"))
      .nativeElement as HTMLButtonElement;
    closeButton.click();
    expect(host.cancelled).toBe(1);
  });

  it("disables the close (X) button while loading", () => {
    const { fixture, host } = setup();
    host.loading = true;
    fixture.detectChanges();

    const closeButton = fixture.debugElement.query(By.css("mat-toolbar button"))
      .nativeElement as HTMLButtonElement;
    expect(closeButton.disabled).toBe(true);

    closeButton.click();
    expect(host.cancelled).toBe(0);
  });
});
