/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LowerCasePipe, NgIf } from "@angular/common";
import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
} from "@angular/core";
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
export class ZaakDetailsAlgemeenTabComponent implements OnChanges {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input() zaakOpschorting?: GeneratedType<"RESTZaakOpschorting">;

  @Output() editCaseDetails = new EventEmitter<void>();

  protected dateFieldIconMap = new Map<string, TextIcon>();

  private readonly translate = inject(TranslateService);

  ngOnChanges() {
    this.setDateFieldIconSet();
  }

  protected zaakDetailFields(): ZaakDetailField[] {
    const bronArchiefprocedure =
      this.zaak.resultaat?.resultaattype?.bronArchiefprocedure;

    const fields: ZaakDetailField[] = [
      {
        label: "status",
        value: this.zaak.status?.naam ?? null,
      },
      {
        label: "registratiedatum",
        value: this.zaak.registratiedatum ?? null,
        format: "date",
      },
      {
        label: "resultaat",
        value: this.zaak.resultaat?.resultaattype?.naam ?? null,
      },
      {
        show: Boolean(this.zaak.einddatum),
        label: "einddatum",
        value: this.zaak.einddatum ?? null,
        format: "date",
      },
      {
        show: Boolean(this.zaak.startdatumBewaartermijn),
        label: "startdatumBewaartermijn",
        value: this.zaak.startdatumBewaartermijn ?? null,
        format: "date",
      },
      {
        show: Boolean(bronArchiefprocedure?.afleidingswijze),
        label: "afleidingswijzeBrondatum",
        value: this.afleidingswijzeBrondatumValue(
          bronArchiefprocedure?.afleidingswijze,
        ),
      },
      {
        show: this.zaak.archiefNominatie === "VERNIETIGEN",
        label: `archiefNominatie.datum.${this.zaak.archiefNominatie}`,
        value: this.zaak.archiefActiedatum ?? null,
        format: "date",
      },
      {
        show: this.zaak.archiefNominatie === "BLIJVEND_BEWAREN",
        label: "archiefNominatie",
        value: String(
          this.translate.instant(
            `archiefNominatie.${this.zaak.archiefNominatie}`,
          ),
        ),
      },
    ];

    return fields.filter(({ show }) => show !== false);
  }

  private afleidingswijzeBrondatumValue(
    afleidingswijze?: GeneratedType<"AfleidingswijzeEnum"> | null,
  ) {
    if (!afleidingswijze) return null;
    // Workaround: the value returned from the backend is lowercase and generated TypeScript types expect uppercase.
    const afleidingswijzeBrondatum: string = afleidingswijze.toUpperCase();

    if (afleidingswijzeBrondatum === "EIGENSCHAP") {
      return (
        this.zaak.resultaat?.resultaattype?.datumKenmerkOmschrijving ?? null
      );
    }

    return String(
      this.translate.instant(
        `afleidingswijzeBrondatum.${afleidingswijzeBrondatum}`,
      ),
    );
  }

  private setDateFieldIconSet() {
    this.dateFieldIconMap.set(
      "einddatumGepland",
      new TextIcon(
        (control: FormControl) => {
          return DateConditionals.isExceeded(
            control.value,
            this.zaak.einddatum,
          );
        },
        "report_problem",
        "warningVerlopen_icon",
        this.zaak.einddatum
          ? "msg.einddatum.overschreden"
          : "msg.datum.overschreden",
        "warning",
      ),
    );

    this.dateFieldIconMap.set(
      "uiterlijkeEinddatumAfdoening",
      new TextIcon(
        (control: FormControl) => {
          return DateConditionals.isExceeded(
            control.value,
            this.zaak.einddatum,
          );
        },
        "report_problem",
        "errorVerlopen_icon",
        this.zaak.einddatum
          ? "msg.einddatum.overschreden"
          : "msg.datum.overschreden",
        "error",
      ),
    );
  }
}
