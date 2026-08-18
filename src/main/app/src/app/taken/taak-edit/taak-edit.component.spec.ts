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
import { TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
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
      lezen: true,
      toekennen: true,
      wijzigen: true,
      toevoegenDocument: true,
    },
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
    zaakIdentificatie: "fakeZaakIdentificatie",
    ...fields,
  });

describe(TaakEditComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let sideNav: MatSidenav;
  let identityService: Pick<
    IdentityService,
    "listBehandelaarGroupsForZaaktype" | "listUsersInGroup"
  >;

  const user = userEvent.setup();

  async function setup(task: GeneratedType<"RestTask"> = makeTask()) {
    sideNav = fromPartial<MatSidenav>({
      close: jest.fn().mockResolvedValue(true),
    });

    const { fixture } = await render(TaakEditComponent, {
      inputs: { task, sideNav },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: IdentityService, useValue: identityService },
        TakenService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    await sleep();
    fixture.detectChanges();

    httpTestingController = TestBed.inject(HttpTestingController);
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

  beforeEach(() => {
    identityService = {
      listBehandelaarGroupsForZaaktype: jest
        .fn()
        .mockReturnValue(of([makeGroup()])),
      listUsersInGroup: jest.fn().mockReturnValue(of([makeUser()])),
    };
  });

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

    expect(
      identityService.listBehandelaarGroupsForZaaktype,
    ).toHaveBeenCalledWith("fakeZaaktypeOmschrijving");
    expect(await openOptionsOf(groepSelect())).toEqual(["fakeGroupNaam"]);
  });

  it("preselects the groep and behandelaar of the task", async () => {
    identityService.listBehandelaarGroupsForZaaktype = jest
      .fn()
      .mockReturnValue(
        of([makeGroup({ id: "fakeGroupId42", naam: "fakeGroupNaam42" })]),
      );
    identityService.listUsersInGroup = jest
      .fn()
      .mockReturnValue(
        of([makeUser({ id: "fakeUserId7", naam: "fakeUserNaam7" })]),
      );

    await setup(
      makeTask({
        groep: makeGroup({ id: "fakeGroupId42", naam: "fakeGroupNaam42" }),
        behandelaar: makeUser({ id: "fakeUserId7", naam: "fakeUserNaam7" }),
      }),
    );

    expect(groepSelect()).toHaveTextContent("fakeGroupNaam42");
    expect(behandelaarSelect()).toHaveTextContent("fakeUserNaam7");
  });

  it("offers the groep of the task even when it is not among the loaded groups", async () => {
    identityService.listBehandelaarGroupsForZaaktype = jest
      .fn()
      .mockReturnValue(of([makeGroup({ id: "fakeOtherGroupId" })]));

    await setup(
      makeTask({
        groep: makeGroup({
          id: "fakeAbsentGroupId",
          naam: "fakeAbsentGroupNaam",
        }),
      }),
    );

    expect(await openOptionsOf(groepSelect())).toEqual([
      "fakeAbsentGroupNaam",
      "fakeGroupNaam",
    ]);
  });

  it("offers the groep of the task only once when it is among the loaded groups", async () => {
    const group = makeGroup({ id: "fakeGroupId", naam: "fakeGroupNaam" });
    identityService.listBehandelaarGroupsForZaaktype = jest
      .fn()
      .mockReturnValue(of([group]));

    await setup(makeTask({ groep: group }));

    expect(await openOptionsOf(groepSelect())).toEqual(["fakeGroupNaam"]);
  });

  it("marks an inactive groep as such in the options", async () => {
    identityService.listBehandelaarGroupsForZaaktype = jest
      .fn()
      .mockReturnValue(
        of([
          makeGroup({ id: "a", naam: "fakeActiveGroup", active: true }),
          makeGroup({ id: "b", naam: "fakeInactiveGroup", active: false }),
          makeGroup({ id: "c", naam: "fakeUnknownGroup", active: undefined }),
        ]),
      );

    await setup(makeTask({ groep: makeGroup({ id: "a" }) }));

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
          lezen: true,
          toekennen: false,
          wijzigen: true,
          toevoegenDocument: true,
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
      identityService.listBehandelaarGroupsForZaaktype = jest
        .fn()
        .mockReturnValue(
          of([
            makeGroup(),
            makeGroup({ id: "fakeOtherGroupId", naam: "fakeOtherGroupNaam" }),
          ]),
        );
      identityService.listUsersInGroup = jest
        .fn()
        .mockReturnValue(
          of([
            makeUser(),
            makeUser({ id: "fakeUserId99", naam: "fakeUserNaam99" }),
          ]),
        );

      await setup(task);
    }

    async function pickOtherGroup() {
      await user.click(groepSelect());
      await user.click(
        screen.getByRole("option", { name: "fakeOtherGroupNaam" }),
      );
      await sleep();
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

      expect(identityService.listUsersInGroup).toHaveBeenCalledWith(
        "fakeOtherGroupId",
      );
      expect(behandelaarSelect()).not.toHaveAttribute("aria-disabled", "true");
      expect(await openOptionsOf(behandelaarSelect())).toEqual([
        "-geen.generiek-",
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
