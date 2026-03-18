/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { TextIcon } from "../edit/text-icon";
import { StaticTextComponent } from "./static-text.component";

describe(StaticTextComponent.name, () => {
  let fixture: ComponentFixture<
    StaticTextComponent<string | number | null | undefined>
  >;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        StaticTextComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaticTextComponent);
  });

  it("should render the label", () => {
    fixture.componentRef.setInput("label", "test.label");
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector("label").textContent).toContain(
      "test.label",
    );
  });

  it("should render value via empty pipe, showing dash when no value", () => {
    fixture.componentRef.setInput("label", "test.label");
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("-");
  });

  it("should render value directly when no maxLength", () => {
    fixture.componentRef.setInput("label", "test.label");
    fixture.componentRef.setInput("value", "some value");
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("some value");
  });

  it("should use read-more when maxLength is set", () => {
    fixture.componentRef.setInput("label", "test.label");
    fixture.componentRef.setInput("value", "some value");
    fixture.componentRef.setInput("maxLength", 5);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector("read-more")).toBeTruthy();
  });

  it("should show icon and emit iconClicked when clicked", () => {
    const icon = new TextIcon(
      () => true,
      "edit",
      "edit-icon",
      "edit.title",
      "edit-class",
    );
    const iconClickedSpy = jest.fn();
    fixture.componentRef.setInput("label", "test.label");
    fixture.componentRef.setInput("icon", icon);
    fixture.detectChanges();

    fixture.componentInstance.iconClicked.subscribe(iconClickedSpy);
    fixture.nativeElement.querySelector("mat-icon").click();

    expect(iconClickedSpy).toHaveBeenCalled();
  });
});
