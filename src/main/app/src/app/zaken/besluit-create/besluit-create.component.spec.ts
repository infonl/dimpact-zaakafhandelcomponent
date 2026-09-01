/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitCreateComponent } from "./besluit-create.component";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  zaaktype: { uuid: "zaaktype-uuid-1" },
});

const fakeBesluittype = fromPartial<GeneratedType<"RestBesluitType">>({
  id: "besluittype-id-1",
  naam: "Besluittype 1",
  publication: { enabled: false },
});

const fakeBesluittypeWithPublication = fromPartial<
  GeneratedType<"RestBesluitType">
>({
  id: "besluittype-id-2",
  naam: "Besluittype 2",
  publication: {
    enabled: true,
    responseTermDays: 6,
    publicationTermDays: 1,
  },
});

describe(BesluitCreateComponent.name, () => {
  const user = userEvent.setup();

  const setup = async (
    besluittypes: GeneratedType<"RestBesluitType">[] = [fakeBesluittype],
  ) => {
    const listResultaattypes = jest
      .spyOn(ZakenService.prototype, "listResultaattypes")
      .mockReturnValue(of([]) as never);
    jest
      .spyOn(ZakenService.prototype, "listBesluittypes")
      .mockReturnValue(of(besluittypes) as never);
    jest
      .spyOn(
        InformatieObjectenService.prototype,
        "listEnkelvoudigInformatieobjecten",
      )
      .mockReturnValue(createQueryOptions([]) as never);

    // the mutation stays pending, so only the payload it is handed is asserted on
    const createBesluit = jest.fn<Promise<void>, [unknown]>(
      () => new Promise<void>(() => {}),
    );
    jest.spyOn(ZakenService.prototype, "createBesluit").mockReturnValue(
      fromPartial({
        mutationKey: ["/rest/zaken/besluit"],
        mutationFn: createBesluit,
      }),
    );

    const sideNav = fromPartial<MatDrawer>({ close: jest.fn() });

    await render(BesluitCreateComponent, {
      inputs: { zaak: fakeZaak, sideNav },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
      ],
    });

    return { sideNav, createBesluit, listResultaattypes };
  };

  const selectBesluittype = async (naam: string) => {
    await user.click(screen.getByLabelText("Besluit"));
    await user.click(screen.getByRole("option", { name: naam }));
  };

  it("offers the besluittypes of the zaak's zaaktype", async () => {
    await setup([fakeBesluittype, fakeBesluittypeWithPublication]);

    await user.click(screen.getByLabelText("Besluit"));

    expect(
      screen.getAllByRole("option").map((option) => option.textContent?.trim()),
    ).toEqual(["Besluittype 1", "Besluittype 2"]);
  });

  it("loads the resultaattypes of the zaak's zaaktype", async () => {
    const { listResultaattypes } = await setup();

    expect(listResultaattypes).toHaveBeenCalledWith("zaaktype-uuid-1");
  });

  it("closes the side panel when the close button is used", async () => {
    const { sideNav } = await setup();

    await user.click(
      screen.getByRole("button", { name: "actie.paneel.sluiten" }),
    );

    expect(sideNav.close).toHaveBeenCalled();
  });

  it("closes the side panel when the cancel button is used", async () => {
    const { sideNav } = await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalled();
  });

  it("cannot be submitted without a besluittype", async () => {
    await setup();

    expect(
      screen.getByRole("button", { name: "actie.aanmaken" }),
    ).toBeDisabled();
  });

  it("can be submitted once a besluittype is chosen", async () => {
    await setup();

    await selectBesluittype("Besluittype 1");

    expect(
      screen.getByRole("button", { name: "actie.aanmaken" }),
    ).toBeEnabled();
  });

  it("hides the publication dates for a besluittype without publication", async () => {
    await setup();

    await selectBesluittype("Besluittype 1");

    expect(screen.queryByLabelText("Publicatiedatum")).toBeNull();
    expect(screen.queryByLabelText("Uiterlijkereactiedatum")).toBeNull();
  });

  it("shows the publication dates for a besluittype that requires publication", async () => {
    await setup([fakeBesluittypeWithPublication]);

    await selectBesluittype("Besluittype 2");

    expect(screen.getByLabelText("Publicatiedatum")).toBeVisible();
    expect(screen.getByLabelText("Uiterlijkereactiedatum")).toBeVisible();
  });

  it("creates the besluit for the chosen besluittype", async () => {
    const { createBesluit } = await setup();
    await selectBesluittype("Besluittype 1");

    await user.click(screen.getByRole("button", { name: "actie.aanmaken" }));

    expect(createBesluit.mock.calls[0][0]).toEqual(
      expect.objectContaining({
        zaakUuid: "zaak-uuid-1",
        besluittypeUuid: "besluittype-id-1",
      }),
    );
  });
});
