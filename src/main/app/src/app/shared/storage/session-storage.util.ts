/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../utils/generated-types";

export type WerklijstZoekParameter =
  `${GeneratedType<"Werklijst">}_ZOEKPARAMETERS`;

export class SessionStorageUtil {
  static getItem<ANY>(key: string, defaultValue?: ANY): ANY {
    let item = JSON.parse(String(sessionStorage.getItem(key)));
    if (defaultValue && !item) {
      // Kopieren om referentie te breken
      item = JSON.parse(JSON.stringify(defaultValue));
      this.setItem(key, item);
    }
    return item;
  }

  static setItem<ANY>(key: string, value: ANY) {
    sessionStorage.setItem(key, JSON.stringify(value));
    return value;
  }

  static removeItem(key: string) {
    sessionStorage.removeItem(key);
  }

  static clearSessionStorage() {
    sessionStorage.clear();
  }
}
