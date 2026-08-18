/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import { of } from "rxjs";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { BAGViewComponent } from "./bag-view.component";

describe(BAGViewComponent.name, () => {
  const setTitle = jest.fn();
  const list = jest.fn();

  function zaakSearchedFor() {
    const zoekParameters = list.mock.lastCall![0] as Parameters<
      ZoekenService["list"]
    >[0];
    return zoekParameters.zoeken?.ZAAK_BAGOBJECTEN;
  }

  async function setup(bagObject: GeneratedType<"RESTBAGObject">) {
    list.mockReturnValue(
      createQueryOptions(
        fromPartial<ZoekResultaat<ZaakZoekObject>>({
          totaal: 0,
          resultaten: [],
          filters: {},
        }),
      ),
    );

    await render(BAGViewComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideRouter([]),
        provideNativeDateAdapter(),
        { provide: ActivatedRoute, useValue: { data: of({ bagObject }) } },
        {
          provide: ZoekenService,
          useValue: fromPartial<ZoekenService>({ list }),
        },
        {
          provide: UtilService,
          useValue: fromPartial<UtilService>({
            setTitle,
            setLoading: jest.fn(),
          }),
        },
      ],
    });

    await sleep();
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

    expect(screen.getByText("objecttype.ADRES")).toBeVisible();
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

    expect(screen.getByText("objecttype.WOONPLAATS")).toBeVisible();
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

    expect(screen.getByText("objecttype.PAND")).toBeVisible();
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

    expect(screen.getByText("objecttype.NUMMERAANDUIDING")).toBeVisible();
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
