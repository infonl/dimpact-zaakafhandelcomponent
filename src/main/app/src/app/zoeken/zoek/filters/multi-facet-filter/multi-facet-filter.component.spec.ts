/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { MultiFacetFilterComponent } from "./multi-facet-filter.component";

const makeFilter = (
  fields: Partial<GeneratedType<"FilterParameters">> = {},
): GeneratedType<"FilterParameters"> =>
  ({ values: [], ...fields }) as Partial<
    GeneratedType<"FilterParameters">
  > as unknown as GeneratedType<"FilterParameters">;

const makeOptie = (
  fields: Partial<GeneratedType<"FilterResultaat">> = {},
): GeneratedType<"FilterResultaat"> =>
  ({ ...fields }) as Partial<
    GeneratedType<"FilterResultaat">
  > as unknown as GeneratedType<"FilterResultaat">;

const opties = [
  makeOptie({ naam: "ZAAK", aantal: 10 }),
  makeOptie({ naam: "TAAK", aantal: 5 }),
];

async function setup(
  filter: GeneratedType<"FilterParameters"> = makeFilter(),
): Promise<{
  component: MultiFacetFilterComponent;
  fixture: ComponentFixture<MultiFacetFilterComponent>;
  loader: HarnessLoader;
}> {
  const fixture = TestBed.createComponent(MultiFacetFilterComponent);
  const component = fixture.componentInstance;
  component.label = "TYPE";
  component.filter = filter;
  component.opties = opties;
  fixture.detectChanges();
  const loader = TestbedHarnessEnvironment.loader(fixture);
  return { component, fixture, loader };
}

describe(MultiFacetFilterComponent.name, () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MultiFacetFilterComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();
  });

  it("renders a checkbox for each optie", async () => {
    const { loader } = await setup();
    const checkboxes = await loader.getAllHarnesses(MatCheckboxHarness);
    expect(checkboxes.length).toBe(2);
  });

  it("pre-selects checkboxes matching filter.values on init", async () => {
    const { component } = await setup(makeFilter({ values: ["ZAAK"] }));
    expect(component["formGroup"].controls["ZAAK"].value).toBe(true);
    expect(component["formGroup"].controls["TAAK"].value).toBe(false);
  });

  it("emits changed with checked values on checkboxChange", async () => {
    const { component } = await setup();
    const emitted: GeneratedType<"FilterParameters">[] = [];
    component.changed.subscribe((v) => emitted.push(v));
    component["formGroup"].controls["ZAAK"].setValue(true);

    component["checkboxChange"]();

    expect(emitted[0].values).toEqual(["ZAAK"]);
  });

  it("returns true from isVertaalbaar for a known facet field", async () => {
    const { component } = await setup();
    expect(component["isVertaalbaar"]("TAAK_STATUS")).toBe(true);
    expect(component["isVertaalbaar"]("UNKNOWN_FIELD")).toBe(false);
  });

  it("invert toggles the inverse flag and emits when checkboxes are checked", async () => {
    const { component } = await setup();
    const emitted: GeneratedType<"FilterParameters">[] = [];
    component.changed.subscribe((v) => emitted.push(v));
    component["formGroup"].controls["ZAAK"].setValue(true);

    component["invert"]();

    expect(component["inverse"]).toBe(true);
    expect(emitted).toHaveLength(1);
    expect(emitted[0].inverse).toBe(true);
  });
});
