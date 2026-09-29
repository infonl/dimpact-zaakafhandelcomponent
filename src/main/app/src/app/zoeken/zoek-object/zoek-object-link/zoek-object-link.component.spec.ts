/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";
import { ZoekObjectLinkComponent } from "./zoek-object-link.component";

type ZoekObject =
  GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">;

const makeZaakZoekObject = (fields: Partial<ZaakZoekObject> = {}) =>
  fromPartial<ZaakZoekObject>({
    type: "ZAAK",
    id: "fakeZaakUuid",
    identificatie: "fakeZaakIdentificatie",
    indicaties: [],
    ...fields,
  });

const makeTaakZoekObject = (fields: Partial<TaakZoekObject> = {}) =>
  fromPartial<TaakZoekObject>({
    type: "TAAK",
    id: "fakeTaakId",
    naam: "fakeTaakNaam",
    ...fields,
  });

const makeDocumentZoekObject = (fields: Partial<DocumentZoekObject> = {}) =>
  fromPartial<DocumentZoekObject>({
    type: "DOCUMENT",
    id: "fakeDocumentUuid",
    titel: "fakeDocumentTitel",
    indicaties: [],
    ...fields,
  });

describe(ZoekObjectLinkComponent.name, () => {
  const setup = async (zoekObject: ZoekObject) => {
    const user = userEvent.setup({ delay: null });
    const sideNav = fromPartial<MatSidenav>({ close: jest.fn() });
    const { fixture } = await render(ZoekObjectLinkComponent, {
      inputs: { zoekObject, sideNav },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [provideRouter([{ path: "**", children: [] }])],
    });
    return { fixture, sideNav, user };
  };

  describe.each([
    {
      type: "ZAAK",
      zoekObject: makeZaakZoekObject(),
      name: "fakeZaakIdentificatie",
      href: "/zaken/fakeZaakIdentificatie",
    },
    {
      type: "TAAK",
      zoekObject: makeTaakZoekObject(),
      name: "fakeTaakNaam",
      href: "/taken/fakeTaakId",
    },
    {
      type: "DOCUMENT",
      zoekObject: makeDocumentZoekObject(),
      name: "fakeDocumentTitel",
      href: "/informatie-objecten/fakeDocumentUuid",
    },
  ])("for a $type", ({ type, zoekObject, name, href }) => {
    it("links to its own page, named after it", async () => {
      await setup(zoekObject);

      expect(screen.getByRole("link", { name })).toHaveAttribute("href", href);
    });

    it("labels it with its translated type", async () => {
      await setup(zoekObject);

      expect(screen.getByText(type.toLowerCase())).toBeVisible();
      expect(screen.getByRole("link", { name })).toHaveAccessibleDescription(
        `actie.${type.toLowerCase()}.bekijken`,
      );
    });
  });

  it("follows the zoekObject when it is replaced", async () => {
    const { fixture } = await setup(makeZaakZoekObject());

    fixture.componentRef.setInput("zoekObject", makeTaakZoekObject());
    fixture.detectChanges();

    expect(screen.getByRole("link", { name: "fakeTaakNaam" })).toHaveAttribute(
      "href",
      "/taken/fakeTaakId",
    );
    expect(screen.getByText("taak")).toBeVisible();
    expect(
      screen.queryByRole("link", { name: "fakeZaakIdentificatie" }),
    ).not.toBeInTheDocument();
  });

  it("refuses to render a zoekObject of an unsupported type", async () => {
    await expect(
      setup(
        fromPartial<ZoekObject>({
          type: "UNKNOWN" as GeneratedType<"ZoekObjectType">,
        }),
      ),
    ).rejects.toThrow("Search object type UNKNOWN is not supported");
  });

  it("shows the indicaties of a ZAAK", async () => {
    await setup(makeZaakZoekObject({ indicaties: ["HOOFDZAAK", "HEROPEND"] }));

    expect(screen.getAllByRole("option")).toHaveLength(2);
  });

  it("shows the indicaties of a DOCUMENT", async () => {
    await setup(makeDocumentZoekObject({ indicaties: ["BESLUIT"] }));

    expect(screen.getAllByRole("option")).toHaveLength(1);
  });

  describe("the side nav", () => {
    it("is closed when the link is followed", async () => {
      const { sideNav, user } = await setup(makeZaakZoekObject());

      await user.click(
        screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
      );

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });

    it("stays open when the link is followed while Control is held, to open it in a new tab", async () => {
      const { sideNav, user } = await setup(makeZaakZoekObject());

      await user.keyboard("{Control>}");
      await user.click(
        screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
      );

      expect(sideNav.close).not.toHaveBeenCalled();
    });

    it("is closed again once Control has been released", async () => {
      const { sideNav, user } = await setup(makeZaakZoekObject());

      await user.keyboard("{Control>}{/Control}");
      await user.click(
        screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
      );

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });

    it("is closed when the link is followed while another key than Control is held", async () => {
      const { sideNav, user } = await setup(makeZaakZoekObject());

      await user.keyboard("{Shift>}");
      await user.click(
        screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
      );

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });

    it("that replaced the previous one is the one that is closed", async () => {
      const { fixture, sideNav, user } = await setup(makeZaakZoekObject());
      const replacingSideNav = fromPartial<MatSidenav>({ close: jest.fn() });

      fixture.componentRef.setInput("sideNav", replacingSideNav);
      fixture.detectChanges();
      await user.click(
        screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
      );

      expect(replacingSideNav.close).toHaveBeenCalledTimes(1);
      expect(sideNav.close).not.toHaveBeenCalled();
    });
  });
});
