/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MatSidenav } from "@angular/material/sidenav";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";
import { ZoekObjectLinkComponent } from "./zoek-object-link.component";

const makeZoekObject = (
  fields: Partial<GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">>,
): GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject"> =>
  fields as unknown as GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">;

const makeZaakZoekObject = (fields: Partial<ZaakZoekObject>): ZaakZoekObject =>
  fields as unknown as ZaakZoekObject;

const makeTaakZoekObject = (fields: Partial<TaakZoekObject>): TaakZoekObject =>
  fields as unknown as TaakZoekObject;

const makeDocumentZoekObject = (
  fields: Partial<DocumentZoekObject>,
): DocumentZoekObject => fields as unknown as DocumentZoekObject;

describe(ZoekObjectLinkComponent.name, () => {
  let component: ZoekObjectLinkComponent;

  beforeEach(() => {
    component = new ZoekObjectLinkComponent();
    component.sideNav = {} as MatSidenav;
  });

  describe("getLink", () => {
    it("returns zaak route for ZAAK type", () => {
      component.zoekObject = makeZaakZoekObject({
        type: "ZAAK",
        identificatie: "ZAAK-2026-001",
      });

      expect(component["getLink"]()).toEqual(["/zaken/", "ZAAK-2026-001"]);
    });

    it("returns taak route for TAAK type", () => {
      component.zoekObject = makeZoekObject({ type: "TAAK", id: "taak-uuid-123" });

      expect(component["getLink"]()).toEqual(["/taken/", "taak-uuid-123"]);
    });

    it("returns document route for DOCUMENT type", () => {
      component.zoekObject = makeZoekObject({ type: "DOCUMENT", id: "doc-uuid-456" });

      expect(component["getLink"]()).toEqual([
        "/informatie-objecten/",
        "doc-uuid-456",
      ]);
    });

    it("throws for unsupported type", () => {
      component.zoekObject = makeZoekObject({
        type: "UNKNOWN" as GeneratedType<"ZoekObjectType">,
      });

      expect(() => component["getLink"]()).toThrow();
    });
  });

  describe("getName", () => {
    it("returns identificatie for ZAAK type", () => {
      component.zoekObject = makeZaakZoekObject({
        type: "ZAAK",
        identificatie: "ZAAK-2026-001",
      });

      expect(component["getName"]()).toBe("ZAAK-2026-001");
    });

    it("returns naam for TAAK type", () => {
      component.zoekObject = makeTaakZoekObject({
        type: "TAAK",
        naam: "Beoordeel aanvraag",
      });

      expect(component["getName"]()).toBe("Beoordeel aanvraag");
    });

    it("returns titel for DOCUMENT type", () => {
      component.zoekObject = makeDocumentZoekObject({
        type: "DOCUMENT",
        titel: "Aanvraagformulier.pdf",
      });

      expect(component["getName"]()).toBe("Aanvraagformulier.pdf");
    });

    it("throws for unsupported type", () => {
      component.zoekObject = makeZoekObject({
        type: "UNKNOWN" as GeneratedType<"ZoekObjectType">,
      });

      expect(() => component["getName"]()).toThrow();
    });
  });

  describe("keyboard _newtab toggle", () => {
    it("sets _newtab to true on Control keydown", () => {
      component["handleKeydown"](new KeyboardEvent("keydown", { key: "Control" }));

      expect(component["_newtab"]).toBe(true);
    });

    it("sets _newtab to false on Control keyup", () => {
      component["_newtab"] = true;
      component["handleKeyup"](new KeyboardEvent("keyup", { key: "Control" }));

      expect(component["_newtab"]).toBe(false);
    });

    it("ignores non-Control keys", () => {
      component["handleKeydown"](new KeyboardEvent("keydown", { key: "Shift" }));

      expect(component["_newtab"]).toBe(false);
    });
  });
});
