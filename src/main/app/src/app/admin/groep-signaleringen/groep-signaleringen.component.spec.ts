/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GroepSignaleringenComponent } from "./groep-signaleringen.component";

const GROUPS_URL = "/rest/identity/groups";
const INSTELLINGEN_URL = "/rest/signaleringen/group/fakeGroupId/instellingen";

const groep = fromPartial<GeneratedType<"RestGroup">>({
  id: "fakeGroupId",
  naam: "Fake groep",
});

const zaakOpNaam = fromPartial<GeneratedType<"RestSignaleringInstellingen">>({
  type: "ZAAK_OP_NAAM",
  subjecttype: "ZAAK",
  isDashboardEnabled: false,
  isMailEnabled: false,
});

describe(GroepSignaleringenComponent.name, () => {
  let fixture: ComponentFixture<GroepSignaleringenComponent>;
  let httpTestingController: HttpTestingController;
  let utilService: Pick<UtilService, "setTitle" | "setLoading">;

  const user = userEvent.setup();

  async function setup() {
    utilService = { setTitle: jest.fn(), setLoading: jest.fn() };

    ({ fixture } = await render(GroepSignaleringenComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
        { provide: UtilService, useValue: utilService },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
      ],
    }));

    httpTestingController = TestBed.inject(HttpTestingController);
    httpTestingController.expectOne(GROUPS_URL).flush([groep]);
    fixture.detectChanges();
  }

  async function respondWithInstellingen(
    instellingen: GeneratedType<"RestSignaleringInstellingen">[],
  ) {
    httpTestingController.expectOne(INSTELLINGEN_URL).flush(instellingen);
    await sleep();
    await sleep();
    fixture.detectChanges();
  }

  async function chooseGroup() {
    await user.click(screen.getByRole("combobox", { name: "groep.kies" }));
    await user.click(screen.getByRole("option", { name: "Fake groep" }));
    await sleep();
  }

  async function chooseGroupWithInstellingen() {
    await chooseGroup();
    await respondWithInstellingen([zaakOpNaam]);
  }

  it("sets the title and offers every group to choose from", async () => {
    await setup();

    expect(utilService.setTitle).toHaveBeenCalledWith(
      "title.signaleringen.settings.groep",
      undefined,
    );

    await user.click(screen.getByRole("combobox", { name: "groep.kies" }));

    expect(screen.getByRole("option", { name: "Fake groep" })).toBeVisible();
  });

  it("reads no settings before a group is chosen", async () => {
    await setup();

    httpTestingController.expectNone(INSTELLINGEN_URL);
  });

  it("shows the settings of the chosen group", async () => {
    await setup();

    await chooseGroupWithInstellingen();

    expect(screen.getByText("signalering.subjecttype.zaak")).toBeVisible();
    expect(
      screen.getByText("signalering.type.zaak-op-naam.group"),
    ).toBeVisible();
    expect(
      screen.getByRole("checkbox", { name: "actie.signalering.dashboard" }),
    ).not.toBeChecked();
  });

  it("marks the table as busy until the settings have arrived", async () => {
    await setup();

    await chooseGroup();

    expect(screen.getByRole("table")).toHaveAttribute("aria-busy", "true");

    await respondWithInstellingen([zaakOpNaam]);

    expect(screen.getByRole("table")).toHaveAttribute("aria-busy", "false");
  });

  it("saves the setting of the chosen group when a checkbox is ticked", async () => {
    await setup();
    await chooseGroupWithInstellingen();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.dashboard" }),
    );
    await sleep();

    expect(utilService.setLoading).toHaveBeenCalledWith(true);
    const request = httpTestingController.expectOne(INSTELLINGEN_URL);
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      ...zaakOpNaam,
      isDashboardEnabled: true,
    });
  });

  it("stops the loading indicator once the saved settings have been read again", async () => {
    await setup();
    await chooseGroupWithInstellingen();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.mail" }),
    );
    await sleep();
    httpTestingController
      .expectOne(INSTELLINGEN_URL)
      .flush({ ...zaakOpNaam, isMailEnabled: true });
    await sleep();

    expect(utilService.setLoading).not.toHaveBeenCalledWith(false);

    await respondWithInstellingen([{ ...zaakOpNaam, isMailEnabled: true }]);

    expect(utilService.setLoading).toHaveBeenCalledWith(false);
  });

  it("re-reads the settings of the group once the save has completed", async () => {
    await setup();
    await chooseGroupWithInstellingen();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.mail" }),
    );
    await sleep();
    httpTestingController
      .expectOne(INSTELLINGEN_URL)
      .flush({ ...zaakOpNaam, isMailEnabled: true });
    await sleep();

    const reread = httpTestingController.expectOne(INSTELLINGEN_URL);
    expect(reread.request.method).toBe("GET");
    reread.flush([{ ...zaakOpNaam, isMailEnabled: true }]);
    await sleep();
  });

  it("stops the loading indicator even when the save fails", async () => {
    await setup();
    await chooseGroupWithInstellingen();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.mail" }),
    );
    await sleep();
    httpTestingController
      .expectOne(INSTELLINGEN_URL)
      .flush(null, { status: 400, statusText: "Bad Request" });
    await sleep();

    expect(utilService.setLoading).toHaveBeenCalledWith(false);
  });
});
