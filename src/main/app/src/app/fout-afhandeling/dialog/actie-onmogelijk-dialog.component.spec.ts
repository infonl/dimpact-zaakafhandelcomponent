/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
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
  const loader: HarnessLoader = TestbedHarnessEnvironment.loader(fixture);
  return { fixture, mockDialogRef, loader };
}

describe(ActieOnmogelijkDialogComponent.name, () => {
  it("renders the onmogelijk body text in the dialog content", () => {
    const { fixture } = setup();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      "dialoog.error.body.onmogelijk.opgeschort",
    );
  });

  it("toolbar close button calls dialogRef.close()", async () => {
    const { loader, mockDialogRef } = setup();
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "[mat-icon-button]" }),
    );
    await button.click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it("actions close button calls dialogRef.close()", async () => {
    const { loader, mockDialogRef } = setup();
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "[mat-raised-button]" }),
    );
    await button.click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });
});
