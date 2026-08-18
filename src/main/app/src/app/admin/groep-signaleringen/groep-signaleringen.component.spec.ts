/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsBeheerService } from "../signaleringen-settings-beheer.service";
import { GroepSignaleringenComponent } from "./groep-signaleringen.component";

const groep = fromPartial<GeneratedType<"RestGroup">>({
  id: "fakeGroupId",
  naam: "Fake groep",
});

const createInstellingen = () =>
  fromPartial<GeneratedType<"RestSignaleringInstellingen">[]>([
    {
      type: "ZAAK_OP_NAAM",
      subjecttype: "ZAAK",
      dashboard: false,
      mail: false,
    },
  ]);

describe(GroepSignaleringenComponent.name, () => {
  let utilServiceMock: Pick<UtilService, "setTitle" | "setLoading">;
  let identityServiceMock: Pick<IdentityService, "listGroups">;
  let signaleringenServiceMock: Pick<
    SignaleringenSettingsBeheerService,
    "list" | "put"
  >;
  let putMutation: ReturnType<typeof createMutationOptions<null>>;
  let container: HTMLElement;

  const user = userEvent.setup();

  async function setup() {
    const rendered = await render(GroepSignaleringenComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideQueryClient(testQueryClient),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        { provide: IdentityService, useValue: identityServiceMock },
        {
          provide: SignaleringenSettingsBeheerService,
          useValue: signaleringenServiceMock,
        },
      ],
    });
    container = rendered.container;
  }

  async function chooseGroup() {
    await user.click(screen.getByRole("combobox", { name: "groep.-kies-" }));
    await user.click(screen.getByRole("option", { name: "Fake groep" }));
  }

  beforeEach(() => {
    utilServiceMock = { setTitle: jest.fn(), setLoading: jest.fn() };
    identityServiceMock = {
      listGroups: jest.fn().mockReturnValue(of([groep])),
    };
    putMutation = createMutationOptions(null);
    signaleringenServiceMock = {
      list: jest.fn().mockReturnValue(of(createInstellingen())),
      put: jest.fn().mockReturnValue(putMutation),
    };
  });

  it("sets the title and offers every group to choose from", async () => {
    await setup();

    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.signaleringen.settings.groep",
      undefined,
    );

    await user.click(screen.getByRole("combobox", { name: "groep.-kies-" }));

    expect(screen.getByRole("option", { name: "Fake groep" })).toBeVisible();
  });

  it("shows the settings of the chosen group", async () => {
    await setup();

    await chooseGroup();

    expect(signaleringenServiceMock.list).toHaveBeenCalledWith("fakeGroupId");
    expect(screen.getByText("signalering.subjecttype.ZAAK")).toBeVisible();
    expect(
      screen.getByText("signalering.type.ZAAK_OP_NAAM.group"),
    ).toBeVisible();
    expect(
      screen.getByRole("checkbox", { name: "actie.signalering.dashboard" }),
    ).not.toBeChecked();
  });

  it("stops showing the loading shade once the settings have arrived", async () => {
    await setup();

    await chooseGroup();

    expect(container.querySelector(".table-wrapper")).not.toHaveClass(
      "table-loading-shade",
    );
  });

  it("saves the setting of the chosen group when a checkbox is ticked", async () => {
    await setup();
    await chooseGroup();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.dashboard" }),
    );

    expect(utilServiceMock.setLoading).toHaveBeenCalledWith(true);
    expect(signaleringenServiceMock.put).toHaveBeenCalledWith("fakeGroupId");
    expect(putMutation.mutationFn).toHaveBeenCalledWith(
      expect.objectContaining({ type: "ZAAK_OP_NAAM", dashboard: true }),
      expect.anything(),
    );
  });

  it("stops the loading indicator once the save has completed", async () => {
    await setup();
    await chooseGroup();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.mail" }),
    );
    await sleep();

    expect(utilServiceMock.setLoading).toHaveBeenCalledWith(false);
  });

  it("stops the loading indicator even when the save fails", async () => {
    (signaleringenServiceMock.put as jest.Mock).mockReturnValue({
      mutationKey: ["failing-mutation"],
      mutationFn: () => Promise.reject(new Error("put failed")),
    });
    await setup();
    await chooseGroup();

    await user.click(
      screen.getByRole("checkbox", { name: "actie.signalering.mail" }),
    );
    await sleep();

    expect(utilServiceMock.setLoading).toHaveBeenCalledWith(false);
  });
});
