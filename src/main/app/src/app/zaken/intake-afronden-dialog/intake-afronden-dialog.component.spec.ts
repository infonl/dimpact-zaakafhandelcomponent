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
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { IntakeAfrondenDialogComponent } from "./intake-afronden-dialog.component";

const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
  id: "fakePlanItemId",
  userEventListenerActie: "INTAKE_AFRONDEN",
});

const afzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
  defaultMail: true,
  mail: "fakeAfzender@example.com",
  replyTo: "fakeReplyTo@example.com",
});

const contactDetails = fromPartial<GeneratedType<"RestContactDetails">>({
  emailadres: "fakeInitiator@example.com",
});

const mailtemplateOntvankelijk = fromPartial<GeneratedType<"RestMailtemplate">>(
  {
    onderwerp: "fakeOnderwerpOntvankelijk",
    body: "<p>fakeBodyOntvankelijk</p>",
  },
);

const mailtemplateNietOntvankelijk = fromPartial<
  GeneratedType<"RestMailtemplate">
>({
  onderwerp: "fakeOnderwerpNietOntvankelijk",
  body: "<p>fakeBodyNietOntvankelijk</p>",
});

function createZaak(
  intakeMail:
    | "BESCHIKBAAR_AAN"
    | "BESCHIKBAAR_UIT"
    | "NIET_BESCHIKBAAR" = "BESCHIKBAAR_AAN",
  temporaryPersonId: string | null = "fakeTemporaryPersonId",
) {
  return fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    zaaktype: fromPartial({
      zaakafhandelparameters: fromPartial({ intakeMail }),
    }),
    initiatorIdentificatie: temporaryPersonId
      ? fromPartial<GeneratedType<"BetrokkeneIdentificatie">>({
          type: "BSN",
          temporaryPersonId,
        })
      : null,
  });
}

describe(IntakeAfrondenDialogComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<IntakeAfrondenDialogComponent>;

  const user = userEvent.setup();

  async function setup(zaak: GeneratedType<"RestZaak"> = createZaak()) {
    dialogRef = fromPartial<MatDialogRef<IntakeAfrondenDialogComponent>>({
      close: jest.fn(),
      disableClose: false,
    });

    const temporaryPersonId = zaak.initiatorIdentificatie?.temporaryPersonId;
    if (temporaryPersonId) {
      testQueryClient.setQueryData(
        [
          "/rest/klanten/contactdetails/person/{temporaryPersonId}",
          { path: { temporaryPersonId } },
        ],
        contactDetails,
      );
    }

    await render(IntakeAfrondenDialogComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        { provide: MAT_DIALOG_DATA, useValue: { zaak, planItem } },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: ZakenService,
          useValue: fromPartial<ZakenService>({
            listAfzendersVoorZaak: () => of([afzender]),
            readDefaultAfzenderVoorZaak: () => of(afzender),
          }),
        },
        {
          provide: MailtemplateService,
          useValue: fromPartial<MailtemplateService>({
            findMailtemplate: (key) =>
              of(
                key === "ZAAK_ONTVANKELIJK"
                  ? mailtemplateOntvankelijk
                  : mailtemplateNietOntvankelijk,
              ),
          }),
        },
        PlanItemsService,
        KlantenService,
        UtilService,
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
  }

  function afrondenButton() {
    return screen.getByRole("button", { name: "planitem.INTAKE_AFRONDEN" });
  }

  function sendMailCheckbox() {
    return screen.queryByRole("checkbox", { name: "sendMail" });
  }

  function contactEmailButton() {
    return screen.queryByRole("button", {
      name: "actie.contact.email.toevoegen",
    });
  }

  async function answerOntvankelijk(answer: "actie.ja" | "actie.nee") {
    await user.click(screen.getByRole("radio", { name: answer }));
  }

  async function expectNoContactDetailsRequest() {
    httpTestingController.expectNone((request) =>
      request.url.includes("/rest/klanten/contactdetails/person/"),
    );
  }

  describe("the mail section", () => {
    it("offers to send a mail and ticks it beforehand when intake mail is switched on", async () => {
      await setup(createZaak("BESCHIKBAAR_AAN"));

      await answerOntvankelijk("actie.ja");

      expect(sendMailCheckbox()).toBeChecked();
      expect(screen.getByLabelText("ontvanger")).toBeVisible();
    });

    it("offers to send a mail without ticking it when intake mail is switched off", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));

      await answerOntvankelijk("actie.ja");

      expect(sendMailCheckbox()).not.toBeChecked();
      expect(screen.queryByLabelText("ontvanger")).toBeNull();
    });

    it("does not offer to send a mail when intake mail is unavailable", async () => {
      await setup(createZaak("NIET_BESCHIKBAAR"));

      await answerOntvankelijk("actie.ja");

      expect(sendMailCheckbox()).toBeNull();
    });
  });

  describe("the contact e-mail address", () => {
    it("fills the ontvanger with the e-mail address of the initiator", async () => {
      await setup(createZaak("BESCHIKBAAR_AAN", "fakeTemporaryPersonId"));
      await answerOntvankelijk("actie.ja");

      await user.click(
        screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
      );

      expect(screen.getByLabelText("ontvanger")).toHaveValue(
        "fakeInitiator@example.com",
      );
    });

    it("prefers the zaak specific contact details over looking up the initiator", async () => {
      await setup(
        fromPartial<GeneratedType<"RestZaak">>({
          uuid: "fakeZaakUuid",
          zaaktype: fromPartial({
            zaakafhandelparameters: fromPartial({
              intakeMail: "BESCHIKBAAR_AAN",
            }),
          }),
          zaakSpecificContactDetails: fromPartial({
            emailAddress: "fakeZaakSpecifiek@example.com",
          }),
        }),
      );
      await answerOntvankelijk("actie.ja");

      await user.click(
        screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
      );

      expect(screen.getByLabelText("ontvanger")).toHaveValue(
        "fakeZaakSpecifiek@example.com",
      );
      await expectNoContactDetailsRequest();
    });

    it("does not offer a contact e-mail address when the initiator has none", async () => {
      await setup(createZaak("BESCHIKBAAR_AAN", null));

      await answerOntvankelijk("actie.ja");

      expect(contactEmailButton()).toBeNull();
      await expectNoContactDetailsRequest();
    });
  });

  describe("the reden for declaring a zaak niet ontvankelijk", () => {
    it("blocks afronden until a reden has been given", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));

      await answerOntvankelijk("actie.nee");
      expect(afrondenButton()).toBeDisabled();

      await user.type(
        screen.getByLabelText("redenNietOntvankelijk"),
        "fakeReden",
      );

      expect(afrondenButton()).toBeEnabled();
    });

    it("is not asked for when the zaak is declared ontvankelijk", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));

      await answerOntvankelijk("actie.nee");
      await answerOntvankelijk("actie.ja");

      expect(screen.queryByLabelText("redenNietOntvankelijk")).toBeNull();
      expect(afrondenButton()).toBeEnabled();
    });
  });

  describe("the mail toggle", () => {
    it("blocks afronden until an ontvanger has been given", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      await user.click(screen.getByRole("checkbox", { name: "sendMail" }));

      expect(afrondenButton()).toBeDisabled();
    });

    it("stops requiring an ontvanger once the mail is switched off again", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      await user.click(screen.getByRole("checkbox", { name: "sendMail" }));
      await user.click(screen.getByRole("checkbox", { name: "sendMail" }));

      expect(afrondenButton()).toBeEnabled();
    });
  });

  describe("closing the dialog", () => {
    it("closes when the cancel button is clicked", async () => {
      await setup();

      await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

      expect(dialogRef.close).toHaveBeenCalled();
    });

    it("closes when the close button in the toolbar is clicked", async () => {
      await setup();

      await user.click(screen.getByRole("button", { name: "actie.sluiten" }));

      expect(dialogRef.close).toHaveBeenCalled();
    });
  });

  describe("afronden", () => {
    async function afronden() {
      await user.click(afrondenButton());
      await sleep();
      return httpTestingController.expectOne(
        "/rest/planitems/doUserEventListenerPlanItem",
      );
    }

    it("sends the ontvankelijk mail to the given ontvanger", async () => {
      await setup(createZaak("BESCHIKBAAR_AAN"));
      await answerOntvankelijk("actie.ja");
      await user.type(
        screen.getByLabelText("ontvanger"),
        "fakeOntvanger@example.com",
      );

      const request = await afronden();

      expect(request.request.body).toEqual(
        expect.objectContaining({
          actie: "INTAKE_AFRONDEN",
          planItemInstanceId: "fakePlanItemId",
          zaakUuid: "fakeZaakUuid",
          zaakOntvankelijk: true,
          restMailGegevens: expect.objectContaining({
            verzender: afzender.mail,
            replyTo: afzender.replyTo,
            ontvanger: "fakeOntvanger@example.com",
            onderwerp: mailtemplateOntvankelijk.onderwerp,
            body: mailtemplateOntvankelijk.body,
            createDocumentFromMail: true,
          }),
        }),
      );
      request.flush(null);
    });

    it("sends a fixed Openbaar vertrouwelijkheidaanduiding in restMailGegevens, since the backend forces it regardless", async () => {
      await setup(createZaak("BESCHIKBAAR_AAN"));
      await answerOntvankelijk("actie.ja");
      await user.type(
        screen.getByLabelText("ontvanger"),
        "fakeOntvanger@example.com",
      );

      const request = await afronden();

      expect(
        request.request.body.restMailGegevens.vertrouwelijkheidaanduiding,
      ).toBe("OPENBAAR");

      request.flush(null);
    });

    it("uses niet-ontvankelijk mailtemplate when ontvankelijk is false", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.nee");
      await user.type(
        screen.getByLabelText("redenNietOntvankelijk"),
        "fakeReden",
      );
      await user.click(screen.getByRole("checkbox", { name: "sendMail" }));
      await user.type(
        screen.getByLabelText("ontvanger"),
        "fakeOntvanger@example.com",
      );

      const request = await afronden();

      expect(request.request.body).toEqual(
        expect.objectContaining({
          zaakOntvankelijk: false,
          resultaatToelichting: "fakeReden",
          restMailGegevens: expect.objectContaining({
            onderwerp: mailtemplateNietOntvankelijk.onderwerp,
            body: mailtemplateNietOntvankelijk.body,
          }),
        }),
      );
      request.flush(null);
    });

    it("sends no mail gegevens when no mail is to be sent", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      const request = await afronden();

      expect(request.request.body).toEqual(
        expect.objectContaining({ restMailGegevens: null }),
      );
      request.flush(null);
    });

    it("closes the dialog with true once the intake has been afgerond", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      (await afronden()).flush(null);
      await sleep();

      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });

    it("closes the dialog with false when afronden fails", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      (await afronden()).flush(null, {
        status: 500,
        statusText: "Server Error",
      });
      await sleep();

      expect(dialogRef.close).toHaveBeenCalledWith(false);
    });
  });
});
