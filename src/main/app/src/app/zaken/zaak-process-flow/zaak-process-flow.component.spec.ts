/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ZaakProcessFlowComponent } from "./zaak-process-flow.component";

describe(ZaakProcessFlowComponent.name, () => {
  let fixture: ComponentFixture<ZaakProcessFlowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakProcessFlowComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakProcessFlowComponent);
    fixture.componentRef.setInput("sideNav", {
      close: jest.fn(),
    } as unknown as MatDrawer);
    fixture.componentRef.setInput("zaakUuid", "test-uuid");
    fixture.detectChanges();
  });

  it("should show the process diagram image for the given zaak", () => {
    const img: HTMLImageElement = fixture.nativeElement.querySelector("img");
    expect(img).toBeTruthy();
    expect(img.src).toContain("/rest/zaken/test-uuid/process-diagram");
  });
});
