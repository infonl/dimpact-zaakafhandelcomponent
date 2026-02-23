/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute, Data } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { ErrorCardComponent } from "./error-card.component";

describe(ErrorCardComponent.name, () => {
  let component: ErrorCardComponent;
  let fixture: ComponentFixture<ErrorCardComponent>;
  let activatedRoute: ActivatedRoute;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorCardComponent, TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: new BehaviorSubject<Data>({}),
          },
        },
      ],
    }).compileComponents();

    activatedRoute = TestBed.inject(ActivatedRoute);
    fixture = TestBed.createComponent(ErrorCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should have a default icon", () => {
    expect(component.iconName).toBe("indeterminate_question_box");
  });

  it("should display a custom icon", () => {
    component.iconName = "error";
    fixture.detectChanges();
    expect(component.iconName).toBe("error");
  });

  it("should have a default title", () => {
    expect(component.title).toBe("error-card.title.default");
  });

  it("should display a custom title from Input", () => {
    component.title = "custom.title";
    fixture.detectChanges();
    expect(component.title).toBe("custom.title");
  });

  it("should have an empty default text", () => {
    expect(component.text).toBe("");
  });

  it("should display custom text from Input", () => {
    component.text = "custom.text";
    fixture.detectChanges();
    expect(component.text).toBe("custom.text");
  });

  it("should override defaults with route data", () => {
    (activatedRoute.data as BehaviorSubject<Data>).next({
      title: "route.title",
      text: "route.text",
      iconName: "warning",
    });

    expect(component.title).toBe("route.title");
    expect(component.text).toBe("route.text");
    expect(component.iconName).toBe("warning");
  });
});
