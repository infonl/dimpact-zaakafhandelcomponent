/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../setupJest";
import { createMutationOptions, fromPartial } from "../../test-helpers";
import { ObjectType } from "../core/websocket/model/object-type";
import { Opcode } from "../core/websocket/model/opcode";
import { WebsocketListener } from "../core/websocket/model/websocket-listener";
import { WebsocketService } from "../core/websocket/websocket.service";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";

const currentUser: GeneratedType<"RestLoggedInUser"> = {
  id: "currentUser",
  naam: "test",
};

describe(NotitiesComponent.name, () => {
  let component: NotitiesComponent;
  let fixture: ComponentFixture<NotitiesComponent>;
  let notitieService: NotitieService;
  let websocketService: WebsocketService;
  let deleteNotitieMutation: ReturnType<
    typeof createMutationOptions<undefined, number>
  >;
  let updateNotitieMutation: ReturnType<
    typeof createMutationOptions<
      GeneratedType<"RestNote">,
      GeneratedType<"RestNote">
    >
  >;
  let createNotitieMutation: ReturnType<
    typeof createMutationOptions<
      GeneratedType<"RestNote">,
      GeneratedType<"RestNote">
    >
  >;
  let notitiesChangedCallback: () => void;

  const editableNotitie = fromPartial<GeneratedType<"RestNote">>({
    id: 1,
    tekst: "fakeTekst1",
    isBewerkenToegestaan: true,
  });

  async function openNotities() {
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Notities" }));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NotitiesComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [],
    }).compileComponents();

    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      currentUser,
    );

    websocketService = TestBed.inject(WebsocketService);
    jest
      .spyOn(websocketService, "addListener")
      .mockImplementation((_opcode, _objectType, _objectId, callback) => {
        notitiesChangedCallback = callback as () => void;
        return fromPartial<WebsocketListener>({});
      });
    jest.spyOn(websocketService, "removeListener").mockImplementation();

    notitieService = TestBed.inject(NotitieService);
    jest.spyOn(notitieService, "listNotities").mockReturnValue(of([]));
    updateNotitieMutation = createMutationOptions<
      GeneratedType<"RestNote">,
      GeneratedType<"RestNote">
    >(fromPartial<GeneratedType<"RestNote">>({}));
    updateNotitieMutation.mutationFn.mockImplementation(
      async (notitie) => notitie,
    );
    jest
      .spyOn(notitieService, "updateNotitie")
      .mockReturnValue(updateNotitieMutation as never);
    createNotitieMutation = createMutationOptions<
      GeneratedType<"RestNote">,
      GeneratedType<"RestNote">
    >(fromPartial<GeneratedType<"RestNote">>({}));
    createNotitieMutation.mutationFn.mockImplementation(async (notitie) => ({
      ...notitie,
      id: 2,
    }));
    jest
      .spyOn(notitieService, "createNotitie")
      .mockReturnValue(createNotitieMutation as never);
    deleteNotitieMutation = createMutationOptions<undefined, number>(undefined);
    jest
      .spyOn(notitieService, "deleteNotitie")
      .mockReturnValue(deleteNotitieMutation as never);

    fixture = TestBed.createComponent(NotitiesComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("zaakUuid", "fakeZaakUuid");
  });

  describe("for a user who may change notities", () => {
    beforeEach(() => {
      fixture.componentRef.setInput("notitieRechten", {
        canLezen: true,
        canWijzigen: true,
      });
      fixture.detectChanges();
    });

    it("should reload the notities when someone else adds one", () => {
      jest.mocked(notitieService.listNotities).mockClear();

      notitiesChangedCallback();

      expect(notitieService.listNotities).toHaveBeenCalledWith("fakeZaakUuid");
    });

    it("should load notities on init", () => {
      expect(notitieService.listNotities).toHaveBeenCalledWith("fakeZaakUuid");
    });

    it("should listen for changes to the notities of the zaak", () => {
      expect(websocketService.addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.ZAAK_NOTITIES,
        "fakeZaakUuid",
        expect.any(Function),
      );
    });

    it("should neither reload the notities nor listen to the new zaak when the zaakUuid changes", () => {
      fixture.componentRef.setInput("zaakUuid", "fakeZaakUuid2");
      fixture.detectChanges();

      expect(notitieService.listNotities).toHaveBeenCalledTimes(1);
      expect(websocketService.addListener).toHaveBeenCalledTimes(1);
    });

    it("should offer to add a notitie", async () => {
      await openNotities();

      expect(
        screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
      ).toBeInTheDocument();
    });

    it("should offer to edit and to delete a notitie the user may edit", async () => {
      jest
        .mocked(notitieService.listNotities)
        .mockReturnValue(of([editableNotitie]));
      notitiesChangedCallback();
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
        await openNotities();

        await user.type(
          screen.getByRole("textbox", { name: "actie.notitie.aanmaken" }),
          "fakeNieuweTekst",
        );
        fixture.detectChanges();
        await user.click(screen.getByRole("button", { name: "actie.opslaan" }));
        await sleep();
        fixture.detectChanges();

        expect(createNotitieMutation.mutationFn).toHaveBeenCalledWith(
          {
            zaakUUID: "fakeZaakUuid",
            tekst: "fakeNieuweTekst",
            gebruikersnaamMedewerker: "currentUser",
          },
          expect.anything(),
        );
        expect(screen.getByText("fakeNieuweTekst")).toBeInTheDocument();
      });
    });

    describe("editing a notitie", () => {
      async function editNotitie(tekst: string) {
        const user = userEvent.setup();
        jest
          .mocked(notitieService.listNotities)
          .mockReturnValue(of([{ ...editableNotitie }]));
        notitiesChangedCallback();
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
        fixture.detectChanges();
      }

      it("should save the new text in the name of the current user and show it", async () => {
        await editNotitie("fakeGewijzigdeTekst");

        expect(updateNotitieMutation.mutationFn).toHaveBeenCalledWith(
          {
            ...editableNotitie,
            tekst: "fakeGewijzigdeTekst",
            gebruikersnaamMedewerker: currentUser.id,
          },
          expect.anything(),
        );
        expect(screen.getByText("fakeGewijzigdeTekst")).toBeInTheDocument();
      });

      it("should not save an empty text", async () => {
        await editNotitie("");

        expect(updateNotitieMutation.mutationFn).not.toHaveBeenCalled();
      });
    });

    it("should delete the selected notitie and remove it from the list", async () => {
      component["notities"] = [
        { id: 1, tekst: "een" } as GeneratedType<"RestNote">,
        { id: 2, tekst: "twee" } as GeneratedType<"RestNote">,
      ];

      component["verwijderNotitie"](2);
      await sleep();
      fixture.detectChanges();

      expect(deleteNotitieMutation.mutationFn).toHaveBeenCalledWith(
        2,
        expect.anything(),
      );
      expect(component["notities"]).toEqual([
        { id: 1, tekst: "een" } as GeneratedType<"RestNote">,
      ]);
    });
  });

  describe("for a user who may only read notities", () => {
    beforeEach(() => {
      jest
        .mocked(notitieService.listNotities)
        .mockReturnValue(of([editableNotitie]));
      fixture.componentRef.setInput("notitieRechten", {
        canLezen: true,
        canWijzigen: false,
      });
      fixture.detectChanges();
    });

    it("should show the notities, without offering to add, edit or delete one", async () => {
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
      fixture.detectChanges();

      await openNotities();

      expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    });
  });
});
