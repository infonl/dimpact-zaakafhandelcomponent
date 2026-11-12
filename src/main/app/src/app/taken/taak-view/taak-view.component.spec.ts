/*
 * SPDX-FileCopyrightText: 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl, FormGroup } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen, within } from "@testing-library/angular";
import { randomUUID } from "crypto";
import moment from "moment";
import { ReplaySubject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { FormioCustomEvent } from "../../formulieren/formio-wrapper/formio-wrapper.component";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { TakenService } from "../taken.service";
import { FormioSetupService } from "./formio/formio-setup-service";
import { TaakViewComponent } from "./taak-view.component";

describe(TaakViewComponent.name, () => {
  let fixture: ComponentFixture<TaakViewComponent>;
  let component: ComponentRef<TaakViewComponent>;

  let websocketService: WebsocketService;
  let taakFormulierenService: TaakFormulierenService;

  let mockActivatedRoute: {
    data: ReplaySubject<{ taak: GeneratedType<"RestTask"> }>;
  };

  const taak: GeneratedType<"RestTask"> = {
    id: "test-id",
    zaakUuid: "test-zaakUuid",
    behandelaar: undefined,
    groep: undefined,
    naam: "test-taak",
    fataledatum: moment().toISOString(),
    creatiedatumTijd: moment().toISOString(),
    formioFormulier: {},
    rechten: {
      canLezen: true,
      canToekennen: true,
      canWijzigen: true,
      canToevoegenDocument: true,
    },
    status: "TOEGEKEND",
    taakdata: {},
    formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    toelichting: undefined,
    toekenningsdatumTijd: moment().toISOString(),
    zaaktypeOmschrijving: "test-zaaktypeOmschrijving",
    zaakIdentificatie: "test-zaakIdentificatie",
  };

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "test-zaak-uuid",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "test-zaaktype-uuid",
      omschrijving: "Test Zaaktype",
      zaakafhandelparameters: {
        afrondenMail: "BESCHIKBAAR_AAN",
        smartDocuments: {},
      },
    }),
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "BSN",
      temporaryPersonId: randomUUID(),
    }),
    resultaat: null,
    besluiten: [],
  });

  const zaakWithSmartDocuments = (
    smartDocuments: GeneratedType<"RestZaaktypeConfiguration">["smartDocuments"] = {},
  ) =>
    fromPartial<GeneratedType<"RestZaak">>({
      ...zaak,
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        ...zaak.zaaktype,
        zaakafhandelparameters: fromPartial<
          GeneratedType<"RestZaaktypeConfiguration">
        >({ afrondenMail: "BESCHIKBAAR_AAN", smartDocuments }),
      }),
    });

  /**
   * The view reads the zaak of the taak first, and only then the reads that
   * depend on it. A read left open keeps the app from becoming stable.
   */
  async function answerReads(answer: GeneratedType<"RestZaak"> = zaak) {
    const httpTestingController = TestBed.inject(HttpTestingController);
    await sleep();
    httpTestingController
      .match(`/rest/zaken/zaak/${taak.zaakUuid}`)
      .forEach((request) => request.flush(answer));
    await sleep();
    httpTestingController
      .match("/rest/taken/test-id/historie")
      .forEach((request) => request.flush([]));
    httpTestingController
      .match("/rest/identity/loggedInUser")
      .forEach((request) =>
        request.flush(
          fromPartial<GeneratedType<"RestUser">>({ username: "user" }),
        ),
      );
    httpTestingController
      .match("/rest/informatieobjecten/informatieobjectenList")
      .forEach((request) => request.flush([]));
    await sleep();
    fixture.detectChanges();
  }

  beforeEach(() => {
    mockActivatedRoute = {
      data: new ReplaySubject<{ taak: GeneratedType<"RestTask"> }>(1),
    };

    TestBed.configureTestingModule({
      imports: [
        TaakViewComponent,
        MatSidenav,
        RouterModule.forRoot([]),
        NoopAnimationsModule,
      ],
      providers: [
        provideTranslateService(),
        WebsocketService,
        TakenService,
        ZakenService,
        TaakFormulierenService,
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaakViewComponent);
    component = fixture.componentRef;
    mockActivatedRoute.data.next({ taak });
    fixture.detectChanges();

    websocketService = TestBed.inject(WebsocketService);

    taakFormulierenService = TestBed.inject(TaakFormulierenService);
  });

  afterEach(() => answerReads());

  describe(TaakViewComponent.prototype.documentCreated.name, () => {
    it("should subscribe to UPDATED on ZAAK_INFORMATIEOBJECTEN", async () => {
      const addListener = jest.spyOn(websocketService, "addListener");

      component.instance.documentCreated();

      expect(addListener).toHaveBeenCalledTimes(1);
      expect(addListener).toHaveBeenCalledWith(
        "UPDATED",
        "ZAAK_INFORMATIEOBJECTEN",
        taak.zaakUuid,
        expect.any(Function),
      );
    });

    it("should reload the history when an UPDATED on ZAAK_INFORMATIEOBJECTEN is received", async () => {
      const httpTestingController = TestBed.inject(HttpTestingController);
      await sleep();
      httpTestingController.expectOne("/rest/taken/test-id/historie").flush([]);
      await sleep();

      component.instance.documentCreated();
      websocketService["onMessage"]({
        timestamp: 1,
        opcode: "UPDATED",
        objectType: "ZAAK_INFORMATIEOBJECTEN",
        objectId: new ScreenEventId(taak.zaakUuid),
      });
      await sleep();

      httpTestingController.expectOne("/rest/taken/test-id/historie").flush([]);
    });

    it("should read the task again when a screen event is received", async () => {
      const httpTestingController = TestBed.inject(HttpTestingController);

      websocketService["onMessage"]({
        timestamp: 1,
        opcode: "ANY",
        objectType: "TAAK",
        objectId: new ScreenEventId(taak.id!),
      });
      await sleep();

      httpTestingController.expectOne("/rest/taken/test-id").flush(taak);
    });
  });

  describe("opening a taak whose zaak is cached", () => {
    it("reads the zaak from the server again, so the taak never shows an outdated zaak", async () => {
      await answerReads();

      mockActivatedRoute.data.next({ taak });
      await sleep();

      TestBed.inject(HttpTestingController)
        .expectOne(`/rest/zaken/zaak/${taak.zaakUuid}`)
        .flush(zaak);
    });
  });

  describe("angular basic task form", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularHandleFormBuilder")
        .mockResolvedValue([]);
    });

    it("shows only the explanation field once the zaak of the taak is read", async () => {
      await answerReads();

      expect(await screen.findAllByRole("textbox")).toHaveLength(1);
    });
  });

  describe("angular task form with fields", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularHandleFormBuilder")
        .mockResolvedValue([{ type: "input", key: "question" }]);
    });

    it("shows the field of the form next to the explanation once the zaak of the taak is read", async () => {
      await answerReads();

      expect(await screen.findAllByRole("textbox")).toHaveLength(2);
    });
  });

  describe("document action buttons visibility for smartDocuments settings", () => {
    const smartDocumentVariants = [
      { enabledGlobally: true, enabledForZaaktype: true, canMaken: true },
      { enabledGlobally: true, enabledForZaaktype: false, canMaken: false },
      { enabledGlobally: true, enabledForZaaktype: null, canMaken: false },
      { enabledGlobally: true, canMaken: false },
      { enabledGlobally: false, enabledForZaaktype: true, canMaken: false },
      { enabledGlobally: false, enabledForZaaktype: false, canMaken: false },
      { enabledGlobally: false, enabledForZaaktype: null, canMaken: false },
      { enabledGlobally: false, canMaken: false },
      { enabledGlobally: null, enabledForZaaktype: true, canMaken: false },
      { enabledGlobally: null, enabledForZaaktype: false, canMaken: false },
      { enabledGlobally: null, enabledForZaaktype: null, canMaken: false },
      { enabledGlobally: null, canMaken: false },
      { enabledForZaaktype: true, canMaken: false },
      { enabledForZaaktype: false, canMaken: false },
      { enabledForZaaktype: null, canMaken: false },
      { canMaken: false },
    ];

    test.each(smartDocumentVariants)(
      "smartDocuments = %o",
      async ({ enabledGlobally, enabledForZaaktype, canMaken }) => {
        await answerReads(
          zaakWithSmartDocuments({
            isEnabledGlobally: enabledGlobally as boolean,
            isEnabledForZaaktype: enabledForZaaktype as boolean,
          }),
        );

        expect(
          await screen.findByRole("button", {
            name: /actie\.document\.toevoegen/,
          }),
        ).toBeInTheDocument();
        expect(
          screen.queryByRole("button", { name: /actie\.document\.maken/ }) !==
            null,
        ).toBe(canMaken);
      },
    );
  });

  describe(TaakViewComponent.prototype.onDocumentCreate.name, () => {
    let formioSetupService: FormioSetupService;

    const event: FormioCustomEvent = {
      type: "click",
      component: { key: "AM_SmartDocuments_Create" },
      data: {
        AM_SmartDocuments_Group: "group-id",
        AM_SmartDocuments_Template: "template-id",
      },
    };

    beforeEach(() => {
      formioSetupService = TestBed.inject(FormioSetupService);
    });

    it("should set smartDocumentsGroupId from extractSmartDocumentsGroupId", () => {
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsGroupId")
        .mockReturnValue("group-id");
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsTemplateId")
        .mockReturnValue("template-id");

      component.instance.onDocumentCreate(event);

      expect(
        (component.instance as unknown as Record<string, () => string>)[
          "smartDocumentsGroupId"
        ](),
      ).toBe("group-id");
    });

    it("should set smartDocumentsTemplateId from extractSmartDocumentsTemplateId", () => {
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsGroupId")
        .mockReturnValue("group-id");
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsTemplateId")
        .mockReturnValue("template-id");

      component.instance.onDocumentCreate(event);

      expect(
        (component.instance as unknown as Record<string, () => string>)[
          "smartDocumentsTemplateId"
        ](),
      ).toBe("template-id");
    });

    it("should not open the sidenav when no template is selected", () => {
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsGroupId")
        .mockReturnValue(undefined);
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsTemplateId")
        .mockReturnValue(undefined);
      const openSpy = jest.spyOn(component.instance.actionsSidenav(), "open");

      component.instance.onDocumentCreate(event);

      expect(openSpy).not.toHaveBeenCalled();
    });

    it("should open the sidenav with 'actie.document.maken' when a template is selected", () => {
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsGroupId")
        .mockReturnValue("group-id");
      jest
        .spyOn(formioSetupService, "extractSmartDocumentsTemplateId")
        .mockReturnValue("template-id");
      const openSpy = jest.spyOn(component.instance.actionsSidenav(), "open");

      component.instance.onDocumentCreate(event);

      expect(
        (component.instance as unknown as Record<string, () => string>)[
          "activeSideAction"
        ](),
      ).toBe("actie.document.maken");
      expect(openSpy).toHaveBeenCalled();
    });
  });

  describe(TaakViewComponent.prototype.editTaak.name, () => {
    it("should open the actions sidenav with 'actie.taak.wijzigen'", () => {
      const openSpy = jest.spyOn(component.instance.actionsSidenav(), "open");

      component.instance.editTaak();

      expect(
        (component.instance as unknown as Record<string, () => string>)[
          "activeSideAction"
        ](),
      ).toBe("actie.taak.wijzigen");
      expect(openSpy).toHaveBeenCalled();
    });
  });

  describe("inactive group indicator", () => {
    it("should show '(inactief)' label when groep is inactive", () => {
      mockActivatedRoute.data.next({
        taak: {
          ...taak,
          groep: { id: "fakeGroupId", naam: "fakeGroupNaam", isActive: false },
        },
      });
      fixture.detectChanges();

      expect(
        within(fixture.nativeElement as HTMLElement).getByText("(inactief)"),
      ).toBeInTheDocument();
    });

    it("should not show '(inactief)' label when groep is active", () => {
      mockActivatedRoute.data.next({
        taak: {
          ...taak,
          groep: { id: "fakeGroupId", naam: "fakeGroupNaam", isActive: true },
        },
      });
      fixture.detectChanges();

      expect(
        within(fixture.nativeElement as HTMLElement).queryByText("(inactief)"),
      ).toBeNull();
    });

    it("should not crash when groep is undefined", () => {
      mockActivatedRoute.data.next({ taak: { ...taak, groep: undefined } });
      fixture.detectChanges();

      expect(
        within(fixture.nativeElement as HTMLElement).queryByText("(inactief)"),
      ).toBeNull();
    });
  });
  describe(TaakViewComponent.prototype.onFormioFormSubmit.name, () => {
    let httpTestingController: HttpTestingController;

    const flushMutationQueue = async () => {
      for (let tick = 0; tick < 3; tick++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
      }
    };

    beforeEach(() => {
      httpTestingController = TestBed.inject(HttpTestingController);
      jest
        .spyOn(taakFormulierenService, "getAngularTaskForm")
        .mockReturnValue(fromPartial({ onTaskCompleted: jest.fn() }));
    });

    it("should report a failure when saving fails", async () => {
      component.instance.onFormioFormSubmit({ data: {}, state: "draft" });
      await flushMutationQueue();

      httpTestingController
        .expectOne("/rest/taken/taakdata")
        .flush("fakeServerError", {
          status: 500,
          statusText: "Internal Server Error",
        });
      await flushMutationQueue();

      expect(component.instance["hasFailed"]()).toBe(true);
    });

    it("should not report a failure when a successful complete follows a failed save", async () => {
      component.instance.onFormioFormSubmit({ data: {}, state: "draft" });
      await flushMutationQueue();
      httpTestingController
        .expectOne("/rest/taken/taakdata")
        .flush("fakeServerError", {
          status: 500,
          statusText: "Internal Server Error",
        });
      await flushMutationQueue();

      component.instance.onFormioFormSubmit({ data: {}, state: "submitted" });
      await flushMutationQueue();
      httpTestingController.expectOne("/rest/taken/complete").flush(taak);
      await flushMutationQueue();

      expect(component.instance["hasFailed"]()).toBe(false);
    });

    it("saves the Form.io values as taakdata, without the submit and save buttons", async () => {
      component.instance.onFormioFormSubmit({
        data: { vraag: "antwoord", submit: "true", save: "true" },
        state: "draft",
      });
      await flushMutationQueue();

      const request = httpTestingController.expectOne("/rest/taken/taakdata");
      expect(request.request.body.taakdata).toEqual({ vraag: "antwoord" });
      request.flush(taak);
    });
  });

  describe(TaakViewComponent.prototype.onHardCodedFormSubmit.name, () => {
    let httpTestingController: HttpTestingController;
    let onTaskCompleted: jest.Mock;

    const formGroup = () =>
      new FormGroup({
        vraag: new FormControl("antwoord"),
        toelichting: new FormControl("uitleg"),
      });

    beforeEach(() => {
      httpTestingController = TestBed.inject(HttpTestingController);
      onTaskCompleted = jest.fn();
      jest
        .spyOn(taakFormulierenService, "getAngularTaskForm")
        .mockReturnValue(fromPartial({ onTaskCompleted }));
    });

    it("saves the form values without completing the taak when the form is saved part-way", async () => {
      component.instance.onHardCodedFormSubmit(formGroup(), true);
      await sleep();

      const request = httpTestingController.expectOne("/rest/taken/taakdata");
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          taakdata: expect.objectContaining({ vraag: "antwoord" }),
          toelichting: "uitleg",
        }),
      );
      request.flush(taak);
      await sleep();

      httpTestingController.expectNone("/rest/taken/complete");
      expect(onTaskCompleted).not.toHaveBeenCalled();
    });

    it("completes the taak with the form values and hands the completed taak to its form", async () => {
      component.instance.onHardCodedFormSubmit(formGroup());
      await sleep();

      const request = httpTestingController.expectOne("/rest/taken/complete");
      expect(request.request.method).toBe("PATCH");
      expect(request.request.body.taakdata).toEqual(
        expect.objectContaining({ vraag: "antwoord" }),
      );
      request.flush({ ...taak, status: "AFGEROND" });
      await sleep();

      expect(onTaskCompleted).toHaveBeenCalledWith(
        expect.objectContaining({ status: "AFGEROND" }),
        expect.anything(),
        expect.anything(),
      );
    });
  });

  describe(TaakViewComponent.prototype.updateTaakdocumenten.name, () => {
    const document = fromPartial<
      GeneratedType<"RestEnkelvoudigInformatieobject">
    >({ uuid: "fakeDocumentUuid", titel: "fakeDocument" });

    it("links a new document to the taak and checks it as an attachment", () => {
      const existingAttachment = fromPartial<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >({ uuid: "fakeExistingUuid" });
      component.instance["form"].addControl(
        "bijlagen",
        new FormControl([existingAttachment]),
      );
      component.instance["formFields"].set([
        fromPartial({ type: "documents", key: "bijlagen", options: [] }),
      ]);

      component.instance.updateTaakdocumenten(document);

      expect(component.instance["taak"]()?.taakdocumenten).toContain(
        "fakeDocumentUuid",
      );
      expect(component.instance["form"].get("bijlagen")?.value).toEqual([
        existingAttachment,
        document,
      ]);
      expect(component.instance["formFields"]()[0]).toEqual(
        expect.objectContaining({ options: [existingAttachment, document] }),
      );
    });

    it("only links the document to the taak when its form has no attachments", () => {
      component.instance.updateTaakdocumenten(document);

      expect(component.instance["taak"]()?.taakdocumenten).toContain(
        "fakeDocumentUuid",
      );
      expect(component.instance["form"].get("bijlagen")).toBeNull();
    });
  });
});
