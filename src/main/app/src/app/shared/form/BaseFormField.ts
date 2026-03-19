/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  computed,
  effect,
  inject,
  input,
  OnDestroy,
  signal,
} from "@angular/core";
import {
  AbstractControl,
  FormGroup,
  ValidationErrors,
  Validators,
} from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom, Observable, Subject, takeUntil } from "rxjs";
import { FormHelper } from "./helpers";

/**
 * This base class is meant to be extended by form field components.
 *
 * It should **never** be used directly in a template.
 */
@Component({
  template: "",
})
export class SingleInputFormField<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends Form[Key]["value"],
> implements OnDestroy
{
  private readonly translateService = inject(TranslateService);

  private readonly controlErrors = signal<ValidationErrors | null>(null);

  protected readonly destroy$ = new Subject<void>();

  protected readonly control = computed<AbstractControl<Option | null> | null>(
    () => {
      const control = this.form().controls[this.key()];

      if (this.readonly()) control.disable({ emitEvent: false });

      return control;
    },
  );

  public readonly form = input.required<FormGroup<Form>>();
  public readonly key = input.required<Key & string>();
  public readonly label = input<string>();
  public readonly readonly = input(false, { transform: booleanAttribute });

  constructor() {
    effect(() => {
      const control = this.control();
      if (!control) {
        this.controlErrors.set(null);
        return;
      }

      control.statusChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.controlErrors.set(control.errors);
      });

      control.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
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

      // Set initial errors
      this.controlErrors.set(control.errors);
    });
  }

  protected readonly formError = computed(() => {
    return FormHelper.getErrorMessage(
      this.controlErrors(),
      this.translateService,
    );
  });

  protected isRequired = computed(
    () => this.control()?.hasValidator(Validators.required) ?? false,
  );

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
  Compare extends (a: Option, b: Option) => boolean = (
    a: Option,
    b: Option,
  ) => boolean,
> extends SingleInputFormField<Form, Key, Option> {
  public readonly options = input.required<
    Array<Option> | Observable<Array<Option>>
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
        this.availableOptions.set(result);
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
  protected compareWith = (a: Option, b: Option) => {
    const compare = this.compare();
    if (compare) return compare.call(this, a, b);

    if (this.optionDisplayValue())
      return this.displayWith(a) === this.displayWith(b);

    return a === b;
  };
}
