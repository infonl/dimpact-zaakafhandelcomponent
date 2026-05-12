/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SimpleChange } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { GeneratedType } from "../../utils/generated-types";
import { ZaakIndicatiesComponent } from "./zaak-indicaties.component";

const mockZaakBase = {
  gerelateerdeZaken: [],
  redenOpschorting: "",
  redenVerlenging: "",
} as unknown as GeneratedType<"RestZaak">;

describe(ZaakIndicatiesComponent.name, () => {
  let component: ZaakIndicatiesComponent;
  let translateInstant: jest.Mock;

  beforeEach(() => {
    translateInstant = jest.fn((key: string) => key);
    component = new ZaakIndicatiesComponent({
      instant: translateInstant,
    } as unknown as TranslateService);
  });

  it("shows no indicaties when zaak has no indicaties", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        { ...mockZaakBase, indicaties: [] },
        true,
      ),
    });

    expect(component["indicaties"]).toHaveLength(0);
  });

  it("OPSCHORTING → icon 'pause', primary=true, toelichting contains reden", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["OPSCHORTING"],
          redenOpschorting: "vakantie",
        },
        true,
      ),
    });

    expect(component["indicaties"]).toHaveLength(1);
    expect(component["indicaties"][0].naam).toBe("OPSCHORTING");
    expect(component["indicaties"][0].icon).toBe("pause");
    expect(component["indicaties"][0].primary).toBe(true);
    expect(component["indicaties"][0].toelichting).toContain("vakantie");
  });

  it("HEROPEND → icon 'restart_alt', primary=true", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["HEROPEND"],
          status: { toelichting: "heropend wegens bezwaar" },
        },
        true,
      ),
    });

    expect(component["indicaties"][0].icon).toBe("restart_alt");
    expect(component["indicaties"][0].primary).toBe(true);
  });

  it("VERLENGD → icon 'update', primary=false", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["VERLENGD"],
          redenVerlenging: "extra tijd nodig",
        },
        true,
      ),
    });

    expect(component["indicaties"][0].icon).toBe("update");
    expect(component["indicaties"][0].primary).toBe(false);
  });

  it("HOOFDZAAK → icon 'account_tree', primary=false", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["HOOFDZAAK"],
          gerelateerdeZaken: [
            { relatieType: "DEELZAAK", identificatie: "ZAAK-001" },
          ],
        },
        true,
      ),
    });

    expect(component["indicaties"][0].icon).toBe("account_tree");
    expect(component["indicaties"][0].primary).toBe(false);
    expect(component["indicaties"][0].outlined).toBe(false);
  });

  it("DEELZAAK → icon 'account_tree', outlined=true", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["DEELZAAK"],
          gerelateerdeZaken: [
            { relatieType: "HOOFDZAAK", identificatie: "ZAAK-000" },
          ],
        },
        true,
      ),
    });

    expect(component["indicaties"][0].icon).toBe("account_tree");
    expect(component["indicaties"][0].outlined).toBe(true);
  });

  it("ONTVANGSTBEVESTIGING_NIET_VERSTUURD → icon 'unsubscribe'", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
        },
        true,
      ),
    });

    expect(component["indicaties"][0].icon).toBe("unsubscribe");
    expect(component["indicaties"][0].primary).toBe(false);
  });

  it("falls back to zaakZoekObject when zaak is not present", () => {
    component.ngOnChanges({
      zaakZoekObject: new SimpleChange(
        undefined,
        {
          indicaties: ["OPSCHORTING"],
          redenOpschorting: "herstelwerkzaamheden",
        } as unknown as ZaakZoekObject,
        true,
      ),
    });

    expect(component["indicaties"]).toHaveLength(1);
    expect(component["indicaties"][0].naam).toBe("OPSCHORTING");
  });

  it("renders all indicaties when multiple are provided", () => {
    component.ngOnChanges({
      zaak: new SimpleChange(
        undefined,
        {
          ...mockZaakBase,
          indicaties: ["VERLENGD", "ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
        },
        true,
      ),
    });

    expect(component["indicaties"].map((i) => i.naam)).toEqual([
      "VERLENGD",
      "ONTVANGSTBEVESTIGING_NIET_VERSTUURD",
    ]);
  });
});
