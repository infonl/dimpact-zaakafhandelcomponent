/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { OnChanges, SimpleChange, SimpleChanges } from "@angular/core";

type DeepPartial<T> = T extends null | undefined
  ? T
  : T extends object
    ? { [P in keyof T]?: DeepPartial<T[P]> }
    : T;

export const fromPartial = <T,>(partial: NoInfer<DeepPartial<T>>): T =>
  partial as T;

export function updateComponentInputs<T extends OnChanges>(
  component: T,
  changes: Partial<T>,
  firstChange = false,
) {
  const simpleChanges: SimpleChanges = {};

  Object.keys(changes).forEach((changeKey) => {
    const typedKey = changeKey as keyof T;
    const value = changes[typedKey] as T[keyof T];
    component[typedKey] = value;
    simpleChanges[changeKey] = new SimpleChange(null, value, firstChange);
  });
  component.ngOnChanges(simpleChanges);
}

export function createMutationOptions<TData, TVariables = void>(data: TData) {
  const mutationFn = jest
    .fn<Promise<TData>, [TVariables]>()
    .mockResolvedValue(data);

  return {
    mutationKey: ["test-mutation"],
    mutationFn,
  };
}
