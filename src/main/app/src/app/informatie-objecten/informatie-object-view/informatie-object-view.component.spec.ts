/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { Location } from "@angular/common";
import { HttpTestingController } from "@angular/common/http/testing";
import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { notifyManager } from "@tanstack/query-core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of, ReplaySubject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { Breadcrumb } from "../../core/breadcrumbs/route-title";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { ConfirmDialogData } from "../../shared/confirm-dialog/confirm-dialog.component";
import { RedenDialogData } from "../../shared/dialog/reden-dialog-form/reden-dialog-form.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { InformatieObjectEditComponent } from "../informatie-object-edit/informatie-object-edit.component";
import { FileFormat } from "../model/file-format";
import { InformatieObjectViewComponent } from "./informatie-object-view.component";

@Component({
  selector: "zac-informatie-object-edit",
  template: "",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
})
class InformatieObjectEditStubComponent {
  readonly infoObject =
    input<GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">>();
  readonly sideNav = input.required<MatDrawer>();
  readonly zaakUuid = input.required<string>();
}

describe(InformatieObjectViewComponent.name, () => {
  const user = userEvent.setup();

  let component: InformatieObjectViewComponent;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let addListener: jest.SpyInstance<
    ReturnType<WebsocketService["addListener"]>,
    Parameters<WebsocketService["addListener"]>
  >;

  const mockActivatedRoute = {
    data: new ReplaySubject<{
      informatieObject: GeneratedType<"RestEnkelvoudigInformatieobject">;
    }>(1),
  };

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-001",
    identificatie: "test",
    indicaties: [],
    omschrijving: "test omschrijving",
    vertrouwelijkheidaanduiding: "OPENBAAR",
    rechten: fromPartial<GeneratedType<"RestZaakRechten">>({}),
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "zaaktype-001",
    }),
  });

  const zaakInformatieobject = fromPartial<
    GeneratedType<"RestZaakInformatieobject">
  >({
    zaakIdentificatie: zaak.identificatie,
  });

  const enkelvoudigInformatieobject = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({
    uuid: "enkelvoudig-informatieobject-001",
    informatieobjectTypeUUID: "test-uuid",
    indicaties: [],
    titel: "test informatieobject",
    vertrouwelijkheidaanduiding: "OPENBAAR",
    rechten: fromPartial<GeneratedType<"RestDocumentRechten">>({}),
    formaat: FileFormat.DOCX,
  });

  const documentUrl = `/rest/informatieobjecten/informatieobject/${enkelvoudigInformatieobject.uuid}`;

  type Reads = {
    laatsteVersie?: GeneratedType<"RestEnkelvoudigInformatieobject">;
    zaakInformatieobjecten?: GeneratedType<"RestZaakInformatieobject">[];
    historie?: GeneratedType<"HistoryLine">[];
  };

  function withRechten(
    rechten: Partial<GeneratedType<"RestDocumentRechten">>,
    document: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">> = {},
  ) {
    return fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
      ...enkelvoudigInformatieobject,
      ...document,
      rechten: fromPartial<GeneratedType<"RestDocumentRechten">>(rechten),
    });
  }

  async function answerReads({
    laatsteVersie = enkelvoudigInformatieobject,
    zaakInformatieobjecten = [zaakInformatieobject],
    historie = [],
  }: Reads = {}) {
    await sleep();
    httpTestingController.expectOne(documentUrl).flush(laatsteVersie);
    httpTestingController
      .expectOne(`${documentUrl}/zaakinformatieobjecten`)
      .flush(zaakInformatieobjecten);
    httpTestingController.expectOne(`${documentUrl}/historie`).flush(historie);
    await sleep();
    TestBed.tick();
    fixture.detectChanges();
  }

  async function openView(
    reads: Reads = {},
    routeInformatieObject = enkelvoudigInformatieobject,
  ) {
    fixture = TestBed.createComponent(InformatieObjectViewComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    mockActivatedRoute.data.next({ informatieObject: routeInformatieObject });
    fixture.detectChanges();

    await answerReads(reads);
  }

  function sendDocumentUpdate() {
    const [, , , callback] = addListener.mock.lastCall!;
    callback(fromPartial<ScreenEvent>({}));
  }

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());
    await TestBed.configureTestingModule({
      imports: [InformatieObjectViewComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
      ],
    })
      .overrideComponent(InformatieObjectViewComponent, {
        remove: { imports: [InformatieObjectEditComponent] },
        add: { imports: [InformatieObjectEditStubComponent] },
      })
      .compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    addListener = jest.spyOn(TestBed.inject(WebsocketService), "addListener");
    jest
      .spyOn(TestBed.inject(ZakenService), "readZaakByID")
      .mockReturnValue(of(zaak));
  });

  afterEach(() => notifyManager.setScheduler((fn) => setTimeout(fn, 0)));

  it("resizes the content along with the menu, so that the document card does not end up behind a menu that renders after the page", async () => {
    await openView();

    expect(component.sideNavContainer().autosize).toBe(true);
  });

  describe("when the document changes on the server", () => {
    it("reads the document, its zaak koppelingen and its historie again, and shows the latest version", async () => {
      await openView();

      sendDocumentUpdate();
      await answerReads({
        laatsteVersie: { ...enkelvoudigInformatieobject, titel: "fakeTitel" },
        historie: [
          fromPartial<GeneratedType<"HistoryLine">>({ door: "fakeUserName" }),
        ],
      });

      expect(screen.getAllByText("fakeTitel").length).toBeGreaterThan(0);
      await user.click(screen.getByRole("tab", { name: /historie/ }));
      fixture.detectChanges();
      expect(await screen.findByText("fakeUserName")).toBeInTheDocument();
    });

    it("shows the latest version also when an older version was opened", async () => {
      await openView(
        { laatsteVersie: { ...enkelvoudigInformatieobject, versie: 2 } },
        { ...enkelvoudigInformatieobject, versie: 1, titel: "fakeOudeTitel" },
      );
      expect(screen.getAllByText("fakeOudeTitel").length).toBeGreaterThan(0);

      sendDocumentUpdate();
      await answerReads({
        laatsteVersie: {
          ...enkelvoudigInformatieobject,
          versie: 2,
          titel: "fakeNieuweTitel",
        },
      });

      expect(screen.queryByText("fakeOudeTitel")).not.toBeInTheDocument();
      expect(screen.getAllByText("fakeNieuweTitel").length).toBeGreaterThan(0);
    });
  });

  describe("actie.nieuwe.versie.toevoegen", () => {
    it("should not have a button when the user does not have the right to add a new version", async () => {
      await openView({
        laatsteVersie: withRechten({ canToevoegenNieuweVersie: false }),
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.nieuwe.versie.toevoegen" }),
      );

      expect(button).toBeNull();
    });

    it("should open the sidebar with the current version once it is read", async () => {
      await openView({
        laatsteVersie: withRechten({ canToevoegenNieuweVersie: true }),
      });

      await user.click(
        screen.getByRole("button", { name: "actie.nieuwe.versie.toevoegen" }),
      );
      await sleep();
      httpTestingController.expectOne(`${documentUrl}/huidigeversie`).flush(
        fromPartial<
          GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">
        >({
          uuid: enkelvoudigInformatieobject.uuid,
          titel: enkelvoudigInformatieobject.titel,
        }),
      );
      await sleep();

      expect(component.actionsSidenav().opened).toBe(true);
    });
  });

  describe("actie.converteren", () => {
    it("should have a button when the document is of format DOCX and the user has the right to convert a document", async () => {
      await openView({
        laatsteVersie: withRechten({ canConverteren: true }),
      });

      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeTruthy();
    });

    it("should not have a button when the document is of format DOCX and the user does not have the right to convert a document", async () => {
      await openView({
        laatsteVersie: withRechten({ canConverteren: false }),
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeNull();
    });

    it("should not have a button when the document is of format TEXT and the user has the right to convert a document", async () => {
      await openView(
        { laatsteVersie: withRechten({ canConverteren: true }) },
        { ...enkelvoudigInformatieobject, formaat: FileFormat.TEXT },
      );

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.converteren" }),
      );

      expect(button).toBeNull();
    });

    it("converts the document within its zaak to PDF", async () => {
      await openView({
        laatsteVersie: withRechten({ canConverteren: true }),
      });

      await user.click(
        screen.getByRole("button", { name: "actie.converteren" }),
      );
      await sleep();

      const request = httpTestingController.expectOne(
        `${documentUrl}/convert?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
    });
  });

  describe("actie.unlock", () => {
    it("should not have a button when the document is not locked", async () => {
      await openView({
        laatsteVersie: withRechten(
          { canOntgrendelen: true },
          { gelockedDoor: undefined },
        ),
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );

      expect(button).toBeNull();
    });

    it("should not have a button when the document is locked but the user does not have the right to unlock", async () => {
      await openView({
        laatsteVersie: withRechten(
          { canOntgrendelen: false },
          { gelockedDoor: { username: "user-001", naam: "Test User" } },
        ),
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "actie.unlock" }),
      );

      expect(button).toBeNull();
    });

    it("unlocks the document within its zaak when clicked and a zaak is present", async () => {
      await openView({
        laatsteVersie: withRechten(
          { canOntgrendelen: true },
          { gelockedDoor: { username: "user-001", naam: "Test User" } },
        ),
      });

      await user.click(screen.getByRole("button", { name: "actie.unlock" }));
      await sleep();

      const request = httpTestingController.expectOne(
        `${documentUrl}/unlock?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
      await answerReads();
    });

    it("unlocks the document without a zaak when clicked and no zaak is present", async () => {
      await openView({
        laatsteVersie: withRechten(
          { canOntgrendelen: true },
          { gelockedDoor: { username: "user-001", naam: "Test User" } },
        ),
        zaakInformatieobjecten: [],
      });

      await user.click(screen.getByRole("button", { name: "actie.unlock" }));
      await sleep();

      const request = httpTestingController.expectOne(({ url }) =>
        url.startsWith(`${documentUrl}/unlock`),
      );
      expect(request.request.url).not.toContain("zaak=");
      request.flush(null);
      await answerReads({ zaakInformatieobjecten: [] });
    });

    it("reads the document again once it is unlocked, so that the lock button replaces the unlock button", async () => {
      await openView({
        laatsteVersie: withRechten(
          { canOntgrendelen: true, canVergrendelen: true },
          { gelockedDoor: { username: "user-001", naam: "Test User" } },
        ),
      });

      await user.click(screen.getByRole("button", { name: "actie.unlock" }));
      await sleep();
      httpTestingController
        .expectOne(`${documentUrl}/unlock?zaak=${zaak.uuid}`)
        .flush(null);
      await answerReads({
        laatsteVersie: withRechten(
          { canOntgrendelen: true, canVergrendelen: true },
          { gelockedDoor: undefined },
        ),
      });

      expect(
        screen.queryByRole("button", { name: "actie.unlock" }),
      ).not.toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "actie.lock" }),
      ).toBeInTheDocument();
    });
  });

  describe("actie.lock", () => {
    beforeEach(async () => {
      await openView({
        laatsteVersie: withRechten(
          { canVergrendelen: true, canOntgrendelen: true },
          { gelockedDoor: undefined },
        ),
      });
    });

    it("locks the document within its zaak and keeps the button disabled until the updated document replaces it", async () => {
      await user.click(screen.getByRole("button", { name: "actie.lock" }));
      await sleep();

      const request = httpTestingController.expectOne(
        `${documentUrl}/lock?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
      fixture.detectChanges();

      expect(screen.getByRole("button", { name: "actie.lock" })).toBeDisabled();

      await answerReads({
        laatsteVersie: withRechten(
          { canVergrendelen: true, canOntgrendelen: true },
          { gelockedDoor: { username: "user-001", naam: "Test User" } },
        ),
      });

      expect(
        screen.queryByRole("button", { name: "actie.lock" }),
      ).not.toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "actie.unlock" }),
      ).toBeInTheDocument();
    });

    it("enables the button again when locking fails", async () => {
      jest
        .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
        .mockReturnValue(of());
      await user.click(screen.getByRole("button", { name: "actie.lock" }));
      await sleep();

      httpTestingController
        .expectOne(`${documentUrl}/lock?zaak=${zaak.uuid}`)
        .flush(null, { status: 500, statusText: "Server Error" });

      await sleep();
      fixture.detectChanges();

      expect(screen.getByRole("button", { name: "actie.lock" })).toBeEnabled();
      httpTestingController.expectNone(documentUrl);
    });
  });

  describe("actie.ondertekenen", () => {
    let dialog: MatDialog;

    beforeEach(async () => {
      dialog = TestBed.inject(MatDialog);
      jest
        .spyOn(dialog, "open")
        .mockReturnValue(fromPartial<MatDialogRef<unknown>>({}));
      await openView({
        laatsteVersie: withRechten({ canOndertekenen: true }),
      });
    });

    it("does not sign the document while the confirmation dialog is still open", async () => {
      await user.click(
        screen.getByRole("button", { name: "actie.ondertekenen" }),
      );
      await sleep();

      expect(dialog.open).toHaveBeenCalled();
      httpTestingController.expectNone(
        `${documentUrl}/onderteken?zaak=${zaak.uuid}`,
      );
    });

    it("signs the document within its zaak once confirmed, so that the dialog can close", async () => {
      await user.click(
        screen.getByRole("button", { name: "actie.ondertekenen" }),
      );
      const { observable } = jest.mocked(dialog.open).mock.calls.at(-1)![1]!
        .data as ConfirmDialogData;
      const onSigned = jest.fn();
      observable!.subscribe(onSigned);
      await sleep();

      const request = httpTestingController.expectOne(
        `${documentUrl}/onderteken?zaak=${zaak.uuid}`,
      );
      expect(request.request.method).toBe("POST");
      request.flush(null);
      await sleep();

      expect(onSigned).toHaveBeenCalled();
    });
  });

  describe("actie.verwijderen", () => {
    let dialog: MatDialog;

    beforeEach(async () => {
      dialog = TestBed.inject(MatDialog);
      jest
        .spyOn(dialog, "open")
        .mockReturnValue(
          fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
        );
      await openView();
    });

    describe("a document without a zaak", () => {
      it("does not delete it while the confirmation dialog is still open", () => {
        component.zaak = undefined;

        component["openDocumentVerwijderenDialog"]();

        httpTestingController.expectNone(documentUrl);
      });
    });

    describe("a document belonging to a zaak", () => {
      it("reports a failing delete through the error handler", async () => {
        const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
        const foutAfhandelen = jest
          .spyOn(foutAfhandelingService, "foutAfhandelen")
          .mockReturnValue(of());
        component.zaak = zaak;

        component["openDocumentVerwijderenDialog"]();
        const { callback } = jest.mocked(dialog.open).mock.calls.at(-1)![1]!
          .data as RedenDialogData;
        callback!("fakeReden").subscribe({ error: () => undefined });
        await new Promise(requestAnimationFrame);
        httpTestingController
          .expectOne(documentUrl)
          .flush(null, { status: 500, statusText: "Server Error" });
        await new Promise(requestAnimationFrame);

        expect(foutAfhandelen).toHaveBeenCalled();
      });
    });

    describe("once the document is deleted", () => {
      let router: Router;

      beforeEach(() => {
        jest
          .mocked(dialog.open)
          .mockReturnValue(
            fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(true) }),
          );
        router = TestBed.inject(Router);
        jest.spyOn(router, "navigateByUrl").mockResolvedValue(true);
      });

      function givenNavigationTrail(navigationTrail: Breadcrumb[]) {
        TestBed.inject(Location).replaceState(
          `/informatie-objecten/${enkelvoudigInformatieobject.uuid}`,
          "",
          { navigationTrail },
        );
      }

      it("returns to the page the document was opened from", () => {
        givenNavigationTrail([
          { title: "fakeZaak", url: "/zaken/fakeZaak", key: "fakeKey" },
          { title: "fakeDocument", url: "/informatie-objecten/fakeUuid" },
        ]);
        component.zaak = zaak;

        component["openDocumentVerwijderenDialog"]();

        expect(router.navigateByUrl).toHaveBeenCalledWith("/zaken/fakeZaak");
      });

      it("goes to the zaak of the document when it was not opened from another page, so that a new tab does not land on a page the user may not see", () => {
        givenNavigationTrail([
          { title: "fakeDocumenten", url: "/informatie-objecten" },
          { title: "fakeDocument", url: "/informatie-objecten/fakeUuid" },
        ]);
        component.zaak = zaak;

        component["openDocumentVerwijderenDialog"]();

        expect(router.navigateByUrl).toHaveBeenCalledWith(
          `/zaken/${zaak.identificatie}`,
        );
      });

      it("goes to the ontkoppelde documenten when it was not opened from another page and has no zaak", () => {
        givenNavigationTrail([]);
        component.zaak = undefined;

        component["openDocumentVerwijderenDialog"]();

        expect(router.navigateByUrl).toHaveBeenCalledWith(
          "/documenten/ontkoppelde",
        );
      });
    });
  });
});
