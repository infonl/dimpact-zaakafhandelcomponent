/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { sleep } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";

const loggedInUser = fromPartial<GeneratedType<"RestLoggedInUser">>({
  username: "currentUser",
  naam: "test",
});

const notitie = (fields: Partial<GeneratedType<"RestNote">> = {}) =>
  fromPartial<GeneratedType<"RestNote">>({
    id: 1,
    zaakUUID: "fakeZaakUuid",
    tekst: "fakeTekst1",
    isBewerkenToegestaan: true,
    ...fields,
  });

describe(NotitiesComponent.name, () => {
  let fixture: ComponentFixture<NotitiesComponent>;
  let httpTestingController: HttpTestingController;
  let sideNav: MatDrawer;

  async function setup({
    notities = [],
    notitieRechten,
  }: {
    notities?: GeneratedType<"RestNote">[];
    notitieRechten?: GeneratedType<"RestNotitieRechten">;
  } = {}) {
    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });

    ({ fixture } = await render(NotitiesComponent, {
      inputs: { sideNav, zaakUuid: "fakeZaakUuid", notitieRechten },
      imports: [NoopAnimationsModule],
      providers: [provideTranslateService()],
    }));

    httpTestingController = TestBed.inject(HttpTestingController);
    httpTestingController
      .expectOne("/rest/identity/loggedInUser")
      .flush(loggedInUser);
    await respondWithNotities(notities);
  }

  async function respondWithNotities(notities: GeneratedType<"RestNote">[]) {
    httpTestingController
      .expectOne("/rest/notities/zaken/fakeZaakUuid")
      .flush(notities);
    await sleep();
    await sleep();
    fixture.detectChanges();
  }

  async function answerMutation(
    request: TestRequest,
    body: Parameters<TestRequest["flush"]>[0],
  ) {
    request.flush(body);
    await sleep();
    await sleep();
    fixture.detectChanges();
  }

  describe("the heading", () => {
    it("shows the number of notities behind the label", async () => {
      await setup({
        notities: [notitie({ id: 1 }), notitie({ id: 2 })],
        notitieRechten: { canLezen: true, canWijzigen: false },
      });

      expect(
        screen.getByRole("heading", { level: 3, name: "notities (2)" }),
      ).toBeInTheDocument();
    });

    it("shows only the label for a zaak without notities", async () => {
      await setup({ notitieRechten: { canLezen: true, canWijzigen: false } });

      expect(
        screen.getByRole("heading", { level: 3, name: "notities" }),
      ).toBeInTheDocument();
    });
  });

  describe("closing the panel", () => {
    it("closes the side panel with the close button", async () => {
      await setup({ notitieRechten: { canLezen: true, canWijzigen: false } });

      const [closeButton] = screen.getAllByRole("button", {
        name: "actie.sluiten",
      });
      await userEvent.setup().click(closeButton);

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });

    it("closes the side panel with the button Sluiten at the bottom", async () => {
      await setup({ notitieRechten: { canLezen: true, canWijzigen: false } });

      const [, sluitenButton] = screen.getAllByRole("button", {
        name: "actie.sluiten",
      });
      await userEvent.setup().click(sluitenButton);

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });
  });

  describe("for a user who may change notities", () => {
    const notitieRechten = { canLezen: true, canWijzigen: true };

    it("shows the notities of the zaak", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      expect(screen.getByText("fakeTekst1")).toBeInTheDocument();
    });

    it("shows the notities of the new zaak when the zaakUuid changes", async () => {
      await setup({ notitieRechten });

      fixture.componentRef.setInput("zaakUuid", "fakeZaakUuid2");
      fixture.detectChanges();
      httpTestingController
        .expectOne("/rest/notities/zaken/fakeZaakUuid2")
        .flush([notitie({ zaakUUID: "fakeZaakUuid2", tekst: "fakeTekst2" })]);
      await sleep();
      await sleep();
      fixture.detectChanges();

      expect(screen.getByText("fakeTekst2")).toBeInTheDocument();
    });

    it("shows the most recently changed notitie first, after the notities without a change time", async () => {
      await setup({
        notities: [
          notitie({
            id: 1,
            tekst: "fakeOudsteTekst",
            tijdstipLaatsteWijziging: "2026-01-01T10:00:00Z",
          }),
          notitie({
            id: 2,
            tekst: "fakeNieuwsteTekst",
            tijdstipLaatsteWijziging: "2026-03-01T10:00:00Z",
          }),
          notitie({ id: 3, tekst: "fakeTekstZonderTijdstip" }),
        ],
        notitieRechten,
      });

      expect(
        screen
          .getAllByText(/^fake.*Tekst/)
          .map((element) => element.textContent?.trim()),
      ).toEqual([
        "fakeTekstZonderTijdstip",
        "fakeNieuwsteTekst",
        "fakeOudsteTekst",
      ]);
    });

    it("offers to edit and to delete a notitie the user may edit", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      expect(
        screen.getByRole("button", { name: "actie.bewerken" }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "actie.verwijderen" }),
      ).toBeInTheDocument();
    });

    it("offers neither edit nor delete for a notitie the user may not edit", async () => {
      await setup({
        notities: [notitie({ isBewerkenToegestaan: false })],
        notitieRechten,
      });

      expect(
        screen.queryByRole("button", { name: "actie.bewerken" }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "actie.verwijderen" }),
      ).not.toBeInTheDocument();
    });

    describe("adding a notitie", () => {
      let scrollIntoView: Element["scrollIntoView"] | undefined;

      beforeEach(() => {
        scrollIntoView = Element.prototype.scrollIntoView;
        Element.prototype.scrollIntoView = jest.fn();
      });

      afterEach(() => {
        Element.prototype.scrollIntoView =
          scrollIntoView as Element["scrollIntoView"];
      });

      it("disables the send button while the field is empty", async () => {
        await setup({ notitieRechten });

        expect(
          screen.getByRole("button", { name: "actie.opslaan" }),
        ).toBeDisabled();
      });

      it("adds the notitie to the zaak in the name of the current user, shows it at the top and empties the field", async () => {
        const user = userEvent.setup();
        await setup({
          notities: [
            notitie({
              id: 1,
              tekst: "fakeBestaandeTekst",
              tijdstipLaatsteWijziging: "2026-01-01T10:00:00Z",
            }),
          ],
          notitieRechten,
        });

        await user.type(
          screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
          "fakeNieuweTekst",
        );
        fixture.detectChanges();
        await user.click(screen.getByRole("button", { name: "actie.opslaan" }));
        await sleep();

        const request = httpTestingController.expectOne("/rest/notities");
        expect(request.request.method).toBe("POST");
        expect(request.request.body).toEqual({
          zaakUUID: "fakeZaakUuid",
          tekst: "fakeNieuweTekst",
          gebruikersnaamMedewerker: "currentUser",
        });
        await answerMutation(request, {
          ...request.request.body,
          id: 2,
          tijdstipLaatsteWijziging: "2026-02-01T10:00:00Z",
        });

        expect(
          screen
            .getAllByText(/^fake.*Tekst$/)
            .map((element) => element.textContent?.trim()),
        ).toEqual(["fakeNieuweTekst", "fakeBestaandeTekst"]);
        expect(
          screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
        ).toHaveValue("");
        expect(
          screen.getByRole("heading", { level: 3, name: "notities (2)" }),
        ).toBeInTheDocument();
      });
    });

    describe("editing a notitie", () => {
      async function startEditing() {
        const user = userEvent.setup();
        await setup({ notities: [notitie()], notitieRechten });
        await user.click(
          screen.getByRole("button", { name: "actie.bewerken" }),
        );
        fixture.detectChanges();
        return user;
      }

      async function editNotitie(tekst: string) {
        const user = await startEditing();
        const textbox = screen.getByRole("textbox", {
          name: "actie.notitie.wijzigen",
        });
        await user.clear(textbox);
        await user.type(textbox, tekst);
        fixture.detectChanges();
        const [opslaanButton] = screen.getAllByRole("button", {
          name: "actie.opslaan",
        });
        await user.click(opslaanButton);
        await sleep();
      }

      it("starts with the current text of the notitie and disables adding a notitie meanwhile", async () => {
        await startEditing();

        expect(
          screen.getByRole("textbox", { name: "actie.notitie.wijzigen" }),
        ).toHaveValue("fakeTekst1");
        expect(
          screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
        ).toBeDisabled();
      });

      it("saves the new text in the name of the current user and shows it", async () => {
        await editNotitie("fakeGewijzigdeTekst");

        const request = httpTestingController.expectOne("/rest/notities");
        expect(request.request.method).toBe("PATCH");
        expect(request.request.body).toEqual({
          ...notitie(),
          tekst: "fakeGewijzigdeTekst",
          gebruikersnaamMedewerker: "currentUser",
        });
        await answerMutation(request, request.request.body);

        expect(screen.getByText("fakeGewijzigdeTekst")).toBeInTheDocument();
        expect(
          screen.queryByRole("textbox", { name: "actie.notitie.wijzigen" }),
        ).not.toBeInTheDocument();
      });

      it("disables saving an empty text", async () => {
        const user = await startEditing();

        await user.clear(
          screen.getByRole("textbox", { name: "actie.notitie.wijzigen" }),
        );
        fixture.detectChanges();

        const [opslaanButton] = screen.getAllByRole("button", {
          name: "actie.opslaan",
        });
        expect(opslaanButton).toBeDisabled();
      });

      it("keeps the old text and leaves edit mode on cancel", async () => {
        const user = await startEditing();
        await user.type(
          screen.getByRole("textbox", { name: "actie.notitie.wijzigen" }),
          "fakeNietOpgeslagen",
        );

        await user.click(
          screen.getByRole("button", { name: "actie.annuleren" }),
        );
        fixture.detectChanges();

        expect(screen.getByText("fakeTekst1")).toBeInTheDocument();
        expect(
          screen.queryByRole("textbox", { name: "actie.notitie.wijzigen" }),
        ).not.toBeInTheDocument();
        httpTestingController.expectNone("/rest/notities");
      });
    });

    it("deletes the selected notitie, removes it from the list and lowers the count", async () => {
      await setup({
        notities: [
          notitie({ id: 1, tekst: "een", isBewerkenToegestaan: false }),
          notitie({ id: 2, tekst: "twee" }),
        ],
        notitieRechten,
      });

      await userEvent
        .setup()
        .click(screen.getByRole("button", { name: "actie.verwijderen" }));
      await sleep();
      const request = httpTestingController.expectOne("/rest/notities/2");
      expect(request.request.method).toBe("DELETE");
      await answerMutation(request, null);

      expect(screen.getByText("een")).toBeInTheDocument();
      expect(screen.queryByText("twee")).not.toBeInTheDocument();
      expect(
        screen.getByRole("heading", { level: 3, name: "notities (1)" }),
      ).toBeInTheDocument();
    });
  });

  describe("for a user who may only read notities", () => {
    const notitieRechten = { canLezen: true, canWijzigen: false };

    it("shows the notities, without offering to add, edit or delete one", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      expect(screen.getByText("fakeTekst1")).toBeInTheDocument();
      expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "actie.bewerken" }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "actie.verwijderen" }),
      ).not.toBeInTheDocument();
    });

    it("offers to add a notitie once the user may change notities", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      fixture.componentRef.setInput("notitieRechten", {
        canLezen: true,
        canWijzigen: true,
      });
      fixture.detectChanges();

      expect(
        screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
      ).toBeInTheDocument();
    });
  });

  describe("without notitie rights", () => {
    it("does not offer to add a notitie", async () => {
      await setup();

      expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    });
  });
});
