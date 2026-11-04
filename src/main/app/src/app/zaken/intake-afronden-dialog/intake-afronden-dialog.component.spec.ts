/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { KlantenService } from "../../klanten/klanten.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { IntakeAfrondenDialogComponent } from "./intake-afronden-dialog.component";

const planItem = fromPartial<GeneratedType<"RestPlanItem">>({
  id: "fakePlanItemId",
  userEventListenerActie: "INTAKE_AFRONDEN",
});

const afzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
  id: 1,
  isDefaultMail: true,
  mail: "fakeAfzender@example.com",
  replyTo: "fakeReplyTo@example.com",
});

const andereAfzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
  id: 2,
  isDefaultMail: false,
  mail: "fakeAndereAfzender@example.com",
  suffix: "gegevens.mail.afzender.GEMEENTE",
});

const contactDetails = fromPartial<GeneratedType<"RestContactDetails">>({
  emailadres: "fakeInitiator@example.com",
});

const afzendersKey = (uuid: string) => [
  "/rest/zaken/zaak/{uuid}/afzender",
  { path: { uuid } },
];

const defaultAfzenderKey = (uuid: string) => [
  "/rest/zaken/zaak/{uuid}/afzender/default",
  { path: { uuid } },
];

function deepFreeze<T>(value: T): T {
  Object.values(value as object).forEach((child) => {
    if (typeof child === "object" && child !== null) deepFreeze(child);
  });
  return Object.freeze(value);
}

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

  function cacheAfzenders(
    uuid: string,
    afzenders: GeneratedType<"RestZaakAfzender">[],
    defaultAfzender: GeneratedType<"RestZaakAfzender"> | null,
  ) {
    testQueryClient.setQueryData(afzendersKey(uuid), deepFreeze(afzenders));
    testQueryClient.setQueryData(
      defaultAfzenderKey(uuid),
      defaultAfzender && deepFreeze(defaultAfzender),
    );
  }

  async function setup(
    zaak: GeneratedType<"RestZaak"> = createZaak(),
    {
      afzenders = [afzender, andereAfzender],
      defaultAfzender = afzender,
    }: {
      afzenders?: GeneratedType<"RestZaakAfzender">[];
      defaultAfzender?: GeneratedType<"RestZaakAfzender"> | null;
    } = {},
  ) {
    dialogRef = fromPartial<MatDialogRef<IntakeAfrondenDialogComponent>>({
      close: jest.fn(),
      disableClose: false,
    });

    cacheAfzenders(zaak.uuid, afzenders, defaultAfzender);

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

    const rendered = await render(IntakeAfrondenDialogComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        { provide: MAT_DIALOG_DATA, useValue: { zaak, planItem } },
        { provide: MatDialogRef, useValue: dialogRef },
        PlanItemsService,
        KlantenService,
        UtilService,
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    httpTestingController
      .expectOne("/rest/mailtemplates/ZAAK_ONTVANKELIJK/fakeZaakUuid")
      .flush(mailtemplateOntvankelijk);
    httpTestingController
      .expectOne("/rest/mailtemplates/ZAAK_NIET_ONTVANKELIJK/fakeZaakUuid")
      .flush(mailtemplateNietOntvankelijk);
    await sleep();
    rendered.fixture.detectChanges();
    return rendered;
  }

  function afrondenButton() {
    return screen.getByRole("button", { name: "planitem.intake-afronden" });
  }

  function sendMailCheckbox() {
    return screen.queryByRole("checkbox", { name: "send-mail" });
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
        screen.getByLabelText("reden-niet-ontvankelijk"),
        "fakeReden",
      );

      expect(afrondenButton()).toBeEnabled();
    });

    it("is not asked for when the zaak is declared ontvankelijk", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));

      await answerOntvankelijk("actie.nee");
      await answerOntvankelijk("actie.ja");

      expect(screen.queryByLabelText("reden-niet-ontvankelijk")).toBeNull();
      expect(afrondenButton()).toBeEnabled();
    });
  });

  describe("the mail toggle", () => {
    it("blocks afronden until an ontvanger has been given", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      await user.click(screen.getByRole("checkbox", { name: "send-mail" }));

      expect(afrondenButton()).toBeDisabled();
    });

    it("stops requiring an ontvanger once the mail is switched off again", async () => {
      await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      await user.click(screen.getByRole("checkbox", { name: "send-mail" }));
      await user.click(screen.getByRole("checkbox", { name: "send-mail" }));

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
          isZaakOntvankelijk: true,
          restMailGegevens: expect.objectContaining({
            verzender: afzender.mail,
            replyTo: afzender.replyTo,
            ontvanger: "fakeOntvanger@example.com",
            onderwerp: mailtemplateOntvankelijk.onderwerp,
            body: mailtemplateOntvankelijk.body,
            shouldCreateDocumentFromMail: true,
          }),
        }),
      );
      request.flush(null);
    });

    it("sends a fixed Openbaar vertrouwelijkheidaanduiding in restMailGegevens, since this flow does not expose a confidentiality choice", async () => {
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
        screen.getByLabelText("reden-niet-ontvankelijk"),
        "fakeReden",
      );
      await user.click(screen.getByRole("checkbox", { name: "send-mail" }));
      await user.type(
        screen.getByLabelText("ontvanger"),
        "fakeOntvanger@example.com",
      );

      const request = await afronden();

      expect(request.request.body).toEqual(
        expect.objectContaining({
          isZaakOntvankelijk: false,
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

    it("disables afronden and annuleren while the intake is being afgerond", async () => {
      const { fixture } = await setup(createZaak("BESCHIKBAAR_UIT"));
      await answerOntvankelijk("actie.ja");

      const request = await afronden();
      fixture.detectChanges();

      expect(afrondenButton()).toBeDisabled();
      expect(
        screen.getByRole("button", { name: "actie.annuleren" }),
      ).toBeDisabled();
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

  describe("the verzender", () => {
    function verzenderSelect() {
      return screen.getByRole("combobox", { name: /verzender/i });
    }

    async function setupWithVerzender(
      ...setupArguments: Parameters<typeof setup>
    ) {
      const rendered = await setup(...setupArguments);
      await answerOntvankelijk("actie.ja");
      await settle(rendered.fixture);
      return rendered;
    }

    async function settle(
      fixture: ComponentFixture<IntakeAfrondenDialogComponent>,
    ) {
      for (let cycle = 0; cycle < 3; cycle++) {
        await sleep();
        fixture.detectChanges();
      }
    }

    async function arriveReRead(
      fixture: ComponentFixture<IntakeAfrondenDialogComponent>,
    ) {
      const nieuweDefault = fromPartial<GeneratedType<"RestZaakAfzender">>({
        id: 3,
        isDefaultMail: true,
        mail: "fakeNieuweDefault@example.com",
      });
      testQueryClient.setQueryData(afzendersKey("fakeZaakUuid"), [
        afzender,
        andereAfzender,
        nieuweDefault,
      ]);
      testQueryClient.setQueryData(
        defaultAfzenderKey("fakeZaakUuid"),
        nieuweDefault,
      );
      await settle(fixture);
    }

    it("offers the cached afzenders and preselects the default without reading them again, so that opening the dialog again is immediate", async () => {
      await setupWithVerzender();

      httpTestingController.expectNone(
        "/rest/zaken/zaak/fakeZaakUuid/afzender",
      );
      httpTestingController.expectNone(
        "/rest/zaken/zaak/fakeZaakUuid/afzender/default",
      );
      expect(verzenderSelect()).toHaveTextContent(afzender.mail!);

      await user.click(verzenderSelect());
      expect(
        screen.getByRole("option", { name: /fakeAndereAfzender@example.com/ }),
      ).toBeVisible();
    });

    it("shows the suffix of a cached afzender as a translation key", async () => {
      await setupWithVerzender();

      await user.click(verzenderSelect());

      expect(
        screen.getByRole("option", {
          name: /gegevens\.mail\.afzender\.gemeente/,
        }),
      ).toBeVisible();
    });

    it("selects nothing when the zaak has no default afzender", async () => {
      await setupWithVerzender(createZaak(), { defaultAfzender: null });

      expect(verzenderSelect()).not.toHaveTextContent(afzender.mail!);
    });

    it("keeps the verzender the user chose when a re-read of the afzenders arrives", async () => {
      const { fixture } = await setupWithVerzender();

      await user.click(verzenderSelect());
      await user.click(
        screen.getByRole("option", { name: /fakeAndereAfzender@example.com/ }),
      );
      await arriveReRead(fixture);

      expect(verzenderSelect()).toHaveTextContent(
        "fakeAndereAfzender@example.com",
      );
    });

    it("follows a re-read of the afzenders while the user has not chosen a verzender", async () => {
      const { fixture } = await setupWithVerzender();

      await arriveReRead(fixture);

      expect(verzenderSelect()).toHaveTextContent(
        "fakeNieuweDefault@example.com",
      );
    });
  });
});
