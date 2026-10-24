/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { TestBed } from "@angular/core/testing";
import { FormioWrapperComponent } from "./formio-wrapper.component";

describe(FormioWrapperComponent.name, () => {
  let component: FormioWrapperComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FormioWrapperComponent],
    }).compileComponents();

    component = TestBed.inject(FormioWrapperComponent);
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
