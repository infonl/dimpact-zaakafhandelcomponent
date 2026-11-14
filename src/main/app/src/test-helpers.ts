/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { OnChanges, SimpleChange, SimpleChanges } from "@angular/core";

export function updateComponentInputs<T extends OnChanges>(
  component: T,
  changes: Partial<T>,
  firstChange = false,
) {
  const simpleChanges: SimpleChanges = {};

  Object.keys(changes).forEach((changeKey) => {
    component[changeKey] = changes[changeKey];
    simpleChanges[changeKey] = new SimpleChange(
      null,
      changes[changeKey],
      firstChange,
    );
  });
  component.ngOnChanges(simpleChanges);
}
