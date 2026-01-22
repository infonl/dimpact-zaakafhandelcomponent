/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { MatIconModule } from "@angular/material/icon";
import { MatCardModule } from "@angular/material/card";
import { ErrorCardComponent } from "./error-card.component";

describe(ErrorCardComponent.name, () => {
  let component: ErrorCardComponent;
  let fixture: ComponentFixture<ErrorCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ErrorCardComponent],
      imports: [TranslateModule.forRoot(), MatIconModule, MatCardModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorCardComponent);
    component = fixture.componentInstance;
    component.title = "test.title";
    component.text = "test.text";
    fixture.detectChanges();
  });

  it("should have a default icon", () => {
    const icon = fixture.nativeElement.querySelector("mat-icon");
    expect(icon.textContent).toContain("indeterminate_question_box");
  });

  it("should display a custom icon", () => {
    component.iconName = "custom_icon";
    fixture.detectChanges();
    const icon = fixture.nativeElement.querySelector("mat-icon");
    expect(icon.textContent).toContain("custom_icon");
  });
});
