/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ZoekenColumn } from "../model/zoeken-column";
import { ColumnPickerValue } from "./column-picker-value";
import { ColumnPickerComponent } from "./column-picker.component";

const makeColumns = (
  fields: Partial<Record<ZoekenColumn, ColumnPickerValue>> = {},
): Map<ZoekenColumn, ColumnPickerValue> =>
  new Map(Object.entries(fields) as [ZoekenColumn, ColumnPickerValue][]);

describe(ColumnPickerComponent.name, () => {
  let fixture: ComponentFixture<ColumnPickerComponent>;
  let component: ColumnPickerComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        ColumnPickerComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ColumnPickerComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it("excludes STICKY columns from the selectable list", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.SELECT]: ColumnPickerValue.STICKY,
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
      [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.HIDDEN,
    });

    expect([...component["_columns"].keys()]).not.toContain(
      ZoekenColumn.SELECT,
    );
    expect([...component["_columns"].keys()]).toContain(ZoekenColumn.NAAM);
    expect([...component["_columns"].keys()]).toContain(
      ZoekenColumn.CREATIEDATUM,
    );
  });

  it("marks VISIBLE columns as selected when columnSrc is set", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
      [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.HIDDEN,
    });

    expect(component["isSelected"](ZoekenColumn.NAAM)).toBe(true);
    expect(component["isSelected"](ZoekenColumn.CREATIEDATUM)).toBe(false);
  });

  it("resets selection state when columnSrc is reassigned", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
    });
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.HIDDEN,
    });

    expect(component["isSelected"](ZoekenColumn.NAAM)).toBe(false);
  });

  it("resets changed flag when menu opens", () => {
    component["changed"] = true;
    component["menuOpened"]();
    expect(component["changed"]).toBe(false);
  });

  it("toggles column from VISIBLE to HIDDEN on selectionChanged", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
    });

    const mockOption = { value: ZoekenColumn.NAAM };
    const mockEvent = { options: [mockOption] } as never;
    component["selectionChanged"](mockEvent);

    expect(component["_columnSrc"].get(ZoekenColumn.NAAM)).toBe(
      ColumnPickerValue.HIDDEN,
    );
    expect(component["changed"]).toBe(true);
  });

  it("toggles column from HIDDEN to VISIBLE on selectionChanged", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.HIDDEN,
    });

    const mockOption = { value: ZoekenColumn.NAAM };
    const mockEvent = { options: [mockOption] } as never;
    component["selectionChanged"](mockEvent);

    expect(component["_columnSrc"].get(ZoekenColumn.NAAM)).toBe(
      ColumnPickerValue.VISIBLE,
    );
  });

  it("emits columnsChanged when updateColumns is called after a change", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
    });
    component["changed"] = true;

    const emitted: Map<ZoekenColumn, ColumnPickerValue>[] = [];
    component.columnsChanged.subscribe((v) => emitted.push(v));

    component["updateColumns"]();

    expect(emitted).toHaveLength(1);
  });

  it("does not emit columnsChanged when nothing changed", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
    });
    component["changed"] = false;

    const emitted: Map<ZoekenColumn, ColumnPickerValue>[] = [];
    component.columnsChanged.subscribe((v) => emitted.push(v));

    component["updateColumns"]();

    expect(emitted).toHaveLength(0);
  });

  it("renders the column picker trigger button", async () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
    });
    fixture.detectChanges();

    const button = await loader.getHarness(MatButtonHarness);
    expect(button).toBeTruthy();
  });

  it("exposes columns map via getter", () => {
    component.columnSrc = makeColumns({
      [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
    });
    expect(component["columns"].size).toBe(1);
    expect(component["columns"].has(ZoekenColumn.NAAM)).toBe(true);
  });
});
