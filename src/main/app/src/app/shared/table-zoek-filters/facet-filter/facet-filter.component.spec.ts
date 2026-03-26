/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { SimpleChange } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../utils/generated-types";
import { FacetFilterComponent } from "./facet-filter.component";

const makeFilterParameters = (
  fields: Partial<GeneratedType<"FilterParameters">> = {},
): GeneratedType<"FilterParameters"> =>
  ({ values: [], inverse: false, ...fields }) as unknown as GeneratedType<"FilterParameters">;

const makeFilterResultaat = (
  fields: Partial<GeneratedType<"FilterResultaat">> = {},
): GeneratedType<"FilterResultaat"> =>
  ({ naam: "optie", aantal: 1, ...fields }) as unknown as GeneratedType<"FilterResultaat">;

describe(FacetFilterComponent.name, () => {
  let fixture: ComponentFixture<FacetFilterComponent>;
  let component: FacetFilterComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FacetFilterComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FacetFilterComponent);
    component = fixture.componentInstance;
    component.label = "status";
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("getFilters()", () => {
    it("sorts opties alphabetically by naam", () => {
      component.opties = [
        makeFilterResultaat({ naam: "zebra" }),
        makeFilterResultaat({ naam: "appel" }),
        makeFilterResultaat({ naam: "midden" }),
      ];

      const sorted = component["getFilters"]();

      expect(sorted?.map((o) => o.naam)).toEqual(["appel", "midden", "zebra"]);
    });

    it("returns undefined when opties is undefined", () => {
      component.opties = undefined;

      expect(component["getFilters"]()).toBeUndefined();
    });

    it("returns empty array when opties is empty", () => {
      component.opties = [];

      expect(component["getFilters"]()).toEqual([]);
    });
  });

  describe("isVertaalbaar()", () => {
    it("returns true for keys present in VERTAALBARE_FACETTEN", () => {
      expect(component["isVertaalbaar"]("indicaties")).toBe(true);
      expect(component["isVertaalbaar"]("vertrouwelijkheidaanduiding")).toBe(true);
      expect(component["isVertaalbaar"]("archiefNominatie")).toBe(true);
    });

    it("returns false for unknown keys", () => {
      expect(component["isVertaalbaar"]("status")).toBe(false);
      expect(component["isVertaalbaar"]("")).toBe(false);
    });
  });

  describe("ngOnInit — setSelected()", () => {
    it("initialises the form control from filter.values[0]", () => {
      component.filter = makeFilterParameters({ values: ["open"] });

      component.ngOnInit();

      expect(component["selected"].value).toBe("open");
    });

    it("sets control to null when filter is absent", () => {
      component.filter = undefined;

      component.ngOnInit();

      expect(component["selected"].value).toBeNull();
    });

    it("sets control to null when filter.values is empty", () => {
      component.filter = makeFilterParameters({ values: [] });

      component.ngOnInit();

      expect(component["selected"].value).toBeNull();
    });
  });

  describe("ngOnChanges()", () => {
    it("updates the control when filter changes after the first change", () => {
      component.filter = makeFilterParameters({ values: ["oud"] });
      component.ngOnInit();

      component.filter = makeFilterParameters({ values: ["nieuw"] });
      component.ngOnChanges({
        filter: new SimpleChange(
          makeFilterParameters({ values: ["oud"] }),
          component.filter,
          false,
        ),
      });

      expect(component["selected"].value).toBe("nieuw");
    });

    it("does NOT update the control on the first change", () => {
      component["selected"].setValue("pre-existing");

      component.filter = makeFilterParameters({ values: ["first"] });
      component.ngOnChanges({
        filter: new SimpleChange(undefined, component.filter, true),
      });

      expect(component["selected"].value).toBe("pre-existing");
    });
  });

  describe("change()", () => {
    it("emits FilterParameters with the selected value when a value is selected", () => {
      const emitted: GeneratedType<"FilterParameters">[] = [];
      component.changed.subscribe((v) => emitted.push(v));

      component["selected"].setValue("open");
      component["change"]();

      expect(emitted).toEqual([{ values: ["open"], inverse: false }]);
    });

    it("emits FilterParameters with empty values array when nothing is selected", () => {
      const emitted: GeneratedType<"FilterParameters">[] = [];
      component.changed.subscribe((v) => emitted.push(v));

      component["selected"].setValue(undefined);
      component["change"]();

      expect(emitted).toEqual([{ values: [], inverse: false }]);
    });

    it("always emits inverse: false", () => {
      const emitted: GeneratedType<"FilterParameters">[] = [];
      component.changed.subscribe((v) => emitted.push(v));

      component["selected"].setValue("gesloten");
      component["change"]();

      expect(emitted[0].inverse).toBe(false);
    });
  });

  describe("template rendering", () => {
    it("sets the mat-select id to label + '_filter'", async () => {
      component.label = "behandelaar";
      fixture.detectChanges();

      const select = await loader.getHarness(MatSelectHarness);
      const id = await (await select.host()).getAttribute("id");
      expect(id).toBe("behandelaar_filter");
    });

    it("renders the 'alle' option as first option", async () => {
      component.opties = [makeFilterResultaat({ naam: "optie1" })];
      fixture.detectChanges();

      const select = await loader.getHarness(MatSelectHarness);
      await select.open();
      const options = await select.getOptions();

      expect(options.length).toBeGreaterThanOrEqual(2);
      expect(await options[0].getText()).toBeTruthy();
    });

    it("renders raw naam for non-translatable labels", () => {
      component.label = "status";
      component.opties = [makeFilterResultaat({ naam: "open" })];
      fixture.detectChanges();

      const names = component["getFilters"]()?.map((o) => o.naam);
      expect(names).toContain("open");
    });
  });
});
