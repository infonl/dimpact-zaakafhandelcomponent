/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../utils/generated-types";
import { FacetFilterComponent } from "./facet-filter.component";

type FilterParameters = GeneratedType<"FilterParameters">;
type FilterResultaat = GeneratedType<"FilterResultaat">;

type FacetFilterInputs = {
  label: string;
  filter?: FilterParameters;
  opties?: FilterResultaat[];
};

const makeFilterParameters = (values: string[]) =>
  fromPartial<FilterParameters>({ values, inverse: false });

const makeOpties = (...namen: string[]) =>
  namen.map((naam) => fromPartial<FilterResultaat>({ naam, aantal: 1 }));

describe(FacetFilterComponent.name, () => {
  const user = userEvent.setup({ delay: null });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FacetFilterComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();
  });

  const setup = async (inputs: FacetFilterInputs) => {
    const fixture = TestBed.createComponent(FacetFilterComponent);
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    const changed = jest.fn<void, [FilterParameters]>();
    fixture.componentInstance.changed.subscribe(changed);
    fixture.autoDetectChanges();
    await fixture.whenStable();
    return { fixture, changed };
  };

  const select = () => screen.getByRole("combobox");

  const optionNames = async () => {
    await user.click(select());
    return screen
      .getAllByRole("option")
      .map((option) => option.textContent?.trim());
  };

  describe("the select", () => {
    it("gets the id <label>_filter", async () => {
      await setup({ label: "behandelaar" });

      expect(select()).toHaveAttribute("id", "behandelaar_filter");
    });

    it("shows the 'alle' placeholder when no filter is bound", async () => {
      await setup({ label: "status", opties: makeOpties("open") });

      expect(select()).toHaveTextContent("filter.-alle-");
    });
  });

  describe("the options", () => {
    it("lists 'alle' first, followed by the opties sorted by naam", async () => {
      await setup({
        label: "status",
        opties: makeOpties("zebra", "appel", "midden"),
      });

      expect(await optionNames()).toEqual([
        "filter.-alle-",
        "appel",
        "midden",
        "zebra",
      ]);
    });

    it("lists only 'alle' when no opties are bound", async () => {
      await setup({ label: "status" });

      expect(await optionNames()).toEqual(["filter.-alle-"]);
    });

    it("lists only 'alle' when the parent binds undefined opties", async () => {
      await setup({ label: "status", opties: undefined });

      expect(await optionNames()).toEqual(["filter.-alle-"]);
    });

    it("shows the '-geen-' translation of the label for the -NULL- optie", async () => {
      await setup({ label: "status", opties: makeOpties("-NULL-") });

      expect(await optionNames()).toEqual(["filter.-alle-", "status.-geen-"]);
    });

    it.each([
      ["indicaties", "indicatie.VERLENGD"],
      ["vertrouwelijkheidaanduiding", "vertrouwelijkheidaanduiding.VERLENGD"],
      ["archiefNominatie", "archiefNominatie.VERLENGD"],
    ])(
      "translates the optie naam with the facet prefix for label %s",
      async (label, translationKey) => {
        await setup({ label, opties: makeOpties("VERLENGD") });

        expect(await optionNames()).toEqual(["filter.-alle-", translationKey]);
      },
    );

    it("shows the new opties when the parent binds a different list", async () => {
      const { fixture } = await setup({
        label: "status",
        opties: makeOpties("oud"),
      });

      fixture.componentRef.setInput("opties", makeOpties("nieuw", "anders"));
      fixture.detectChanges();

      expect(await optionNames()).toEqual(["filter.-alle-", "anders", "nieuw"]);
    });
  });

  describe("the selected optie", () => {
    it("is the first value of the bound filter", async () => {
      await setup({
        label: "status",
        filter: makeFilterParameters(["open", "gesloten"]),
        opties: makeOpties("gesloten", "open"),
      });

      expect(select()).toHaveTextContent("open");
    });

    it("follows a different filter bound by the parent", async () => {
      const { fixture } = await setup({
        label: "status",
        filter: makeFilterParameters(["oud"]),
        opties: makeOpties("oud", "nieuw"),
      });

      fixture.componentRef.setInput("filter", makeFilterParameters(["nieuw"]));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(select()).toHaveTextContent("nieuw");
    });

    it("is cleared when the parent binds a filter without values", async () => {
      const { fixture } = await setup({
        label: "status",
        filter: makeFilterParameters(["open"]),
        opties: makeOpties("open"),
      });

      fixture.componentRef.setInput("filter", makeFilterParameters([]));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(select()).toHaveTextContent("filter.-alle-");
    });
  });

  describe("choosing an optie", () => {
    it("emits a filter with the chosen naam as its only value", async () => {
      const { changed } = await setup({
        label: "status",
        opties: makeOpties("open", "gesloten"),
      });

      await user.click(select());
      await user.click(screen.getByRole("option", { name: "gesloten" }));

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({
        values: ["gesloten"],
        inverse: false,
      });
    });

    it("emits a filter without values when 'alle' is chosen", async () => {
      const { changed } = await setup({
        label: "status",
        filter: makeFilterParameters(["open"]),
        opties: makeOpties("open"),
      });

      await user.click(select());
      await user.click(screen.getByRole("option", { name: "filter.-alle-" }));

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith({ values: [], inverse: false });
    });

    it("does not emit when the parent binds a different filter", async () => {
      const { fixture, changed } = await setup({
        label: "status",
        filter: makeFilterParameters(["oud"]),
        opties: makeOpties("oud", "nieuw"),
      });

      fixture.componentRef.setInput("filter", makeFilterParameters(["nieuw"]));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(changed).not.toHaveBeenCalled();
    });
  });
});
