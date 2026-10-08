/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BagZakenTabelComponent } from "../bag-zaken-tabel/bag-zaken-tabel.component";
import { BAGViewComponent } from "./bag-view.component";

@Component({
  selector: "zac-bag-zaken-tabel",
  template: `<p>zaken of: {{ BagObjectIdentificatie() }}</p>`,
  standalone: true,
})
class BagZakenTabelStubComponent {
  readonly BagObjectIdentificatie = input<string>();
}

describe(BAGViewComponent.name, () => {
  const setTitle = jest.fn();

  function zaakSearchedFor() {
    return screen
      .getByText(/^zaken of:/)
      .textContent?.replace("zaken of: ", "");
  }

  async function setup(bagObject: GeneratedType<"RestBagObject">) {
    TestBed.overrideComponent(BAGViewComponent, {
      remove: { imports: [BagZakenTabelComponent] },
      add: { imports: [BagZakenTabelStubComponent] },
    });

    await render(BAGViewComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: ActivatedRoute, useValue: { data: of({ bagObject }) } },
        {
          provide: UtilService,
          useValue: fromPartial<UtilService>({
            setTitle,
            setLoading: jest.fn(),
          }),
        },
      ],
    });
  }

  it("titles the page after the bag object it shows", async () => {
    await setup(
      fromPartial<GeneratedType<"RestBagObject">>({
        bagObjectType: "WOONPLAATS",
        identificatie: "3594",
      }),
    );

    expect(setTitle).toHaveBeenCalledWith("bagobjectgegevens");
  });

  it("shows an adres and the zaken it is linked to", async () => {
    await setup(
      fromPartial<GeneratedType<"RestBagAdres">>({
        bagObjectType: "ADRES",
        identificatie: "0363200000218908",
        omschrijving: "Teststraat 1, Amsterdam",
      }),
    );

    expect(screen.getByText("objecttype.adres")).toBeVisible();
    expect(screen.getByText("Teststraat 1, Amsterdam")).toBeVisible();
    expect(zaakSearchedFor()).toBe("0363200000218908");
  });

  it("shows a woonplaats and the zaken it is linked to", async () => {
    await setup(
      fromPartial<GeneratedType<"RestWoonplaats">>({
        bagObjectType: "WOONPLAATS",
        identificatie: "3594",
        omschrijving: "Amsterdam",
      }),
    );

    expect(screen.getByText("objecttype.woonplaats")).toBeVisible();
    expect(screen.getByText("3594")).toBeVisible();
    expect(zaakSearchedFor()).toBe("3594");
  });

  it("shows a pand and the zaken it is linked to", async () => {
    await setup(
      fromPartial<GeneratedType<"RestPand">>({
        bagObjectType: "PAND",
        identificatie: "0363100012165490",
        omschrijving: "Pand aan de Teststraat",
      }),
    );

    expect(screen.getByText("objecttype.pand")).toBeVisible();
    expect(screen.getByText("0363100012165490")).toBeVisible();
    expect(zaakSearchedFor()).toBe("0363100012165490");
  });

  it("shows an openbare ruimte and the zaken it is linked to", async () => {
    await setup(
      fromPartial<GeneratedType<"RestOpenbareRuimte">>({
        bagObjectType: "OPENBARE_RUIMTE",
        identificatie: "0363300000002244",
        omschrijving: "Teststraat, Amsterdam",
      }),
    );

    expect(screen.getByText("Teststraat, Amsterdam")).toBeVisible();
    expect(screen.getByText("0363300000002244")).toBeVisible();
    expect(zaakSearchedFor()).toBe("0363300000002244");
  });

  it("shows a nummeraanduiding and the zaken it is linked to", async () => {
    await setup(
      fromPartial<GeneratedType<"RestNummeraanduiding">>({
        bagObjectType: "NUMMERAANDUIDING",
        identificatie: "0363200000218908",
        omschrijving: "Teststraat 1",
        huisnummerWeergave: "1",
      }),
    );

    expect(screen.getByText("objecttype.nummeraanduiding")).toBeVisible();
    expect(screen.getByText("Teststraat 1")).toBeVisible();
    expect(zaakSearchedFor()).toBe("0363200000218908");
  });

  it("shows the zaken of an adresseerbaar object, which has no details of its own", async () => {
    await setup(
      fromPartial<GeneratedType<"RestBagObject">>({
        bagObjectType: "ADRESSEERBAAR_OBJECT",
        identificatie: "0363010000721374",
        omschrijving: "Verblijfsobject",
      }),
    );

    expect(screen.queryByText("Verblijfsobject")).toBeNull();
    expect(zaakSearchedFor()).toBe("0363010000721374");
  });

  describe("empty values", () => {
    it("shows the address line of an adres with only the parts that are present", async () => {
      await setup(
        fromPartial<GeneratedType<"RestBagAdres">>({
          bagObjectType: "ADRES",
          identificatie: "0568200000304496",
          openbareRuimteNaam: null,
          huisnummerWeergave: "12",
          postcode: undefined,
          woonplaatsNaam: "Testplaats",
        }),
      );

      expect(screen.getByText("12 Testplaats")).toBeVisible();
      expect(screen.queryByText(/undefined|null/)).toBeNull();
    });

    it("does not show the sections of an adres whose related bag objects are missing", async () => {
      await setup(
        fromPartial<GeneratedType<"RestBagAdres">>({
          bagObjectType: "ADRES",
          identificatie: "0568200000304496",
          panden: [],
          adresseerbaarObject: null,
          nummeraanduiding: null,
          openbareRuimte: null,
          woonplaats: null,
        }),
      );

      expect(screen.getByText("objecttype.adres")).toBeVisible();
      expect(screen.queryByText("objecttype.pand")).toBeNull();
      expect(screen.queryByText("objecttype.nummeraanduiding")).toBeNull();
      expect(screen.queryByText("objecttype.openbare-ruimte")).toBeNull();
      expect(screen.queryByText("objecttype.woonplaats")).toBeNull();
      expect(screen.queryByText("bouwjaar")).toBeNull();
      expect(screen.queryByText("status")).toBeNull();
      expect(screen.queryByText(/undefined|null/)).toBeNull();
    });

    it("does not show the empty fields of the related bag objects of an adres", async () => {
      await setup(
        fromPartial<GeneratedType<"RestBagAdres">>({
          bagObjectType: "ADRES",
          identificatie: "0568200000304496",
          panden: [{ identificatie: "0568100000012345" }],
          adresseerbaarObject: {
            typeAdresseerbaarObject: "VERBLIJFSOBJECT",
            identificatie: "0568010000012345",
            vboOppervlakte: 0,
            vboDoel: "",
          },
        }),
      );

      expect(screen.getByText("objecttype.pand")).toBeVisible();
      expect(screen.getByText("0568100000012345")).toBeVisible();
      expect(screen.getByText("0568010000012345")).toBeVisible();
      expect(screen.queryByText("bouwjaar")).toBeNull();
      expect(screen.queryByText("oppervlakte")).toBeNull();
      expect(screen.queryByText("gebruiksdoel")).toBeNull();
      expect(screen.queryByText(/m2/)).toBeNull();
      expect(screen.queryByText(/undefined|null/)).toBeNull();
    });

    it("does not show the empty fields of a nummeraanduiding", async () => {
      await setup(
        fromPartial<GeneratedType<"RestNummeraanduiding">>({
          bagObjectType: "NUMMERAANDUIDING",
          identificatie: "0568200000304496",
          huisnummer: 12,
          huisletter: null,
          huisnummertoevoeging: null,
          openbareRuimte: null,
          woonplaats: null,
        }),
      );

      expect(screen.getByText("huisnummer")).toBeVisible();
      expect(screen.queryByText("huisletter")).toBeNull();
      expect(screen.queryByText("huisnummertoevoeging")).toBeNull();
      expect(screen.queryByText("straat")).toBeNull();
      expect(screen.queryByText("objecttype.openbare-ruimte")).toBeNull();
      expect(screen.queryByText(/undefined|null/)).toBeNull();
    });
  });
});
