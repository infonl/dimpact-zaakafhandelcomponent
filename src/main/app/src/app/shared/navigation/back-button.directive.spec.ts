/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NavigationService } from "./navigation.service";
import { BackButtonDirective } from "./back-button.directive";

@Component({
  template: "<button zacBackButton>back</button>",
  imports: [BackButtonDirective],
})
class TestHostComponent {}

describe(BackButtonDirective.name, () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let navigationServiceMock: Pick<NavigationService, "back">;

  beforeEach(async () => {
    navigationServiceMock = { back: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [{ provide: NavigationService, useValue: navigationServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it("should navigate back when clicked", () => {
    fixture.nativeElement.querySelector("button").click();

    expect(navigationServiceMock.back).toHaveBeenCalled();
  });
});
