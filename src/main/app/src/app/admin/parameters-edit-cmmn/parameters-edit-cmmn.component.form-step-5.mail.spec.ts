/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

// rendering this seven step form once per test needs more room than the default timeout
describe("Mailgegevens form step", () => {
  const user = userEvent.setup();

  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "case-1",
    naam: "Case Definition 1",
  });

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaaktypeConfiguration">
  >({
    caseDefinition,
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: { uuid: "test-uuid" },
    zaakNietOntvankelijkResultaattype: {
      id: "resultaat-1",
      naam: "Afgehandeld",
    },
    zaakAfzenders: [
      {
        speciaal: false,
        defaultMail: false,
        mail: "test@example.com",
        replyTo: undefined,
      },
      {
        speciaal: false,
        defaultMail: false,
        mail: "test2@example.com",
        replyTo: undefined,
      },
    ],
    humanTaskParameters: [],
    mailtemplateKoppelingen: [],
    zaakbeeindigParameters: [],
    smartDocuments: { enabledGlobally: false, enabledForZaaktype: false },
    userEventListenerParameters: [],
    betrokkeneKoppelingen: { brpKoppelen: false, kvkKoppelen: false },
    brpDoelbindingen: {
      zoekWaarde: "",
      raadpleegWaarde: "",
      verwerkingregisterWaarde: "",
    },
    productaanvraagtype: null,
    automaticEmailConfirmation: {
      enabled: false,
      templateName: null,
      emailSender: null,
      emailReply: null,
    },
  });

  async function setup() {
    const { fixture } = await render(ParametersEditCmmnComponent, {
      imports: [TranslateModule.forRoot(), RouterModule, NoopAnimationsModule],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: fromPartial<ActivatedRoute>({
            data: of({
              parameters: {
                zaakafhandelParameters,
                isSavedZaakafhandelParameters: true,
              },
            }),
          }),
        },
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listCaseDefinitions: () => of([caseDefinition]),
            listFormulierDefinities: () => of([]),
            listReplyTos: () => of([{ mail: "reply1@example.com" }]),
            listZaakbeeindigRedenen: () => of([]),
            listResultaattypes: () => of([]),
            updateZaakafhandelparameters: () =>
              createMutationOptions(zaakafhandelParameters),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () => of([]),
            listAfzenders: () => of(["test@example.com", "other@example.com"]),
            listBrpSearchValues: () => of([]),
            listBrpViewValues: () => of([]),
            listBrpProcessingValues: () => of([]),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listGroups: () => of([{ id: "test-group-id", naam: "test-group" }]),
            listUsersInGroup: () =>
              of([{ id: "test-user-id", naam: "test-user" }]),
          }),
        },
        {
          provide: MailtemplateBeheerService,
          useValue: fromPartial<MailtemplateBeheerService>({
            listKoppelbareMailtemplates: () => of([]),
          }),
        },
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readBrpDoelbindingSetupEnabled: () => of(false),
          }),
        },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();

    await user.click(screen.getByRole("tab", { name: /gegevens.mail/ }));

    return screen.getByRole("tabpanel", { name: /gegevens.mail/ });
  }

  function afzenderRow(mail: HTMLElement, afzender: string) {
    return within(mail).getByRole("row", { name: new RegExp(afzender) });
  }

  it("lists the afzenders of the zaaktype", async () => {
    const mail = await setup();

    expect(afzenderRow(mail, "test@example.com")).toBeVisible();
    expect(afzenderRow(mail, "test2@example.com")).toBeVisible();
  });

  it("refuses to save while no afzender is marked as the default one", async () => {
    const mail = await setup();

    expect(within(mail).getByText("validators.required")).toBeVisible();
    expect(
      within(mail).getByRole("button", { name: "actie.opslaan" }),
    ).toBeDisabled();
  });

  it("accepts the afzender that is marked as the default one", async () => {
    const mail = await setup();

    await user.click(
      within(afzenderRow(mail, "test@example.com")).getByRole("radio"),
    );

    expect(
      within(afzenderRow(mail, "test@example.com")).getByRole("radio"),
    ).toBeChecked();
    expect(
      within(mail).queryByText("validators.required"),
    ).not.toBeInTheDocument();
    expect(
      within(mail).getByRole("button", { name: "actie.opslaan" }),
    ).toBeEnabled();
  });

  it("adds an afzender that is offered in the menu", async () => {
    const mail = await setup();

    await user.click(
      within(mail).getByRole("button", { name: "Afzender toevoegen" }),
    );
    await user.click(
      screen.getByRole("menuitem", { name: "other@example.com" }),
    );

    expect(afzenderRow(mail, "other@example.com")).toBeVisible();
  });

  it("removes an afzender from the list", async () => {
    const mail = await setup();

    await user.click(
      within(afzenderRow(mail, "test2@example.com")).getByRole("button", {
        name: "actie.verwijderen",
      }),
    );

    expect(
      within(mail).queryByRole("row", { name: /test2@example.com/ }),
    ).not.toBeInTheDocument();
    expect(afzenderRow(mail, "test@example.com")).toBeVisible();
  });
});
