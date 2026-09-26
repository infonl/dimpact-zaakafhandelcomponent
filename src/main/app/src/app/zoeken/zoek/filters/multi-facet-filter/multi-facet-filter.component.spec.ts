/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { MultiFacetFilterComponent } from "./multi-facet-filter.component";

type FilterParameters = GeneratedType<"FilterParameters">;
type FilterResultaat = GeneratedType<"FilterResultaat">;

const OPTIES: FilterResultaat[] = [
  { naam: "ZAAK", aantal: 10 },
  { naam: "TAAK", aantal: 5 },
];

describe(MultiFacetFilterComponent.name, () => {
  let fixture: ComponentFixture<MultiFacetFilterComponent>;
  let changed: jest.Mock<void, [FilterParameters]>;

  const user = userEvent.setup({ delay: null });

  async function setup({
    label = "TYPE",
    filter = { values: [] },
    opties = OPTIES,
  }: {
    label?: string;
    filter?: FilterParameters;
    opties?: FilterResultaat[];
  } = {}) {
    changed = jest.fn();
    const rendered = await render(MultiFacetFilterComponent, {
      inputs: { label, filter, opties },
      on: { changed },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
    });
    fixture = rendered.fixture;
  }

  function checkbox(name: string) {
    return screen.getByRole("checkbox", { name });
  }

  describe("showing the opties", () => {
    it("shows the label as a translated title", async () => {
      await setup({ label: "ZAAK_ARCHIEF_NOMINATIE" });

      expect(
        screen.getByText("zoeken.filter.zaak_archief_nominatie"),
      ).toBeVisible();
    });

    it("renders a checkbox with the count for each optie", async () => {
      await setup();

      expect(screen.getAllByRole("checkbox")).toHaveLength(2);
      expect(checkbox("type.ZAAK")).toBeInTheDocument();
      expect(screen.getByText("10")).toBeVisible();
      expect(checkbox("type.TAAK")).toBeInTheDocument();
      expect(screen.getByText("5")).toBeVisible();
    });

    it.each([
      ["TAAK_STATUS", "taak.status.ZAAK"],
      ["TOEGEKEND", "zoeken.filter.jaNee.ZAAK"],
      ["ZAAK_INDICATIES", "indicatie.ZAAK"],
      ["DOCUMENT_INDICATIES", "indicatie.ZAAK"],
      ["DOCUMENT_STATUS", "informatieobject.status.ZAAK"],
      ["ZAAK_ARCHIEF_NOMINATIE", "archiefNominatie.ZAAK"],
    ])(
      "translates the optie names of the %s facet",
      async (label, optieName) => {
        await setup({ label });

        expect(checkbox(optieName)).toBeInTheDocument();
      },
    );

    it("shows the optie names as they are for a facet without translations", async () => {
      await setup({ label: "ZAAKTYPE" });

      expect(checkbox("ZAAK")).toBeInTheDocument();
      expect(checkbox("TAAK")).toBeInTheDocument();
    });

    it("checks the opties that are in the values of the filter", async () => {
      await setup({ filter: { values: ["ZAAK"] } });

      expect(checkbox("type.ZAAK")).toBeChecked();
      expect(checkbox("type.TAAK")).not.toBeChecked();
    });

    it.each([
      ["is absent", { values: [] }, "toggle_off"],
      ["is false", { values: [], inverse: false }, "toggle_off"],
      ["is true", { values: [], inverse: true }, "toggle_on"],
      [
        'is the string "true"',
        { values: [], inverse: "true" as unknown as boolean },
        "toggle_on",
      ],
    ])(
      "shows the inverse toggle when the inverse of the filter %s",
      async (_description, filter, toggle) => {
        await setup({ filter });

        expect(screen.getByText(toggle)).toBeVisible();
      },
    );

    it("updates the counts but keeps the checked opties when the opties change afterwards", async () => {
      await setup({ filter: { values: ["TAAK"] } });

      fixture.componentRef.setInput("opties", [
        { naam: "ZAAK", aantal: 3 },
        { naam: "TAAK", aantal: 1 },
      ]);
      fixture.detectChanges();

      expect(screen.getByText("3")).toBeVisible();
      expect(screen.getByText("1")).toBeVisible();
      expect(checkbox("type.TAAK")).toBeChecked();
    });

    it("keeps the checked opties it started with when the filter input changes afterwards", async () => {
      await setup({ filter: { values: ["TAAK"] } });

      fixture.componentRef.setInput("filter", {
        values: ["ZAAK"],
        inverse: true,
      });
      fixture.detectChanges();

      expect(checkbox("type.ZAAK")).not.toBeChecked();
      expect(checkbox("type.TAAK")).toBeChecked();
      expect(screen.getByText("toggle_off")).toBeVisible();
    });
  });

  describe("changing the selection", () => {
    it("emits the checked opties when an optie is checked", async () => {
      await setup();

      await user.click(checkbox("type.ZAAK"));

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({
        values: ["ZAAK"],
        inverse: false,
      });
    });

    it("emits the checked opties in the order of the opties", async () => {
      await setup({ filter: { values: ["TAAK"] } });

      await user.click(checkbox("type.ZAAK"));

      expect(changed).toHaveBeenCalledWith({
        values: ["ZAAK", "TAAK"],
        inverse: false,
      });
    });

    it("emits no values when the last checked optie is unchecked", async () => {
      await setup({ filter: { values: ["ZAAK"] } });

      await user.click(checkbox("type.ZAAK"));

      expect(changed).toHaveBeenCalledWith({ values: [], inverse: false });
    });

    it("emits the inverse of the filter along with the checked opties", async () => {
      await setup({ filter: { values: [], inverse: true } });

      await user.click(checkbox("type.TAAK"));

      expect(changed).toHaveBeenCalledWith({ values: ["TAAK"], inverse: true });
    });

    it("leaves the filter of the parent untouched", async () => {
      const filter: FilterParameters = { values: ["TAAK"] };
      await setup({ filter });

      await user.click(checkbox("type.ZAAK"));
      await user.click(screen.getByText("toggle_off"));

      expect(filter).toEqual({ values: ["TAAK"] });
    });
  });

  describe("inverting the filter", () => {
    it("switches the toggle on and off", async () => {
      await setup();

      await user.click(screen.getByText("toggle_off"));

      expect(screen.getByText("toggle_on")).toBeVisible();

      await user.click(screen.getByText("toggle_on"));

      expect(screen.getByText("toggle_off")).toBeVisible();
    });

    it("emits the inverted filter when an optie is checked", async () => {
      await setup({ filter: { values: ["ZAAK"] } });

      await user.click(screen.getByText("toggle_off"));

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({ values: ["ZAAK"], inverse: true });
    });

    it("emits the filter as not inverted when an inverted filter is toggled", async () => {
      await setup({ filter: { values: ["ZAAK"], inverse: true } });

      await user.click(screen.getByText("toggle_on"));

      expect(changed).toHaveBeenCalledWith({
        values: ["ZAAK"],
        inverse: false,
      });
    });

    it("does not emit when no optie is checked", async () => {
      await setup();

      await user.click(screen.getByText("toggle_off"));

      expect(changed).not.toHaveBeenCalled();
    });
  });
});
