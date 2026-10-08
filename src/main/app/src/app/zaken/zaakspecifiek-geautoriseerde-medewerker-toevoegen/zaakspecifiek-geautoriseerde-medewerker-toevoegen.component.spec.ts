/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakspecifiekGeautoriseerdeMedewerkerToevoegenComponent } from "./zaakspecifiek-geautoriseerde-medewerker-toevoegen.component";

describe(ZaakspecifiekGeautoriseerdeMedewerkerToevoegenComponent.name, () => {
  const user = userEvent.setup({ delay: null });
  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    zaaktype: { omschrijving: "fakeZaaktype" },
  });
  const groep = fromPartial<GeneratedType<"RestGroup">>({
    id: "fakeGroepId",
    naam: "fakeGroepNaam",
  });
  const kandidaat = fromPartial<GeneratedType<"RestUser">>({
    id: "fakeMedewerkerId",
    naam: "fakeMedewerkerNaam",
  });

  let fixture: ComponentFixture<ZaakspecifiekGeautoriseerdeMedewerkerToevoegenComponent>;
  let httpTestingController: HttpTestingController;
  let sideNav: MatDrawer;
  let utilService: UtilService;

  async function setup() {
    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    const rendered = await render(
      ZaakspecifiekGeautoriseerdeMedewerkerToevoegenComponent,
      {
        imports: [NoopAnimationsModule, TranslateModule.forRoot()],
        inputs: { zaak, sideNav },
      },
    );
    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation();

    httpTestingController
      .expectOne("/rest/identity/zaaktype/fakeZaaktype/behandelaar-groups")
      .flush([groep]);
    await settle();
  }

  async function settle() {
    await sleep();
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  async function chooseGroep() {
    await sleep();
    await user.click(screen.getByRole("combobox", { name: /groep/i }));
    await user.click(
      await screen.findByRole("option", { name: "fakeGroepNaam" }),
    );
    httpTestingController
      .expectOne(
        "/rest/zaken/zaak/fakeZaakUuid/zaakspecifiek-geautoriseerde-medewerkers/kandidaten?groepId=fakeGroepId",
      )
      .flush([kandidaat]);
    await settle();
  }

  it("disables the medewerker select until a groep is chosen", async () => {
    await setup();

    expect(
      screen.getByRole("combobox", { name: /medewerker/i }),
    ).toHaveAttribute("aria-disabled", "true");
  });

  it("lists the kandidaten of the chosen groep", async () => {
    await setup();
    await chooseGroep();

    await user.click(screen.getByRole("combobox", { name: /medewerker/i }));

    expect(
      await screen.findByRole("option", { name: "fakeMedewerkerNaam" }),
    ).toBeInTheDocument();
  });

  it("adds the chosen medewerker, confirms it and closes the panel", async () => {
    await setup();
    await chooseGroep();
    await user.click(screen.getByRole("combobox", { name: /medewerker/i }));
    await user.click(
      await screen.findByRole("option", { name: "fakeMedewerkerNaam" }),
    );

    await user.click(screen.getByRole("button", { name: "actie.toevoegen" }));

    const request = httpTestingController.expectOne(
      "/rest/zaken/zaak/fakeZaakUuid/zaakspecifiek-geautoriseerde-medewerkers",
    );
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      groepId: "fakeGroepId",
      medewerkerId: "fakeMedewerkerId",
    });
    request.flush(null);
    await settle();

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.zaakspecifiek-geautoriseerde-medewerker.toegevoegd",
      { medewerker: "fakeMedewerkerNaam" },
    );
    expect(sideNav.close).toHaveBeenCalled();
  });
});
