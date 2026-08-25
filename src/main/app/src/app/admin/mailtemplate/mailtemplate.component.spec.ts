/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { MailtemplateComponent } from "./mailtemplate.component";

const bestaandTemplate = fromPartial<GeneratedType<"RestMailtemplate">>({
  id: 42,
  mailTemplateNaam: "Bestaand template",
  mail: "TAAK_ONTVANGSTBEVESTIGING",
  onderwerp: "Bestaand onderwerp",
  body: "Bestaand body",
  defaultMailtemplate: true,
});

// The rich-text editor makes rendering and typing slow enough to exceed the default timeout.
describe(MailtemplateComponent.name, () => {
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let router: Router;
  let httpTestingController: HttpTestingController;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;
  let detectChanges: () => void;

  const user = userEvent.setup({ delay: null });

  // jsdom has no layout, so the rich-text editor cannot measure its selection.
  const zeroRect = fromPartial<DOMRect>({
    top: 0,
    left: 0,
    bottom: 0,
    right: 0,
    width: 0,
    height: 0,
    x: 0,
    y: 0,
  });
  const originalGetClientRects = Range.prototype.getClientRects;
  const originalGetBoundingClientRect = Range.prototype.getBoundingClientRect;
  const originalElementFromPoint = document.elementFromPoint;

  beforeAll(() => {
    Range.prototype.getClientRects = () =>
      fromPartial<DOMRectList>({
        length: 1,
        item: () => zeroRect,
        0: zeroRect,
      });
    Range.prototype.getBoundingClientRect = () => zeroRect;
    document.elementFromPoint = () => null;
  });

  afterAll(() => {
    Range.prototype.getClientRects = originalGetClientRects;
    Range.prototype.getBoundingClientRect = originalGetBoundingClientRect;
    document.elementFromPoint = originalElementFromPoint;
  });

  function htmlEditor(label: string) {
    const field = screen.getByText(label).closest("div");
    return field!.querySelector<HTMLElement>("[contenteditable='true']")!;
  }

  function saveButton() {
    return screen.getByRole("button", { name: "actie.opslaan" });
  }

  async function setup(
    template?: GeneratedType<"RestMailtemplate">,
    variabelen: GeneratedType<"MailTemplateVariables">[] = [],
  ) {
    const rendered = await render(MailtemplateComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: ActivatedRoute,
          useValue: { data: of(template ? { template } : {}) },
        },
      ],
    });

    detectChanges = rendered.detectChanges;
    mailtemplateBeheerService = TestBed.inject(MailtemplateBeheerService);
    router = TestBed.inject(Router);
    httpTestingController = TestBed.inject(HttpTestingController);

    jest
      .spyOn(mailtemplateBeheerService, "ophalenVariabelenVoorMail")
      .mockReturnValue(of(variabelen));
    jest.spyOn(router, "navigate").mockResolvedValue(true);
  }

  async function editTemplateName(extra: string) {
    await user.type(
      screen.getByRole("textbox", { name: "MailTemplateNaam" }),
      extra,
    );
  }

  async function fillInNewTemplate() {
    await user.click(screen.getByRole("textbox", { name: "MailTemplateNaam" }));
    await user.paste("Nieuw template");
    await user.click(screen.getByRole("combobox"));
    await user.click(
      screen.getByRole("option", { name: "mail.TAAK_ONTVANGSTBEVESTIGING" }),
    );
    await user.click(htmlEditor("Onderwerp"));
    await user.paste("Onderwerp");
    await user.click(htmlEditor("Body"));
    await user.paste("Body tekst");
  }

  beforeEach(() => {
    utilServiceMock = { setTitle: jest.fn(), openSnackbar: jest.fn() };
  });

  it("sets the title", async () => {
    await setup();

    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.mailtemplate",
      undefined,
    );
  });

  it("only offers to save once every required field is filled in", async () => {
    await setup();

    expect(saveButton()).toBeDisabled();

    await fillInNewTemplate();

    expect(saveButton()).toBeEnabled();
  });

  it("shows the template that is being edited", async () => {
    await setup(bestaandTemplate);

    expect(
      screen.getByRole("textbox", { name: "MailTemplateNaam" }),
    ).toHaveValue("Bestaand template");
    expect(htmlEditor("Onderwerp")).toHaveTextContent("Bestaand onderwerp");
    expect(htmlEditor("Body")).toHaveTextContent("Bestaand body");
  });

  it("does not allow changing the mail type of an existing template", async () => {
    await setup(bestaandTemplate);

    expect(screen.getByRole("combobox")).toHaveAttribute(
      "aria-disabled",
      "true",
    );
  });

  it("returns to the overview when cancelling", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(router.navigate).toHaveBeenCalledWith(["/admin/mailtemplates"]);
  });

  it("offers the variables of the chosen mail type", async () => {
    await setup(undefined, ["GEMEENTE", "ZAAK_URL"]);

    await user.click(screen.getByRole("combobox"));
    await user.click(
      screen.getByRole("option", { name: "mail.TAAK_ONTVANGSTBEVESTIGING" }),
    );
    await user.click(screen.getAllByRole("button", { name: "variabelen" })[0]);

    expect(
      mailtemplateBeheerService.ophalenVariabelenVoorMail,
    ).toHaveBeenCalledWith("TAAK_ONTVANGSTBEVESTIGING");
    expect(
      screen.getByRole("menuitem", {
        name: "GEMEENTE: mailtemplate.variabele.GEMEENTE",
      }),
    ).toBeVisible();
  });

  it("posts a new template and returns to the overview", async () => {
    await setup();
    await fillInNewTemplate();

    await user.click(saveButton());
    await sleep();

    const request = httpTestingController.expectOne(
      "/rest/beheer/mailtemplates",
    );
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      mail: "TAAK_ONTVANGSTBEVESTIGING",
      mailTemplateNaam: "Nieuw template",
      onderwerp: "Onderwerp",
      body: expect.stringContaining("Body tekst"),
      defaultMailtemplate: false,
    });

    request.flush({});
    await sleep();

    expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
      "msg.mailtemplate.opgeslagen",
    );
    expect(router.navigate).toHaveBeenCalledWith(["/admin/mailtemplates"]);
  });

  it("does not offer to save again while a save is in progress", async () => {
    await setup();
    await fillInNewTemplate();

    await user.click(saveButton());
    await sleep();
    detectChanges();

    expect(saveButton()).toBeDisabled();

    httpTestingController.expectOne("/rest/beheer/mailtemplates").flush({});
  });

  it("puts to the template id when saving an existing template", async () => {
    await setup(bestaandTemplate);
    await editTemplateName(" gewijzigd");

    await user.click(saveButton());
    await sleep();

    const request = httpTestingController.expectOne(
      "/rest/beheer/mailtemplates/42",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual(
      expect.objectContaining({
        mailTemplateNaam: "Bestaand template gewijzigd",
        mail: "TAAK_ONTVANGSTBEVESTIGING",
        defaultMailtemplate: true,
      }),
    );

    request.flush({});
    await sleep();
  });

  it("invalidates the saved template's own query after a successful update", async () => {
    const invalidateQueries = jest
      .spyOn(testQueryClient, "invalidateQueries")
      .mockResolvedValue();
    await setup(bestaandTemplate);
    await editTemplateName(" gewijzigd");

    await user.click(saveButton());
    await sleep();
    httpTestingController.expectOne("/rest/beheer/mailtemplates/42").flush({});
    await sleep();

    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: mailtemplateBeheerService.readMailtemplateQuery(42).queryKey,
    });
  });

  it("does not invalidate when creating a new template", async () => {
    const invalidateQueries = jest
      .spyOn(testQueryClient, "invalidateQueries")
      .mockResolvedValue();
    await setup();
    await fillInNewTemplate();

    await user.click(saveButton());
    await sleep();
    httpTestingController.expectOne("/rest/beheer/mailtemplates").flush({});
    await sleep();

    expect(invalidateQueries).not.toHaveBeenCalled();
  });
});
