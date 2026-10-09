/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { sleep } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
import { ObjectType } from "../core/websocket/model/object-type";
import { Opcode } from "../core/websocket/model/opcode";
import { WebsocketListener } from "../core/websocket/model/websocket-listener";
import { WebsocketService } from "../core/websocket/websocket.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";

const loggedInUser = fromPartial<GeneratedType<"RestLoggedInUser">>({
  id: "currentUser",
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
  let websocketService: WebsocketService;
  let notitiesChangedCallback: () => void;

  async function setup({
    notities = [],
    notitieRechten,
  }: {
    notities?: GeneratedType<"RestNote">[];
    notitieRechten?: GeneratedType<"RestNotitieRechten">;
  } = {}) {
    websocketService = fromPartial<WebsocketService>({
      addListener: jest.fn((_opcode, _objectType, _objectId, callback) => {
        notitiesChangedCallback = callback as () => void;
        return fromPartial<WebsocketListener>({});
      }),
      removeListener: jest.fn(),
    });

    ({ fixture } = await render(NotitiesComponent, {
      inputs: { zaakUuid: "fakeZaakUuid", notitieRechten },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [{ provide: WebsocketService, useValue: websocketService }],
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

  async function openNotities() {
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Notities" }));
    fixture.detectChanges();
  }

  describe("for a user who may change notities", () => {
    const notitieRechten = { canLezen: true, canWijzigen: true };

    it("should show the notities of the zaak", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      await openNotities();

      expect(screen.getByText("fakeTekst1")).toBeInTheDocument();
    });

    it("should listen for changes to the notities of the zaak", async () => {
      await setup({ notitieRechten });

      expect(websocketService.addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.ZAAK_NOTITIES,
        "fakeZaakUuid",
        expect.any(Function),
      );
    });

    it("should reload the notities when someone else adds one", async () => {
      await setup({ notitieRechten });

      notitiesChangedCallback();
      await respondWithNotities([notitie({ tekst: "fakeNieuweTekst" })]);
      await openNotities();

      expect(screen.getByText("fakeNieuweTekst")).toBeInTheDocument();
    });

    it("should show the notities of the new zaak and listen to it instead when the zaakUuid changes", async () => {
      await setup({ notitieRechten });

      fixture.componentRef.setInput("zaakUuid", "fakeZaakUuid2");
      fixture.detectChanges();
      httpTestingController
        .expectOne("/rest/notities/zaken/fakeZaakUuid2")
        .flush([notitie({ zaakUUID: "fakeZaakUuid2", tekst: "fakeTekst2" })]);
      await sleep();
      fixture.detectChanges();
      await openNotities();

      expect(screen.getByText("fakeTekst2")).toBeInTheDocument();
      expect(websocketService.removeListener).toHaveBeenCalledTimes(1);
      expect(websocketService.addListener).toHaveBeenLastCalledWith(
        Opcode.UPDATED,
        ObjectType.ZAAK_NOTITIES,
        "fakeZaakUuid2",
        expect.any(Function),
      );
    });

    it("should stop listening for changes to the notities when it is destroyed", async () => {
      await setup({ notitieRechten });

      fixture.destroy();

      expect(websocketService.removeListener).toHaveBeenCalledTimes(1);
    });

    it("should offer to add a notitie", async () => {
      await setup({ notitieRechten });

      await openNotities();

      expect(
        screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
      ).toBeInTheDocument();
    });

    it("should offer to edit and to delete a notitie the user may edit", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      await openNotities();

      expect(
        screen.getByRole("button", { name: "actie.bewerken" }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "actie.verwijderen" }),
      ).toBeInTheDocument();
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

      it("should add the notitie to the zaak, in the name of the current user", async () => {
        const user = userEvent.setup();
        await setup({ notitieRechten });
        await openNotities();

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
        await answerMutation(request, { ...request.request.body, id: 2 });

        expect(screen.getByText("fakeNieuweTekst")).toBeInTheDocument();
      });
    });

    describe("editing a notitie", () => {
      async function editNotitie(tekst: string) {
        const user = userEvent.setup();
        await setup({ notities: [notitie()], notitieRechten });
        await openNotities();

        await user.click(
          screen.getByRole("button", { name: "actie.bewerken" }),
        );
        fixture.detectChanges();
        const textbox = screen.getByRole("textbox", {
          name: "actie.notitie.wijzigen",
        });
        await user.clear(textbox);
        if (tekst) await user.type(textbox, tekst);
        fixture.detectChanges();
        const [opslaanButton] = screen.getAllByRole("button", {
          name: "actie.opslaan",
        });
        await user.click(opslaanButton);
        await sleep();
      }

      it("should save the new text in the name of the current user and show it", async () => {
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
      });

      it("should not save an empty text", async () => {
        await editNotitie("");

        httpTestingController.expectNone("/rest/notities");
      });
    });

    it("should delete the selected notitie and remove it from the list", async () => {
      await setup({
        notities: [
          notitie({ id: 1, tekst: "een", isBewerkenToegestaan: false }),
          notitie({ id: 2, tekst: "twee" }),
        ],
        notitieRechten,
      });
      await openNotities();

      await userEvent
        .setup()
        .click(screen.getByRole("button", { name: "actie.verwijderen" }));
      await sleep();
      const request = httpTestingController.expectOne("/rest/notities/2");
      expect(request.request.method).toBe("DELETE");
      await answerMutation(request, null);

      expect(screen.getByText("een")).toBeInTheDocument();
      expect(screen.queryByText("twee")).not.toBeInTheDocument();
    });
  });

  describe("for a user who may only read notities", () => {
    const notitieRechten = { canLezen: true, canWijzigen: false };

    it("should show the notities, without offering to add, edit or delete one", async () => {
      await setup({ notities: [notitie()], notitieRechten });

      await openNotities();

      expect(screen.getByText("fakeTekst1")).toBeInTheDocument();
      expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "actie.bewerken" }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "actie.verwijderen" }),
      ).not.toBeInTheDocument();
    });

    it("should offer to add a notitie once the user may change notities", async () => {
      await setup({ notities: [notitie()], notitieRechten });
      await openNotities();

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
    it("should not offer to add a notitie", async () => {
      await setup();

      await openNotities();

      expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    });
  });
});
