/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  booleanAttribute,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation,
} from "@angular/core";
import {
  ExtendedComponentSchema,
  FormioAppConfig,
  FormioBaseComponent,
  FormioComponent,
  FormioHookOptions,
  FormioModule,
} from "@formio/angular";
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
export class FormioWrapperComponent implements OnInit, AfterViewInit {
  @Input() form: unknown;
  @Input() submission: unknown;
  @Input() options?: FormioHookOptions;
  @Input({ required: true, transform: booleanAttribute }) readOnly = false;
  @Output() formSubmit = new EventEmitter<FormioSubmitEvent>();
  @Output() formChange = new EventEmitter<FormioChangeEvent>();
  @Output() createDocument = new EventEmitter<FormioCustomEvent>();
  @Output() submissionDone = new EventEmitter<boolean>();

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

  async ngOnInit() {
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
    this.submissionDone.emit(true);
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

export interface FormioCustomEvent {
  type: string;
  component: ExtendedComponentSchema;
  data: Record<string, string>;
  event?: Event;
}

export interface FormioSubmitEvent {
  data: Record<string, string>;
  state: string;
}

export interface FormioChangeEvent {
  data: Record<string, string>;
}
