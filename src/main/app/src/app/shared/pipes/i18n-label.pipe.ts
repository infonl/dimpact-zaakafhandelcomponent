/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Pipe, PipeTransform } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { toI18nKey } from "../utils/i18n-key";

// Impure, like the translate pipe, so the label updates once the translations have loaded.
@Pipe({ name: "i18nLabel", standalone: true, pure: false })
export class I18nLabelPipe implements PipeTransform {
  private readonly translateService = inject(TranslateService);

  transform(label?: string | null) {
    if (!label) return "";

    const key = toI18nKey(label);
    const translation = this.translateService.instant(key);
    return translation === key ? label : translation;
  }
}
