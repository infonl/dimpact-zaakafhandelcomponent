/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ReadMoreComponent } from "../read-more/read-more.component";
import { TextIcon } from "../edit/text-icon";
import { StaticTextComponent } from "./static-text.component";

describe(StaticTextComponent.name, () => {
  let fixture: ComponentFixture<
    StaticTextComponent<string | number | null | undefined>
  >;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        StaticTextComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaticTextComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it("should render the label", () => {
    fixture.componentRef.setInput("label", "test.label");
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("test.label");
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

    expect(
      fixture.debugElement.query(By.directive(ReadMoreComponent)),
    ).toBeTruthy();
  });

  it("should show icon and emit iconClicked when clicked", async () => {
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
    const matIcon = await loader.getHarness(MatIconHarness);
    await (await matIcon.host()).click();

    expect(iconClickedSpy).toHaveBeenCalled();
  });

  it("should apply icon styleClass as CSS class on mat-icon", async () => {
    const icon = new TextIcon(
      () => true,
      "edit",
      "edit-icon",
      "edit.title",
      "my-custom-class",
    );
    fixture.componentRef.setInput("label", "test.label");
    fixture.componentRef.setInput("icon", icon);
    fixture.detectChanges();

    const matIcon = await loader.getHarness(MatIconHarness);
    expect(await (await matIcon.host()).hasClass("my-custom-class")).toBe(true);
  });
});
