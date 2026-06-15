/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, output } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { UtilService } from "../../core/service/util.service";
import { ZacDate } from "../../shared/form/date/date";
import { ZacDocuments } from "../../shared/form/documents/documents";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { StaleTimes } from "../../shared/http/zac-query-client";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";

@Component({
  selector: "zac-informatie-verzenden",
  templateUrl: "./informatie-object-verzenden.component.html",
  styleUrls: ["./informatie-object-verzenden.component.less"],
  standalone: true,
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslateModule,
    ZacDate,
    ZacDocuments,
    ZacTextarea,
  ],
})
export class InformatieObjectVerzendenComponent {
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly utilService = inject(UtilService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly documentSent = output<void>();

  protected readonly documentenQuery = injectQuery(() => ({
    ...this.informatieObjectenService.listInformatieobjectenVoorVerzendenQuery(
      this.zaak().uuid,
    ),
    staleTime: StaleTimes.Instant,
  }));

  protected readonly form = this.formBuilder.group({
    documenten: this.formBuilder.control<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >([], [Validators.required]),
    verzenddatum: this.formBuilder.control<Moment | null>(moment(), [
      Validators.required,
    ]),
    toelichting: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(1000),
    ]),
  });

  protected readonly verzendenMutation = injectMutation(() => ({
    ...this.informatieObjectenService.verzenden(),
    onSuccess: (_, { informatieobjecten }) => {
      this.utilService.openSnackbar(
        informatieobjecten.length > 1
          ? "msg.documenten.verzenden.uitgevoerd"
          : "msg.document.verzenden.uitgevoerd",
      );
      this.documentSent.emit();
    },
    onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
  }));

  protected submit() {
    const { documenten, verzenddatum, toelichting } = this.form.value;

    this.verzendenMutation.mutate({
      zaakUuid: this.zaak().uuid,
      verzenddatum: verzenddatum!.toISOString(),
      informatieobjecten: documenten?.map(({ uuid }) => uuid!) ?? [],
      toelichting,
    });
  }
}
