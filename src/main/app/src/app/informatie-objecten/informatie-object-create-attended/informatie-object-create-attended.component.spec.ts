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
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectCreateAttendedComponent } from "./informatie-object-create-attended.component";

const CREATE_URL = "/rest/document-creation/create-document-attended";
const TEMPLATES_URL =
  "/rest/zaakafhandelparameters/fakeZaaktypeUuid/smartdocuments-templates-mapping";
const INFORMATIEOBJECTTYPES_URL =
  "/rest/informatieobjecten/informatieobjecttypes/fakeZaaktypeUuid";

const zaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "fakeZaakUuid",
  zaaktype: { uuid: "fakeZaaktypeUuid" },
});

const templateGroup = fromPartial<
  GeneratedType<"RestMappedSmartDocumentsTemplateGroup">
>({
  id: "fakeGroupId1",
  name: "Group One",
  templates: [
    {
      id: "fakeTemplateId1",
      name: "Template One",
      informatieObjectTypeUUID: "fakeInformatieobjectTypeUuid",
    },
    {
      id: "fakeTemplateId2",
      name: "Template Two",
      informatieObjectTypeUUID: null,
    },
  ],
  groups: null,
});

const singleTemplateGroup = fromPartial<
  GeneratedType<"RestMappedSmartDocumentsTemplateGroup">
>({
  id: "fakeGroupId2",
  name: "Group Two",
  templates: [{ id: "fakeTemplateId3", name: "Template Three" }],
  groups: null,
});

const loggedInUser = fromPartial<GeneratedType<"RestUser">>({
  id: "fakeUserId1",
  naam: "fakeUserName1",
});

const informatieobjecttype = fromPartial<
  GeneratedType<"RestInformatieobjecttype">
>({
  uuid: "fakeInformatieobjectTypeUuid",
  omschrijving: "Bijlage",
  vertrouwelijkheidaanduiding: "OPENBAAR",
});

describe(InformatieObjectCreateAttendedComponent.name, () => {
  let fixture: ComponentFixture<InformatieObjectCreateAttendedComponent>;
  let httpTestingController: HttpTestingController;
  let sideNav: MatDrawer;
  let documentCreated: jest.Mock;
  let foutAfhandelen: jest.SpyInstance;

  const user = userEvent.setup({ delay: null });

  jest.setTimeout(20_000);

  async function setup(
    inputs: {
      smartDocumentsGroupId?: string;
      smartDocumentsTemplateId?: string;
    } = {},
  ) {
    testQueryClient.setQueryData(["/rest/identity/loggedInUser"], loggedInUser);

    sideNav = fromPartial<MatDrawer>({
      close: jest.fn().mockResolvedValue(undefined),
    });
    documentCreated = jest.fn();

    const { fixture: renderedFixture } = await render(
      InformatieObjectCreateAttendedComponent,
      {
        inputs: { zaak, sideNav, ...inputs },
        imports: [NoopAnimationsModule, TranslateModule.forRoot()],
        providers: [
          provideRouter([]),
          provideHttpClient(withInterceptorsFromDi()),
          provideHttpClientTesting(),
          provideMomentDateAdapter(),
          provideTanStackQuery(testQueryClient),
          provideQueryClient(testQueryClient),
          VertrouwelijkaanduidingToTranslationKeyPipe,
        ],
      },
    );

    fixture = renderedFixture;
    fixture.componentInstance.document.subscribe(documentCreated);
    httpTestingController = TestBed.inject(HttpTestingController);
    foutAfhandelen = jest
      .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
      .mockReturnValue(EMPTY);

    await sleep();
    httpTestingController
      .expectOne(INFORMATIEOBJECTTYPES_URL)
      .flush([informatieobjecttype]);
    httpTestingController
      .expectOne(TEMPLATES_URL)
      .flush([templateGroup, singleTemplateGroup]);
    await sleep();
    fixture.detectChanges();
  }

  function field(label: string) {
    return screen.getByLabelText(label);
  }

  function submitButton() {
    return screen.getByRole("button", { name: "actie.toevoegen" });
  }

  async function choose(label: string, option: string) {
    await user.click(field(label));
    await user.click(screen.getByRole("option", { name: option }));
  }

  async function fillInValidForm() {
    await choose("sjabloonGroep", "Group One");
    await choose("sjabloon", "Template One");
    await user.type(field("titel"), "Aanvraag formulier");
  }

  it("announces what the drawer is for", async () => {
    await setup();

    expect(screen.getByText("actie.document.maken")).toBeVisible();
  });

  it("offers the template groups configured for the zaaktype", async () => {
    await setup();

    await user.click(field("sjabloonGroep"));

    expect(screen.getByRole("option", { name: "Group One" })).toBeVisible();
    expect(screen.getByRole("option", { name: "Group Two" })).toBeVisible();
  });

  it("offers the templates of the chosen template group", async () => {
    await setup();

    await choose("sjabloonGroep", "Group One");
    await user.click(field("sjabloon"));

    expect(screen.getByRole("option", { name: "Template One" })).toBeVisible();
    expect(screen.getByRole("option", { name: "Template Two" })).toBeVisible();
  });

  it("chooses the only template of a group without asking", async () => {
    await setup();

    await choose("sjabloonGroep", "Group Two");

    expect(field("sjabloon")).toHaveValue("Template Three");
    expect(field("sjabloon")).toBeDisabled();
  });

  it("locks the template group it was opened for", async () => {
    await setup({ smartDocumentsGroupId: "fakeGroupId1" });

    expect(field("sjabloonGroep")).toHaveValue("Group One");
    expect(field("sjabloonGroep")).toBeDisabled();
    expect(field("sjabloon")).toHaveValue("");
  });

  it("locks the template it was opened for", async () => {
    await setup({
      smartDocumentsGroupId: "fakeGroupId1",
      smartDocumentsTemplateId: "fakeTemplateId1",
    });

    expect(field("sjabloonGroep")).toHaveValue("Group One");
    expect(field("sjabloon")).toHaveValue("Template One");
    expect(field("sjabloon")).toBeDisabled();
  });

  it("fills in the informatieobjecttype and vertrouwelijkheid of the template", async () => {
    await setup();

    await choose("sjabloonGroep", "Group One");
    await choose("sjabloon", "Template One");

    expect(field("informatieobjectType")).toHaveValue("Bijlage");
    expect(field("vertrouwelijkheidaanduiding")).toHaveValue(
      "vertrouwelijkheidaanduiding.OPENBAAR",
    );
  });

  it("fills in the logged in user as the author", async () => {
    await setup();

    expect(field("auteur")).toHaveValue("fakeUserName1");
  });

  it("keeps the submit disabled until the form is filled in", async () => {
    await setup();

    expect(submitButton()).toBeDisabled();

    await fillInValidForm();

    expect(submitButton()).toBeEnabled();
  });

  it("creates the document and opens it for editing", async () => {
    const windowOpen = jest.spyOn(window, "open").mockReturnValue(null);
    await setup();
    await fillInValidForm();

    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(CREATE_URL);
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toMatchObject({
      author: "fakeUserName1",
      smartDocumentsTemplateGroupId: "fakeGroupId1",
      smartDocumentsTemplateId: "fakeTemplateId1",
      title: "Aanvraag formulier",
      zaakUuid: "fakeZaakUuid",
    });
    request.flush({ redirectURL: "https://example.com/doc", message: null });
    await sleep();

    expect(documentCreated).toHaveBeenCalled();
    expect(windowOpen).toHaveBeenCalledWith("https://example.com/doc");
  });

  it("reports the message when there is no document to open", async () => {
    await setup();
    await fillInValidForm();

    await user.click(submitButton());
    await sleep();
    httpTestingController.expectOne(CREATE_URL).flush({
      redirectURL: null,
      message: "Document created without redirect",
    });
    await sleep();
    fixture.detectChanges();

    expect(screen.getByText("Document created without redirect")).toBeVisible();
  });

  it("routes a failed creation through the error handler", async () => {
    await setup();
    await fillInValidForm();

    await user.click(submitButton());
    await sleep();
    httpTestingController
      .expectOne(CREATE_URL)
      .flush("boom", { status: 500, statusText: "Server Error" });
    await sleep();

    expect(foutAfhandelen).toHaveBeenCalled();
    expect(documentCreated).not.toHaveBeenCalled();
  });

  it("does not offer to create a second document while one is being created", async () => {
    await setup();
    await fillInValidForm();

    await user.click(submitButton());
    await sleep();
    fixture.detectChanges();

    expect(submitButton()).toBeDisabled();
    httpTestingController
      .expectOne(CREATE_URL)
      .flush({ redirectURL: null, message: "done" });
  });

  it("closes the drawer when the creation is cancelled", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalled();
  });
});
