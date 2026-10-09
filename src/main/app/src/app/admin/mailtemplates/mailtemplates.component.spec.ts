/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, waitFor, within } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { sleep } from "../../../../setupJest";
import { fromPartial } from "../../../test-helpers";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplatesComponent } from "./mailtemplates.component";

const mailtemplate = (
  fields: Partial<GeneratedType<"RestMailtemplate">> = {},
) =>
  fromPartial<GeneratedType<"RestMailtemplate">>({
    id: 1,
    mailTemplateNaam: "fakeTemplateNaam",
    mail: "TAAK_ONTVANGSTBEVESTIGING",
    onderwerp: "fakeOnderwerp",
    body: "fakeBody",
    defaultMailtemplate: false,
    ...fields,
  });

const koppeling = fromPartial<GeneratedType<"RESTMailtemplateKoppeling">>({
  id: 1,
  mailtemplate: { id: 1 },
  zaakafhandelParameters: {
    zaaktype: { omschrijving: "fakeZaaktype", uuid: "fakeZaaktypeUuid" },
  },
});

describe(MailtemplatesComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;

  const user = userEvent.setup();

  async function renderMailtemplates() {
    utilServiceMock = { setTitle: jest.fn(), openSnackbar: jest.fn() };

    const rendered = await render(MailtemplatesComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);

    return rendered;
  }

  async function setup({
    mailtemplates = [mailtemplate()],
    koppelingen = [],
  }: {
    mailtemplates?: GeneratedType<"RestMailtemplate">[];
    koppelingen?: GeneratedType<"RESTMailtemplateKoppeling">[];
  } = {}) {
    const rendered = await renderMailtemplates();
    await respondWithMailtemplates(mailtemplates);
    httpTestingController
      .expectOne("/rest/beheer/mailtemplatekoppeling")
      .flush(koppelingen);
    await waitFor(() =>
      expect(screen.queryByText("msg.loading")).not.toBeInTheDocument(),
    );

    return rendered;
  }

  async function respondWithMailtemplates(
    mailtemplates: GeneratedType<"RestMailtemplate">[],
  ) {
    await sleep();
    httpTestingController
      .expectOne("/rest/beheer/mailtemplates")
      .flush(mailtemplates);
  }

  function rowNames() {
    return screen
      .getAllByRole("row")
      .map((row) => within(row).queryAllByRole("cell")[1]?.textContent?.trim())
      .filter(Boolean);
  }

  it("sets the title", async () => {
    await setup();

    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.mailtemplates",
      undefined,
    );
  });

  it("shows a row for each mailtemplate", async () => {
    await setup({
      mailtemplates: [
        mailtemplate({ id: 1, mailTemplateNaam: "fakeEerste" }),
        mailtemplate({ id: 2, mailTemplateNaam: "fakeTweede" }),
      ],
    });

    expect(screen.getByText("fakeEerste")).toBeInTheDocument();
    expect(screen.getByText("fakeTweede")).toBeInTheDocument();
  });

  it("shows that the mailtemplates are loading", async () => {
    await renderMailtemplates();

    expect(screen.getByText("msg.loading")).toBeInTheDocument();

    await respondWithMailtemplates([]);
    httpTestingController
      .expectOne("/rest/beheer/mailtemplatekoppeling")
      .flush([]);
  });

  it("says so when there are no mailtemplates", async () => {
    await setup({ mailtemplates: [] });

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
  });

  describe("deleting a mailtemplate", () => {
    it("deletes the mailtemplate once the user confirms, and reloads the list", async () => {
      await setup();

      await user.click(
        screen.getByRole("button", { name: "actie.verwijderen" }),
      );
      await user.click(await screen.findByRole("button", { name: "actie.ja" }));
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/beheer/mailtemplates/1",
      );
      expect(request.request.method).toBe("DELETE");
      request.flush(null);
      await respondWithMailtemplates([]);

      expect(
        await screen.findByText("msg.geen.gegevens.gevonden"),
      ).toBeInTheDocument();
      expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
        "msg.mailtemplate.verwijderen.uitgevoerd",
      );
    });

    it("does not delete the mailtemplate when the user declines", async () => {
      await setup();

      await user.click(
        screen.getByRole("button", { name: "actie.verwijderen" }),
      );
      await user.click(
        await screen.findByRole("button", { name: "actie.nee" }),
      );
      await sleep();

      httpTestingController.expectNone("/rest/beheer/mailtemplates/1");
    });

    it("does not allow deleting a mailtemplate that a zaaktype uses", async () => {
      await setup({ koppelingen: [koppeling] });

      expect(
        screen.getByRole("button", { name: "actie.verwijderen" }),
      ).toBeDisabled();
    });

    it("does not offer to delete the default mailtemplate", async () => {
      await setup({
        mailtemplates: [mailtemplate({ defaultMailtemplate: true })],
      });

      expect(screen.getByText("done")).toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "actie.verwijderen" }),
      ).not.toBeInTheDocument();
    });
  });

  describe("the zaaktypes that use a mailtemplate", () => {
    it("are listed with the mailtemplate", async () => {
      await setup({ koppelingen: [koppeling] });

      expect(
        screen.getAllByText("msg.mailtemplate.verwijderen.gekoppeld"),
      ).not.toHaveLength(0);
      expect(
        screen.getByRole("link", { name: "actie.zaakafhandelparameters" }),
      ).toHaveAttribute("href", "/admin/parameters/fakeZaaktypeUuid");
    });

    it("are not listed with a mailtemplate that no zaaktype uses", async () => {
      await setup({
        mailtemplates: [mailtemplate({ id: 99 })],
        koppelingen: [koppeling],
      });

      expect(
        screen.queryByRole("link", { name: "actie.zaakafhandelparameters" }),
      ).not.toBeInTheDocument();
    });

    it("can be expanded and collapsed by clicking the mailtemplate", async () => {
      const { detectChanges } = await setup({ koppelingen: [koppeling] });

      expect(screen.getByText("keyboard_arrow_down")).toBeInTheDocument();

      await user.click(screen.getByText("fakeTemplateNaam"));
      detectChanges();
      expect(screen.getByText("keyboard_arrow_up")).toBeInTheDocument();

      await user.click(screen.getByText("fakeTemplateNaam"));
      detectChanges();
      expect(screen.getByText("keyboard_arrow_down")).toBeInTheDocument();
    });
  });

  it("filters the mailtemplates", async () => {
    const { detectChanges } = await setup({
      mailtemplates: [
        mailtemplate({ id: 1, mailTemplateNaam: "fakeEerste" }),
        mailtemplate({ id: 2, mailTemplateNaam: "fakeTweede" }),
      ],
    });

    await user.type(screen.getByRole("textbox", { name: "filter" }), "tweede");
    detectChanges();

    expect(screen.queryByText("fakeEerste")).not.toBeInTheDocument();
    expect(screen.getByText("fakeTweede")).toBeInTheDocument();
  });

  it("sorts the mailtemplates by name", async () => {
    const { detectChanges } = await setup({
      mailtemplates: [
        mailtemplate({ id: 1, mailTemplateNaam: "fakeB" }),
        mailtemplate({ id: 2, mailTemplateNaam: "fakeA" }),
      ],
    });

    await user.click(screen.getByText("mail-template-naam"));
    detectChanges();

    expect(rowNames()).toEqual(["fakeA", "fakeB"]);
  });
});
