/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { ZaakProcessFlowComponent } from "./zaak-process-flow.component";

const BPMN_DEFINITION = {
  processDefinitionKey: "test-key",
  processDefinitionName: "Test Process",
  processDefinitionVersion: 3,
};

describe(ZaakProcessFlowComponent.name, () => {
  let fixture: ComponentFixture<ZaakProcessFlowComponent>;
  let closeMock: jest.Mock;

  const getButton = (iconName: string): HTMLButtonElement => {
    const buttons = fixture.nativeElement.querySelectorAll(
      "button",
    ) as NodeListOf<HTMLButtonElement>;
    return Array.from(buttons).find(
      (btn) => btn.querySelector("mat-icon")?.textContent?.trim() === iconName,
    ) as HTMLButtonElement;
  };

  const getImage = (): HTMLImageElement =>
    fixture.nativeElement.querySelector("img");

  const getZoomTransform = (): string => getImage().style.transform;

  const dispatchArrowKey = (key: string) =>
    document.dispatchEvent(new KeyboardEvent("keydown", { key }));

  beforeEach(async () => {
    closeMock = jest.fn();

    await TestBed.configureTestingModule({
      imports: [
        ZaakProcessFlowComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakProcessFlowComponent);
    fixture.componentRef.setInput("sideNav", {
      close: closeMock,
    } as unknown as MatDrawer);
    fixture.componentRef.setInput("zaakUuid", "test-uuid");
    fixture.componentRef.setInput("bpmnProcessDefinition", BPMN_DEFINITION);
    fixture.detectChanges();
  });

  describe("header", () => {
    it("shows the process definition name and version", () => {
      const heading: HTMLElement = fixture.nativeElement.querySelector("h3");
      expect(heading.textContent).toContain("Test Process");
      expect(heading.textContent).toContain("3");
    });

    it("calls sideNav.close() when the close button is clicked", () => {
      getButton("close").click();
      expect(closeMock).toHaveBeenCalled();
    });
  });

  describe("zoom-in button", () => {
    it("increases the zoom level when clicked", () => {
      expect(getZoomTransform()).toBe("scale(1)");
      getButton("add").click();
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1.25)");
    });

    it("is disabled at maximum zoom", () => {
      for (let i = 0; i < 12; i++) {
        getButton("add").click();
        fixture.detectChanges();
      }
      expect(getButton("add").disabled).toBe(true);
    });
  });

  describe("zoom-out button", () => {
    it("decreases the zoom level when clicked", () => {
      getButton("add").click();
      fixture.detectChanges();
      getButton("remove").click();
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1)");
    });

    it("is disabled at minimum zoom", () => {
      for (let i = 0; i < 3; i++) {
        getButton("remove").click();
        fixture.detectChanges();
      }
      expect(getButton("remove").disabled).toBe(true);
    });
  });

  describe("reset zoom button", () => {
    it("resets zoom to 1 after zooming in", () => {
      getButton("add").click();
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1.25)");

      getButton("zoom_out_map").click();
      fixture.detectChanges();
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

    it("zooms in when the diagram container is clicked", () => {
      fixture.nativeElement.querySelector(".diagram-container").click();
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1.25)");
    });
  });

  describe("keyboard shortcuts", () => {
    it("zooms in on ArrowUp", () => {
      dispatchArrowKey("ArrowUp");
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1.25)");
    });

    it("zooms out on ArrowDown", () => {
      dispatchArrowKey("ArrowUp");
      fixture.detectChanges();
      dispatchArrowKey("ArrowDown");
      fixture.detectChanges();
      expect(getZoomTransform()).toBe("scale(1)");
    });

    it("scrolls the container left on ArrowLeft", () => {
      const container: HTMLDivElement =
        fixture.nativeElement.querySelector(".diagram-container");
      const scrollByMock = jest.fn();
      Object.defineProperty(container, "scrollBy", { value: scrollByMock });

      dispatchArrowKey("ArrowLeft");

      expect(scrollByMock).toHaveBeenCalledWith({
        left: -100,
        behavior: "smooth",
      });
    });

    it("scrolls the container right on ArrowRight", () => {
      const container: HTMLDivElement =
        fixture.nativeElement.querySelector(".diagram-container");
      const scrollByMock = jest.fn();
      Object.defineProperty(container, "scrollBy", { value: scrollByMock });

      dispatchArrowKey("ArrowRight");

      expect(scrollByMock).toHaveBeenCalledWith({
        left: 100,
        behavior: "smooth",
      });
    });

    it("does not zoom when an input element has focus", () => {
      const input = document.createElement("input");
      document.body.appendChild(input);
      input.focus();

      dispatchArrowKey("ArrowUp");
      fixture.detectChanges();

      expect(getZoomTransform()).toBe("scale(1)");
      document.body.removeChild(input);
    });
  });
});
