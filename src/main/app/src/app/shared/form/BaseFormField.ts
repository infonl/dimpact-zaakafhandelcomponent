/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  computed,
  DoCheck,
  effect,
  inject,
  input,
  OnDestroy,
  signal,
} from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { injectIsMutating } from "@tanstack/angular-query-experimental";
import { lastValueFrom, Observable, Subject, takeUntil } from "rxjs";
import { toI18nKey } from "../utils/i18n-key";
import { trackControl } from "./control-state";
import { FormHelper } from "./helpers";

/**
 * This base class is meant to be extended by form field components.
 *
 * It should **never** be used directly in a template.
 */
@Component({
  template: "",
  host: {
    class: "zac-form-field",
    "[attr.inert]": "isSubmitting() ? '' : null",
  },
})
export class SingleInputFormField<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
    Option extends Form[Key]["value"],
  >
  implements DoCheck, OnDestroy
{
  private readonly translateService = inject(TranslateService);

  private readonly mutatingCount = injectIsMutating();

  /**
   * Blocks the field with `inert` rather than disabling its control, so the
   * control keeps its value, validity and deliberately disabled state.
   */
  protected readonly isSubmitting = computed(() => this.mutatingCount() > 0);

  protected readonly destroy$ = new Subject<void>();

  protected readonly control = computed<AbstractControl<Option | null> | null>(
    () => this.form().controls[this.key()],
  );

  public readonly form = input.required<FormGroup<Form>>();
  public readonly key = input.required<Key & string>();
  public readonly label = input<string>();
  public readonly readonly = input(false, { transform: booleanAttribute });

  protected readonly labelKey = computed(
    () => this.label() ?? toI18nKey(this.key()),
  );

  private readonly readControl = trackControl(this.control);
  protected readonly value = this.readControl((control) => control.value);
  protected readonly disabled = this.readControl((control) => control.disabled);
  private readonly controlErrors = this.readControl(
    (control) => control.errors,
  );
  private readonly hasRequiredValidator = this.readControl((control) =>
    control.hasValidator(Validators.required),
  );
  protected readonly isRequired = computed(
    () => this.hasRequiredValidator() ?? false,
  );

  constructor() {
    effect(() => {
      if (this.readonly()) this.control()?.disable({ emitEvent: false });
    });

    effect((onCleanup) => {
      const control = this.control();
      if (!control) return;

      const subscription = control.valueChanges.subscribe((value) => {
        switch (typeof value) {
          case "string":
            if (value) break;
            control.setValue(null); // Set empty strings to null to sent to backend
            break;
          case "number":
            if (!isNaN(value)) break;
            control.setValue(null); // Set NaN to null to sent to backend
            break;
          default:
          // No action needed
        }
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  protected readonly formError = computed(() => {
    return FormHelper.getErrorMessage(
      this.controlErrors() ?? null,
      this.translateService,
    );
  });

  ngDoCheck() {
    this.readControl.check();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

/**
 * This base class is meant to be extended by form field components.
 *
 * It should **never** be used directly in a template.
 */
@Component({
  template: "",
})
export class MultiInputFormField<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
  OptionDisplayValue extends keyof Option | ((option: Option) => string),
  Compare extends (a: Option, b?: Option | null) => boolean = (
    a: Option,
    b?: Option | null,
  ) => boolean,
> extends SingleInputFormField<Form, Key, Option> {
  public readonly options = input.required<
    ReadonlyArray<Option> | Observable<ReadonlyArray<Option>>
  >();
  public readonly optionDisplayValue = input<OptionDisplayValue>();
  protected readonly compare = input<Compare>();

  protected readonly isLoading = signal(false);
  protected readonly availableOptions = signal<Option[]>([]);

  constructor() {
    super();
    effect(async () => {
      const options = this.options();

      this.isLoading.set(true);

      try {
        const result =
          options instanceof Observable
            ? await lastValueFrom(options.pipe(takeUntil(this.destroy$)))
            : options;
        this.availableOptions.set([...result]);
      } finally {
        this.isLoading.set(false);
      }
    });
  }

  // Needs to be an arrow function in order to de-link the reference to `this`
  // when used in the template `[displayWith]="displayWith"`
  protected displayWith = (option?: Option | null) => {
    if (!option) return "";

    const displayValue = this.optionDisplayValue();
    switch (typeof displayValue) {
      case "undefined":
        return String(option);
      case "function":
        return displayValue.call(this, option);
      default:
        return String(option[displayValue as unknown as keyof Option]);
    }
  };

  // Needs to be an arrow function to de-link the reference to `this`
  // when used in the template `[compareWith]="compareWith"`
  protected compareWith = (a: Option, b?: Option | null) => {
    const compare = this.compare();
    if (compare) return compare.call(this, a, b);

    if (this.optionDisplayValue())
      return this.displayWith(a) === this.displayWith(b);

    return a === b;
  };
}
