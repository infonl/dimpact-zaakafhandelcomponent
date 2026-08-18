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
describe("Beeindiging form step", () => {
  const user = userEvent.setup();

  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "case-1",
    naam: "Case Definition 1",
  });

  const reden = fromPartial<GeneratedType<"RestZaakbeeindigReden">>({
    id: "1",
    naam: "Reden 1",
  });

  const resultaattype = fromPartial<GeneratedType<"RestResultaattype">>({
    id: "resultaat-1",
    naam: "Afgehandeld",
  });

  const zaakafhandelParameters = fromPartial<
    GeneratedType<"RestZaaktypeConfiguration">
  >({
    caseDefinition,
    defaultGroepId: "test-group-id",
    defaultBehandelaarId: "test-user-id",
    zaaktype: { uuid: "test-uuid" },
    zaakNietOntvankelijkResultaattype: resultaattype,
    zaakAfzenders: [
      {
        speciaal: false,
        defaultMail: true,
        mail: "test@example.com",
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
            listZaakbeeindigRedenen: () => of([reden]),
            listResultaattypes: () => of([resultaattype]),
            updateZaakafhandelparameters: () =>
              createMutationOptions(zaakafhandelParameters),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () => of([]),
            listAfzenders: () => of(["other@example.com"]),
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

    await user.click(screen.getByRole("tab", { name: /gegevens.beeindiging/ }));

    return screen.getByRole("tabpanel", { name: /gegevens.beeindiging/ });
  }

  async function chooseResultaat(row: HTMLElement, naam: string) {
    // the select of a table cell has no form field around it, so the click has to land inside the select
    await user.click(within(row).getByText("resultaat.-kies-"));
    await user.click(screen.getByRole("option", { name: naam }));
  }

  function redenRow(beeindiging: HTMLElement, naam: string) {
    return within(beeindiging).getByRole("row", { name: new RegExp(naam) });
  }

  it("lists the zaak niet ontvankelijk reden next to the redenen of the zaaktype", async () => {
    const beeindiging = await setup();

    expect(redenRow(beeindiging, "zaakIsNietOntvankelijk")).toBeVisible();
    expect(redenRow(beeindiging, "Reden 1")).toBeVisible();
  });

  it("always ends a zaak that is niet ontvankelijk", async () => {
    const beeindiging = await setup();

    const checkbox = within(
      redenRow(beeindiging, "zaakIsNietOntvankelijk"),
    ).getByRole("checkbox");
    expect(checkbox).toBeChecked();
    expect(checkbox).toBeDisabled();
  });

  it("leaves a reden of the zaaktype unchosen", async () => {
    const beeindiging = await setup();

    expect(
      within(redenRow(beeindiging, "Reden 1")).getByRole("checkbox"),
    ).not.toBeChecked();
  });

  it("demands a resultaat for a reden that is chosen", async () => {
    const beeindiging = await setup();
    const opslaan = within(beeindiging).getByRole("button", {
      name: "actie.opslaan",
    });
    const row = redenRow(beeindiging, "Reden 1");

    await user.click(within(row).getByRole("checkbox"));

    expect(within(row).getByRole("checkbox")).toBeChecked();
    expect(opslaan).toBeDisabled();

    await chooseResultaat(row, "Afgehandeld");

    expect(opslaan).toBeEnabled();
  });

  it("drops the demand for a resultaat when the reden is unchosen again", async () => {
    const beeindiging = await setup();
    const opslaan = within(beeindiging).getByRole("button", {
      name: "actie.opslaan",
    });
    const row = redenRow(beeindiging, "Reden 1");

    await user.click(within(row).getByRole("checkbox"));
    await user.click(within(row).getByRole("checkbox"));

    expect(within(row).getByRole("checkbox")).not.toBeChecked();
    expect(opslaan).toBeEnabled();
  });
});
