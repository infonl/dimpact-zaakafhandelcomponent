/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { BackButtonDirective } from "./back-button.directive";
import { NavigationService } from "./navigation.service";

jest.autoMockOn();
describe("BackButtonDirective", () => {
  let directive: BackButtonDirective;
  const mockNavigationService = { back: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BackButtonDirective,
        { provide: NavigationService, useValue: mockNavigationService },
      ],
      imports: [RouterTestingModule.withRoutes([])],
    }).compileComponents();
    directive = TestBed.inject(BackButtonDirective);
  });

  it("should create an instance", () => {
    expect(directive).toBeTruthy();
  });

  it("should call navigation back", () => {
    directive.onClick();

    expect(mockNavigationService.back).toHaveBeenCalled();
  });
});
