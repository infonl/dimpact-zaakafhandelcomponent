/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { computed, effect, Signal, signal, untracked } from "@angular/core";
import { AbstractControl } from "@angular/forms";

/**
 * Reads the state of a reactive form control as signals, so an OnPush template
 * also shows a value, validity or disabled state that another component or code
 * changed. Call it in an injection context.
 *
 * A change made with `emitEvent: false` sends no event; call `check()` from
 * `ngDoCheck` to pick those up as well.
 */
export function trackControl<Control extends AbstractControl>(
  control: Signal<Control | null | undefined>,
) {
  const changes = signal(0);
  let snapshot: unknown[] | undefined;

  effect((onCleanup) => {
    const subscription = control()?.events.subscribe(() =>
      changes.update((count) => count + 1),
    );
    onCleanup(() => subscription?.unsubscribe());
  });

  const read = <Result,>(read: (control: Control) => Result) =>
    computed(() => {
      changes();
      const current = control();
      return current ? read(current) : undefined;
    });

  const check = () => {
    const current = untracked(control);
    const next = [
      current?.value,
      current?.status,
      current?.errors,
      current?.pristine,
    ];
    const previous = snapshot;
    snapshot = next;
    if (!previous || next.every((part, index) => part === previous[index]))
      return;
    changes.update((count) => count + 1);
  };

  return Object.assign(read, { check });
}
