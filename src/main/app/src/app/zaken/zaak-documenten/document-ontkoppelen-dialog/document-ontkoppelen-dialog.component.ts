/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MAT_DIALOG_DATA } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { ZacDialogBody } from "../../../shared/dialog/dialog-body/dialog-body.component";
import { ZacTextarea } from "../../../shared/form/textarea/textarea";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../zaken.service";

export type DocumentOntkoppelenDialogData = {
  zaakUuid: string;
  zaakIdentificatie: string;
  document: GeneratedType<"RestEnkelvoudigInformatieobject">;
};

@Component({
  selector: "zac-document-ontkoppelen-dialog",
  templateUrl: "./document-ontkoppelen-dialog.component.html",
  standalone: true,
  imports: [ReactiveFormsModule, ZacDialogBody, ZacTextarea],
})
export class DocumentOntkoppelenDialogComponent {
  private readonly data =
    inject<DocumentOntkoppelenDialogData>(MAT_DIALOG_DATA);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly zakenService = inject(ZakenService);
  private readonly translateService = inject(TranslateService);

  protected readonly form = new FormGroup({
    reden: new FormControl<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
    ]),
  });

  private readonly andereZakenQuery = injectQuery(() => ({
    queryKey: ["informatieobject-zaakidentificaties", this.data.document.uuid],
    queryFn: () =>
      lastValueFrom(
        this.informatieObjectenService.listZaakIdentificatiesForInformatieobject(
          this.data.document.uuid!,
        ),
      ),
  }));

  protected readonly melding = computed(() => {
    const andereZaken = (this.andereZakenQuery.data() ?? [])
      .filter((identificatie) => identificatie !== this.data.zaakIdentificatie)
      .join(", ");
    return andereZaken
      ? this.translateService.instant(
          "msg.document.ontkoppelen.meerdere.zaken.bevestigen",
          { zaken: andereZaken, document: this.data.document.titel },
        )
      : this.translateService.instant("msg.document.ontkoppelen.bevestigen", {
          document: this.data.document.titel,
        });
  });

  protected readonly ontkoppel = () =>
    this.zakenService.ontkoppelInformatieObject({
      zaakUUID: this.data.zaakUuid,
      documentUUID: this.data.document.uuid!,
      reden: this.form.getRawValue().reden ?? "",
    });
}
