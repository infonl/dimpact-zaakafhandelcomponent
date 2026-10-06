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

  async function setup(bagObject: GeneratedType<"RESTBAGObject">) {
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
      fromPartial<GeneratedType<"RESTBAGObject">>({
        bagObjectType: "WOONPLAATS",
        identificatie: "3594",
      }),
    );

    expect(setTitle).toHaveBeenCalledWith("bagobjectgegevens");
  });

  it("shows an adres and the zaken it is linked to", async () => {
    await setup(
      fromPartial<GeneratedType<"RESTBAGAdres">>({
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
      fromPartial<GeneratedType<"RESTWoonplaats">>({
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
      fromPartial<GeneratedType<"RESTPand">>({
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
      fromPartial<GeneratedType<"RESTOpenbareRuimte">>({
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
      fromPartial<GeneratedType<"RESTNummeraanduiding">>({
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
      fromPartial<GeneratedType<"RESTBAGObject">>({
        bagObjectType: "ADRESSEERBAAR_OBJECT",
        identificatie: "0363010000721374",
        omschrijving: "Verblijfsobject",
      }),
    );

    expect(screen.queryByText("Verblijfsobject")).toBeNull();
    expect(zaakSearchedFor()).toBe("0363010000721374");
  });
});
