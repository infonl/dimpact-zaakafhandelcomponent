/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DATE_LOCALE } from "@angular/material/core";
import {
  MatEndDateHarness,
  MatStartDateHarness,
} from "@angular/material/datepicker/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { DatumRange } from "../../../zoeken/model/datum-range";
import { DateRangeFilterComponent } from "./date-range-filter.component";

describe(DateRangeFilterComponent.name, () => {
  let fixture: ComponentFixture<DateRangeFilterComponent>;
  let component: DateRangeFilterComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DateRangeFilterComponent, NoopAnimationsModule],
      providers: [{ provide: MAT_DATE_LOCALE, useValue: "nl-NL" }],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DateRangeFilterComponent);
    component = fixture.componentInstance;
    component.range = new DatumRange();
    component.label = "test.label";
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it("should display dates in dd-MM-yyyy format", async () => {
    component.range = new DatumRange(
      new Date(2026, 2, 25),
      new Date(2026, 2, 31),
    );
    component.ngOnChanges();
    fixture.detectChanges();
    await fixture.whenStable();

    const startInput = await loader.getHarness(MatStartDateHarness);
    const endInput = await loader.getHarness(MatEndDateHarness);
    expect(await startInput.getValue()).toBe("25-3-2026");
    expect(await endInput.getValue()).toBe("31-3-2026");
  });

  describe("ngOnChanges", () => {
    it("should sync dateVan and dateTM from the range input", () => {
      const van = new Date(2024, 0, 1);
      const tot = new Date(2024, 0, 31);
      component.range = new DatumRange(van, tot);

      component.ngOnChanges();

      expect(component["dateVan"].value).toEqual(van);
      expect(component["dateTM"].value).toEqual(tot);
    });

    it("should initialise range to an empty DatumRange when range is falsy", () => {
      component.range = null as unknown as DatumRange;

      component.ngOnChanges();

      expect(component.range).toBeInstanceOf(DatumRange);
      expect(component.range.van).toBeNull();
      expect(component.range.tot).toBeNull();
    });
  });

  describe("hasRange", () => {
    it("should return false when both dates are null", () => {
      component.range = new DatumRange();

      expect(component["hasRange"]()).toBe(false);
    });

    it("should return false when only van is set", () => {
      component.range = new DatumRange(new Date(2024, 0, 1), null);

      expect(component["hasRange"]()).toBe(false);
    });

    it("should return true when both van and tot are set", () => {
      component.range = new DatumRange(
        new Date(2024, 0, 1),
        new Date(2024, 0, 31),
      );

      expect(component["hasRange"]()).toBe(true);
    });
  });

  describe("clearDate", () => {
    it("should reset form controls, clear range van/tot, and emit changed", () => {
      component.range = new DatumRange(
        new Date(2024, 0, 1),
        new Date(2024, 0, 31),
      );
      component.ngOnChanges();
      const emitted: DatumRange[] = [];
      component.changed.subscribe((val) => emitted.push(val));

      const event = { stopPropagation: jest.fn() } as unknown as MouseEvent;
      component["clearDate"](event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(component["dateVan"].value).toBeNull();
      expect(component["dateTM"].value).toBeNull();
      expect(component.range.van).toBeNull();
      expect(component.range.tot).toBeNull();
      expect(emitted).toHaveLength(1);
    });
  });

  describe("change", () => {
    it("should update range.van and range.tot from the form controls", () => {
      const van = new Date(2024, 0, 1);
      const tot = new Date(2024, 0, 31);
      component["dateVan"].setValue(van);
      component["dateTM"].setValue(tot);

      component["change"]();

      expect(component.range.van).toEqual(van);
      expect(component.range.tot).toEqual(tot);
    });

    it("should emit changed when both dates are set", () => {
      const van = new Date(2024, 0, 1);
      const tot = new Date(2024, 0, 31);
      component["dateVan"].setValue(van);
      component["dateTM"].setValue(tot);
      const emitted: DatumRange[] = [];
      component.changed.subscribe((val) => emitted.push(val));

      component["change"]();

      expect(emitted).toHaveLength(1);
    });

    it("should not emit changed when only one date is set", () => {
      component["dateVan"].setValue(new Date(2024, 0, 1));
      component["dateTM"].setValue(null);
      const emitted: DatumRange[] = [];
      component.changed.subscribe((val) => emitted.push(val));

      component["change"]();

      expect(emitted).toHaveLength(0);
    });
  });
});
