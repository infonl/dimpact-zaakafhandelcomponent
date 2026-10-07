/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BAGService } from "../bag.service";
import { BagZoekComponent } from "./bag-zoek.component";

const makeBagObject = (
  fields: Partial<GeneratedType<"RestBagObject">> = {},
): GeneratedType<"RestBagObject"> =>
  fromPartial<GeneratedType<"RestBagObject">>({
    identificatie: "0363010000012345",
    bagObjectType: "ADRES",
    ...fields,
  });

describe(BagZoekComponent.name, () => {
  const user = userEvent.setup();
  const listAdressen = jest.fn();
  let detectChanges: () => void;
  let sideNav: MatDrawer;

  function returnFromSearch(...bagObjecten: GeneratedType<"RestBagObject">[]) {
    listAdressen.mockReturnValue(
      createQueryOptions({ resultaten: bagObjecten }),
    );
  }

  async function setup({
    gekoppeldeBagObjecten,
    onBagObject,
  }: {
    gekoppeldeBagObjecten?:
      | GeneratedType<"RestBagObject">[]
      | FormControl<GeneratedType<"RestBagObject">[] | null>;
    onBagObject?: (bagObject: GeneratedType<"RestBagObject">) => void;
  } = {}) {
    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    const rendered = await render(BagZoekComponent, {
      inputs: {
        sideNav,
        ...(gekoppeldeBagObjecten ? { gekoppeldeBagObjecten } : {}),
      },
      on: onBagObject ? { bagObject: onBagObject } : {},
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideRouter([]),
        {
          provide: BAGService,
          useValue: fromPartial<BAGService>({ listAdressen }),
        },
      ],
    });

    detectChanges = rendered.detectChanges;
  }

  async function search(trefwoorden: string) {
    if (trefwoorden) {
      await user.type(screen.getByLabelText("bag-objecten"), trefwoorden);
    }
    await user.click(screen.getByRole("button", { name: "actie.zoeken" }));
    await sleep();
  }

  function rowOf(identificatie: string) {
    return screen.findByRole("row", { name: new RegExp(identificatie) });
  }

  it("lists the bag objects found for the entered keywords", async () => {
    returnFromSearch(makeBagObject());
    await setup();

    await search("Teststraat 1");

    expect(listAdressen).toHaveBeenCalledWith({ trefwoorden: "Teststraat 1" });
    expect(await rowOf("0363010000012345")).toBeVisible();
  });

  it("does not search when no keywords are entered", async () => {
    await setup();

    await search("");

    expect(listAdressen).not.toHaveBeenCalled();
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("clears the keywords and the results", async () => {
    returnFromSearch(makeBagObject());
    await setup();
    await search("Teststraat");

    await user.click(screen.getByRole("button", { name: "actie.wissen" }));
    detectChanges();

    expect(screen.getByLabelText("bag-objecten")).toHaveValue("");
    expect(screen.queryByRole("row", { name: /0363010000012345/ })).toBeNull();
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  describe("linking a bag object", () => {
    it("emits the bag object of the row it was clicked on", async () => {
      const bagObject = makeBagObject();
      const onBagObject = jest.fn();
      returnFromSearch(bagObject);
      await setup({ onBagObject });
      await search("Teststraat 1");

      await user.click(
        within(await rowOf("0363010000012345")).getByRole("button", {
          name: "actie.koppelen",
        }),
      );
      detectChanges();

      expect(onBagObject).toHaveBeenCalledWith(bagObject);
      expect(
        within(await rowOf("0363010000012345")).getByRole("button", {
          name: "actie.koppelen",
        }),
      ).toBeDisabled();
    });

    it("adds the bag object to the linked form control", async () => {
      const alreadyLinked = makeBagObject({ identificatie: "existing" });
      const bagObject = makeBagObject({ identificatie: "new" });
      const gekoppeldeBagObjecten = new FormControl<
        GeneratedType<"RestBagObject">[] | null
      >([alreadyLinked]);
      returnFromSearch(bagObject);
      await setup({ gekoppeldeBagObjecten, onBagObject: jest.fn() });
      await search("Teststraat 1");

      await user.click(
        within(await rowOf("new")).getByRole("button", {
          name: "actie.koppelen",
        }),
      );

      expect(gekoppeldeBagObjecten.value).toEqual([alreadyLinked, bagObject]);
    });

    it("cannot link a bag object that is already linked", async () => {
      returnFromSearch(makeBagObject({ identificatie: "123" }));
      await setup({
        gekoppeldeBagObjecten: [
          makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
        ],
        onBagObject: jest.fn(),
      });

      await search("Teststraat 1");

      expect(
        within(await rowOf("123")).getByRole("button", {
          name: "actie.koppelen",
        }),
      ).toBeDisabled();
    });

    it("can link a bag object with another identificatie", async () => {
      returnFromSearch(makeBagObject({ identificatie: "456" }));
      await setup({
        gekoppeldeBagObjecten: [makeBagObject({ identificatie: "123" })],
        onBagObject: jest.fn(),
      });

      await search("Teststraat 1");

      expect(
        within(await rowOf("456")).getByRole("button", {
          name: "actie.koppelen",
        }),
      ).toBeEnabled();
    });

    it("can link a bag object with another bag object type", async () => {
      returnFromSearch(
        makeBagObject({ identificatie: "123", bagObjectType: "PAND" }),
      );
      await setup({
        gekoppeldeBagObjecten: [
          makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
        ],
        onBagObject: jest.fn(),
      });

      await search("Teststraat 1");

      expect(
        within(await rowOf("123")).getByRole("button", {
          name: "actie.koppelen",
        }),
      ).toBeEnabled();
    });
  });

  it("adds the bag object to the linked array it was given", async () => {
    const bagObject = makeBagObject();
    const gekoppeldeBagObjecten: GeneratedType<"RestBagObject">[] = [];
    returnFromSearch(bagObject);
    await setup({ gekoppeldeBagObjecten, onBagObject: jest.fn() });
    await search("Teststraat 1");

    await user.click(
      within(await rowOf("0363010000012345")).getByRole("button", {
        name: "actie.koppelen",
      }),
    );

    expect(gekoppeldeBagObjecten).toEqual([bagObject]);
  });

  it("closes the side nav and opens the page of the bag object", async () => {
    returnFromSearch(makeBagObject());
    await setup();
    const navigate = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);
    await search("Teststraat 1");

    await user.click(
      within(await rowOf("0363010000012345")).getByRole("button", {
        name: "actie.bag-object.bekijken",
      }),
    );

    expect(sideNav.close).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith([
      "/bag-objecten",
      "adres",
      "0363010000012345",
    ]);
  });

  it("closes the side nav from the header", async () => {
    await setup({ onBagObject: jest.fn() });

    const [header] = screen.getAllByRole("heading", {
      name: "actie.bag-object.koppelen",
    });
    await user.click(within(header).getByRole("button"));

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });

  it("closes the side nav when cancelled", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });

  describe("related objects of a row", () => {
    it("cannot be shown for a bag object that is not an adres", async () => {
      returnFromSearch(makeBagObject({ bagObjectType: "PAND" }));
      await setup();

      await search("Teststraat 1");

      expect(
        within(await rowOf("0363010000012345")).queryByRole("button", {
          name: "actie.gerelateerde.gegevens.tonen",
        }),
      ).toBeNull();
    });

    it("cannot be shown for an adres without related objects", async () => {
      returnFromSearch(
        makeBagObject(
          fromPartial<GeneratedType<"RestBagAdres">>({
            bagObjectType: "ADRES",
            openbareRuimte: undefined,
            nummeraanduiding: undefined,
            woonplaats: undefined,
            panden: [],
          }),
        ),
      );
      await setup();

      await search("Teststraat 1");

      expect(
        within(await rowOf("0363010000012345")).queryByRole("button", {
          name: "actie.gerelateerde.gegevens.tonen",
        }),
      ).toBeNull();
    });

    it("shows the nummeraanduiding of an adres as an extra row", async () => {
      returnFromSearch(
        makeBagObject(
          fromPartial<GeneratedType<"RestBagAdres">>({
            bagObjectType: "ADRES",
            nummeraanduiding: {
              identificatie: "0363200000400021",
              bagObjectType: "NUMMERAANDUIDING",
            },
          }),
        ),
      );
      await setup();
      await search("Teststraat 1");

      await user.click(
        within(await rowOf("0363010000012345")).getByRole("button", {
          name: "actie.gerelateerde.gegevens.tonen",
        }),
      );
      detectChanges();

      expect(await rowOf("0363200000400021")).toBeVisible();
    });
  });
});
