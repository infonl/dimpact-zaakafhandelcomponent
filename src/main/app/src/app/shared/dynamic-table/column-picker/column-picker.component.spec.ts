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
import { ColumnPickerValue } from "./column-picker-value";
import { ColumnPickerComponent } from "./column-picker.component";

const makeColumns = (
  fields: Partial<Record<string, ColumnPickerValue>> = {},
): Map<string, ColumnPickerValue> =>
  new Map(Object.entries(fields) as [string, ColumnPickerValue][]);

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
      id: ColumnPickerValue.STICKY,
      naam: ColumnPickerValue.VISIBLE,
      datum: ColumnPickerValue.HIDDEN,
    });

    expect([...component["_columns"].keys()]).not.toContain("id");
    expect([...component["_columns"].keys()]).toContain("naam");
    expect([...component["_columns"].keys()]).toContain("datum");
  });

  it("marks VISIBLE columns as selected when columnSrc is set", () => {
    component.columnSrc = makeColumns({
      naam: ColumnPickerValue.VISIBLE,
      datum: ColumnPickerValue.HIDDEN,
    });

    expect(component["isSelected"]("naam")).toBe(true);
    expect(component["isSelected"]("datum")).toBe(false);
  });

  it("resets selection state when columnSrc is reassigned", () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.VISIBLE });
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.HIDDEN });

    expect(component["isSelected"]("naam")).toBe(false);
  });

  it("resets changed flag when menu opens", () => {
    component["changed"] = true;
    component["menuOpened"]();
    expect(component["changed"]).toBe(false);
  });

  it("toggles column from VISIBLE to HIDDEN on selectionChanged", () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.VISIBLE });

    const mockOption = { value: "naam" };
    const mockEvent = { options: [mockOption] } as never;
    component["selectionChanged"](mockEvent);

    expect(component["_columnSrc"].get("naam")).toBe(ColumnPickerValue.HIDDEN);
    expect(component["changed"]).toBe(true);
  });

  it("toggles column from HIDDEN to VISIBLE on selectionChanged", () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.HIDDEN });

    const mockOption = { value: "naam" };
    const mockEvent = { options: [mockOption] } as never;
    component["selectionChanged"](mockEvent);

    expect(component["_columnSrc"].get("naam")).toBe(ColumnPickerValue.VISIBLE);
  });

  it("emits columnsChanged when updateColumns is called after a change", () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.VISIBLE });
    component["changed"] = true;

    const emitted: Map<string, ColumnPickerValue>[] = [];
    component.columnsChanged.subscribe((v) => emitted.push(v));

    component["updateColumns"]();

    expect(emitted).toHaveLength(1);
  });

  it("does not emit columnsChanged when nothing changed", () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.VISIBLE });
    component["changed"] = false;

    const emitted: Map<string, ColumnPickerValue>[] = [];
    component.columnsChanged.subscribe((v) => emitted.push(v));

    component["updateColumns"]();

    expect(emitted).toHaveLength(0);
  });

  it("renders the column picker trigger button", async () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.VISIBLE });
    fixture.detectChanges();

    const button = await loader.getHarness(MatButtonHarness);
    expect(button).toBeTruthy();
  });

  it("exposes columns map via getter", () => {
    component.columnSrc = makeColumns({ naam: ColumnPickerValue.VISIBLE });
    expect(component["columns"].size).toBe(1);
    expect(component["columns"].has("naam")).toBe(true);
  });
});
