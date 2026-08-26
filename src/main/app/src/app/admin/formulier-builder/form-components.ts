/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export type FormioComponent = {
  key?: string;
  type?: string;
  attributes?: Record<string, string>;
  properties?: Record<string, string>;
  components?: unknown;
  columns?: unknown;
  /** A table nests its cells here, but a textarea uses the same name for its height in lines. */
  rows?: unknown;
  refreshOn?: unknown;
  widget?: unknown;
};

function toComponentArray(value: unknown): FormioComponent[] {
  return Array.isArray(value) ? (value as FormioComponent[]) : [];
}

function nestedComponentsOf(component: FormioComponent): FormioComponent[] {
  const cells = [
    ...toComponentArray(component.columns),
    ...toComponentArray(
      Array.isArray(component.rows) ? component.rows.flat() : [],
    ),
  ];

  return [
    ...toComponentArray(component.components),
    ...cells.flatMap((cell) => toComponentArray(cell.components)),
  ];
}

/** Returns the components themselves, not copies, so a caller may edit them in place. */
export function flattenComponents(components: unknown): FormioComponent[] {
  return toComponentArray(components).flatMap((component) => [
    component,
    ...flattenComponents(nestedComponentsOf(component)),
  ]);
}
