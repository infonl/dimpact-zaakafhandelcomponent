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
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { OntvangstbevestigingComponent } from "./ontvangstbevestiging.component";

const ZAAK_UUID = "test-zaak-uuid";
const TEMPORARY_PERSON_ID = "person-123";

const afzendersUrl = `/rest/zaken/zaak/${ZAAK_UUID}/afzender`;
const defaultAfzenderUrl = `/rest/zaken/zaak/${ZAAK_UUID}/afzender/default`;
const mailtemplateUrl = `/rest/mailtemplates/TAAK_ONTVANGSTBEVESTIGING/${ZAAK_UUID}`;
const documentsUrl = "/rest/informatieobjecten/informatieobjectenList";
const contactDetailsUrl = `/rest/klanten/contactdetails/person/${TEMPORARY_PERSON_ID}`;
const acknowledgeUrl = `/rest/mail/acknowledge/${ZAAK_UUID}`;

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

const mailtemplate = fromPartial<GeneratedType<"RestMailtemplate">>({
  onderwerp: "<p>Ontvangstbevestiging van uw verzoek</p>",
  body: "<p>Wij hebben uw verzoek ontvangen.</p>",
  defaultMailtemplate: true,
  variabelen: ["ZAAK_NUMMER", "ZAAK_INITIATOR"],
});

const documents = [
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "doc-1",
    titel: "Document 1",
    bestandsnaam: "document-1.pdf",
  }),
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "doc-2",
    titel: "Document 2",
    bestandsnaam: "document-2.pdf",
  }),
];

const contactDetails = fromPartial<GeneratedType<"RestContactDetails">>({
  emailadres: "initiator@example.com",
  telefoonnummer: "0612345678",
});

describe(OntvangstbevestigingComponent.name, () => {
  let fixture: ComponentFixture<OntvangstbevestigingComponent>;
  let httpTestingController: HttpTestingController;
  let ontvangstBevestigd: jest.Mock;
  let openSnackbar: jest.SpyInstance;

  const sideNav = fromPartial<MatDrawer>({ close: jest.fn() });

  const user = userEvent.setup({ delay: null });

  async function setup(zaak: GeneratedType<"RestZaak"> = zaakMetInitiator) {
    ontvangstBevestigd = jest.fn();

    const rendered = await render(OntvangstbevestigingComponent, {
      bindings: [
        inputBinding("zaak", () => zaak),
        inputBinding("sideNav", () => sideNav),
        outputBinding<boolean>("ontvangstBevestigd", (bevestigd) =>
          ontvangstBevestigd(bevestigd),
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
    const row = screen.getByRole("row", { name: new RegExp(titel) });
    return within(row).getByRole("checkbox");
  }

  function submitButton() {
    return screen.getByRole("button", { name: "actie.verstuur" });
  }

  async function fillInTheRecipient(recipient: string) {
    await user.click(ontvangerField());
    await user.paste(recipient);
    await user.tab();
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

    expect(
      screen.getByText("Ontvangstbevestiging van uw verzoek"),
    ).toBeVisible();
    expect(screen.getByText("Wij hebben uw verzoek ontvangen.")).toBeVisible();
  });

  it("offers the variables of the mailtemplate", async () => {
    await setup();
    await respondToInitialRequests();

    await user.click(screen.getAllByRole("button", { name: "variabelen" })[0]);

    expect(
      screen.getByRole("menuitem", { name: /^ZAAK_NUMMER:/ }),
    ).toBeVisible();
    expect(
      screen.getByRole("menuitem", { name: /^ZAAK_INITIATOR:/ }),
    ).toBeVisible();
  });

  it("offers every document of the zaak as attachment", async () => {
    await setup();
    await respondToInitialRequests();

    expect(screen.getByText("Document 1")).toBeVisible();
    expect(screen.getByText("Document 2")).toBeVisible();
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

  it("cannot be sent before anything is filled in", async () => {
    await setup();
    await respondToInitialRequests();

    expect(submitButton()).toBeDisabled();
  });

  it("rejects a recipient that is not an e-mail address", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInTheRecipient("invalid-email");

    expect(screen.getByText("validators.email")).toBeVisible();
    expect(submitButton()).toBeDisabled();
  });

  it("accepts a recipient that is an e-mail address", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInTheRecipient("valid@example.com");

    expect(screen.queryByText("validators.email")).not.toBeInTheDocument();
    expect(submitButton()).toBeEnabled();
  });

  it("shows how much of the maximum subject length is used", async () => {
    await setup();
    await respondToInitialRequests();

    expect(screen.getByText(/^\d+ \/ 100$/)).toBeVisible();
  });

  it("sends the acknowledgement that was filled in", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInTheRecipient("recipient@example.com");
    await user.click(documentCheckbox("Document 1"));
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(acknowledgeUrl);

    expect(request.request.method).toBe("POST");
    expect(request.request.body).toMatchObject({
      verzender: afzenders[0].mail,
      ontvanger: "recipient@example.com",
      onderwerp: mailtemplate.onderwerp,
      body: mailtemplate.body,
      bijlagen: "doc-1",
      createDocumentFromMail: true,
    });

    request.flush({});
    await sleep();
  });

  it("joins the attachments of the acknowledgement with a semicolon", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInTheRecipient("recipient@example.com");
    await user.click(documentCheckbox("Document 1"));
    await user.click(documentCheckbox("Document 2"));
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(acknowledgeUrl);

    expect(request.request.body.bijlagen).toBe("doc-1;doc-2");

    request.flush({});
    await sleep();
  });

  it("reports the acknowledgement as sent once the request succeeds", async () => {
    await setup();
    await respondToInitialRequests();

    await fillInTheRecipient("recipient@example.com");
    await user.click(submitButton());
    await sleep();

    httpTestingController.expectOne(acknowledgeUrl).flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith("msg.email.verstuurd");
    expect(ontvangstBevestigd).toHaveBeenCalledWith(true);
  });
});
