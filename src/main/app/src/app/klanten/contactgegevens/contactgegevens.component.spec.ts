/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ContactgegevensComponent } from "./contactgegevens.component";

describe("ContactgegevensComponent", () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ContactgegevensComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();
  });

  it("should render phone number and email address", () => {
    const fixture = TestBed.createComponent(ContactgegevensComponent);
    fixture.componentRef.setInput("contactDetails", {
      telephoneNumber: "0612345678",
      emailAddress: "test@example.com",
    });
    fixture.componentRef.setInput("toevoegenToegestaan", false);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain("0612345678");
    expect(el.textContent).toContain("test@example.com");
  });
});
