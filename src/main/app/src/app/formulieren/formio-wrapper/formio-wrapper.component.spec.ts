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
});
