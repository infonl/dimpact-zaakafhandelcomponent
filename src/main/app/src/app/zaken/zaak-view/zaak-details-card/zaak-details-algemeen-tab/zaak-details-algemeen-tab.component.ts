/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LowerCasePipe, NgIf } from "@angular/common";
import { Component, computed, inject, input, output } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatIconButton } from "@angular/material/button";
import { MatDivider } from "@angular/material/divider";
import { MatIcon } from "@angular/material/icon";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { TextIcon } from "../../../../shared/edit/text-icon";
import { DatumPipe } from "../../../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../../../shared/pipes/empty.pipe";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { StaticTextComponent } from "../../../../shared/static-text/static-text.component";
import { GeneratedType } from "../../../../shared/utils/generated-types";

type ZaakDetailField = {
  /** omitting this renders the field; only an explicit `false` hides it */
  show?: boolean;
  label: string;
  value: string | null;
  format?: "date";
};

@Component({
  selector: "zac-zaak-details-algemeen-tab",
  templateUrl: "./zaak-details-algemeen-tab.component.html",
  styleUrls: ["./zaak-details-algemeen-tab.component.less"],
  styles: [":host { display: block; }"],
  standalone: true,
  imports: [
    NgIf,
    LowerCasePipe,
    MatDivider,
    MatIcon,
    MatIconButton,
    StaticTextComponent,
    DatumPipe,
    EmptyPipe,
    TranslatePipe,
    VertrouwelijkaanduidingToTranslationKeyPipe,
  ],
})
export class ZaakDetailsAlgemeenTabComponent {
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly zaakOpschorting = input<GeneratedType<"RESTZaakOpschorting">>();

  readonly editCaseDetails = output<void>();

  private readonly translate = inject(TranslateService);

  protected readonly dateFieldIconMap = computed(() =>
    this.buildDateFieldIconSet(this.zaak().einddatum),
  );

  protected readonly zaakDetailFields = computed<ZaakDetailField[]>(() => {
    const zaak = this.zaak();
    const bronArchiefprocedure =
      zaak.resultaat?.resultaattype?.bronArchiefprocedure;

    const fields: ZaakDetailField[] = [
      {
        label: "status",
        value: zaak.status?.naam ?? null,
      },
      {
        label: "registratiedatum",
        value: zaak.registratiedatum ?? null,
        format: "date",
      },
      {
        label: "resultaat",
        value: zaak.resultaat?.resultaattype?.naam ?? null,
      },
      {
        show: Boolean(zaak.einddatum),
        label: "einddatum",
        value: zaak.einddatum ?? null,
        format: "date",
      },
      {
        show: Boolean(zaak.startdatumBewaartermijn),
        label: "startdatumBewaartermijn",
        value: zaak.startdatumBewaartermijn ?? null,
        format: "date",
      },
      {
        show: Boolean(bronArchiefprocedure?.afleidingswijze),
        label: "afleidingswijzeBrondatum",
        value: this.afleidingswijzeBrondatumValue(
          zaak,
          bronArchiefprocedure?.afleidingswijze,
        ),
      },
      {
        show: zaak.archiefNominatie === "VERNIETIGEN",
        label: `archiefNominatie.datum.${zaak.archiefNominatie}`,
        value: zaak.archiefActiedatum ?? null,
        format: "date",
      },
      {
        show: zaak.archiefNominatie === "BLIJVEND_BEWAREN",
        label: "archiefNominatie",
        value: String(
          this.translate.instant(`archiefNominatie.${zaak.archiefNominatie}`),
        ),
      },
    ];

    return fields.filter(({ show }) => show !== false);
  });

  private afleidingswijzeBrondatumValue(
    zaak: GeneratedType<"RestZaak">,
    afleidingswijze?: GeneratedType<"AfleidingswijzeEnum"> | null,
  ) {
    if (!afleidingswijze) return null;
    // Workaround: the value returned from the backend is lowercase and generated TypeScript types expect uppercase.
    const afleidingswijzeBrondatum: string = afleidingswijze.toUpperCase();

    if (afleidingswijzeBrondatum === "EIGENSCHAP") {
      return zaak.resultaat?.resultaattype?.datumKenmerkOmschrijving ?? null;
    }

    return String(
      this.translate.instant(
        `afleidingswijzeBrondatum.${afleidingswijzeBrondatum}`,
      ),
    );
  }

  private buildDateFieldIconSet(einddatum?: string | null) {
    const overschredenMessage = einddatum
      ? "msg.einddatum.overschreden"
      : "msg.datum.overschreden";
    const isExceeded = (control: FormControl) =>
      DateConditionals.isExceeded(control.value, einddatum);

    return new Map<string, TextIcon>([
      [
        "einddatumGepland",
        new TextIcon(
          isExceeded,
          "report_problem",
          "warningVerlopen_icon",
          overschredenMessage,
          "warning",
        ),
      ],
      [
        "uiterlijkeEinddatumAfdoening",
        new TextIcon(
          isExceeded,
          "report_problem",
          "errorVerlopen_icon",
          overschredenMessage,
          "error",
        ),
      ],
    ]);
  }
}
