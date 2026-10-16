/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input } from "@angular/core";
import { MatIcon } from "@angular/material/icon";
import { MatTooltip } from "@angular/material/tooltip";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
  selector: "zac-zaakspecifiek-geautoriseerd-icon",
  templateUrl: "./zaakspecifiek-geautoriseerd-icon.component.html",
  styleUrls: ["./zaakspecifiek-geautoriseerd-icon.component.less"],
  imports: [MatIcon, MatTooltip, TranslatePipe],
})
export class ZaakspecifiekGeautoriseerdIconComponent {
  readonly isZaakspecifiekGeautoriseerd = input.required<
    boolean | null | undefined
  >();
}
