/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import moment from "moment";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";
import { ZaakAfhandelenDialogComponent } from "./zaak-afhandelen-dialog.component";

const zaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "fakeZaakUuid",
  zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
    uuid: "fakeZaaktypeUuid",
    omschrijving: "fakeZaaktypeOmschrijving",
    zaakafhandelparameters: { afrondenMail: "BESCHIKBAAR_UIT" },
  }),
  initiatorIdentificatie: fromPartial<GeneratedType<"BetrokkeneIdentificatie">>(
    {
      type: "BSN",
      temporaryPersonId: "fakeTemporaryPersonId",
    },
  ),
  resultaat: null,
  besluiten: [],
});

const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
  id: "fakePlanItemId",
  userEventListenerActie: "ZAAK_AFHANDELEN",
  toelichting: "fakePlanItemToelichting",
});

const resultaattypeMetBrondatum = fromPartial<
  GeneratedType<"RestResultaattype">
>({
  id: "fakeResultaattypeId1",
  naam: "fakeResultaatMetBrondatum",
  besluitVerplicht: false,
  datumKenmerkVerplicht: true,
});

const resultaattypeMetBesluit = fromPartial<GeneratedType<"RestResultaattype">>(
  {
    id: "fakeResultaattypeId2",
    naam: "fakeResultaatMetBesluit",
    besluitVerplicht: true,
    datumKenmerkVerplicht: false,
  },
);

const resultaattypeZonderVerplichtingen = fromPartial<
  GeneratedType<"RestResultaattype">
>({
  id: "fakeResultaattypeId3",
  naam: "fakeResultaatZonderVerplichtingen",
  besluitVerplicht: false,
  datumKenmerkVerplicht: false,
});

const resultaattypes = [
  resultaattypeMetBrondatum,
  resultaattypeMetBesluit,
  resultaattypeZonderVerplichtingen,
];

const afzenders = [
  fromPartial<GeneratedType<"RestZaakAfzender">>({
    mail: "fakeAfzender@example.com",
    suffix: "fakeAfzenderSuffix",
    replyTo: "fakeReplyTo@example.com",
  }),
];

const mailtemplate = fromPartial<GeneratedType<"RestMailtemplate">>({
  onderwerp: "fakeOnderwerp",
  body: "fakeMailBody",
});

const besluit = fromPartial<GeneratedType<"RestBesluit">>({
  uuid: "fakeBesluitUuid",
  url: "https://example.com/besluit",
});

describe(ZaakAfhandelenDialogComponent.name, () => {
  let fixture: ComponentFixture<ZaakAfhandelenDialogComponent>;
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<ZaakAfhandelenDialogComponent>;

  const user = userEvent.setup();

  async function setup({
    zaakToHandle = zaak,
    planItemToHandle = planItem,
  }: {
    zaakToHandle?: GeneratedType<"RestZaak">;
    planItemToHandle?: GeneratedType<"RESTPlanItem"> | null;
  } = {}) {
    dialogRef = fromPartial<MatDialogRef<ZaakAfhandelenDialogComponent>>({
      close: jest.fn(),
      disableClose: false,
    });

    const rendered = await render(ZaakAfhandelenDialogComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { zaak: zaakToHandle, planItem: planItemToHandle },
        },
        CustomValidators,
        ZakenService,
        MailtemplateService,
        KlantenService,
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);

    jest
      .spyOn(TestBed.inject(ZakenService), "listResultaattypes")
      .mockReturnValue(of(resultaattypes));
    jest
      .spyOn(TestBed.inject(ZakenService), "listAfzendersVoorZaak")
      .mockReturnValue(of(afzenders));
    jest
      .spyOn(TestBed.inject(MailtemplateService), "findMailtemplate")
      .mockReturnValue(of(mailtemplate));

    return rendered;
  }

  function seedQueries(zaakToHandle: GeneratedType<"RestZaak">) {
    testQueryClient.setQueryData(
      ["resultaattypes", zaakToHandle.zaaktype.uuid],
      resultaattypes,
    );
    testQueryClient.setQueryData(["afzenders", zaakToHandle.uuid], afzenders);
    testQueryClient.setQueryData(
      ["mailtemplate", zaakToHandle.uuid],
      mailtemplate,
    );

    const temporaryPersonId =
      zaakToHandle.initiatorIdentificatie?.temporaryPersonId;
    if (temporaryPersonId) {
      testQueryClient.setQueryData(
        [
          "/rest/klanten/contactdetails/person/{temporaryPersonId}",
          { path: { temporaryPersonId } },
        ],
        fromPartial<GeneratedType<"RestContactDetails">>({
          emailadres: "fakeInitiator@example.com",
        }),
      );
    }
  }

  function submitButton() {
    return screen.queryByRole("button", { name: "actie.zaak.afhandelen" });
  }

  function sendMailCheckbox() {
    return screen.queryByRole("checkbox", { name: "sendMail" });
  }

  function verzenderSelect() {
    return screen.queryByRole("combobox", { name: /Verzender/ });
  }

  function ontvangerField() {
    return screen.queryByRole("textbox", { name: /Ontvanger/ });
  }

  function brondatumField() {
    return screen.getByLabelText("zaak.brondatum");
  }

  async function chooseResultaattype(naam: string) {
    await user.click(screen.getByRole("combobox", { name: /Resultaat/ }));
    await user.click(screen.getByRole("option", { name: new RegExp(naam) }));
  }

  async function toggleSendMail() {
    await user.click(screen.getByRole("checkbox", { name: "sendMail" }));
  }

  async function openVerzenderOptions() {
    await user.click(screen.getByRole("combobox", { name: /Verzender/ }));
  }

  async function submit() {
    await user.click(
      screen.getByRole("button", { name: "actie.zaak.afhandelen" }),
    );
  }

  describe("the mail fields", () => {
    it("shows the verzender and ontvanger fields once sending a mail is checked", async () => {
      seedQueries(zaak);
      await setup();

      await toggleSendMail();

      expect(verzenderSelect()).toBeVisible();
      expect(ontvangerField()).toBeVisible();
    });

    it("hides the verzender and ontvanger fields again once sending a mail is unchecked", async () => {
      seedQueries(zaak);
      await setup();

      await toggleSendMail();
      await toggleSendMail();

      expect(verzenderSelect()).toBeNull();
      expect(ontvangerField()).toBeNull();
    });

    it("shows the mail body in the expandable panel", async () => {
      seedQueries(zaak);
      await setup();

      await toggleSendMail();
      await user.click(screen.getByRole("button", { name: "body" }));

      expect(screen.getByText("fakeMailBody")).toBeVisible();
    });

    it("shows the suffix of an afzender in the dropdown options", async () => {
      seedQueries(zaak);
      await setup();

      await toggleSendMail();
      await openVerzenderOptions();

      expect(
        screen.getByRole("option", { name: /fakeAfzenderSuffix/ }),
      ).toHaveTextContent("fakeAfzender@example.com fakeAfzenderSuffix");
    });

    it("shows only the mail address of the afzender once it is selected", async () => {
      seedQueries(zaak);
      await setup();

      await toggleSendMail();
      await openVerzenderOptions();
      await user.click(
        screen.getByRole("option", { name: /fakeAfzenderSuffix/ }),
      );

      expect(verzenderSelect()).toHaveTextContent("fakeAfzender@example.com");
      expect(verzenderSelect()).not.toHaveTextContent("fakeAfzenderSuffix");
    });
  });

  describe("a resultaattype that requires a besluit", () => {
    it("offers to record a besluit instead of afhandelen the zaak", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatMetBesluit");

      expect(
        screen.getByRole("button", { name: "actie.besluit.vastleggen" }),
      ).toBeEnabled();
      expect(submitButton()).toBeNull();
    });

    it("closes the dialog to record a besluit when the besluit-vastleggen button is clicked", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatMetBesluit");
      await user.click(
        screen.getByRole("button", { name: "actie.besluit.vastleggen" }),
      );

      expect(dialogRef.close).toHaveBeenCalledWith("openBesluitVastleggen");
    });

    it("offers to afhandelen the zaak when it already has a besluit", async () => {
      const zaakMetBesluit = fromPartial<GeneratedType<"RestZaak">>({
        ...zaak,
        besluiten: [besluit],
      });
      seedQueries(zaakMetBesluit);
      await setup({ zaakToHandle: zaakMetBesluit });

      await chooseResultaattype("fakeResultaatMetBesluit");

      expect(submitButton()).toBeVisible();
    });
  });

  it("closes the dialog when the cancel button is clicked", async () => {
    seedQueries(zaak);
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(dialogRef.close).toHaveBeenCalled();
  });

  describe("afhandelen a plan item", () => {
    it("refuses to submit while no resultaattype has been chosen", async () => {
      seedQueries(zaak);
      await setup();

      expect(submitButton()).toBeDisabled();
    });

    it("allows submitting once a resultaattype has been chosen", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatZonderVerplichtingen");

      expect(submitButton()).toBeEnabled();
    });

    it("afhandelt the plan item with the chosen resultaattype", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatZonderVerplichtingen");
      await submit();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/planitems/doUserEventListenerPlanItem",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          actie: "ZAAK_AFHANDELEN",
          planItemInstanceId: "fakePlanItemId",
          zaakUuid: "fakeZaakUuid",
          resultaattypeUuid: "fakeResultaattypeId3",
        }),
      );
      request.flush({});
    });

    it("sends a fixed Openbaar vertrouwelijkheidaanduiding in restMailGegevens, since this flow does not expose a confidentiality choice", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatZonderVerplichtingen");
      await toggleSendMail();
      await openVerzenderOptions();
      await user.click(
        screen.getByRole("option", { name: /fakeAfzenderSuffix/ }),
      );
      await user.type(
        screen.getByRole("textbox", { name: /Ontvanger/ }),
        "recipient@example.com",
      );
      await submit();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/planitems/doUserEventListenerPlanItem",
      );
      expect(request.request.body.restMailGegevens).toMatchObject({
        verzender: afzenders[0].mail,
        replyTo: afzenders[0].replyTo,
        ontvanger: "recipient@example.com",
        onderwerp: mailtemplate.onderwerp,
        body: mailtemplate.body,
        createDocumentFromMail: true,
        vertrouwelijkheidaanduiding: "OPENBAAR",
      });

      request.flush({});
    });

    it("refuses another submit once the zaak has been afgehandeld", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatZonderVerplichtingen");
      await submit();
      await sleep();

      httpTestingController
        .expectOne("/rest/planitems/doUserEventListenerPlanItem")
        .flush({});
      await sleep();
      fixture.detectChanges();

      expect(submitButton()).toBeDisabled();
    });
  });

  describe("the brondatum of a resultaattype that requires one", () => {
    it("sends the brondatum along on submit", async () => {
      seedQueries(zaak);
      await setup();
      const brondatum = moment().add(1, "day");

      await chooseResultaattype("fakeResultaatMetBrondatum");
      await user.type(
        screen.getByRole("textbox", { name: /Toelichting/ }),
        "fakeToelichting",
      );
      await user.type(brondatumField(), brondatum.format("YYYY-MM-DD"));
      await submit();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/planitems/doUserEventListenerPlanItem",
      );
      expect(request.request.body).toEqual(
        expect.objectContaining({
          actie: "ZAAK_AFHANDELEN",
          planItemInstanceId: "fakePlanItemId",
          zaakUuid: "fakeZaakUuid",
          resultaattypeUuid: "fakeResultaattypeId1",
          resultaatToelichting: "fakeToelichting",
          brondatum: brondatum.startOf("day").toISOString(),
        }),
      );
      request.flush({});
    });

    it("refuses a brondatum before today", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatMetBrondatum");
      await user.type(
        brondatumField(),
        moment().subtract(1, "day").format("YYYY-MM-DD"),
      );

      expect(submitButton()).toBeDisabled();
    });

    it("accepts a brondatum of today", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatMetBrondatum");
      await user.type(brondatumField(), moment().format("YYYY-MM-DD"));

      expect(submitButton()).toBeEnabled();
    });

    it("accepts an empty brondatum", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatMetBrondatum");

      expect(submitButton()).toBeEnabled();
    });

    it("offers no date before today", async () => {
      seedQueries(zaak);
      await setup();

      await chooseResultaattype("fakeResultaatMetBrondatum");

      expect(brondatumField()).toHaveAttribute(
        "min",
        moment().startOf("day").format(),
      );
    });
  });

  describe("the contact email address", () => {
    it("fills the ontvanger with the initiator email address", async () => {
      seedQueries(zaak);
      await setup();

      await toggleSendMail();
      await user.click(
        screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
      );

      expect(ontvangerField()).toHaveValue("fakeInitiator@example.com");
    });

    it("prefers the zaak specific contact email address over the initiator", async () => {
      const zaakMetContactgegevens = fromPartial<GeneratedType<"RestZaak">>({
        ...zaak,
        zaakSpecificContactDetails: fromPartial({
          emailAddress: "fakeContact@example.com",
        }),
      });
      seedQueries(zaakMetContactgegevens);
      await setup({ zaakToHandle: zaakMetContactgegevens });

      await toggleSendMail();
      await user.click(
        screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
      );

      expect(ontvangerField()).toHaveValue("fakeContact@example.com");
    });

    it("fills the ontvanger with the zaak specific contact email address when there is no initiator", async () => {
      const zaakZonderInitiator = fromPartial<GeneratedType<"RestZaak">>({
        ...zaak,
        initiatorIdentificatie: undefined,
        zaakSpecificContactDetails: fromPartial({
          emailAddress: "fakeContact@example.com",
        }),
        zaaktype: {
          ...zaak.zaaktype,
          zaakafhandelparameters: { afrondenMail: "BESCHIKBAAR_AAN" },
        },
      });
      seedQueries(zaakZonderInitiator);
      await setup({ zaakToHandle: zaakZonderInitiator });

      await user.click(
        screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
      );

      expect(ontvangerField()).toHaveValue("fakeContact@example.com");
    });
  });

  describe("a zaaktype that sends a mail on afronden by default", () => {
    const zaakMetAfrondenMailAan = fromPartial<GeneratedType<"RestZaak">>({
      ...zaak,
      zaaktype: {
        ...zaak.zaaktype,
        zaakafhandelparameters: { afrondenMail: "BESCHIKBAAR_AAN" },
      },
    });

    it("checks sending a mail and shows the verzender field upfront", async () => {
      seedQueries(zaakMetAfrondenMailAan);
      await setup({ zaakToHandle: zaakMetAfrondenMailAan });

      expect(sendMailCheckbox()).toBeChecked();
      expect(verzenderSelect()).toBeVisible();
    });
  });

  describe("a zaaktype that cannot send a mail on afronden", () => {
    const zaakZonderAfrondenMail = fromPartial<GeneratedType<"RestZaak">>({
      ...zaak,
      zaaktype: {
        ...zaak.zaaktype,
        zaakafhandelparameters: { afrondenMail: "NIET_BESCHIKBAAR" },
      },
    });

    it("offers no mail fields at all", async () => {
      seedQueries(zaakZonderAfrondenMail);
      await setup({ zaakToHandle: zaakZonderAfrondenMail });

      expect(sendMailCheckbox()).toBeNull();
      expect(verzenderSelect()).toBeNull();
    });
  });

  describe("afsluiten a zaak without a plan item", () => {
    it("closes the zaak with the chosen resultaattype", async () => {
      seedQueries(zaak);
      await setup({ planItemToHandle: null });

      await chooseResultaattype("fakeResultaatZonderVerplichtingen");
      await submit();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/zaken/zaak/fakeZaakUuid/afsluiten",
      );
      expect(request.request.method).toBe("PATCH");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          resultaattypeUuid: "fakeResultaattypeId3",
        }),
      );
      request.flush({});
    });

    it("caches the closed zaak returned by the afsluiten mutation and closes the dialog", async () => {
      seedQueries(zaak);
      await setup({ planItemToHandle: null });
      const cacheZaak = jest.spyOn(TestBed.inject(ZakenService), "cacheZaak");
      const fakeClosedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "fakeZaakUuid",
      });

      await chooseResultaattype("fakeResultaatZonderVerplichtingen");
      await submit();
      await sleep();

      httpTestingController
        .expectOne("/rest/zaken/zaak/fakeZaakUuid/afsluiten")
        .flush(fakeClosedZaak);
      await sleep();

      expect(cacheZaak).toHaveBeenCalledWith(fakeClosedZaak);
      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });
  });
});
