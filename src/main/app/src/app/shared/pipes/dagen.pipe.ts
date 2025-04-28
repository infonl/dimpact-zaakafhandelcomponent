/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";

@Pipe({ name: "dagen" })
export class DagenPipe implements PipeTransform {
  constructor(private translate: TranslateService) {}

  transform(value: unknown) {
    if (!value) return null;

    const today = moment().startOf("day");
    const verloopt = moment(value).startOf("day");
    const daysDifference = verloopt.diff(today, "days");

    switch (Math.abs(daysDifference)) {
      case 0:
        return this.translate.instant("verloopt.vandaag");
      case 1:
        return this.translate.instant(
          daysDifference > 0 ? "verloopt.over.dag" : "verloopt.verleden.dag",
        );
      default:
        return this.translate.instant(
          daysDifference > 0
            ? "verloopt.over.dagen"
            : "verloopt.verleden.dagen",
          {
            dagen: Math.abs(daysDifference),
          },
        );
    }
  }
}
