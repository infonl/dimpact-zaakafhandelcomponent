/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { MatIcon } from "@angular/material/icon";
import { MatTooltip } from "@angular/material/tooltip";
import { TranslatePipe } from "@ngx-translate/core";
import { EmptyPipe } from "../pipes/empty.pipe";

@Component({
  selector: "zac-zaak-identificatie",
  templateUrl: "./zaak-identificatie.component.html",
  styleUrls: ["./zaak-identificatie.component.less"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [MatIcon, MatTooltip, TranslatePipe, EmptyPipe],
})
export class ZaakIdentificatieComponent {
  readonly identificatie = input.required<string | null | undefined>();
  readonly isZaakspecifiekGeautoriseerd = input.required<
    boolean | null | undefined
  >();
}
