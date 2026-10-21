/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { ZaakProcessFlowComponent } from "./zaak-process-flow.component";

const ZAAK = {
  uuid: "test-uuid",
  identificatie: "ZAAK-2026-001",
  bpmnProcessDefinition: {
    processDefinitionKey: "test-key",
    processDefinitionName: "Test Process",
    processDefinitionVersion: 3,
  },
};

describe(ZaakProcessFlowComponent.name, () => {
  let fixture: ComponentFixture<ZaakProcessFlowComponent>;
  let closeMock: jest.Mock;
  const user = userEvent.setup();

  const getButton = (tooltip: string): HTMLButtonElement =>
    screen.getByRole("button", { description: tooltip });

  const getImage = (): HTMLImageElement =>
    screen.getByRole("img", { name: "Test Process - versie 3" });

  const getZoomTransform = (): string => getImage().style.transform;

  const clickAndRender = async (element: HTMLElement) => {
    await user.click(element);
    fixture.detectChanges();
  };

  const mockScrollBy = () => {
    const scrollByMock = jest.fn();
    Object.defineProperty(HTMLElement.prototype, "scrollBy", {
      value: scrollByMock,
      configurable: true,
    });
    return scrollByMock;
  };

  beforeEach(async () => {
    closeMock = jest.fn();

    await TestBed.configureTestingModule({
      imports: [ZaakProcessFlowComponent, NoopAnimationsModule],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakProcessFlowComponent);
    fixture.componentRef.setInput(
      "sideNav",
      fromPartial<MatDrawer>({ close: closeMock }),
    );
    fixture.componentRef.setInput("zaak", ZAAK);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    delete (HTMLElement.prototype as Partial<HTMLElement>).scrollBy;
  });

  describe("header", () => {
    it("shows the zaak identificatie", () => {
      expect(
        screen.getByRole("heading", { level: 3, name: /ZAAK-2026-001/ }),
      ).toBeInTheDocument();
    });

    it("calls sideNav.close() when the close button is clicked", async () => {
      const closeButton = await TestbedHarnessEnvironment.loader(
        fixture,
      ).getHarness(MatButtonHarness.with({ text: "close" }));
      await closeButton.click();
      expect(closeMock).toHaveBeenCalled();
    });
  });

  describe("zoom-in button", () => {
    it("increases the zoom level when clicked", async () => {
      expect(getZoomTransform()).toBe("scale(1)");
      await clickAndRender(getButton("actie.zoom.in"));
      expect(getZoomTransform()).toBe("scale(1.25)");
    });

    it("is disabled at maximum zoom", async () => {
      for (let i = 0; i < 12; i++) {
        await clickAndRender(getButton("actie.zoom.in"));
      }
      expect(getButton("actie.zoom.in")).toBeDisabled();
    });
  });

  describe("zoom-out button", () => {
    it("decreases the zoom level when clicked", async () => {
      await clickAndRender(getButton("actie.zoom.in"));
      await clickAndRender(getButton("actie.zoom.out"));
      expect(getZoomTransform()).toBe("scale(1)");
    });

    it("is disabled at minimum zoom", async () => {
      for (let i = 0; i < 3; i++) {
        await clickAndRender(getButton("actie.zoom.out"));
      }
      expect(getButton("actie.zoom.out")).toBeDisabled();
    });
  });

  describe("reset zoom button", () => {
    it("resets zoom to 1 after zooming in", async () => {
      await clickAndRender(getButton("actie.zoom.in"));
      expect(getZoomTransform()).toBe("scale(1.25)");

      await clickAndRender(getButton("actie.zoom.reset"));
      expect(getZoomTransform()).toBe("scale(1)");
    });
  });

  describe("diagram image", () => {
    it("shows the process diagram for the given zaak", () => {
      expect(getImage().src).toContain("/rest/zaken/test-uuid/process-diagram");
    });

    it("includes a cache-busting timestamp in the src", () => {
      expect(getImage().src).toContain("?t=");
    });

    it("applies the scale transform reflecting the current zoom level", () => {
      expect(getZoomTransform()).toBe("scale(1)");
    });

    it("zooms in when the diagram is clicked", async () => {
      await clickAndRender(getImage());
      expect(getZoomTransform()).toBe("scale(1.25)");
    });
  });

  describe("keyboard shortcuts", () => {
    it("zooms in on ArrowUp", async () => {
      await user.keyboard("{ArrowUp}");
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1.25)");
    });

    it("zooms out on ArrowDown", async () => {
      await user.keyboard("{ArrowUp}");
      fixture.detectChanges();
      await user.keyboard("{ArrowDown}");
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1)");
    });

    it("scrolls the diagram container left on ArrowLeft", async () => {
      const scrollByMock = mockScrollBy();

      await user.keyboard("{ArrowLeft}");

      expect(scrollByMock).toHaveBeenCalledWith({
        left: -100,
        behavior: "smooth",
      });
      expect(scrollByMock.mock.contexts[0]).toHaveClass("diagram-container");
    });

    it("scrolls the diagram container right on ArrowRight", async () => {
      const scrollByMock = mockScrollBy();

      await user.keyboard("{ArrowRight}");

      expect(scrollByMock).toHaveBeenCalledWith({
        left: 100,
        behavior: "smooth",
      });
      expect(scrollByMock.mock.contexts[0]).toHaveClass("diagram-container");
    });

    it("does not zoom when an input element has focus", async () => {
      const input = document.createElement("input");
      document.body.appendChild(input);
      input.focus();

      await user.keyboard("{ArrowUp}");
      fixture.detectChanges();

      expect(getZoomTransform()).toBe("scale(1)");
      document.body.removeChild(input);
    });
  });
});
