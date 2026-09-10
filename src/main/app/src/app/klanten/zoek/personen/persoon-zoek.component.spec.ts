/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantenService } from "../../klanten.service";
import { FormCommunicatieService } from "../form-communicatie-service";
import { PersoonZoekComponent } from "./persoon-zoek.component";

const personenParameters = fromPartial<GeneratedType<"RestPersonenParameters">>(
  {
    bsn: "REQ",
    geboortedatum: "OPT",
    gemeenteVanInschrijving: "OPT",
    geslachtsnaam: "NON",
    huisnummer: "NON",
    postcode: "NON",
    straat: "NON",
    voornamen: "NON",
    voorvoegsel: "NON",
  },
);

describe(PersoonZoekComponent.name, () => {
  const user = userEvent.setup();
  const listPersonen = jest.fn();
  let detectChanges: () => void;

  async function setup(
    gemeenten: GeneratedType<"RestBrpGemeente">[] = [],
  ): Promise<void> {
    listPersonen.mockReturnValue(
      createQueryOptions(
        fromPartial<GeneratedType<"RESTResultaatRestPersoon">>({
          resultaten: [],
        }),
      ),
    );

    const rendered = await render(PersoonZoekComponent, {
      inputs: { zaaktypeUUID: "fakeZaaktypeUuid" },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideNativeDateAdapter(),
        {
          provide: KlantenService,
          useValue: fromPartial<KlantenService>({
            getPersonenParameters: () => of([personenParameters]),
            listPersonen: listPersonen as never,
            listAuthorisedBrpGemeenten: () =>
              ({
                queryKey: ["brpGemeenten"],
                queryFn: () => Promise.resolve(gemeenten),
              }) as never,
          }),
        },
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readGemeenteCode: () => of("1234"),
          }),
        },
        {
          provide: UtilService,
          useValue: fromPartial<UtilService>({ setLoading: jest.fn() }),
        },
        {
          provide: FormCommunicatieService,
          useValue: fromPartial<FormCommunicatieService>({
            itemSelected$: of({ selected: false, uuid: "fakeUuid" }),
            notifyItemSelected: jest.fn(),
          }),
        },
      ],
    });

    detectChanges = rendered.detectChanges;
    await sleep();
    detectChanges();
  }

  async function search() {
    await user.click(screen.getByRole("button", { name: "actie.zoeken" }));
    await sleep();
    detectChanges();
  }

  it("searches for the persoon with the entered bsn", async () => {
    await setup();

    await user.type(screen.getByLabelText("Bsn"), "999990408");
    await search();

    expect(listPersonen).toHaveBeenCalledWith(
      expect.objectContaining({ bsn: "999990408" }),
      "fakeZaaktypeUuid",
    );
  });

  it("cannot search before a complete combination of fields is filled in", async () => {
    await setup();

    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeDisabled();
  });

  it("disables the fields that cannot be combined with the bsn", async () => {
    await setup();

    await user.type(screen.getByLabelText("Bsn"), "999990408");

    for (const label of [
      "Voornamen",
      "Voorvoegsel",
      "Geslachtsnaam",
      "Straat",
      "Postcode",
      "Huisnummer",
    ]) {
      expect(screen.getByLabelText(label)).toBeDisabled();
    }
  });

  it("keeps the fields that are optional next to the bsn enabled", async () => {
    await setup();

    await user.type(screen.getByLabelText("Bsn"), "999990408");

    expect(screen.getByLabelText("Geboortedatum")).toBeEnabled();
    expect(screen.getByLabelText("GemeenteVanInschrijving")).toBeEnabled();
  });

  it("searches with the gemeente code that was typed in", async () => {
    await setup();

    await user.type(screen.getByLabelText("Bsn"), "999990408");
    await user.type(screen.getByLabelText("GemeenteVanInschrijving"), "1234");
    await search();

    expect(listPersonen).toHaveBeenCalledWith(
      expect.objectContaining({ gemeenteVanInschrijving: "1234" }),
      "fakeZaaktypeUuid",
    );
  });

  it("searches with the code of the gemeente that was chosen", async () => {
    await setup([
      fromPartial<GeneratedType<"RestBrpGemeente">>({
        code: "0344",
        naam: "Utrecht",
      }),
      fromPartial<GeneratedType<"RestBrpGemeente">>({
        code: "0363",
        naam: "Amsterdam",
      }),
    ]);

    await user.type(screen.getByLabelText("Bsn"), "999990408");
    await user.click(screen.getByRole("combobox"));
    await user.click(screen.getByRole("option", { name: "Amsterdam" }));
    await search();

    expect(listPersonen).toHaveBeenCalledWith(
      expect.objectContaining({ gemeenteVanInschrijving: "0363" }),
      "fakeZaaktypeUuid",
    );
  });

  it("chooses the gemeente when there is only one to choose from", async () => {
    await setup([
      fromPartial<GeneratedType<"RestBrpGemeente">>({
        code: "0344",
        naam: "Utrecht",
      }),
    ]);

    const gemeente = await screen.findByRole("combobox");

    expect(await within(gemeente).findByText("Utrecht")).toBeVisible();
  });
});
