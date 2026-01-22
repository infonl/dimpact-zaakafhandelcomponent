/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ErrorCardComponent } from "./error-card.component";

describe(ErrorCardComponent.name, () => {
  let component: ErrorCardComponent;
  let fixture: ComponentFixture<ErrorCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ErrorCardComponent],
      imports: [TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({}),
          },
        },
      ],
    }).compileComponents();

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

  it("should display title from route data", async () => {
    const activatedRoute = TestBed.inject(ActivatedRoute);
    (activatedRoute.data as any) = of({ title: "route.title" });

    const newFixture = TestBed.createComponent(ErrorCardComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.title).toBe("route.title");
  });

  it("should have an empty default text", () => {
    expect(component.text).toBe("");
  });

  it("should display custom text from Input", () => {
    component.text = "custom.text";
    fixture.detectChanges();
    expect(component.text).toBe("custom.text");
  });

  it("should display text from route data", async () => {
    const activatedRoute = TestBed.inject(ActivatedRoute);
    (activatedRoute.data as any) = of({ text: "route.text" });

    const newFixture = TestBed.createComponent(ErrorCardComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.text).toBe("route.text");
  });
});
