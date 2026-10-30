/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { computed, effect, Signal, signal } from "@angular/core";
import { AbstractControl } from "@angular/forms";

/**
 * Reads the state of a reactive form control as signals, so an OnPush template
 * also shows a value, validity or disabled state that another component or code
 * changed. Call it in an injection context.
 */
export function trackControl<Control extends AbstractControl>(
  control: Signal<Control | null | undefined>,
) {
  const changes = signal(0);

  effect((onCleanup) => {
    const subscription = control()?.events.subscribe(() =>
      changes.update((count) => count + 1),
    );
    onCleanup(() => subscription?.unsubscribe());
  });

  return <Result>(read: (control: Control) => Result) =>
    computed(() => {
      changes();
      const current = control();
      return current ? read(current) : undefined;
    });
}
