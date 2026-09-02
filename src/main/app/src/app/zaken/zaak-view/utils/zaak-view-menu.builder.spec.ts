/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "../../../../test-helpers";
import { ButtonMenuItem } from "../../../shared/side-nav/menu-item/button-menu-item";
import { MenuItem } from "../../../shared/side-nav/menu-item/menu-item";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  buildZaakMenu,
  userEventListenerIcon,
  ZaakMenuHandlers,
  ZaakMenuPlanItems,
} from "./zaak-view-menu.builder";

type Zaaktype = GeneratedType<"RestZaak">["zaaktype"];

function createHandlers(): jest.Mocked<ZaakMenuHandlers> {
  return {
    openSideAction: jest.fn(),
    startHumanTask: jest.fn(),
    startUserEventListener: jest.fn(),
    heropenen: jest.fn(),
    opschorten: jest.fn(),
    verlengen: jest.fn(),
    hervatten: jest.fn(),
    afbreken: jest.fn(),
    afsluiten: jest.fn(),
    brondatumZetten: jest.fn(),
  };
}

function createZaak(
  zaak: Partial<GeneratedType<"RestZaak">> = {},
  rechten: Partial<GeneratedType<"RestZaakRechten">> = {},
) {
  return fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    zaaktype: fromPartial({}),
    ...zaak,
    rechten: fromPartial(rechten),
  });
}

const noPlanItems: ZaakMenuPlanItems = { userEventListener: [], humanTask: [] };

function titles(menu: MenuItem[]) {
  return menu.map(({ title }) => title);
}

function buttonNamed(menu: MenuItem[], title: string) {
  const item = menu.find((menuItem) => menuItem.title === title);
  return item as ButtonMenuItem | undefined;
}

describe(buildZaakMenu.name, () => {
  let handlers: jest.Mocked<ZaakMenuHandlers>;

  beforeEach(() => {
    handlers = createHandlers();
  });

  describe("while the plan items are still loading", () => {
    it("renders the zaak section without the acties, taken and koppelingen sections", () => {
      const menu = buildZaakMenu(
        createZaak({}, { behandelen: true, wijzigen: true, afbreken: true }),
        null,
        handlers,
        true,
      );

      expect(titles(menu)).toEqual(["zaak"]);
    });
  });

  describe("the zaak section", () => {
    it("offers the ontvangstbevestiging only when it has not been sent yet", () => {
      const rechten = { behandelen: true, versturenOntvangstbevestiging: true };

      expect(
        titles(
          buildZaakMenu(
            createZaak({ heeftOntvangstbevestigingVerstuurd: false }, rechten),
            noPlanItems,
            handlers,
            false,
          ),
        ),
      ).toContain("actie.ontvangstbevestiging.versturen");

      expect(
        titles(
          buildZaakMenu(
            createZaak({ heeftOntvangstbevestigingVerstuurd: true }, rechten),
            noPlanItems,
            handlers,
            false,
          ),
        ),
      ).not.toContain("actie.ontvangstbevestiging.versturen");
    });

    it("hides the behandelaar actions on a procesgestuurde zaak", () => {
      const menu = buildZaakMenu(
        createZaak(
          { isProcesGestuurd: true },
          { behandelen: true, versturenEmail: true },
        ),
        noPlanItems,
        handlers,
        false,
      );

      expect(titles(menu)).not.toContain("actie.mail.versturen");
    });

    it("offers document maken only when SmartDocuments is enabled both globally and for the zaaktype", () => {
      const menuFor = (enabledGlobally: boolean, enabledForZaaktype: boolean) =>
        titles(
          buildZaakMenu(
            createZaak(
              {
                zaaktype: fromPartial({
                  zaakafhandelparameters: fromPartial({
                    smartDocuments: fromPartial({
                      enabledGlobally,
                      enabledForZaaktype,
                    }),
                  }),
                }),
              },
              { creerenDocument: true },
            ),
            noPlanItems,
            handlers,
            false,
          ),
        );

      expect(menuFor(true, true)).toContain("actie.document.maken");
      expect(menuFor(true, false)).not.toContain("actie.document.maken");
      expect(menuFor(false, true)).not.toContain("actie.document.maken");
    });

    it("offers toevoegen and verzenden whenever documents may be created, regardless of SmartDocuments", () => {
      const menu = buildZaakMenu(
        createZaak({}, { creerenDocument: true }),
        noPlanItems,
        handlers,
        false,
      );

      expect(titles(menu)).toEqual(
        expect.arrayContaining([
          "actie.document.toevoegen",
          "actie.document.verzenden",
        ]),
      );
    });

    it("offers zaakdata bekijken only when the zaak actually carries zaakdata", () => {
      const rechten = { bekijkenZaakdata: true };

      expect(
        titles(
          buildZaakMenu(
            createZaak({ zaakdata: { fakeKey: "fakeValue" } }, rechten),
            noPlanItems,
            handlers,
            false,
          ),
        ),
      ).toContain("actie.zaakdata.bekijken");

      expect(
        titles(
          buildZaakMenu(
            createZaak({ zaakdata: {} }, rechten),
            noPlanItems,
            handlers,
            false,
          ),
        ),
      ).not.toContain("actie.zaakdata.bekijken");
    });

    it("routes every zaak section button to the side action panel", () => {
      const menu = buildZaakMenu(
        createZaak({}, { creerenDocument: true }),
        noPlanItems,
        handlers,
        false,
      );

      buttonNamed(menu, "actie.document.toevoegen")?.fn();

      expect(handlers.openSideAction).toHaveBeenCalled();
    });
  });

  describe("the acties section", () => {
    it.each([
      [
        "actie.zaak.heropenen",
        { isOpen: false },
        { heropenen: true },
        "heropenen" as const,
      ],
      [
        "actie.zaak.opschorten",
        {
          isOpen: true,
          zaaktype: fromPartial<Zaaktype>({ opschortingMogelijk: true }),
        },
        { behandelen: true },
        "opschorten" as const,
      ],
      [
        "actie.zaak.verlengen",
        {
          isOpen: true,
          zaaktype: fromPartial<Zaaktype>({ verlengingMogelijk: true }),
        },
        { wijzigenDoorlooptijd: true },
        "verlengen" as const,
      ],
      [
        "actie.zaak.hervatten",
        { isOpgeschort: true },
        { behandelen: true },
        "hervatten" as const,
      ],
      [
        "actie.zaak.afbreken",
        { isOpen: true },
        { afbreken: true },
        "afbreken" as const,
      ],
      [
        "actie.zaak.afsluiten",
        { isHeropend: true },
        { behandelen: true },
        "afsluiten" as const,
      ],
    ])("wires %s to its own handler", (title, zaak, rechten, handler) => {
      const menu = buildZaakMenu(
        createZaak(zaak, { behandelen: true, ...rechten }),
        noPlanItems,
        handlers,
        false,
      );

      buttonNamed(menu, title)?.fn();

      expect(handlers[handler]).toHaveBeenCalled();
    });

    it("offers brondatum zetten only when the resultaattype derives the brondatum from an eigenschap", () => {
      const zaakWith = (afleidingswijze: string) =>
        createZaak(
          {
            resultaat: fromPartial({
              resultaattype: fromPartial({
                bronArchiefprocedure: fromPartial({
                  // ZGW sends lowercase values; the generated union lists the uppercase enum names
                  afleidingswijze:
                    afleidingswijze as GeneratedType<"AfleidingswijzeEnum">,
                }),
              }),
            }),
          },
          { brondatumZetten: true },
        );

      expect(
        titles(
          buildZaakMenu(zaakWith("eigenschap"), noPlanItems, handlers, false),
        ),
      ).toContain("actie.zaak.brondatumZetten");
      expect(
        titles(
          buildZaakMenu(zaakWith("afgehandeld"), noPlanItems, handlers, false),
        ),
      ).not.toContain("actie.zaak.brondatumZetten");
    });

    it("does not offer opschorten when the zaak was already opgeschort before", () => {
      const menu = buildZaakMenu(
        createZaak(
          {
            isOpen: true,
            eerdereOpschorting: true,
            zaaktype: fromPartial({ opschortingMogelijk: true }),
          },
          { behandelen: true },
        ),
        noPlanItems,
        handlers,
        false,
      );

      expect(titles(menu)).not.toContain("actie.zaak.opschorten");
    });

    it("omits the acties header when there is neither an action nor a user event listener", () => {
      const menu = buildZaakMenu(
        createZaak({ isOpen: true }, { behandelen: true }),
        noPlanItems,
        handlers,
        false,
      );

      expect(titles(menu)).not.toContain("actie.zaak.acties");
    });
  });

  describe("the plan item sections", () => {
    const humanTask = (naam: string, id: string) =>
      fromPartial<GeneratedType<"RESTPlanItem">>({ naam, id });

    it("sorts the human tasks by name", () => {
      const menu = buildZaakMenu(
        createZaak({}, { behandelen: true }),
        {
          userEventListener: [],
          humanTask: [
            humanTask("Zienswijze", "3"),
            humanTask("Advies", "1"),
            humanTask("Goedkeuren", "2"),
          ],
        },
        handlers,
        false,
      );

      expect(titles(menu)).toEqual(
        expect.arrayContaining(["Advies", "Goedkeuren", "Zienswijze"]),
      );
      expect(titles(menu).indexOf("Advies")).toBeLessThan(
        titles(menu).indexOf("Goedkeuren"),
      );
      expect(titles(menu).indexOf("Goedkeuren")).toBeLessThan(
        titles(menu).indexOf("Zienswijze"),
      );
    });

    it("hides both plan item sections from a user without the behandelen recht", () => {
      const menu = buildZaakMenu(
        createZaak({}, { behandelen: false }),
        {
          userEventListener: [
            fromPartial<GeneratedType<"RESTPlanItem">>({
              userEventListenerActie: "INTAKE_AFRONDEN",
            }),
          ],
          humanTask: [humanTask("Advies", "1")],
        },
        handlers,
        false,
      );

      expect(titles(menu)).not.toContain("actie.taak.starten");
      expect(titles(menu)).not.toContain("planitem.INTAKE_AFRONDEN");
    });

    it("wires a human task to startHumanTask with the plan item it belongs to", () => {
      const planItem = humanTask("Advies", "1");
      const menu = buildZaakMenu(
        createZaak({}, { behandelen: true }),
        { userEventListener: [], humanTask: [planItem] },
        handlers,
        false,
      );

      buttonNamed(menu, "Advies")?.fn();

      expect(handlers.startHumanTask).toHaveBeenCalledWith(planItem);
    });

    it("wires a user event listener to startUserEventListener", () => {
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        userEventListenerActie: "ZAAK_AFHANDELEN",
      });
      const menu = buildZaakMenu(
        createZaak({}, { behandelen: true }),
        { userEventListener: [planItem], humanTask: [] },
        handlers,
        false,
      );

      buttonNamed(menu, "planitem.ZAAK_AFHANDELEN")?.fn();

      expect(handlers.startUserEventListener).toHaveBeenCalledWith(planItem);
    });
  });

  describe("the koppelingen section", () => {
    it("is left out entirely for a user with neither behandelen nor wijzigen", () => {
      const menu = buildZaakMenu(
        createZaak({}, { toevoegenBagObject: true }),
        noPlanItems,
        handlers,
        true,
      );

      expect(titles(menu)).not.toContain("koppelingen");
      expect(titles(menu)).not.toContain("actie.bagObject.koppelen");
    });

    it("offers locatie koppelen only while the zaak has no geometrie yet", () => {
      const rechten = { wijzigen: true, wijzigenLocatie: true };

      expect(
        titles(
          buildZaakMenu(createZaak({}, rechten), noPlanItems, handlers, false),
        ),
      ).toContain("actie.zaak.locatie.koppelen");

      expect(
        titles(
          buildZaakMenu(
            createZaak({ zaakgeometrie: fromPartial({}) }, rechten),
            noPlanItems,
            handlers,
            false,
          ),
        ),
      ).not.toContain("actie.zaak.locatie.koppelen");
    });

    it("offers betrokkene koppelen only when the BRP search permission backs the brp koppeling", () => {
      const zaak = createZaak(
        {
          zaaktype: fromPartial({
            zaakafhandelparameters: fromPartial({
              betrokkeneKoppelingen: fromPartial({ brpKoppelen: true }),
            }),
          }),
        },
        { wijzigen: true, toevoegenInitiatorPersoon: true },
      );

      expect(
        titles(buildZaakMenu(zaak, noPlanItems, handlers, true)),
      ).toContain("actie.betrokkene.koppelen");
      expect(
        titles(buildZaakMenu(zaak, noPlanItems, handlers, false)),
      ).not.toContain("actie.betrokkene.koppelen");
    });
  });
});

describe(userEventListenerIcon.name, () => {
  it.each([
    ["INTAKE_AFRONDEN", "thumbs_up_down"],
    ["ZAAK_AFHANDELEN", "thumb_up_alt"],
  ] as const)("maps %s to %s", (actie, icon) => {
    expect(userEventListenerIcon(actie)).toBe(icon);
  });

  it("falls back to a generic icon for an unknown actie", () => {
    expect(userEventListenerIcon(undefined)).toBe("fact_check");
  });
});
