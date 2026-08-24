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
import { inputBinding, outputBinding } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailCreateComponent } from "./mail-create.component";

const ZAAK_UUID = "test-zaak-uuid";
const TEMPORARY_PERSON_ID = "person-123";

const afzendersUrl = `/rest/zaken/zaak/${ZAAK_UUID}/afzender`;
const defaultAfzenderUrl = `/rest/zaken/zaak/${ZAAK_UUID}/afzender/default`;
const mailtemplateUrl = `/rest/mailtemplates/ZAAK_ALGEMEEN/${ZAAK_UUID}`;
const documentsUrl = "/rest/informatieobjecten/informatieobjectenList";
const contactDetailsUrl = `/rest/klanten/contactdetails/person/${TEMPORARY_PERSON_ID}`;
const sendMailUrl = `/rest/mail/send/${ZAAK_UUID}`;

const zaakMetInitiator = fromPartial<GeneratedType<"RestZaak">>({
  uuid: ZAAK_UUID,
  identificatie: "ZAAK-2025-001",
  initiatorIdentificatie: {
    type: "BSN",
    temporaryPersonId: TEMPORARY_PERSON_ID,
  },
});

const afzenders = [
  fromPartial<GeneratedType<"RestZaakAfzender">>({
    defaultMail: true,
    id: 1,
    mail: "beheerder@example.com",
    speciaal: true,
    suffix: "gegevens.mail.afzender.MEDEWERKER",
  }),
  fromPartial<GeneratedType<"RestZaakAfzender">>({
    defaultMail: false,
    mail: "gemeente@example.com",
    speciaal: true,
    suffix: "gegevens.mail.afzender.GEMEENTE",
  }),
];

const mailtemplate = fromPartial<GeneratedType<"RESTMailtemplate">>({
  onderwerp: "<p>Bevestiging ontvangst</p>",
  body: "<p>Geachte,</p>",
  variabelen: ["ZAAK_NUMMER", "ZAAK_TYPE"],
});

const documents = [
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "doc-1",
    titel: "Document 1",
  }),
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "doc-2",
    titel: "Document 2",
  }),
];

const contactDetails = fromPartial<GeneratedType<"RestContactDetails">>({
  emailadres: "initiator@example.com",
});

describe(MailCreateComponent.name, () => {
  let fixture: ComponentFixture<MailCreateComponent>;
  let httpTestingController: HttpTestingController;
  let mailVerstuurd: jest.Mock;
  let openSnackbar: jest.SpyInstance;
  let foutAfhandelen: jest.SpyInstance;

  const sideNav = fromPartial<MatDrawer>({ close: jest.fn() });

  const user = userEvent.setup({ delay: null });

  async function setup(zaak: GeneratedType<"RestZaak"> = zaakMetInitiator) {
    mailVerstuurd = jest.fn();

    const rendered = await render(MailCreateComponent, {
      bindings: [
        inputBinding("zaak", () => zaak),
        inputBinding("sideNav", () => sideNav),
        outputBinding<boolean>("mailVerstuurd", (verstuurd) =>
          mailVerstuurd(verstuurd),
        ),
      ],
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    openSnackbar = jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation(() => undefined);
    foutAfhandelen = jest
      .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
      .mockReturnValue(EMPTY);
  }

  async function respondToInitialRequests({
    withContactDetails = true,
  }: { withContactDetails?: boolean } = {}) {
    await sleep();

    httpTestingController.expectOne(afzendersUrl).flush(afzenders);
    httpTestingController.expectOne(defaultAfzenderUrl).flush(afzenders[0]);
    httpTestingController.expectOne(mailtemplateUrl).flush(mailtemplate);
    httpTestingController.expectOne(documentsUrl).flush(documents);

    if (withContactDetails) {
      httpTestingController.expectOne(contactDetailsUrl).flush(contactDetails);
    } else {
      httpTestingController.expectNone((request) =>
        request.url.includes("/rest/klanten/contactdetails/person/"),
      );
    }

    await sleep();
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  function ontvangerField() {
    return screen.getByRole("textbox", { name: "Ontvanger" });
  }

  function documentCheckbox(titel: string) {
    const row = screen.getByText(titel).closest("tr") as HTMLElement;
    return within(row).getByRole("checkbox");
  }

  function vertrouwelijkheidaanduidingField() {
    return screen.getByRole("combobox", {
      name: "Vertrouwelijkheidaanduiding",
    });
  }

  function submitButton() {
    return screen.getByRole("button", { name: "actie.verstuur" });
  }

  async function selectVertrouwelijkheidaanduiding(
    optionText = "vertrouwelijkheidaanduiding.OPENBAAR",
  ) {
    await user.click(vertrouwelijkheidaanduidingField());
    await user.click(screen.getByRole("option", { name: optionText }));
  }

  async function fillInAMail(...attachments: string[]) {
    await user.click(ontvangerField());
    await user.paste("recipient@example.com");
    await selectVertrouwelijkheidaanduiding();

    for (const attachment of attachments) {
      await user.click(documentCheckbox(attachment));
    }

    fixture.detectChanges();
  }

  it("closes the side navigation when the close button is clicked", async () => {
    await setup();
    await respondToInitialRequests();

    await user.click(screen.getByRole("button", { name: "actie.sluiten" }));

    expect(sideNav.close).toHaveBeenCalled();
  });

  it("offers every afzender of the zaak as sender", async () => {
    await setup();
    await respondToInitialRequests();

    await user.click(screen.getByRole("combobox", { name: "Verzender" }));

    expect(
      screen.getByRole("option", { name: /beheerder@example.com/ }),
    ).toBeVisible();
    expect(
      screen.getByRole("option", { name: /gemeente@example.com/ }),
    ).toBeVisible();
  });

  it("preselects the default afzender of the zaak", async () => {
    await setup();
    await respondToInitialRequests();

    expect(
      screen.getByRole("combobox", { name: "Verzender" }),
    ).toHaveTextContent("beheerder@example.com");
  });

  it("prefills the subject and the body from the mailtemplate", async () => {
    await setup();
    await respondToInitialRequests();

    expect(screen.getByText("Bevestiging ontvangst")).toBeVisible();
    expect(screen.getByText("Geachte,")).toBeVisible();
  });

  it("offers every document of the zaak as attachment", async () => {
    await setup();
    await respondToInitialRequests();

    expect(screen.getByText("Document 1")).toBeVisible();
    expect(screen.getByText("Document 2")).toBeVisible();
  });

  it("offers every vertrouwelijkheidaanduiding option without preselecting one", async () => {
    await setup();
    await respondToInitialRequests();

    expect(vertrouwelijkheidaanduidingField()).toHaveTextContent("");

    await user.click(vertrouwelijkheidaanduidingField());

    expect(screen.getAllByRole("option")).toHaveLength(8);
  });

  it("keeps the mail unsendable until a vertrouwelijkheidaanduiding is selected", async () => {
    await setup();
    await respondToInitialRequests();

    await user.click(ontvangerField());
    await user.paste("recipient@example.com");

    expect(submitButton()).toBeDisabled();

    await selectVertrouwelijkheidaanduiding();

    expect(submitButton()).toBeEnabled();
  });

  it("fills the recipient with the contact e-mail address of the initiator", async () => {
    await setup();
    await respondToInitialRequests();

    await user.click(
      screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
    );

    expect(ontvangerField()).toHaveValue(contactDetails.emailadres);
  });

  it("prefers the zaak specific contact e-mail address over the initiator's", async () => {
    await setup(
      fromPartial<GeneratedType<"RestZaak">>({
        uuid: ZAAK_UUID,
        zaakSpecificContactDetails: { emailAddress: "zaak@example.com" },
      }),
    );
    await respondToInitialRequests({ withContactDetails: false });

    await user.click(
      screen.getByRole("button", { name: "actie.contact.email.toevoegen" }),
    );

    expect(ontvangerField()).toHaveValue("zaak@example.com");
  });

  it("offers no contact e-mail address when the zaak has no initiator", async () => {
    await setup(
      fromPartial<GeneratedType<"RestZaak">>({
        uuid: ZAAK_UUID,
        initiatorIdentificatie: null,
      }),
    );
    await respondToInitialRequests({ withContactDetails: false });

    expect(
      screen.queryByRole("button", { name: "actie.contact.email.toevoegen" }),
    ).not.toBeInTheDocument();
  });

  it("sends the mail that was filled in", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInAMail("Document 1");
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(sendMailUrl);

    expect(request.request.method).toBe("POST");
    expect(request.request.body).toMatchObject({
      verzender: afzenders[0].mail,
      ontvanger: "recipient@example.com",
      vertrouwelijkheidaanduiding: "OPENBAAR",
      onderwerp: "Bevestiging ontvangst",
      body: mailtemplate.body,
      bijlagen: "doc-1",
      createDocumentFromMail: true,
    });

    request.flush({});
    await sleep();
  });

  it("joins the attachments of the mail with a semicolon", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInAMail("Document 1", "Document 2");
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(sendMailUrl);

    expect(request.request.body.bijlagen).toBe("doc-1;doc-2");

    request.flush({});
    await sleep();
  });

  it("reports the mail as sent once the request succeeds", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInAMail();
    await user.click(submitButton());
    await sleep();

    httpTestingController.expectOne(sendMailUrl).flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith("msg.email.verstuurd");
    expect(mailVerstuurd).toHaveBeenCalledWith(true);
  });

  it("reports the failure when the mail could not be sent", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInAMail();
    await user.click(submitButton());
    await sleep();

    httpTestingController
      .expectOne(sendMailUrl)
      .flush({}, { status: 500, statusText: "Internal Server Error" });
    await sleep();

    expect(foutAfhandelen).toHaveBeenCalled();
    expect(mailVerstuurd).toHaveBeenCalledWith(false);
  });
});
