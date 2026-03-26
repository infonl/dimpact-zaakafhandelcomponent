/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { FoutDialogComponent } from "./fout-dialog.component";

function setup(data: string) {
  const mockDialogRef: Pick<MatDialogRef<FoutDialogComponent>, "close"> = {
    close: jest.fn(),
  };

  TestBed.configureTestingModule({
    imports: [
      FoutDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });

  const fixture = TestBed.createComponent(FoutDialogComponent);
  fixture.detectChanges();
  const loader: HarnessLoader = TestbedHarnessEnvironment.loader(fixture);
  return { fixture, mockDialogRef, loader };
}

describe(FoutDialogComponent.name, () => {
  it("renders the injected data string in the dialog content", () => {
    const { fixture } = setup("some.error.translation.key");
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      "some.error.translation.key",
    );
  });

  it("toolbar close button calls dialogRef.close()", async () => {
    const { loader, mockDialogRef } = setup("error.key");
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "[mat-icon-button]" }),
    );
    await button.click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it("actions close button calls dialogRef.close()", async () => {
    const { loader, mockDialogRef } = setup("error.key");
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "[mat-raised-button]" }),
    );
    await button.click();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });
});
