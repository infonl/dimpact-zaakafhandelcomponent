/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class OrderUtil {
  static orderBy<T>(sortKey?: keyof T) {
    return (a: T, b: T): number => {
      const valueA = sortKey ? a[sortKey] : a;
      const valueB = sortKey ? b[sortKey] : b;

      return typeof valueA === "number" && typeof valueB === "number"
        ? valueA - valueB
        : String(valueA).localeCompare(String(valueB));
    };
  }

  static orderAsIs() {
    // Array sort is stable since node.js 12
    return (): number => {
      return 0;
    };
  }
}
