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

  beforeEach(() => {
    const mockElementRef: Pick<ElementRef, "nativeElement"> = {
      nativeElement: {
        shadowRoot: null,
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
});
