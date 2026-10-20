/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, waitFor, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../../../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import {
  createMailForm,
  MailForm,
  toMailtemplateKoppelingen,
  toZaakAfzenders,
} from "./mail-form";
import { ParametersStepMailComponent } from "./parameters-step-mail.component";

const zaakAlgemeenTemplate = fromPartial<GeneratedType<"RestMailtemplate">>({
  id: 4,
  mail: "ZAAK_ALGEMEEN",
  mailTemplateNaam: "Zaak algemeen",
});

const ontvangstbevestigingTemplate = fromPartial<
  GeneratedType<"RestMailtemplate">
>({
  id: 5,
  mail: "TAAK_ONTVANGSTBEVESTIGING",
  mailTemplateNaam: "Ontvangstbevestiging",
});

describe(ParametersStepMailComponent.name, () => {
  const user = userEvent.setup();

  async function setup(
    zaakAfzenders: GeneratedType<"RestZaakAfzender">[] = [
      { mail: "test@example.com", isSpeciaal: false, isDefaultMail: false },
      { mail: "test2@example.com", isSpeciaal: false, isDefaultMail: false },
    ],
    settingsNotInUse: (keyof MailForm["controls"])[] = [],
  ) {
    const form = createMailForm({ zaakAfzenders, mailtemplateKoppelingen: [] });
    settingsNotInUse.forEach((setting) => form.controls[setting].disable());

    await render(ParametersStepMailComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: { form },
      providers: [
        {
          provide: MailtemplateBeheerService,
          useValue: fromPartial<MailtemplateBeheerService>({
            listKoppelbareMailtemplates: () =>
              ({
                ...createQueryOptions([
                  zaakAlgemeenTemplate,
                  ontvangstbevestigingTemplate,
                ]),
                queryKey: ["koppelbareMailtemplates"],
              }) as never,
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listAfzenders: () => ({
              ...createQueryOptions(["other@example.com", "test@example.com"]),
              queryKey: ["afzenders"],
            }),
          }),
        },
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listReplyTos: () => ({
              ...createQueryOptions([
                { mail: "reply1@example.com", isSpeciaal: false },
                { mail: "GEMEENTE", isSpeciaal: true },
              ]),
              queryKey: ["replyTos"],
            }),
          }),
        },
      ],
    });

    return form;
  }

  function afzenderRow(afzender: string) {
    return screen.getByRole("row", { name: new RegExp(afzender) });
  }

  const ontvangstbevestiging = () =>
    screen.getByRole("switch", { name: /ontvangstbevestiging/i });

  describe("status mails", () => {
    it("falls back to BESCHIKBAAR_UIT for both status mails", async () => {
      await setup();

      await waitFor(() =>
        expect(
          screen.getByRole("combobox", { name: /^statusmail.type.intake$/i }),
        ).toHaveTextContent("statusmail.optie.beschikbaar-uit"),
      );
      expect(
        screen.getByRole("combobox", { name: /^statusmail.type.afronden$/i }),
      ).toHaveTextContent("statusmail.optie.beschikbaar-uit");
    });
  });

  describe("for a process definition without status mails, that sends the confirmation of receipt itself", () => {
    const setupWithoutThoseSettings = () =>
      setup(
        [{ mail: "test@example.com", isSpeciaal: false, isDefaultMail: true }],
        ["intakeMail", "afrondenMail", "automaticEmailConfirmation"],
      );

    it("offers the zaakafzenders and the mailtemplate koppelingen", async () => {
      await setupWithoutThoseSettings();

      expect(afzenderRow("test@example.com")).toBeVisible();
      expect(
        screen.getByRole("button", { name: /mail.zaak-algemeen/ }),
      ).toBeVisible();
    });

    it("shows no status mail options and no confirmation email fields", async () => {
      await setupWithoutThoseSettings();

      expect(
        screen.queryByRole("combobox", { name: /^statusmail.type.intake$/i }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("combobox", { name: /^statusmail.type.afronden$/i }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("switch", { name: /ontvangstbevestiging/i }),
      ).not.toBeInTheDocument();
    });
  });

  describe("zaakafzenders", () => {
    it("lists the afzenders of the zaaktype", async () => {
      await setup();

      expect(afzenderRow("test@example.com")).toBeVisible();
      expect(afzenderRow("test2@example.com")).toBeVisible();
    });

    it("demands an afzender that is marked as the default one", async () => {
      const form = await setup();

      expect(screen.getByText("validators.required")).toBeVisible();
      expect(form.valid).toBe(false);
    });

    it("accepts the afzender that is marked as the default one", async () => {
      const form = await setup();

      await user.click(
        within(afzenderRow("test@example.com")).getByRole("radio"),
      );

      expect(
        within(afzenderRow("test@example.com")).getByRole("radio"),
      ).toBeChecked();
      expect(screen.queryByText("validators.required")).not.toBeInTheDocument();
      expect(form.valid).toBe(true);
    });

    it("offers only the afzenders that the zaaktype does not have yet", async () => {
      await setup();

      await user.click(
        await screen.findByRole("button", { name: "Afzender toevoegen" }),
      );

      expect(
        screen.getAllByRole("menuitem").map((item) => item.textContent?.trim()),
      ).toEqual(["other@example.com"]);
    });

    it("adds an afzender that is offered in the menu", async () => {
      const form = await setup();

      await user.click(
        await screen.findByRole("button", { name: "Afzender toevoegen" }),
      );
      await user.click(
        screen.getByRole("menuitem", { name: "other@example.com" }),
      );

      expect(afzenderRow("other@example.com")).toBeVisible();
      expect(toZaakAfzenders(form).map(({ mail }) => mail)).toContain(
        "other@example.com",
      );
    });

    it("stores the reply-to address that is chosen for an afzender", async () => {
      const form = await setup();

      await user.click(
        within(afzenderRow("test@example.com")).getByRole("combobox", {
          name: /^mail.antwoord$/i,
        }),
      );
      await user.click(
        await screen.findByRole("option", { name: "reply1@example.com" }),
      );

      expect(toZaakAfzenders(form)).toContainEqual(
        expect.objectContaining({
          mail: "test@example.com",
          replyTo: "reply1@example.com",
        }),
      );
    });

    it("removes an afzender from the list", async () => {
      const form = await setup();

      await user.click(
        within(afzenderRow("test2@example.com")).getByRole("button", {
          name: "actie.verwijderen",
        }),
      );

      expect(
        screen.queryByRole("row", { name: /test2@example.com/ }),
      ).not.toBeInTheDocument();
      expect(toZaakAfzenders(form).map(({ mail }) => mail)).toEqual([
        "test@example.com",
      ]);
    });
  });

  describe("mailtemplate koppelingen", () => {
    it("stores the mail template that is chosen for a mail", async () => {
      const form = await setup();

      await user.click(
        screen.getByRole("button", { name: /mail.zaak-algemeen/ }),
      );
      await user.click(
        within(
          screen.getByRole("region", { name: /mail.zaak-algemeen/ }),
        ).getByRole("combobox", { name: /^title.mailtemplate$/i }),
      );
      await user.click(
        await screen.findByRole("option", { name: "Zaak algemeen" }),
      );

      expect(toMailtemplateKoppelingen(form)).toEqual([
        { mailtemplate: zaakAlgemeenTemplate },
      ]);
    });
  });

  describe("automatische ontvangstbevestiging", () => {
    it("starts switched off without asking for a template or a sender", async () => {
      await setup();

      expect(ontvangstbevestiging()).not.toBeChecked();
      expect(
        screen.queryByRole("group", {
          name: "gegevens.mail.ontvangstbevestiging",
        }),
      ).not.toBeInTheDocument();
    });

    it("demands a template and a sender once switched on", async () => {
      const form = await setup([
        { mail: "test@example.com", isSpeciaal: false, isDefaultMail: true },
      ]);

      await user.click(ontvangstbevestiging());

      expect(
        within(
          screen.getByRole("group", {
            name: "gegevens.mail.ontvangstbevestiging",
          }),
        ).getByRole("combobox", { name: /mail.antwoord/i }),
      ).toBeVisible();
      expect(form.valid).toBe(false);
    });

    it("drops the demand when switched back off", async () => {
      const form = await setup([
        { mail: "test@example.com", isSpeciaal: false, isDefaultMail: true },
      ]);

      await user.click(ontvangstbevestiging());
      await user.click(ontvangstbevestiging());

      await waitFor(() => expect(form.valid).toBe(true));
    });
  });
});
