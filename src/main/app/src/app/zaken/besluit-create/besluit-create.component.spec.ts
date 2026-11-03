/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitCreateComponent } from "./besluit-create.component";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  zaaktype: { uuid: "zaaktype-uuid-1" },
});

const otherZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-2",
  zaaktype: { uuid: "zaaktype-uuid-1" },
});

const zaakOfOtherZaaktype = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-3",
  zaaktype: { uuid: "zaaktype-uuid-2" },
});

const fakeBesluittype = fromPartial<GeneratedType<"RestBesluitType">>({
  id: "besluittype-id-1",
  naam: "Besluittype 1",
  publication: { isEnabled: false },
});

const fakeBesluittypeWithPublication = fromPartial<
  GeneratedType<"RestBesluitType">
>({
  id: "besluittype-id-2",
  naam: "Besluittype 2",
  publication: {
    isEnabled: true,
    responseTermDays: 6,
    publicationTermDays: 1,
  },
});

describe(BesluitCreateComponent.name, () => {
  const user = userEvent.setup();

  const setup = async (
    besluittypes: GeneratedType<"RestBesluitType">[] = [fakeBesluittype],
  ) => {
    const listEnkelvoudigInformatieobjecten = jest
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

    const { rerender, fixture } = await render(BesluitCreateComponent, {
      inputs: { zaak: fakeZaak, sideNav },
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
      ],
    });

    const httpTestingController = TestBed.inject(HttpTestingController);
    await respondWithBesluittypes(
      httpTestingController,
      "zaaktype-uuid-1",
      besluittypes,
    );

    const showZaak = async (zaak: GeneratedType<"RestZaak">) => {
      await rerender({ inputs: { zaak, sideNav }, partialUpdate: true });
      fixture.detectChanges();
    };

    return {
      sideNav,
      createBesluit,
      listEnkelvoudigInformatieobjecten,
      showZaak,
      httpTestingController,
    };
  };

  const respondWithBesluittypes = async (
    httpTestingController: HttpTestingController,
    zaaktypeUuid: string,
    besluittypes: GeneratedType<"RestBesluitType">[],
  ) => {
    await sleep();
    httpTestingController
      .expectOne({
        method: "GET",
        url: `/rest/zaken/besluittypes/${zaaktypeUuid}`,
      })
      .flush(besluittypes);
    await sleep();
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

  it("offers the besluittypes of the zaaktype of the zaak it currently shows", async () => {
    const { httpTestingController, showZaak } = await setup();

    await showZaak(zaakOfOtherZaaktype);
    await respondWithBesluittypes(httpTestingController, "zaaktype-uuid-2", [
      fakeBesluittypeWithPublication,
    ]);
    await user.click(screen.getByLabelText("Besluit"));

    expect(
      screen.getAllByRole("option").map((option) => option.textContent?.trim()),
    ).toEqual(["Besluittype 2"]);
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

  it("looks up the documents of the zaak that fit the chosen besluittype", async () => {
    const { listEnkelvoudigInformatieobjecten } = await setup();

    await selectBesluittype("Besluittype 1");

    expect(listEnkelvoudigInformatieobjecten).toHaveBeenCalledWith({
      zaakUUID: "zaak-uuid-1",
      besluittypeUUID: "besluittype-id-1",
    });
  });

  it("looks up the documents of the zaak it currently shows", async () => {
    const { listEnkelvoudigInformatieobjecten, showZaak } = await setup();
    await showZaak(otherZaak);

    await selectBesluittype("Besluittype 1");

    expect(listEnkelvoudigInformatieobjecten).toHaveBeenCalledWith({
      zaakUUID: "zaak-uuid-2",
      besluittypeUUID: "besluittype-id-1",
    });
  });

  it("creates the besluit for the zaak it currently shows", async () => {
    const { createBesluit, showZaak } = await setup();
    await showZaak(otherZaak);
    await selectBesluittype("Besluittype 1");

    await user.click(screen.getByRole("button", { name: "actie.aanmaken" }));

    expect(createBesluit.mock.calls[0][0]).toEqual(
      expect.objectContaining({ zaakUuid: "zaak-uuid-2" }),
    );
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
