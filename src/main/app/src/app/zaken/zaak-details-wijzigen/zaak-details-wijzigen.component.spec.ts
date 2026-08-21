/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MomentDateAdapter,
} from "@angular/material-moment-adapter";
import {
  DateAdapter,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE,
} from "@angular/material/core";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { CaseDetailsEditComponent } from "./zaak-details-wijzigen.component";

const sideNav = fromPartial<MatDrawer>({ close: jest.fn() });

const loggedInUser = fromPartial<GeneratedType<"RestLoggedInUser">>({
  id: "fakeLoggedInUserId",
  naam: "fakeLoggedInUserNaam",
});

const zaaktype = (
  servicenorm?: boolean | null,
): GeneratedType<"RestZaaktype"> =>
  fromPartial<GeneratedType<"RestZaaktype">>({
    uuid: "fakeZaaktypeUuid",
    omschrijving: "fakeZaaktypeOmschrijving",
    servicenorm,
  });

const rechten = (
  overrides: Partial<GeneratedType<"RestZaakRechten">> = {},
): GeneratedType<"RestZaakRechten"> =>
  fromPartial<GeneratedType<"RestZaakRechten">>({
    wijzigen: true,
    wijzigenDoorlooptijd: true,
    toekennen: true,
    ...overrides,
  });

const baseZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "fakeZaakUuid",
  omschrijving: "fakeZaakOmschrijving",
  startdatum: "2024-01-01",
  communicatiekanaal: "email",
  vertrouwelijkheidaanduiding: "OPENBAAR",
  rechten: rechten(),
  zaaktype: zaaktype(),
});

const groep = fromPartial<GeneratedType<"RestGroup">>({
  id: "fakeGroepId",
  naam: "fakeGroepNaam",
  active: true,
});

describe(CaseDetailsEditComponent.name, () => {
  let fixture: ComponentFixture<CaseDetailsEditComponent>;
  let identityService: IdentityService;
  let zakenService: ZakenService;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(() => notifyManager.setScheduler((fn) => fn()));
  afterEach(() => notifyManager.setScheduler(queueMicrotask));

  async function setup({
    zaakOverrides = {},
    groups = [],
    usersInGroup = [],
  }: {
    zaakOverrides?: Partial<GeneratedType<"RestZaak">>;
    groups?: GeneratedType<"RestGroup">[];
    usersInGroup?: GeneratedType<"RestUser">[];
  } = {}) {
    const { fixture: renderedFixture } = await render(
      CaseDetailsEditComponent,
      {
        inputs: {
          zaak: fromPartial<GeneratedType<"RestZaak">>({
            ...baseZaak,
            ...zaakOverrides,
          }),
          loggedInUser,
          sideNav,
        },
        imports: [TranslateModule.forRoot(), NoopAnimationsModule],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          provideQueryClient(testQueryClient),
          {
            provide: DateAdapter,
            useClass: MomentDateAdapter,
            deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS],
          },
          {
            provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS,
            useValue: { strict: false },
          },
          {
            provide: MAT_DATE_FORMATS,
            useValue: {
              parse: { dateInput: "yyyy-MM-DD" },
              display: {
                dateInput: "yyyy-MM-DD",
                monthYearLabel: "MMMM YYYY",
                dateA11yLabel: "LL",
                monthYearA11yLabel: "MMMM YYYY",
              },
            },
          },
        ],
        configureTestBed: (testBed) => {
          const identity = testBed.inject(IdentityService);
          jest
            .spyOn(identity, "listBehandelaarGroupsForZaaktype")
            .mockReturnValue(of(groups));
          jest
            .spyOn(identity, "listUsersInGroup")
            .mockReturnValue(of(usersInGroup));
          jest
            .spyOn(
              testBed.inject(ReferentieTabelService),
              "listCommunicatiekanalen",
            )
            .mockReturnValue(of(["email", "telefoon", "post"]));
        },
      },
    );

    fixture = renderedFixture;
    identityService = TestBed.inject(IdentityService);
    zakenService = TestBed.inject(ZakenService);
    httpTestingController = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function field(label: string) {
    return screen.getByLabelText(label);
  }

  function saveButton() {
    return screen.getByRole("button", { name: "actie.opslaan" });
  }

  async function fillInDate(label: string, date: string) {
    const input = field(label);
    await user.clear(input);
    await user.type(input, date);
    await user.tab();
    fixture.detectChanges();
  }

  async function openSelect(label: string) {
    await user.click(field(label));
    return screen.getByRole("listbox");
  }

  async function chooseOption(label: string, optionName: string) {
    const listbox = await openSelect(label);
    await user.click(within(listbox).getByRole("option", { name: optionName }));
  }

  async function makeSubmittable(reden = "fakeReden") {
    await user.type(field("Omschrijving"), "gewijzigd");
    await user.type(field("Reden"), reden);
  }

  const updateZaakRequest = () =>
    httpTestingController.expectOne(
      (request) =>
        request.method === "PATCH" &&
        request.url.includes("/rest/zaken/zaak/fakeZaakUuid"),
    );

  describe("permissions and servicenorm", () => {
    it.each`
      servicenorm  | disabled
      ${undefined} | ${true}
      ${null}      | ${true}
      ${false}     | ${true}
      ${true}      | ${false}
    `(
      "renders the streefdatum field as disabled=$disabled when the servicenorm is $servicenorm",
      async ({
        servicenorm,
        disabled,
      }: {
        servicenorm: boolean | null | undefined;
        disabled: boolean;
      }) => {
        await setup({ zaakOverrides: { zaaktype: zaaktype(servicenorm) } });

        if (disabled) {
          expect(field("EinddatumGepland")).toBeDisabled();
        } else {
          expect(field("EinddatumGepland")).toBeEnabled();
        }
      },
    );

    it("locks the editable zaak details when the user may not change the zaak", async () => {
      await setup({
        zaakOverrides: { rechten: rechten({ wijzigen: false }) },
      });

      expect(field("Communicatiekanaal")).toHaveAttribute(
        "aria-disabled",
        "true",
      );
      expect(field("Vertrouwelijkheidaanduiding")).toHaveAttribute(
        "aria-disabled",
        "true",
      );
      expect(field("Omschrijving")).toBeDisabled();
      expect(field("Toelichting")).toBeDisabled();
    });

    it("locks the groep when the user may not assign the zaak", async () => {
      await setup({
        zaakOverrides: { rechten: rechten({ toekennen: false }) },
      });

      expect(field("Groep")).toHaveAttribute("aria-disabled", "true");
    });

    it("locks the dates when the user may not change the doorlooptijd", async () => {
      await setup({
        zaakOverrides: { rechten: rechten({ wijzigenDoorlooptijd: false }) },
      });

      expect(field("Startdatum")).toBeDisabled();
      expect(field("UiterlijkeEinddatumAfdoening")).toBeDisabled();
    });

    it("leaves the dates editable for a procesgestuurde zaak the user may change", async () => {
      await setup({ zaakOverrides: { isProcesGestuurd: true } });

      expect(field("Startdatum")).toBeEnabled();
      expect(field("UiterlijkeEinddatumAfdoening")).toBeEnabled();
    });
  });

  describe("date validation", () => {
    const zaakWithAllDates = (startdatum: string) => ({
      startdatum,
      einddatumGepland: "2024-01-20",
      uiterlijkeEinddatumAfdoening: "2024-01-30",
      zaaktype: zaaktype(true),
    });

    it("reports a startdatum moved past the streefdatum", async () => {
      await setup({ zaakOverrides: zaakWithAllDates("2024-01-10") });

      await fillInDate("Startdatum", "2024-01-25");
      expect(
        screen.getByText("msg.error.date.invalid.datum.start-na-streef"),
      ).toBeVisible();
    });

    it("reports a streefdatum moved before the startdatum", async () => {
      await setup({ zaakOverrides: zaakWithAllDates("2024-01-10") });

      await fillInDate("EinddatumGepland", "2024-01-05");

      expect(
        screen.getByText("msg.error.date.invalid.datum.start-na-streef"),
      ).toBeVisible();
    });

    it("reports a startdatum moved past the fatale datum", async () => {
      await setup({ zaakOverrides: zaakWithAllDates("2024-01-10") });

      await fillInDate("Startdatum", "2024-02-01");

      expect(
        screen.getByText("msg.error.date.invalid.datum.start-na-fatale"),
      ).toBeVisible();
    });

    it("reports a fatale datum moved before the startdatum", async () => {
      await setup({
        zaakOverrides: {
          startdatum: "2024-01-20",
          uiterlijkeEinddatumAfdoening: "2024-02-01",
          zaaktype: zaaktype(true),
        },
      });

      await fillInDate("UiterlijkeEinddatumAfdoening", "2024-01-15");

      expect(
        screen.getByText("msg.error.date.invalid.datum.start-na-fatale"),
      ).toBeVisible();
    });

    it("reports a streefdatum moved past the fatale datum", async () => {
      await setup({ zaakOverrides: zaakWithAllDates("2024-01-01") });

      await fillInDate("EinddatumGepland", "2024-02-01");

      expect(
        screen.getByText("msg.error.date.invalid.datum.streef-na-fatale"),
      ).toBeVisible();
    });

    it("reports a fatale datum moved before the streefdatum", async () => {
      await setup({ zaakOverrides: zaakWithAllDates("2024-01-01") });

      await fillInDate("UiterlijkeEinddatumAfdoening", "2024-01-15");

      expect(
        screen.getByText("msg.error.date.invalid.datum.streef-na-fatale"),
      ).toBeVisible();
    });

    it("refuses to save while a date violation is on screen", async () => {
      await setup({ zaakOverrides: zaakWithAllDates("2024-01-10") });

      await makeSubmittable();
      await fillInDate("Startdatum", "2024-01-25");

      expect(saveButton()).toBeDisabled();
    });

    it("refuses to save without a startdatum", async () => {
      await setup();

      await makeSubmittable();
      await user.clear(field("Startdatum"));

      expect(saveButton()).toBeDisabled();
    });
  });

  describe("the groep dropdown", () => {
    it("asks for the groups of the zaaktype of the zaak", async () => {
      await setup();

      expect(
        identityService.listBehandelaarGroupsForZaaktype,
      ).toHaveBeenCalledWith("fakeZaaktypeOmschrijving");
    });

    it("offers the group of the zaak even when it is no longer available", async () => {
      await setup({
        groups: [groep],
        zaakOverrides: {
          groep: fromPartial<GeneratedType<"RestGroup">>({
            id: "fakeVerlopenGroepId",
            naam: "fakeVerlopenGroepNaam",
            active: false,
          }),
        },
      });

      const listbox = await openSelect("Groep");

      expect(within(listbox).getAllByRole("option")).toHaveLength(2);
      expect(
        within(listbox).getByRole("option", {
          name: /fakeVerlopenGroepNaam \(inactief\)/,
        }),
      ).toBeVisible();
    });

    it("offers the group of the zaak only once when it is still available", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });

      const listbox = await openSelect("Groep");

      expect(within(listbox).getAllByRole("option")).toHaveLength(1);
      expect(
        within(listbox).getByRole("option", { name: "fakeGroepNaam" }),
      ).toBeVisible();
    });

    it("offers only the available groups when the zaak has none", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep: undefined } });

      const listbox = await openSelect("Groep");

      expect(within(listbox).getAllByRole("option")).toHaveLength(1);
    });
  });

  describe("the behandelaar dropdown", () => {
    const behandelaar = fromPartial<GeneratedType<"RestUser">>({
      id: "fakeUserId",
      naam: "fakeUserNaam",
    });

    it("offers the users of the chosen group", async () => {
      await setup({ groups: [groep], usersInGroup: [behandelaar] });

      await chooseOption("Groep", "fakeGroepNaam");
      fixture.detectChanges();

      expect(identityService.listUsersInGroup).toHaveBeenCalledWith(
        "fakeGroepId",
      );
      const listbox = await openSelect("Behandelaar");
      expect(
        within(listbox).getByRole("option", { name: "fakeUserNaam" }),
      ).toBeVisible();
    });

    it("stays locked until a group is chosen", async () => {
      await setup({ usersInGroup: [behandelaar] });

      expect(field("Behandelaar")).toHaveAttribute("aria-disabled", "true");
    });

    it("is cleared and locked again when the groep is emptied", async () => {
      await setup({ groups: [groep], usersInGroup: [behandelaar] });

      const form = fixture.componentInstance["form"];
      form.controls.groep.setValue(null);

      expect(form.controls.behandelaar.disabled).toBe(true);
      expect(form.controls.behandelaar.value).toBeNull();
    });
  });

  describe("the details of the zaak", () => {
    it("shows the vertrouwelijkheidaanduiding of the zaak", async () => {
      await setup({
        zaakOverrides: { vertrouwelijkheidaanduiding: "OPENBAAR" },
      });

      expect(field("Vertrouwelijkheidaanduiding")).toHaveTextContent(
        "vertrouwelijkheidaanduiding.OPENBAAR",
      );
    });

    it("shows the toelichting of the zaak", async () => {
      await setup({ zaakOverrides: { toelichting: "fakeToelichting" } });

      expect(field("Toelichting")).toHaveValue("fakeToelichting");
    });
  });

  describe("the reden field", () => {
    it("is locked while nothing has been changed", async () => {
      await setup();

      expect(field("Reden")).toBeDisabled();
    });

    it("unlocks as soon as something is changed", async () => {
      await setup();

      await user.type(field("Omschrijving"), "gewijzigd");

      expect(field("Reden")).toBeEnabled();
    });
  });

  describe("closing the panel", () => {
    it("closes the panel from the toolbar", async () => {
      await setup();

      await user.click(
        screen.getByRole("button", { name: "actie.paneel.sluiten" }),
      );

      expect(sideNav.close).toHaveBeenCalled();
    });

    it("closes the panel from the cancel button", async () => {
      await setup();

      await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

      expect(sideNav.close).toHaveBeenCalled();
    });
  });

  describe("saving", () => {
    it("cannot be saved while nothing has been changed", async () => {
      await setup();

      expect(saveButton()).toBeDisabled();
    });

    it("can be saved once the form is valid and changed", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });

      await makeSubmittable();

      expect(saveButton()).toBeEnabled();
    });

    it("sends the reden along with the changed zaak and closes the panel", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });

      await makeSubmittable("fakeReden");
      await user.click(saveButton());
      await sleep();

      const request = updateZaakRequest();
      expect(request.request.body).toEqual(
        expect.objectContaining({ reden: "fakeReden" }),
      );

      request.flush({});
      await sleep();

      expect(sideNav.close).toHaveBeenCalled();
    });

    it("assigns the zaak to yourself when you pick yourself as behandelaar", async () => {
      await setup({
        groups: [groep],
        zaakOverrides: { groep },
        usersInGroup: [
          fromPartial<GeneratedType<"RestUser">>({
            id: loggedInUser.id,
            naam: "fakeLoggedInUserNaam",
          }),
        ],
      });
      jest
        .spyOn(zakenService, "toekennenAanIngelogdeMedewerker")
        .mockReturnValue(createMutationOptions(undefined) as never);

      await chooseOption("Behandelaar", "fakeLoggedInUserNaam");
      await makeSubmittable();
      await user.click(saveButton());
      await sleep();

      expect(zakenService.toekennenAanIngelogdeMedewerker).toHaveBeenCalled();
      updateZaakRequest().flush({});
    });

    it("assigns the zaak to the behandelaar you pick", async () => {
      await setup({
        groups: [groep],
        zaakOverrides: { groep },
        usersInGroup: [
          fromPartial<GeneratedType<"RestUser">>({
            id: "fakeAndereUserId",
            naam: "fakeAndereUserNaam",
          }),
        ],
      });
      jest
        .spyOn(zakenService, "toekennen")
        .mockReturnValue(of(undefined) as never);

      await chooseOption("Behandelaar", "fakeAndereUserNaam");
      await makeSubmittable();
      await user.click(saveButton());
      await sleep();

      expect(zakenService.toekennen).toHaveBeenCalled();
      updateZaakRequest().flush({});
    });

    it("does not reassign the zaak when groep and behandelaar are untouched", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });
      jest.spyOn(zakenService, "toekennen");
      jest.spyOn(zakenService, "toekennenAanIngelogdeMedewerker");

      await makeSubmittable();
      await user.click(saveButton());
      await sleep();

      expect(zakenService.toekennen).not.toHaveBeenCalled();
      expect(
        zakenService.toekennenAanIngelogdeMedewerker,
      ).not.toHaveBeenCalled();
      updateZaakRequest().flush({});
    });

    it("blocks a second save while the first one is still running", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });

      await makeSubmittable();
      await user.click(saveButton());
      await sleep();
      fixture.detectChanges();

      expect(saveButton()).toBeDisabled();
      const requests = httpTestingController.match(
        (request) =>
          request.method === "PATCH" &&
          request.url.includes("/rest/zaken/zaak/fakeZaakUuid"),
      );
      expect(requests).toHaveLength(1);

      requests[0].flush({});
      await sleep();
    });

    it("caches the zaak returned by the save so the view updates without a refetch", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });
      const updatedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-123",
        omschrijving: "fakeUpdatedOmschrijving",
      });
      const cacheZaak = jest.spyOn(zakenService, "cacheZaak");

      await makeSubmittable("fakeReden");
      await user.click(saveButton());
      await sleep();
      updateZaakRequest().flush(updatedZaak);
      await sleep();

      expect(cacheZaak).toHaveBeenCalledWith(updatedZaak);
    });

    it("keeps the save button locked after a successful save", async () => {
      await setup({ groups: [groep], zaakOverrides: { groep } });

      await makeSubmittable();
      await user.click(saveButton());
      await sleep();
      updateZaakRequest().flush({});
      await sleep();
      fixture.detectChanges();

      expect(saveButton()).toBeDisabled();
    });
  });
});
