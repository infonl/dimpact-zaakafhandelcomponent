/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { GeneratedType } from "../../utils/generated-types";
import { DateRangeFilterComponent } from "./date-range-filter.component";

type DatumRange = GeneratedType<"RestDatumRange">;

type DateRangeFilterInputs = {
  range: DatumRange | null | undefined;
  label?: string;
  showLabel?: boolean;
};

describe(DateRangeFilterComponent.name, () => {
  const user = userEvent.setup({ delay: null });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        DateRangeFilterComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideMomentDateAdapter()],
    }).compileComponents();
  });

  const setup = async (inputs: DateRangeFilterInputs) => {
    const fixture = TestBed.createComponent(DateRangeFilterComponent);
    Object.entries({ label: "creatiedatum", ...inputs }).forEach(
      ([name, value]) => fixture.componentRef.setInput(name, value),
    );
    const changed = jest.fn<void, [DatumRange]>();
    fixture.componentInstance.changed.subscribe(changed);
    fixture.autoDetectChanges();
    await fixture.whenStable();
    return { fixture, changed };
  };

  const startInput = () => screen.getByPlaceholderText("zoeken.filter.van");
  const endInput = () =>
    screen.getByPlaceholderText("zoeken.filter.tot_en_met");
  const clearIcon = () => screen.queryByText("clear");
  const calendarToggle = () =>
    screen.queryByRole("button", { name: "Open calendar" });

  const typeDate = async (input: HTMLElement, date: string) => {
    await user.clear(input);
    await user.type(input, date);
    await user.tab();
  };

  describe("the range input", () => {
    it("shows the dates of the bound range in DD-MM-YYYY format", async () => {
      await setup({
        range: {
          van: new Date(2026, 2, 25).toISOString(),
          tot: new Date(2026, 2, 31).toISOString(),
        },
      });

      expect(startInput()).toHaveValue("25-03-2026");
      expect(endInput()).toHaveValue("31-03-2026");
    });

    it.each([null, undefined])(
      "shows empty date inputs when the bound range is %s",
      async (range) => {
        await setup({ range });

        expect(startInput()).toHaveValue("");
        expect(endInput()).toHaveValue("");
      },
    );

    it("shows the dates of a different range bound by the parent", async () => {
      const { fixture } = await setup({
        range: { van: new Date(2024, 0, 1).toISOString(), tot: null },
      });

      fixture.componentRef.setInput("range", {
        van: new Date(2025, 5, 10).toISOString(),
        tot: new Date(2025, 5, 20).toISOString(),
      });
      fixture.detectChanges();
      await fixture.whenStable();

      expect(startInput()).toHaveValue("10-06-2025");
      expect(endInput()).toHaveValue("20-06-2025");
    });

    it("empties the date inputs when the parent binds null after a range", async () => {
      const { fixture } = await setup({
        range: {
          van: new Date(2024, 0, 1).toISOString(),
          tot: new Date(2024, 0, 31).toISOString(),
        },
      });

      fixture.componentRef.setInput("range", null);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(startInput()).toHaveValue("");
      expect(endInput()).toHaveValue("");
      expect(clearIcon()).not.toBeInTheDocument();
    });
  });

  describe("the label", () => {
    it("is not shown when showLabel is not set", async () => {
      await setup({ range: null, label: "creatiedatum" });

      expect(screen.queryByText("Creatiedatum")).not.toBeInTheDocument();
    });

    it("is shown translated and capitalised when showLabel is true", async () => {
      await setup({ range: null, label: "creatiedatum", showLabel: true });

      expect(
        screen.getByRole("group", { name: "Creatiedatum" }),
      ).toBeInTheDocument();
    });
  });

  describe("the suffix", () => {
    it("shows the calendar toggle and no clear icon while no date is set", async () => {
      await setup({ range: { van: null, tot: null } });

      expect(calendarToggle()).toBeInTheDocument();
      expect(clearIcon()).not.toBeInTheDocument();
    });

    it.each([
      {
        dateThatIsSet: "van",
        range: { van: new Date(2024, 0, 1).toISOString(), tot: null },
      },
      {
        dateThatIsSet: "tot",
        range: { van: null, tot: new Date(2024, 0, 31).toISOString() },
      },
    ])(
      "shows the clear icon instead of the calendar toggle when only $dateThatIsSet is set",
      async ({ range }) => {
        await setup({ range });

        expect(clearIcon()).toBeInTheDocument();
        expect(calendarToggle()).not.toBeInTheDocument();
      },
    );
  });

  describe("clicking the clear icon", () => {
    it("empties the date inputs and emits a range without dates", async () => {
      const { changed } = await setup({
        range: {
          van: new Date(2024, 0, 1).toISOString(),
          tot: new Date(2024, 0, 31).toISOString(),
        },
      });

      await user.click(screen.getByText("clear"));

      expect(startInput()).toHaveValue("");
      expect(endInput()).toHaveValue("");
      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({ van: null, tot: null });
      expect(calendarToggle()).toBeInTheDocument();
    });

    it("clears the bound range object itself and emits that same object", async () => {
      const range: DatumRange = {
        van: new Date(2024, 0, 1).toISOString(),
        tot: new Date(2024, 0, 31).toISOString(),
      };
      const { changed } = await setup({ range });

      await user.click(screen.getByText("clear"));

      expect(range).toEqual({ van: null, tot: null });
      expect(changed.mock.calls[0][0]).toBe(range);
    });
  });

  describe("typing a date", () => {
    it("emits the range with the typed dates as ISO strings", async () => {
      const { changed } = await setup({ range: { van: null, tot: null } });

      await typeDate(startInput(), "01-01-2024");

      expect(changed).toHaveBeenLastCalledWith({
        van: new Date(2024, 0, 1).toISOString(),
        tot: null,
      });

      await typeDate(endInput(), "31-01-2024");

      expect(changed).toHaveBeenLastCalledWith({
        van: new Date(2024, 0, 1).toISOString(),
        tot: new Date(2024, 0, 31).toISOString(),
      });
    });

    it("writes the typed dates into the bound range object and emits that same object", async () => {
      const range: DatumRange = { van: null, tot: null };
      const { changed } = await setup({ range });

      await typeDate(startInput(), "01-01-2024");

      expect(range).toEqual({
        van: new Date(2024, 0, 1).toISOString(),
        tot: null,
      });
      expect(changed.mock.calls[0][0]).toBe(range);
    });

    it("shows the clear icon once a date is typed", async () => {
      await setup({ range: { van: null, tot: null } });

      await typeDate(startInput(), "01-01-2024");

      expect(clearIcon()).toBeInTheDocument();
      expect(calendarToggle()).not.toBeInTheDocument();
    });

    it("does not emit when the only date is erased, but does clear it in the bound range", async () => {
      const range: DatumRange = {
        van: new Date(2024, 0, 1).toISOString(),
        tot: null,
      };
      const { changed } = await setup({ range });

      await user.clear(startInput());
      await user.tab();

      expect(changed).not.toHaveBeenCalled();
      expect(range).toEqual({ van: null, tot: null });
    });
  });
});
