/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect, inject } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import { Moment } from "moment";
import { firstValueFrom } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { ZacQueryClient } from "../../shared/http/zac-query-client";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-afhandelen-dialog.component.html",
  styleUrls: ["./zaak-afhandelen-dialog.component.less"],
})
export class ZaakAfhandelenDialogComponent {
  private readonly dialogRef = inject(
    MatDialogRef<ZaakAfhandelenDialogComponent>,
  );
  public readonly data = inject(MAT_DIALOG_DATA) as {
    zaak: GeneratedType<"RestZaak">;
    planItem?: GeneratedType<"RESTPlanItem">;
  };
  private readonly formBuilder = inject(FormBuilder);
  private readonly zakenService = inject(ZakenService);
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly klantenService = inject(KlantenService);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  private sendMailDefault: boolean;

  formGroup = this.formBuilder.group({
    resultaattype:
      this.formBuilder.control<GeneratedType<"RestResultaattype"> | null>(null),
    toelichting: this.formBuilder.control<string>(""),
    sendMail: this.formBuilder.control<boolean>(false),
    verzender:
      this.formBuilder.control<GeneratedType<"RestZaakAfzender"> | null>(null),
    ontvanger: this.formBuilder.control<string>("", [CustomValidators.email]),
    brondatumEigenschap: this.formBuilder.control<Moment | null>(null),
  });

  protected readonly resultaattypesQuery = injectQuery(() => ({
    queryKey: ["resultaattypes", this.data.zaak.zaaktype.uuid],
    queryFn: () =>
      firstValueFrom(
        this.zakenService.listResultaattypes(this.data.zaak.zaaktype.uuid),
      ),
  }));

  protected readonly afzendersQuery = injectQuery(() => ({
    queryKey: ["afzenders", this.data.zaak.uuid],
    queryFn: () =>
      firstValueFrom(
        this.zakenService.listAfzendersVoorZaak(this.data.zaak.uuid),
      ),
  }));

  protected readonly mailtemplateQuery = injectQuery(() => ({
    queryKey: ["mailtemplate", this.data.zaak.uuid],
    queryFn: () =>
      firstValueFrom(
        this.mailtemplateService.findMailtemplate(
          "ZAAK_AFGEHANDELD",
          this.data.zaak.uuid,
        ),
      ),
  }));

  protected readonly initiatorEmailQuery = injectQuery(() => {
    const bsn = this.data.zaak.initiatorIdentificatie?.bsnNummer;
    if (!bsn) {
      return { queryKey: [], queryFn: () => Promise.resolve(null) };
    }
    return {
      queryKey: ["initiatorEmail", bsn],
      queryFn: () =>
        firstValueFrom(this.klantenService.getContactDetailsForPerson(bsn)),
    };
  });

  protected readonly afsluitenMutation = injectMutation(() => ({
    ...this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/afsluiten", {
      path: { uuid: this.data.zaak.uuid },
    }),
    onSuccess: () => this.dialogRef.close(true),
    onError: (error) => {
      this.foutAfhandelingService.foutAfhandelen(error);
      this.dialogRef.close(false);
    },
  }));

  protected readonly planItemAfhandelenMutation = injectMutation(() => ({
    ...this.zacQueryClient.POST("/rest/planitems/doUserEventListenerPlanItem"),
    onSuccess: () => this.dialogRef.close(true),
    onError: (error) => {
      this.foutAfhandelingService.foutAfhandelen(error);
      this.dialogRef.close(false);
    },
  }));

  constructor() {
    effect(() => {
      const afzenders = this.afzendersQuery.data();
      this.formGroup.controls.verzender.setValue(
        afzenders?.find((afzender) => afzender.defaultMail) ?? null,
      );
    });

    const zaakafhandelparameters =
      this.data.zaak.zaaktype.zaakafhandelparameters;
    this.sendMailDefault =
      zaakafhandelparameters?.afrondenMail === "BESCHIKBAAR_AAN";

    if (!this.data.zaak.resultaat) {
      this.formGroup.controls.resultaattype.addValidators(Validators.required);
    }
    if (this.sendMailDefault && this.data.planItem) {
      this.formGroup.controls.sendMail.setValue(true);
      this.formGroup.controls.verzender.addValidators(Validators.required);
      this.formGroup.controls.ontvanger.addValidators([
        Validators.required,
        CustomValidators.email,
      ]);
    }

    this.formGroup.controls.sendMail.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (value) {
          this.formGroup.controls.verzender.setValidators([
            Validators.required,
          ]);
          this.formGroup.controls.ontvanger.setValidators([
            Validators.required,
            CustomValidators.email,
          ]);
        } else {
          this.formGroup.controls.verzender.clearValidators();
          this.formGroup.controls.ontvanger.clearValidators();
        }
        this.formGroup.controls.verzender.updateValueAndValidity();
        this.formGroup.controls.ontvanger.updateValueAndValidity();
      });

    this.formGroup.controls.resultaattype.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (value?.besluitVerplicht && !this.data.zaak.besluiten?.length) {
          this.formGroup.controls.toelichting.disable();
          this.formGroup.controls.sendMail.disable();
          this.formGroup.controls.verzender.disable();
          this.formGroup.controls.ontvanger.disable();
        } else {
          this.formGroup.controls.toelichting.enable();
          this.formGroup.controls.sendMail.enable();
          this.formGroup.controls.verzender.enable();
          this.formGroup.controls.ontvanger.enable();
        }

        if (value?.datumKenmerkVerplicht) {
          this.formGroup.controls.brondatumEigenschap.addValidators([
            Validators.required,
          ]);
        } else {
          this.formGroup.controls.brondatumEigenschap.removeValidators([
            Validators.required,
          ]);
        }
        this.formGroup.controls.brondatumEigenschap.updateValueAndValidity();
      });
  }

  protected close() {
    this.dialogRef.close();
  }

  protected afhandelen() {
    this.dialogRef.disableClose = true;
    if (!this.data.planItem) {
      this.afsluiten();
      return;
    }

    this.planItemAfhandelen(this.data.planItem);
  }

  private afsluiten() {
    const { value } = this.formGroup;
    this.afsluitenMutation.mutate({
      reden: value.toelichting,
      resultaattypeUuid: value.resultaattype!.id,
      brondatumEigenschap: value.brondatumEigenschap?.toISOString(),
    });
  }

  private planItemAfhandelen(planItem: GeneratedType<"RESTPlanItem">) {
    const { value } = this.formGroup;
    const mailtemplate = this.mailtemplateQuery.data();

    const restMailGegevens =
      value.sendMail && mailtemplate
        ? ({
            verzender: value.verzender?.mail,
            replyTo: value.verzender?.replyTo,
            ontvanger: value.ontvanger,
            onderwerp: mailtemplate.onderwerp,
            body: mailtemplate.body,
            createDocumentFromMail: true,
          } satisfies GeneratedType<"RESTMailGegevens">)
        : undefined;

    this.planItemAfhandelenMutation.mutate({
      actie: "ZAAK_AFHANDELEN",
      planItemInstanceId: planItem.id,
      zaakUuid: this.data.zaak.uuid,
      resultaattypeUuid:
        this.data.zaak.resultaat?.resultaattype?.id ?? value.resultaattype?.id,
      resultaatToelichting: value.toelichting,
      restMailGegevens,
      brondatumEigenschap: value.brondatumEigenschap?.toISOString(),
    });
  }

  protected setInitiatorEmail() {
    const email = this.initiatorEmailQuery.data()?.emailadres;
    this.formGroup.controls.ontvanger.setValue(email ?? null);
  }

  protected openBesluitVastleggen() {
    this.dialogRef.close("openBesluitVastleggen");
  }
}
