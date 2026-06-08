/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatMenuHarness } from "@angular/material/menu/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { Editor } from "ngx-editor";
import { VariabelenKiesMenuComponent } from "./variabelen-kies-menu.component";

describe(VariabelenKiesMenuComponent.name, () => {
  let componentRef: ComponentRef<VariabelenKiesMenuComponent>;
  let fixture: ComponentFixture<VariabelenKiesMenuComponent>;
  let loader: HarnessLoader;
  let mockEditor: Editor;
  let insertTextSpy: jest.SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        VariabelenKiesMenuComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    insertTextSpy = jest.fn().mockReturnValue({ exec: jest.fn() });
    mockEditor = {
      commands: { insertText: insertTextSpy },
    } as unknown as Editor;

    fixture = TestBed.createComponent(VariabelenKiesMenuComponent);
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);

    componentRef.setInput("editor", mockEditor);
    componentRef.setInput("variabelen", ["naam", "adres"]);
    fixture.detectChanges();
  });

  it("should render the trigger button", async () => {
    const button = await loader.getHarness(MatButtonHarness);
    expect(button).toBeTruthy();
  });

  it("should open the menu when button is clicked", async () => {
    const menu = await loader.getHarness(MatMenuHarness);
    await menu.open();
    expect(await menu.isOpen()).toBe(true);
  });

  it("should show one item per variable", async () => {
    const menu = await loader.getHarness(MatMenuHarness);
    await menu.open();
    const items = await menu.getItems();
    expect(items.length).toBe(2);
  });

  it("should insert variable text into editor when item is clicked", async () => {
    const menu = await loader.getHarness(MatMenuHarness);
    await menu.open();
    const items = await menu.getItems();
    await items[0].click();

    expect(insertTextSpy).toHaveBeenCalledWith("{naam}");
  });
});
