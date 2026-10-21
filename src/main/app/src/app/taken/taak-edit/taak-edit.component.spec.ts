/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
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
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../taken.service";
import { TaakEditComponent } from "./taak-edit.component";

const makeGroup = (fields: Partial<GeneratedType<"RestGroup">> = {}) =>
  fromPartial<GeneratedType<"RestGroup">>({
    id: "fakeGroupId",
    naam: "fakeGroupNaam",
    ...fields,
  });

const makeUser = (fields: Partial<GeneratedType<"RestUser">> = {}) =>
  fromPartial<GeneratedType<"RestUser">>({
    id: "fakeUserId",
    naam: "fakeUserNaam",
    ...fields,
  });

const makeTask = (fields: Partial<GeneratedType<"RestTask">> = {}) =>
  fromPartial<GeneratedType<"RestTask">>({
    id: "fakeTaakId",
    zaakUuid: "fakeZaakUuid",
    zaaktypeUUID: "fakeZaaktypeUuid",
    naam: "fakeTaakNaam",
    status: "TOEGEKEND",
    groep: makeGroup(),
    behandelaar: undefined,
    rechten: {
      canLezen: true,
      canToekennen: true,
      canWijzigen: true,
      canToevoegenDocument: true,
    },
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
    zaakIdentificatie: "fakeZaakIdentificatie",
    ...fields,
  });

describe(TaakEditComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<TaakEditComponent>;
  let sideNav: MatSidenav;

  const user = userEvent.setup();

  async function setup(
    task: GeneratedType<"RestTask"> = makeTask(),
    {
      groups = [makeGroup()],
      users = [makeUser()],
    }: {
      groups?: GeneratedType<"RestGroup">[];
      users?: GeneratedType<"RestUser">[];
    } = {},
  ) {
    sideNav = fromPartial<MatSidenav>({
      close: jest.fn().mockResolvedValue(true),
    });

    ({ fixture } = await render(TaakEditComponent, {
      inputs: { task, sideNav },
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        TakenService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }));

    httpTestingController = TestBed.inject(HttpTestingController);

    await respondWith(
      `/rest/identity/zaaktype/${task.zaaktypeOmschrijving}/behandelaar-groups`,
      groups,
    );
    if (task.groep) {
      await respondWith(`/rest/identity/groups/${task.groep.id}/users`, users);
    }
  }

  async function respondWith(url: string, body: unknown[]) {
    await sleep();
    httpTestingController.expectOne(url).flush(body);
    await sleep();
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  const groepSelect = () => screen.getByRole("combobox", { name: /Groep/ });
  const behandelaarSelect = () =>
    screen.getByRole("combobox", { name: /Behandelaar/ });
  const redenInput = () => screen.getByRole("textbox", { name: /Reden/ });
  const wijzigenButton = () =>
    screen.getByRole("button", { name: "actie.wijzigen" });

  async function openOptionsOf(select: HTMLElement) {
    await user.click(select);
    const options = screen
      .getAllByRole("option")
      .map((option) => option.textContent?.trim());
    await user.keyboard("{Escape}");
    return options;
  }

  it("shows the edit heading", async () => {
    await setup();

    expect(screen.getByRole("heading")).toHaveTextContent(
      "actie.taak.wijzigen",
    );
  });

  it("closes the side navigation from the close button", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.sluiten" }));

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });

  it("offers the groups that may handle the zaaktype of the task", async () => {
    await setup(makeTask({ zaaktypeOmschrijving: "fakeZaaktypeOmschrijving" }));

    expect(await openOptionsOf(groepSelect())).toEqual(["fakeGroupNaam"]);
  });

  it("preselects the groep and behandelaar of the task", async () => {
    await setup(
      makeTask({
        groep: makeGroup({ id: "fakeGroupId42", naam: "fakeGroupNaam42" }),
        behandelaar: makeUser({ id: "fakeUserId7", naam: "fakeUserNaam7" }),
      }),
      {
        groups: [makeGroup({ id: "fakeGroupId42", naam: "fakeGroupNaam42" })],
        users: [makeUser({ id: "fakeUserId7", naam: "fakeUserNaam7" })],
      },
    );

    expect(groepSelect()).toHaveTextContent("fakeGroupNaam42");
    expect(behandelaarSelect()).toHaveTextContent("fakeUserNaam7");
  });

  it("offers the groep of the task even when it is not among the loaded groups", async () => {
    await setup(
      makeTask({
        groep: makeGroup({
          id: "fakeAbsentGroupId",
          naam: "fakeAbsentGroupNaam",
        }),
      }),
      { groups: [makeGroup({ id: "fakeOtherGroupId" })] },
    );

    expect(await openOptionsOf(groepSelect())).toEqual([
      "fakeAbsentGroupNaam",
      "fakeGroupNaam",
    ]);
  });

  it("offers the groep of the task only once when it is among the loaded groups", async () => {
    const group = makeGroup({ id: "fakeGroupId", naam: "fakeGroupNaam" });
    await setup(makeTask({ groep: group }), { groups: [group] });

    expect(await openOptionsOf(groepSelect())).toEqual(["fakeGroupNaam"]);
  });

  it("marks an inactive groep as such in the options", async () => {
    await setup(makeTask({ groep: makeGroup({ id: "a" }) }), {
      groups: [
        makeGroup({ id: "a", naam: "fakeActiveGroup", isActive: true }),
        makeGroup({ id: "b", naam: "fakeInactiveGroup", isActive: false }),
        makeGroup({ id: "c", naam: "fakeUnknownGroup", isActive: undefined }),
      ],
    });

    expect(await openOptionsOf(groepSelect())).toEqual([
      "fakeActiveGroup",
      "fakeInactiveGroup (inactief)",
      "fakeUnknownGroup",
    ]);
  });

  it("does not let you pick a behandelaar while the task has no groep", async () => {
    await setup(makeTask({ groep: undefined }));

    expect(behandelaarSelect()).toHaveAttribute("aria-disabled", "true");
  });

  it("cannot be edited when the task is done", async () => {
    await setup(makeTask({ status: "AFGEROND" }));

    expect(groepSelect()).toHaveAttribute("aria-disabled", "true");
    expect(redenInput()).toBeDisabled();
    expect(wijzigenButton()).toBeDisabled();
  });

  it("cannot be edited without the right to assign the task", async () => {
    await setup(
      makeTask({
        rechten: {
          canLezen: true,
          canToekennen: false,
          canWijzigen: true,
          canToevoegenDocument: true,
        },
      }),
    );

    expect(groepSelect()).toHaveAttribute("aria-disabled", "true");
    expect(redenInput()).toBeDisabled();
  });

  it("can be edited when the task is open and may be assigned", async () => {
    await setup(makeTask({ status: "TOEGEKEND" }));

    expect(groepSelect()).not.toHaveAttribute("aria-disabled", "true");
    expect(redenInput()).toBeEnabled();
  });

  describe("picking another groep", () => {
    async function setupWithTwoGroups(
      task: GeneratedType<"RestTask"> = makeTask(),
    ) {
      await setup(task, {
        groups: [
          makeGroup(),
          makeGroup({ id: "fakeOtherGroupId", naam: "fakeOtherGroupNaam" }),
        ],
      });
    }

    async function pickOtherGroup() {
      await user.click(groepSelect());
      await user.click(
        screen.getByRole("option", { name: "fakeOtherGroupNaam" }),
      );
      await respondWith("/rest/identity/groups/fakeOtherGroupId/users", [
        makeUser(),
        makeUser({ id: "fakeUserId99", naam: "fakeUserNaam99" }),
      ]);
    }

    it("clears the behandelaar", async () => {
      await setupWithTwoGroups(makeTask({ behandelaar: makeUser() }));
      expect(behandelaarSelect()).toHaveTextContent("fakeUserNaam");

      await pickOtherGroup();

      expect(behandelaarSelect()).not.toHaveTextContent("fakeUserNaam");
    });

    it("offers the users of that groep as behandelaar", async () => {
      await setupWithTwoGroups();

      await pickOtherGroup();

      expect(behandelaarSelect()).not.toHaveAttribute("aria-disabled", "true");
      expect(await openOptionsOf(behandelaarSelect())).toEqual([
        "geen.generiek",
        "fakeUserNaam",
        "fakeUserNaam99",
      ]);
    });
  });

  describe("submitting", () => {
    it("assigns the task to the picked groep and behandelaar", async () => {
      await setup(
        makeTask({
          id: "fakeTaakId1",
          zaakUuid: "fakeZaakUuid1",
          groep: makeGroup({ id: "fakeGroupId1" }),
          behandelaar: makeUser({ id: "fakeUserId1" }),
        }),
      );

      await user.type(redenInput(), "fakeReden");
      await user.click(wijzigenButton());
      await sleep();

      const request = httpTestingController.expectOne("/rest/taken/toekennen");
      expect(request.request.method).toBe("PATCH");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          taakId: "fakeTaakId1",
          zaakUuid: "fakeZaakUuid1",
          groepId: "fakeGroupId1",
          behandelaarId: "fakeUserId1",
          reden: "fakeReden",
        }),
      );
      request.flush({});
    });

    it("assigns the task without a behandelaar when none is picked", async () => {
      await setup(
        makeTask({
          id: "fakeTaakId2",
          zaakUuid: "fakeZaakUuid2",
          groep: makeGroup({ id: "fakeGroupId2" }),
          behandelaar: undefined,
        }),
      );

      await user.type(redenInput(), "fakeReden");
      await user.click(wijzigenButton());
      await sleep();

      const request = httpTestingController.expectOne("/rest/taken/toekennen");
      expect(request.request.body).toEqual(
        expect.objectContaining({ behandelaarId: undefined }),
      );
      request.flush({});
    });

    it("closes the side navigation once the task is assigned", async () => {
      await setup();

      await user.type(redenInput(), "fakeReden");
      await user.click(wijzigenButton());
      await sleep();

      httpTestingController.expectOne("/rest/taken/toekennen").flush({});
      await sleep();

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });
  });
});
