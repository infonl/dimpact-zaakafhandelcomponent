/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../../shared/utils/generated-types";
import { DateFilterComponent } from "./date-filter.component";

const makeRange = (
  fields: Partial<GeneratedType<"RestDatumRange">>,
): GeneratedType<"RestDatumRange"> =>
  fields as Partial<
    GeneratedType<"RestDatumRange">
  > as unknown as GeneratedType<"RestDatumRange">;

describe(DateFilterComponent.name, () => {
  let component: DateFilterComponent;

  beforeEach(() => {
    component = new DateFilterComponent();
    component.label = "Startdatum";
  });

  describe("ngOnInit", () => {
    it("sets dateVan from range.van", () => {
      component.range = makeRange({ van: "2026-01-15" });
      component.ngOnInit();

      expect(component["dateVan"].value).toEqual(new Date("2026-01-15"));
    });

    it("sets dateTM from range.tot", () => {
      component.range = makeRange({ tot: "2026-03-31" });
      component.ngOnInit();

      expect(component["dateTM"].value).toEqual(new Date("2026-03-31"));
    });

    it("leaves dateVan null when range.van is absent", () => {
      component.range = makeRange({});
      component.ngOnInit();

      expect(component["dateVan"].value).toBeNull();
    });

    it("leaves dateTM null when range.tot is absent", () => {
      component.range = makeRange({});
      component.ngOnInit();

      expect(component["dateTM"].value).toBeNull();
    });

    it("leaves both controls null when range is undefined", () => {
      component.range = undefined;
      component.ngOnInit();

      expect(component["dateVan"].value).toBeNull();
      expect(component["dateTM"].value).toBeNull();
    });
  });

  describe("change", () => {
    it("emits updated range with van as ISO string", () => {
      const emitted: GeneratedType<"RestDatumRange">[] = [];
      component.changed.subscribe((r) => emitted.push(r));
      component.range = makeRange({ van: "2026-01-01" });
      component["dateVan"].setValue(new Date("2026-06-01"));

      component["change"]();

      expect(emitted).toHaveLength(1);
      expect(emitted[0].van).toContain("2026-06-01");
    });

    it("emits updated range with tot as ISO string", () => {
      const emitted: GeneratedType<"RestDatumRange">[] = [];
      component.changed.subscribe((r) => emitted.push(r));
      component.range = makeRange({ tot: "2026-01-01" });
      component["dateTM"].setValue(new Date("2026-12-31"));

      component["change"]();

      expect(emitted).toHaveLength(1);
      expect(emitted[0].tot).toContain("2026-12-31");
    });

    it("creates a new range when van property was absent", () => {
      const emitted: GeneratedType<"RestDatumRange">[] = [];
      component.changed.subscribe((r) => emitted.push(r));
      component.range = makeRange({});
      component["dateVan"].setValue(new Date("2026-06-01"));

      component["change"]();

      expect(emitted[0].van).toContain("2026-06-01");
    });

    it("emits null ISO when control is cleared", () => {
      const emitted: GeneratedType<"RestDatumRange">[] = [];
      component.changed.subscribe((r) => emitted.push(r));
      component.range = makeRange({ van: "2026-01-01" });
      component["dateVan"].setValue(null);

      component["change"]();

      expect(emitted[0].van).toBeUndefined();
    });
  });

  describe("expanded", () => {
    it("returns true when van is set", () => {
      component.range = makeRange({ van: "2026-01-01" });
      expect(component["expanded"]()).toBe(true);
    });

    it("returns true when tot is set", () => {
      component.range = makeRange({ tot: "2026-12-31" });
      expect(component["expanded"]()).toBe(true);
    });

    it("returns false when both are absent", () => {
      component.range = makeRange({});
      expect(component["expanded"]()).toBe(false);
    });

    it("returns false when range is undefined", () => {
      component.range = undefined;
      expect(component["expanded"]()).toBe(false);
    });
  });
});
