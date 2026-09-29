/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { ElementRef } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import {
  configure,
  render,
  RenderComponentOptions,
  within,
} from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { FormioCustomFunctions } from "../formio-custom-functions/formio-custom-functions";
import { FormioBootstrapLoaderService } from "./formio-bootstrap-loader.service";
import { FormioWrapperComponent } from "./formio-wrapper.component";

describe(FormioWrapperComponent.name, () => {
  describe("used without its template", () => {
    let component: FormioWrapperComponent;
    let bootstrapLoader: FormioBootstrapLoaderService;
    let mockElementRef: Pick<ElementRef, "nativeElement">;

    beforeEach(() => {
      mockElementRef = {
        nativeElement: {
          shadowRoot: {
            adoptedStyleSheets: [],
          },
        },
      };

      TestBed.configureTestingModule({
        providers: [
          FormioWrapperComponent,
          { provide: ElementRef, useValue: mockElementRef },
          {
            provide: FormioCustomFunctions,
            useValue: {
              prepareFormContext: jest.fn().mockResolvedValue({}),
              asContextValue: jest.fn((value: unknown) => value),
            },
          },
        ],
      }).compileComponents();

      component = TestBed.inject(FormioWrapperComponent);
      bootstrapLoader = TestBed.inject(FormioBootstrapLoaderService);

      jest
        .spyOn(bootstrapLoader, "getBootstrapStyleSheet")
        .mockResolvedValue(new CSSStyleSheet());
    });

    describe(FormioWrapperComponent.prototype.onChange.name, () => {
      it(`should emit events that has data`, () => {
        const event = { data: "value" };
        const listener = jest.spyOn(component.formChange, "emit");

        component.onChange(event);

        expect(listener).toHaveBeenCalledTimes(1);
        expect(listener).toHaveBeenCalledWith(event);
      });

      it(`should filter out events without data`, () => {
        const event = { key: "value" };
        const listener = jest.spyOn(component.formChange, "emit");

        component.onChange(event);

        expect(listener).toHaveBeenCalledTimes(0);
      });
    });

    describe(FormioWrapperComponent.prototype.onSubmit.name, () => {
      it("should not report the submission as done before it has settled", () => {
        const listener = jest.spyOn(component.submissionDone, "emit");

        component.onSubmit({ data: {}, state: "submitted" });

        expect(listener).not.toHaveBeenCalled();
      });
    });

    describe("Bootstrap CSS loading", () => {
      it("should load Bootstrap CSS on init", async () => {
        await component.ngOnInit();

        expect(bootstrapLoader.getBootstrapStyleSheet).toHaveBeenCalledTimes(1);
      });

      it("should adopt Bootstrap stylesheet into shadow DOM", async () => {
        const mockSheet = new CSSStyleSheet();
        jest
          .spyOn(bootstrapLoader, "getBootstrapStyleSheet")
          .mockResolvedValue(mockSheet);

        await component.ngOnInit();

        expect(
          mockElementRef.nativeElement.shadowRoot.adoptedStyleSheets,
        ).toContain(mockSheet);
      });

      it("should not throw error when shadowRoot is null", async () => {
        mockElementRef.nativeElement.shadowRoot = null;

        await expect(component.ngOnInit()).resolves.not.toThrow();
      });

      it("should set stylesLoaded to true after initialization", async () => {
        expect(component["stylesLoaded"]).toBe(false);

        await component.ngOnInit();

        expect(component["stylesLoaded"]).toBe(true);
      });

      it("should handle errors gracefully", async () => {
        const consoleSpy = jest.spyOn(console, "error").mockImplementation();
        jest
          .spyOn(bootstrapLoader, "getBootstrapStyleSheet")
          .mockRejectedValue(new Error("Failed to load"));

        await component.ngOnInit();

        expect(consoleSpy).toHaveBeenCalledWith(
          "Failed to load Bootstrap CSS:",
          expect.any(Error),
        );
        consoleSpy.mockRestore();
      });
    });

    describe(FormioWrapperComponent.prototype.onClickInside.name, () => {
      it("should stop propagation when clicking inside a .choices widget (to prevent closing)", () => {
        const mockChoicesElement = {
          classList: {
            contains: (className: string) => className === "choices",
          },
        };
        const event = {
          composedPath: () => [mockChoicesElement],
          stopPropagation: jest.fn(),
        } as unknown as MouseEvent;

        component.onClickInside(event);

        expect(event.stopPropagation).toHaveBeenCalledTimes(1);
      });

      it("should NOT stop propagation when clicking outside the widget (to allow closing)", () => {
        const mockOtherElement = {
          classList: {
            contains: () => false,
          },
        };
        const event = {
          composedPath: () => [mockOtherElement],
          stopPropagation: jest.fn(),
        } as unknown as MouseEvent;

        component.onClickInside(event);

        expect(event.stopPropagation).not.toHaveBeenCalled();
      });

      it("should handle elements without classList gracefully", () => {
        const mockElementNoClassList = {};
        const event = {
          composedPath: () => [mockElementNoClassList],
          stopPropagation: jest.fn(),
        } as unknown as MouseEvent;

        component.onClickInside(event);

        expect(event.stopPropagation).not.toHaveBeenCalled();
      });
    });

    describe(FormioWrapperComponent.prototype.onFormioReady.name, () => {
      let addLanguageSpy: jest.Mock;
      let mockFormioBaseComponent: { formio: { addLanguage: jest.Mock } };

      beforeEach(() => {
        addLanguageSpy = jest.fn();
        mockFormioBaseComponent = {
          formio: { addLanguage: addLanguageSpy },
        };
      });

      it("should register Dutch translations and activate them when browser language is 'nl'", () => {
        jest.spyOn(navigator, "language", "get").mockReturnValue("nl");

        component.onFormioReady(mockFormioBaseComponent as never);

        expect(addLanguageSpy).toHaveBeenCalledWith(
          "nl",
          expect.any(Object),
          true,
        );
      });

      it("should register Dutch translations and activate them when browser language is 'nl-NL'", () => {
        jest.spyOn(navigator, "language", "get").mockReturnValue("nl-NL");

        component.onFormioReady(mockFormioBaseComponent as never);

        expect(addLanguageSpy).toHaveBeenCalledWith(
          "nl",
          expect.any(Object),
          true,
        );
      });

      it("should register Dutch translations but not activate them when browser language is 'en'", () => {
        jest.spyOn(navigator, "language", "get").mockReturnValue("en");

        component.onFormioReady(mockFormioBaseComponent as never);

        expect(addLanguageSpy).toHaveBeenCalledWith(
          "nl",
          expect.any(Object),
          false,
        );
      });

      it("should pass the actual Dutch translation object with correct translations", () => {
        jest.spyOn(navigator, "language", "get").mockReturnValue("nl");

        component.onFormioReady(mockFormioBaseComponent as never);

        const translations = addLanguageSpy.mock.calls[0][1];
        expect(translations.required).toBe("{{field}} is verplicht.");
        expect(translations.submit).toBe("Indienen");
        expect(translations.next).toBe("Volgende");
        expect(translations.previous).toBe("Vorige");
        expect(translations.cancel).toBe("Annuleren");
        expect(translations.invalid_email).toBe(
          "{{field}} moet een geldig e-mailadres zijn.",
        );
        expect(translations.minLength).toBe(
          "{{field}} moet minimaal {{length}} tekens bevatten.",
        );
        expect(translations.maxLength).toBe(
          "{{field}} mag maximaal {{length}} tekens bevatten.",
        );
        expect(translations.january).toBe("Januari");
        expect(translations.december).toBe("December");
      });

      it("should not throw when formio is null", () => {
        const nullFormio = { formio: null };

        expect(() =>
          component.onFormioReady(nullFormio as never),
        ).not.toThrow();
      });
    });

    describe("ngAfterViewInit should patch document.activeElement correctly", () => {
      let originalActiveElement: PropertyDescriptor | undefined;

      beforeEach(() => {
        originalActiveElement = Object.getOwnPropertyDescriptor(
          Document.prototype,
          "activeElement",
        );

        FormioWrapperComponent["activeElementPatched"] = false;
      });

      afterEach(() => {
        if (originalActiveElement) {
          Object.defineProperty(
            document,
            "activeElement",
            originalActiveElement,
          );
        }
        FormioWrapperComponent["activeElementPatched"] = false;
      });

      it("should patch document.activeElement only once", () => {
        component.ngAfterViewInit();
        expect(FormioWrapperComponent["activeElementPatched"]).toBe(true);

        const spy = jest.spyOn(Object, "defineProperty");
        component.ngAfterViewInit();
        expect(spy).not.toHaveBeenCalled();
      });
    });
  });

  describe("rendering a Form.io form", () => {
    const form = {
      display: "form",
      components: [
        { type: "textfield", key: "naam", label: "Naam", input: true },
        {
          type: "content",
          key: "context",
          html: "<p>Zaak {{ zaak.identificatie }}, taak {{ taak.naam }}</p>",
          input: false,
        },
        {
          type: "button",
          key: "submit",
          label: "Opslaan",
          action: "submit",
          input: true,
        },
      ],
    };
    const otherForm = {
      display: "form",
      components: [
        { type: "textfield", key: "adres", label: "Adres", input: true },
      ],
    };
    const zaak = fromPartial<GeneratedType<"RestZaak">>({
      identificatie: "fakeZaakIdentificatie1",
    });
    const otherZaak = fromPartial<GeneratedType<"RestZaak">>({
      identificatie: "fakeZaakIdentificatie2",
    });
    const taak = fromPartial<GeneratedType<"RestTask">>({
      naam: "fakeTaakNaam1",
      taakdata: { naam: "fakeNaam1" },
    });
    const otherTaak = fromPartial<GeneratedType<"RestTask">>({
      naam: "fakeTaakNaam2",
      taakdata: { naam: "fakeNaam2" },
    });
    const submitDoneMessage = "Inzending voltooid.";
    const submitErrorMessage =
      "Controleer het formulier en corrigeer alle fouten voordat u indient.";

    let prepareFormContext: jest.Mock;
    let asContextValue: jest.Mock;

    // Testing Library prints the container into the message of a failed query, and cannot print
    // the shadow root that holds the form - that would replace every failure with a TypeError.
    beforeAll(() =>
      configure({
        dom: { getElementError: (message) => new Error(message ?? "") },
      }),
    );
    afterAll(() => configure({ dom: {} }));

    beforeEach(() => {
      jest.spyOn(navigator, "language", "get").mockReturnValue("nl-NL");
      prepareFormContext = jest.fn(
        async (
          _form: unknown,
          _taakdata: Record<string, unknown>,
          contextZaak?: object,
          contextTaak?: object,
        ) => ({ zaak: contextZaak, taak: contextTaak }),
      );
      asContextValue = jest.fn((value: unknown) => value);
    });

    async function renderFormioWrapper(
      inputs: RenderComponentOptions<FormioWrapperComponent>["inputs"] = {},
    ) {
      const formSubmit = jest.fn();
      const submissionDone = jest.fn();
      const submissionError = jest.fn();
      const { fixture } = await render(FormioWrapperComponent, {
        inputs: {
          form,
          zaak,
          taak,
          readOnly: false,
          submitPending: false,
          ...inputs,
        },
        on: { formSubmit, submissionDone, submissionError },
        providers: [
          {
            provide: FormioCustomFunctions,
            useValue: { prepareFormContext, asContextValue },
          },
          {
            provide: FormioBootstrapLoaderService,
            useValue: {
              getBootstrapStyleSheet: jest
                .fn()
                .mockResolvedValue(new CSSStyleSheet()),
            },
          },
        ],
      });
      const formio = within(fixture.nativeElement.shadowRoot);

      return {
        fixture,
        formio,
        user: userEvent.setup(),
        formSubmit,
        submissionDone,
        submissionError,
      };
    }

    async function renderOpenForm(
      inputs: RenderComponentOptions<FormioWrapperComponent>["inputs"] = {},
    ) {
      const rendered = await renderFormioWrapper(inputs);
      const textbox = await rendered.formio.findByRole("textbox", {
        name: "Naam",
      });
      return { ...rendered, textbox };
    }

    async function renderFormWithSubmitInFlight(
      inputs: RenderComponentOptions<FormioWrapperComponent>["inputs"] = {},
    ) {
      const rendered = await renderOpenForm(inputs);
      await rendered.user.click(
        rendered.formio.getByRole("button", { name: "Opslaan" }),
      );
      rendered.fixture.componentRef.setInput("submitPending", true);
      rendered.fixture.detectChanges();
      return rendered;
    }

    describe("a form that is built", () => {
      it("should render nothing while there is no form", async () => {
        const { fixture, formio } = await renderFormioWrapper({
          form: undefined,
        });
        await fixture.whenStable();

        expect(formio.queryByRole("textbox")).not.toBeInTheDocument();
      });

      it("should render the fields of the form, filled in with the taakdata of the taak", async () => {
        const { formio } = await renderFormioWrapper();

        expect(
          await formio.findByDisplayValue("fakeNaam1"),
        ).toHaveAccessibleName("Naam");
      });

      it("should build an empty form when the taak carries no taakdata", async () => {
        const taakWithoutTaakdata = fromPartial<GeneratedType<"RestTask">>({
          naam: "fakeTaakNaam1",
          taakdata: null,
        });

        const { textbox } = await renderOpenForm({ taak: taakWithoutTaakdata });

        expect(textbox).toHaveValue("");
        expect(prepareFormContext).toHaveBeenCalledWith(
          form,
          {},
          zaak,
          taakWithoutTaakdata,
        );
      });

      it("should build the eval context once, from the form, the taakdata, the zaak and the taak", async () => {
        const { formio } = await renderFormioWrapper();

        expect(
          await formio.findByText(
            "Zaak fakeZaakIdentificatie1, taak fakeTaakNaam1",
          ),
        ).toBeInTheDocument();
        expect(prepareFormContext).toHaveBeenCalledTimes(1);
        expect(prepareFormContext).toHaveBeenCalledWith(
          form,
          { naam: "fakeNaam1" },
          zaak,
          taak,
        );
      });

      it("should hand over what the user entered when the user submits the form", async () => {
        const { formio, user, formSubmit } = await renderFormioWrapper();
        const textbox = await formio.findByDisplayValue("fakeNaam1");

        await user.clear(textbox);
        await user.type(textbox, "fakeTekst");
        await user.click(formio.getByRole("button", { name: "Opslaan" }));

        expect(formSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            data: expect.objectContaining({ naam: "fakeTekst" }),
            state: "submitted",
          }),
        );
      });
    });

    describe("a form or zaak that changes", () => {
      it("should rebuild the form for a new form", async () => {
        const { fixture, formio } = await renderOpenForm();

        fixture.componentRef.setInput("form", otherForm);
        fixture.detectChanges();

        expect(
          await formio.findByRole("textbox", { name: "Adres" }),
        ).toBeInTheDocument();
        expect(
          formio.queryByRole("textbox", { name: "Naam" }),
        ).not.toBeInTheDocument();
        expect(prepareFormContext).toHaveBeenCalledTimes(2);
      });

      it("should rebuild the form with a new eval context for a new zaak", async () => {
        const { fixture, formio } = await renderOpenForm();

        fixture.componentRef.setInput("zaak", otherZaak);
        fixture.detectChanges();

        expect(
          formio.queryByRole("textbox", { name: "Naam" }),
        ).not.toBeInTheDocument();
        expect(
          await formio.findByText(
            "Zaak fakeZaakIdentificatie2, taak fakeTaakNaam1",
          ),
        ).toBeInTheDocument();
        expect(prepareFormContext).toHaveBeenCalledTimes(2);
        expect(prepareFormContext).toHaveBeenLastCalledWith(
          form,
          { naam: "fakeNaam1" },
          otherZaak,
          taak,
        );
      });

      it("should keep what the user entered when a new zaak rebuilds the form", async () => {
        const { fixture, formio, user } = await renderFormioWrapper();
        const textbox = await formio.findByDisplayValue("fakeNaam1");
        await user.clear(textbox);
        await user.type(textbox, "fakeTekst");

        fixture.componentRef.setInput("zaak", otherZaak);
        fixture.detectChanges();

        expect(await formio.findByDisplayValue("fakeTekst")).not.toBe(textbox);
      });
    });

    describe("a new taak for a form that is open", () => {
      it("should show the new taak in the form without rebuilding the eval context", async () => {
        const { fixture, formio } = await renderOpenForm();

        fixture.componentRef.setInput("taak", otherTaak);
        fixture.detectChanges();

        expect(
          formio.getByText("Zaak fakeZaakIdentificatie1, taak fakeTaakNaam2"),
        ).toBeInTheDocument();
        expect(asContextValue).toHaveBeenCalledWith(otherTaak, "taak");
        expect(prepareFormContext).toHaveBeenCalledTimes(1);
      });

      it("should replace what the user entered with the taakdata of the new taak", async () => {
        const { fixture, formio, user } = await renderFormioWrapper();
        const textbox = await formio.findByDisplayValue("fakeNaam1");
        await user.clear(textbox);
        await user.type(textbox, "fakeTekst");

        fixture.componentRef.setInput("taak", otherTaak);
        fixture.detectChanges();

        expect(
          await formio.findByDisplayValue("fakeNaam2"),
        ).toHaveAccessibleName("Naam");
      });

      it("should hold the new taak back while a submit is in flight, and show it once the submit settles", async () => {
        const { fixture, formio } = await renderFormWithSubmitInFlight();

        fixture.componentRef.setInput("taak", otherTaak);
        fixture.detectChanges();

        expect(
          formio.getByText("Zaak fakeZaakIdentificatie1, taak fakeTaakNaam1"),
        ).toBeInTheDocument();

        fixture.componentRef.setInput("submitPending", false);
        fixture.detectChanges();

        expect(
          formio.getByText("Zaak fakeZaakIdentificatie1, taak fakeTaakNaam2"),
        ).toBeInTheDocument();
      });

      it("should show a new taak that arrives together with the end of the submit, and report the submit as done", async () => {
        const { fixture, formio, submissionDone } =
          await renderFormWithSubmitInFlight();

        fixture.componentRef.setInput("taak", otherTaak);
        fixture.componentRef.setInput("submitPending", false);
        fixture.detectChanges();

        expect(
          formio.getByText("Zaak fakeZaakIdentificatie1, taak fakeTaakNaam2"),
        ).toBeInTheDocument();
        expect(await formio.findByText(submitDoneMessage)).toBeInTheDocument();
        expect(submissionDone).toHaveBeenCalledTimes(1);
      });
    });

    describe("a read-only form", () => {
      it("should build a read-only form with disabled fields and a disabled submit button", async () => {
        const { formio, textbox } = await renderOpenForm({ readOnly: true });

        expect(textbox).toBeDisabled();
        expect(formio.getByRole("button", { name: "Opslaan" })).toBeDisabled();
      });

      it("should disable the fields and the submit button of an open form that becomes read-only", async () => {
        const { fixture, formio } = await renderOpenForm();

        fixture.componentRef.setInput("readOnly", true);
        fixture.detectChanges();

        expect(formio.getByRole("textbox", { name: "Naam" })).toBeDisabled();
        expect(formio.getByRole("button", { name: "Opslaan" })).toBeDisabled();
      });

      it("should enable the fields again once the form is no longer read-only", async () => {
        const { fixture, formio } = await renderOpenForm({ readOnly: true });

        fixture.componentRef.setInput("readOnly", false);
        fixture.detectChanges();

        expect(formio.getByRole("textbox", { name: "Naam" })).toBeEnabled();
        expect(formio.getByRole("button", { name: "Opslaan" })).toBeEnabled();
      });

      it("should treat an empty readOnly attribute as read-only", async () => {
        const { fixture, formio } = await renderOpenForm();

        fixture.componentRef.setInput("readOnly", "");
        fixture.detectChanges();

        expect(formio.getByRole("textbox", { name: "Naam" })).toBeDisabled();
      });

      it("should build the form with the read-only state that holds once the form arrives", async () => {
        const { fixture, formio } = await renderFormioWrapper({
          form: undefined,
        });
        fixture.componentRef.setInput("readOnly", true);
        fixture.detectChanges();

        fixture.componentRef.setInput("form", form);
        fixture.detectChanges();

        expect(
          await formio.findByRole("textbox", { name: "Naam" }),
        ).toBeDisabled();
      });
    });

    describe("a submit in flight", () => {
      it("should lock the fields without redrawing the form, which would discard the spinner on the submit button", async () => {
        const { fixture, formio, textbox } = await renderOpenForm();

        fixture.componentRef.setInput("submitPending", true);
        fixture.detectChanges();

        expect(formio.getByRole("textbox", { name: "Naam" })).toBe(textbox);
        expect(textbox).toBeDisabled();
      });

      it("should unlock the fields once the submit settles", async () => {
        const { fixture, textbox } = await renderFormWithSubmitInFlight();

        fixture.componentRef.setInput("submitPending", false);
        fixture.detectChanges();

        expect(textbox).toBeEnabled();
      });

      it("should keep a read-only form locked after the submit settles", async () => {
        const { fixture, textbox } = await renderFormWithSubmitInFlight({
          readOnly: true,
        });

        fixture.componentRef.setInput("submitPending", false);
        fixture.detectChanges();

        expect(textbox).toBeDisabled();
      });

      it("should report a submit that settles to Form.io as done, which confirms it on the submit button", async () => {
        const { fixture, formio, submissionDone, submissionError } =
          await renderFormWithSubmitInFlight();

        fixture.componentRef.setInput("submitPending", false);
        fixture.detectChanges();

        expect(formio.getByText(submitDoneMessage)).toBeInTheDocument();
        expect(submissionDone).toHaveBeenCalledTimes(1);
        expect(submissionDone).toHaveBeenCalledWith({});
        expect(submissionError).not.toHaveBeenCalled();
      });

      it("should report a submit that failed to Form.io as an error, not as done", async () => {
        const { fixture, formio, submissionDone, submissionError } =
          await renderFormWithSubmitInFlight();

        fixture.componentRef.setInput("submitFailed", true);
        fixture.componentRef.setInput("submitPending", false);
        fixture.detectChanges();

        expect(formio.getByText(submitErrorMessage)).toBeInTheDocument();
        expect(formio.queryByText(submitDoneMessage)).not.toBeInTheDocument();
        expect(submissionError).toHaveBeenCalledWith(
          expect.objectContaining({ message: "submit failed" }),
        );
        expect(submissionDone).not.toHaveBeenCalled();
      });

      it("should report nothing while the submit is still running", async () => {
        const { submissionDone, submissionError } =
          await renderFormWithSubmitInFlight();

        expect(submissionDone).not.toHaveBeenCalled();
        expect(submissionError).not.toHaveBeenCalled();
      });

      it("should report nothing for a submit the form never made", async () => {
        const { submissionDone, submissionError } = await renderOpenForm();

        expect(submissionDone).not.toHaveBeenCalled();
        expect(submissionError).not.toHaveBeenCalled();
      });

      it("should report nothing when only submitFailed changes", async () => {
        const { fixture, submissionDone, submissionError } =
          await renderOpenForm();

        fixture.componentRef.setInput("submitFailed", true);
        fixture.detectChanges();

        expect(submissionDone).not.toHaveBeenCalled();
        expect(submissionError).not.toHaveBeenCalled();
      });

      it("should not throw when a submit settles before Form.io has built the form", async () => {
        const { fixture, submissionDone } = await renderFormioWrapper({
          form: undefined,
        });
        fixture.componentRef.setInput("submitPending", true);
        fixture.detectChanges();

        fixture.componentRef.setInput("submitPending", false);

        expect(() => fixture.detectChanges()).not.toThrow();
        expect(submissionDone).toHaveBeenCalledTimes(1);
      });
    });
  });
});
