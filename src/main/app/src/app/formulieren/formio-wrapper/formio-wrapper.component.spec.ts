/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { ElementRef } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { FormioBootstrapLoaderService } from "./formio-bootstrap-loader.service";
import { FormioWrapperComponent } from "./formio-wrapper.component";

describe(FormioWrapperComponent.name, () => {
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
      // 1. Mock the DOM element that has the 'choices' class (the dropdown widget)
      const mockChoicesElement = {
        classList: {
          contains: (className: string) => className === "choices",
        },
      };

      // 2. Create a mock event where composedPath returns our mock element
      const event = {
        composedPath: () => [mockChoicesElement],
        stopPropagation: jest.fn(),
      } as unknown as MouseEvent;

      // 3. Call the listener
      component.onClickInside(event);

      // 4. Verify propagation is stopped (dropdown stays open)
      expect(event.stopPropagation).toHaveBeenCalledTimes(1);
    });

    it("should NOT stop propagation when clicking outside the widget (to allow closing)", () => {
      // 1. Mock a generic element (like the form background) without 'choices' class
      const mockOtherElement = {
        classList: {
          contains: () => false, // No 'choices' class found
        },
      };

      // 2. Create a mock event
      const event = {
        composedPath: () => [mockOtherElement],
        stopPropagation: jest.fn(),
      } as unknown as MouseEvent;

      // 3. Call the listener
      component.onClickInside(event);

      // 4. Verify propagation is NOT stopped (dropdown closes normally)
      expect(event.stopPropagation).not.toHaveBeenCalled();
    });

    it("should handle elements without classList gracefully", () => {
      // 1. Mock an element that might appear in path but has no classList (e.g. Document or Window)
      const mockElementNoClassList = {};

      const event = {
        composedPath: () => [mockElementNoClassList],
        stopPropagation: jest.fn(),
      } as unknown as MouseEvent;

      component.onClickInside(event);

      // Should not crash and should not stop propagation
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

      expect(() => component.onFormioReady(nullFormio as never)).not.toThrow();
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
        Object.defineProperty(document, "activeElement", originalActiveElement);
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
