/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture } from "@angular/core/testing";
import {
  MAT_DATE_LOCALE,
  provideNativeDateAdapter,
} from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { DateFilterComponent } from "./date-filter.component";

type DatumRange = GeneratedType<"RestDatumRange">;

const LABEL = "fakeDatumLabel";
const JANUARY_15 = new Date(2026, 0, 15).toISOString();
const MARCH_31 = new Date(2026, 2, 31).toISOString();
const TYPED_JUNE_1 = "2026-06-01";
const JUNE_1 = new Date(TYPED_JUNE_1).toISOString();
const TYPED_DECEMBER_31 = "2026-12-31";
const DECEMBER_31 = new Date(TYPED_DECEMBER_31).toISOString();

describe(DateFilterComponent.name, () => {
  let fixture: ComponentFixture<DateFilterComponent>;
  let changed: jest.Mock<void, [DatumRange]>;

  const user = userEvent.setup({ delay: null });

  async function setup(inputs: { range?: DatumRange | undefined } = {}) {
    changed = jest.fn();
    const rendered = await render(DateFilterComponent, {
      inputs: { label: LABEL, ...inputs },
      on: { changed },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideNativeDateAdapter(),
        { provide: MAT_DATE_LOCALE, useValue: "nl-NL" },
      ],
    });
    fixture = rendered.fixture;
  }

  function panelHeader() {
    return screen.getByRole("button", { name: LABEL });
  }

  function vanField() {
    return screen.getByRole("textbox", { name: "zoeken.filter.van" });
  }

  function totField() {
    return screen.getByRole("textbox", { name: "zoeken.filter.tot_en_met" });
  }

  async function typeDate(field: HTMLElement, typed: string) {
    await user.clear(field);
    await user.type(field, typed);
    await user.tab();
  }

  describe("showing the range", () => {
    it("shows the label as the panel title", async () => {
      await setup();

      expect(panelHeader()).toBeVisible();
    });

    it.each([
      ["no range is given", {}],
      ["the range is undefined", { range: undefined }],
      ["the range has no dates", { range: {} }],
    ])("is collapsed when %s", async (_description, inputs) => {
      await setup(inputs);

      expect(panelHeader()).toHaveAttribute("aria-expanded", "false");
    });

    it.each([
      ["a van date", { van: JANUARY_15 }],
      ["a tot date", { tot: MARCH_31 }],
    ])("is expanded when the range has %s", async (_description, range) => {
      await setup({ range });

      expect(panelHeader()).toHaveAttribute("aria-expanded", "true");
    });

    it("fills the van and tot fields from the range", async () => {
      await setup({ range: { van: JANUARY_15, tot: MARCH_31 } });

      expect(vanField()).toHaveValue("15-1-2026");
      expect(totField()).toHaveValue("31-3-2026");
    });

    it("offers to clear a filled field and to pick a date for an empty field", async () => {
      await setup({ range: { van: JANUARY_15 } });

      expect(screen.getAllByText("clear")).toHaveLength(1);
      expect(
        screen.getAllByRole("button", { name: "Open calendar" }),
      ).toHaveLength(1);
    });

    it("keeps showing the dates it started with when the range input changes afterwards", async () => {
      await setup({ range: { van: JANUARY_15 } });

      fixture.componentRef.setInput("range", { van: MARCH_31 });
      fixture.detectChanges();

      expect(vanField()).toHaveValue("15-1-2026");
    });

    it("expands when the range input gets a date afterwards", async () => {
      await setup();

      fixture.componentRef.setInput("range", { tot: MARCH_31 });
      fixture.detectChanges();

      expect(panelHeader()).toHaveAttribute("aria-expanded", "true");
    });
  });

  describe("changing a date", () => {
    it("emits a range with the picked van date when there was no range", async () => {
      await setup({ range: undefined });
      await user.click(panelHeader());

      await typeDate(vanField(), TYPED_JUNE_1);

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({ van: JUNE_1, tot: undefined });
    });

    it("keeps the picked van date when a tot date is picked afterwards", async () => {
      await setup();
      await user.click(panelHeader());

      await typeDate(vanField(), TYPED_JUNE_1);
      await typeDate(totField(), TYPED_DECEMBER_31);

      expect(changed).toHaveBeenCalledTimes(2);
      expect(changed).toHaveBeenLastCalledWith({
        van: JUNE_1,
        tot: DECEMBER_31,
      });
    });

    it("emits the range with the new date and the unchanged other date", async () => {
      await setup({ range: { van: JANUARY_15, tot: MARCH_31 } });

      await typeDate(vanField(), TYPED_JUNE_1);

      expect(changed).toHaveBeenCalledWith({ van: JUNE_1, tot: MARCH_31 });
    });

    it("writes a changed date into the range object of the parent when that date was already set", async () => {
      const range: DatumRange = { van: JANUARY_15, tot: MARCH_31 };
      await setup({ range });

      await typeDate(vanField(), TYPED_JUNE_1);

      expect(range).toEqual({ van: JUNE_1, tot: MARCH_31 });
      expect(changed).toHaveBeenCalledWith(range);
    });

    it("leaves the range object of the parent untouched when the changed date was not set yet", async () => {
      const range: DatumRange = {};
      await setup({ range });
      await user.click(panelHeader());

      await typeDate(vanField(), TYPED_JUNE_1);

      expect(range).toEqual({});
      expect(changed).toHaveBeenCalledWith({ van: JUNE_1, tot: undefined });
    });

    it("emits the range without the van date when that date is cleared", async () => {
      await setup({ range: { van: JANUARY_15, tot: MARCH_31 } });

      await user.click(screen.getAllByText("clear")[0]);

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({ van: undefined, tot: MARCH_31 });
      expect(vanField()).toHaveValue("");
    });

    it("collapses when its only date is cleared", async () => {
      await setup({ range: { van: JANUARY_15 } });

      await user.click(screen.getByText("clear"));

      expect(panelHeader()).toHaveAttribute("aria-expanded", "false");
    });

    it("collapses when a date that was picked without a range is cleared again", async () => {
      await setup();
      await user.click(panelHeader());
      await typeDate(vanField(), TYPED_JUNE_1);

      await user.click(screen.getByText("clear"));

      expect(changed).toHaveBeenLastCalledWith({
        van: undefined,
        tot: undefined,
      });
      expect(panelHeader()).toHaveAttribute("aria-expanded", "false");
    });
  });
});
