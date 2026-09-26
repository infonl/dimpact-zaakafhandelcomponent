/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute, Data } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { BehaviorSubject } from "rxjs";
import { ErrorCardComponent } from "./error-card.component";

describe(ErrorCardComponent.name, () => {
  let fixture: ComponentFixture<ErrorCardComponent>;
  let routeData: BehaviorSubject<Data>;

  beforeEach(async () => {
    routeData = new BehaviorSubject<Data>({});

    await TestBed.configureTestingModule({
      imports: [ErrorCardComponent, TranslateModule.forRoot()],
      providers: [{ provide: ActivatedRoute, useValue: { data: routeData } }],
    }).compileComponents();
  });

  function createComponent(inputs: Record<string, string | undefined> = {}) {
    fixture = TestBed.createComponent(ErrorCardComponent);
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    fixture.detectChanges();
  }

  const icon = () => screen.getByRole("img", { hidden: true });
  const title = (name: string) =>
    screen.getByRole("heading", { level: 2, name });

  describe("without inputs or route data", () => {
    beforeEach(() => createComponent());

    it("shows the default icon", () => {
      expect(icon()).toHaveTextContent("indeterminate_question_box");
    });

    it("shows the default title", () => {
      expect(title("error-card.title.default")).toBeVisible();
    });

    it("shows no text", () => {
      expect(screen.queryByRole("paragraph")).not.toBeInTheDocument();
    });
  });

  it("shows the icon, title and text it is given", () => {
    createComponent({
      iconName: "error",
      title: "fake.title",
      text: "fake.text",
    });

    expect(icon()).toHaveTextContent("error");
    expect(title("fake.title")).toBeVisible();
    expect(screen.getByRole("paragraph")).toHaveTextContent("fake.text");
  });

  it.each([undefined, ""])(
    "shows the default title when the title is %p",
    (value) => {
      createComponent({ title: value });

      expect(title("error-card.title.default")).toBeVisible();
    },
  );

  it("shows no text when the text is explicitly undefined", () => {
    createComponent({ text: undefined });

    expect(screen.queryByRole("paragraph")).not.toBeInTheDocument();
  });

  it("shows an empty icon, not the default icon, when the icon name is explicitly undefined", () => {
    createComponent({ iconName: undefined });

    expect(icon()).toHaveTextContent(/^$/);
  });

  it("shows the icon, title and text of the route data", () => {
    routeData.next({
      iconName: "person_off",
      title: "fake.route.title",
      text: "fake.route.text",
    });
    createComponent();

    expect(icon()).toHaveTextContent("person_off");
    expect(title("fake.route.title")).toBeVisible();
    expect(screen.getByRole("paragraph")).toHaveTextContent("fake.route.text");
  });

  it("keeps the defaults for what the route data does not set", () => {
    routeData.next({ title: "fake.route.title" });
    createComponent();

    expect(icon()).toHaveTextContent("indeterminate_question_box");
    expect(title("fake.route.title")).toBeVisible();
    expect(screen.queryByRole("paragraph")).not.toBeInTheDocument();
  });

  it("shows route data that arrives after it has been rendered", () => {
    createComponent();

    routeData.next({
      iconName: "warning",
      title: "fake.route.title",
      text: "fake.route.text",
    });
    fixture.detectChanges();

    expect(icon()).toHaveTextContent("warning");
    expect(title("fake.route.title")).toBeVisible();
    expect(screen.getByRole("paragraph")).toHaveTextContent("fake.route.text");
  });
});
