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
});
