/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class ExpandableTableData<T> {
  data: T;
  expanded = false;

  constructor(data: T) {
    this.data = data;
  }
}
