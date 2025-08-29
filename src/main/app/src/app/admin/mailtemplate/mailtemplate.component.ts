/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { ActivatedRoute, Router } from "@angular/router";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { mailSelectList } from "../model/mail-utils";

@Component({
  templateUrl: "./mailtemplate.component.html",
  styleUrls: ["./mailtemplate.component.less"],
})
export class MailtemplateComponent
  extends AdminComponent
  implements OnInit, AfterViewInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  protected form = this.formBuilder.group({
    mailTemplateNaam: this.formBuilder.control("", [Validators.required]),
    mail: this.formBuilder.control<{
      label: string;
      value: GeneratedType<"Mail">;
    } | null>(null, [Validators.required]),
    onderwerp: this.formBuilder.control("", [
      Validators.required,
      Validators.maxLength(100),
    ]),
    body: this.formBuilder.control("", [Validators.required]),
    defaultMailtemplate: this.formBuilder.control(false, []),
  });

  protected variabelen: string[] = [];
  private mailTemplate?: GeneratedType<"RESTMailtemplate">;

  protected readonly mailTemplates = mailSelectList();

  constructor(
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private mailTemplateBeheerService: MailtemplateBeheerService,
    private route: ActivatedRoute,
    private router: Router,
    private readonly formBuilder: FormBuilder,
  ) {
    super(utilService, configuratieService);

    this.form.controls.mail.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) return;
        this.mailTemplateBeheerService
          .ophalenVariabelenVoorMail(value.value)
          .subscribe((variabelen) => {
            this.variabelen = variabelen;
          });
      });
  }

  ngOnInit() {
    this.route.data.subscribe((data) => {
      this.mailTemplate = data.template ?? {};

      this.setupMenu("title.mailtemplate");
      this.form.patchValue({
        ...this.mailTemplate,
        mail: this.mailTemplate?.mail
          ? {
              label: "mail." + this.mailTemplate?.mail,
              value: this.mailTemplate?.mail as GeneratedType<"Mail">,
            }
          : null,
      });

      if (!this.mailTemplate?.mail) return;

      this.mailTemplates.push({
        label: "mail." + this.mailTemplate?.mail,
        // @ts-expect-error mail type is incorrect, but we know it is correct
        value: this.mailTemplate?.mail,
      });
      this.form.controls.mail.disable();
    });
  }

  saveMailtemplate() {
    const data = this.form.getRawValue();
    const templateData = {
      mail: data.mail!.value!,
      mailTemplateNaam: data.mailTemplateNaam ?? "",
      onderwerp: data.onderwerp ?? "",
      body: data.body ?? "",
      defaultMailtemplate: data.defaultMailtemplate ?? false,
    };

    const operation = this.mailTemplate?.id
      ? this.mailTemplateBeheerService.updateMailtemplate(
          this.mailTemplate.id,
          templateData,
        )
      : this.mailTemplateBeheerService.createMailtemplate(templateData);

    operation.subscribe(() => {
      this.utilService.openSnackbar("msg.mailtemplate.opgeslagen");
      void this.router.navigate(["/admin/mailtemplates"]);
    });
  }

  cancel() {
    void this.router.navigate(["/admin/mailtemplates"]);
  }
}
