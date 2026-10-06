/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import { toI18nKey } from "../utils/i18n-key";

@Pipe({ name: "i18nKey", standalone: true })
export class I18nKeyPipe implements PipeTransform {
  transform(key?: string | null) {
    return key ? toI18nKey(key) : "";
  }
}
