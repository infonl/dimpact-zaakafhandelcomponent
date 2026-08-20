/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  booleanAttribute,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {
  ExtendedComponentSchema,
  FormioAppConfig,
  FormioBaseComponent,
  FormioComponent,
  FormioHookOptions,
  FormioModule,
} from "@formio/angular";
import { catchError, from, of, ReplaySubject, switchMap } from "rxjs";
import { FormioCustomFunctions } from "../formio-custom-functions/formio-custom-functions";
import { FormioBootstrapLoaderService } from "./formio-bootstrap-loader.service";
import { FORMIO_NL_TRANSLATIONS } from "./formio-wrapper.i18n-translations.nl";

@Component({
  selector: "zac-formio-wrapper",
  templateUrl: "./formio-wrapper.component.html",
  styleUrl: "./formio-wrapper.component.less",
  encapsulation: ViewEncapsulation.ShadowDom,
  standalone: true,
  imports: [FormioModule],
  providers: [
    {
      provide: FormioAppConfig,
      useValue: {
        appUrl: window.location.origin,
        apiUrl: window.location.origin,
      },
    },
  ],
})
export class FormioWrapperComponent
  implements OnInit, OnChanges, AfterViewInit
{
  @Input() form: unknown;
  @Input() submission: unknown;
  @Input() taakdata?: Record<string, unknown>;
  @Input() options?: FormioHookOptions;
  @Input({ required: true, transform: booleanAttribute }) readOnly = false;
  @Input({ required: true, transform: booleanAttribute }) submitPending = false;
  @Input({ transform: booleanAttribute }) submitFailed = false;
  @Output() formSubmit = new EventEmitter<FormioSubmitEvent>();
  @Output() formChange = new EventEmitter<FormioChangeEvent>();
  @Output() createDocument = new EventEmitter<FormioCustomEvent>();
  @Output() submissionDone = new EventEmitter<boolean>();
  @Output() submissionError = new EventEmitter<FormioSubmitError>();

  @HostListener("click", ["$event"])
  onClickInside(event: MouseEvent) {
    const path = event.composedPath() as HTMLElement[];
    const isClickInsideChoicesWidget = path.some((element) => {
      return element.classList && element.classList.contains("choices");
    });
    if (isClickInsideChoicesWidget) {
      event.stopPropagation();
    }
  }

  @ViewChild(FormioComponent, { static: false })
  formioComponent!: FormioComponent;

  private elementRef = inject(ElementRef);
  private bootstrapLoader = inject(FormioBootstrapLoaderService);
  protected stylesLoaded = false;

  private static activeElementPatched = false;
  private readonly customFunctions = inject(FormioCustomFunctions);
  private readonly destroyRef = inject(DestroyRef);
  private readonly rebuild$ = new ReplaySubject<void>(1);
  protected evalContext: Record<string, unknown> = {};
  protected evalContextReady = false;

  ngOnChanges(changes: SimpleChanges) {
    if (changes["form"]) {
      this.rebuild$.next();
    }

    if (changes["readOnly"] && !changes["readOnly"].firstChange) {
      this.applyReadOnly();
    }

    const submitPendingChange = changes["submitPending"];
    if (submitPendingChange && !submitPendingChange.firstChange) {
      // Form.io keeps the submit button spinning until it hears the outcome, and paints it green on
      // `submitDone` - so a failed submit has to be reported as an error instead.
      if (
        submitPendingChange.previousValue &&
        !submitPendingChange.currentValue
      ) {
        if (this.submitFailed) {
          // Form.io renders its own translated `submitError` text, so this message is not displayed -
          // it only has to be a non-empty error for Form.io to mark the button as failed.
          this.submissionError.emit({ message: "submit failed" });
        } else {
          this.submissionDone.emit(true);
        }
      }
      this.applySubmitPending();
    }
  }

  async ngOnInit() {
    this.rebuild$
      .pipe(
        switchMap(() => {
          this.evalContextReady = false;
          const source = from(
            this.customFunctions.prepareFormContext(
              this.form,
              this.taakdata ?? {},
            ),
          );
          return source.pipe(
            catchError((error) => {
              console.error("Failed to build form eval context:", error);
              return of({});
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((evalContext: Record<string, unknown>) => {
        this.evalContext = evalContext;
        this.evalContextReady = true;
      });

    await this.loadBootstrapStyles();
    this.stylesLoaded = true;
  }

  ngAfterViewInit() {
    if (FormioWrapperComponent.activeElementPatched) return;

    // Getting the document.activeElement from the Shadow DOM - Date field text-mask relies on this to determine if the input is focused
    const originalActiveElementGetter = Object.getOwnPropertyDescriptor(
      Document.prototype,
      "activeElement",
    )?.get;

    if (
      !originalActiveElementGetter ||
      typeof originalActiveElementGetter !== "function"
    )
      return;

    Object.defineProperty(document, "activeElement", {
      get() {
        let element = originalActiveElementGetter.call(document);
        while (
          element &&
          element.shadowRoot &&
          element.shadowRoot.activeElement
        ) {
          element = element.shadowRoot.activeElement;
        }
        return element;
      },
      configurable: true,
    });
    FormioWrapperComponent.activeElementPatched = true;
  }

  // Form.io reads `readOnly` while building only, and its components render from `disabled` - hence both.
  private applyReadOnly() {
    const webform = this.formioComponent?.formio as FormioWebform | undefined;
    if (!webform) return;

    webform.options.readOnly = this.readOnly;
    webform.everyComponent((component) => {
      component.options.readOnly = this.readOnly;
      component.disabled = this.readOnly;
    });
    void webform.redraw();
  }

  /**
   * Locks the fields while a submit is in flight. Deliberately does not redraw: a redraw rebuilds the
   * submit button and throws away the spinner Form.io is showing for this very submit.
   */
  private applySubmitPending() {
    const webform = this.formioComponent?.formio as FormioWebform | undefined;
    if (!webform) return;

    const disabled = this.readOnly || this.submitPending;
    webform.everyComponent((component) => {
      // Select and Tags override this setter to disable their Choices widget too.
      component.disabled = disabled;
      // Other components only stamp `disabled` onto the DOM while rendering, so push it onto the inputs.
      component.refs?.input?.forEach((input) =>
        component.setDisabled(input, disabled),
      );
    });
  }

  private async loadBootstrapStyles(): Promise<void> {
    const shadowRoot = this.elementRef.nativeElement.shadowRoot as ShadowRoot;
    if (!shadowRoot) return;

    try {
      const sheet = await this.bootstrapLoader.getBootstrapStyleSheet();
      shadowRoot.adoptedStyleSheets = [sheet, ...shadowRoot.adoptedStyleSheets];
    } catch (error) {
      // Allow form to render without Bootstrap styles
      console.error("Failed to load Bootstrap CSS:", error);
    }
  }

  onSubmit(event: object) {
    this.formSubmit.emit(event as FormioSubmitEvent);
  }

  onChange(event: object) {
    // Filter out form.io change events that do not contain data
    if ("data" in event && event.data)
      this.formChange.emit(event as FormioChangeEvent);
  }

  onFormioReady(formioBaseComponent: FormioBaseComponent) {
    const isDutch = navigator.language.toLowerCase().startsWith("nl") ?? false;
    if (!formioBaseComponent.formio) {
      console.error(
        "Cannot load NL translations: formio instance is not available",
      );
      return;
    }
    formioBaseComponent.formio.addLanguage(
      "nl",
      FORMIO_NL_TRANSLATIONS,
      isDutch,
    );
  }

  onCustomEvent(event: FormioCustomEvent) {
    if (event.type === "createDocument") {
      // Emit to parent
      this.createDocument.emit(event);
    }
  }
}

/** `@formio/angular` types the live form instance as `any`. */
interface FormioWebform {
  options: { readOnly?: boolean };
  everyComponent(callback: (component: FormioLiveComponent) => void): void;
  redraw(): Promise<void>;
}

interface FormioLiveComponent {
  options: { readOnly?: boolean };
  disabled: boolean;
  refs?: { input?: HTMLElement[] };
  setDisabled(element: HTMLElement, disabled: boolean): void;
}

export interface FormioCustomEvent {
  type: string;
  component: ExtendedComponentSchema;
  data: Record<string, string>;
  event?: Event;
}

export interface FormioSubmitError {
  message: string;
}

export interface FormioSubmitEvent {
  data: Record<string, string>;
  state: string;
}

export interface FormioChangeEvent {
  data: Record<string, string>;
}
