/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { MaterialModule } from "../material/material.module";
import { ButtonMenuItem } from "./menu-item/button-menu-item";
import { HrefMenuItem } from "./menu-item/href-menu-item";
import { LinkMenuItem } from "./menu-item/link-menu-item";
import { MenuItem, MenuItemType } from "./menu-item/menu-item";
import { AsyncButtonMenuItem } from "./menu-item/subscription-button-menu-item";
import { MenuMode, MenuState, SideNavComponent } from "./side-nav.component";

describe(SideNavComponent.name, () => {
  let fixture: ComponentFixture<SideNavComponent>;
  let component: SideNavComponent;
  let loader: HarnessLoader;
  let utilServiceMock: { setLoading: jest.Mock };

  beforeEach(async () => {
    utilServiceMock = {
      setLoading: jest.fn(),
    };

    await TestBed.configureTestingModule({
      declarations: [SideNavComponent],
      imports: [
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MaterialModule,
        RouterModule.forRoot([]),
      ],
    })
      .overrideProvider(UtilService, { useValue: utilServiceMock })
      .compileComponents();

    fixture = TestBed.createComponent(SideNavComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("menu item rendering", () => {
    it("should render header menu items", async () => {
      const headerItem: MenuItem = {
        type: MenuItemType.HEADER,
        title: "test.header",
        icon: "home",
      } as MenuItem;

      fixture.componentRef.setInput("menu", [headerItem]);
      fixture.detectChanges();

      const subheaders = fixture.nativeElement.querySelectorAll("h3");
      expect(subheaders.length).toBe(1);
      expect(subheaders[0].textContent.trim()).toContain("test.header");
    });

    it("should render divider after header menu items", async () => {
      const headerItem: MenuItem = {
        type: MenuItemType.HEADER,
        title: "test.header",
        icon: "home",
      } as MenuItem;

      fixture.componentRef.setInput("menu", [headerItem]);
      fixture.detectChanges();

      const dividers = fixture.nativeElement.querySelectorAll("mat-divider");
      expect(dividers.length).toBe(1);
    });

    it("should render link menu items", () => {
      const linkItem = new LinkMenuItem("test.link", "/test-url", "link");

      fixture.componentRef.setInput("menu", [linkItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector("a[mat-list-item]");
      expect(anchor).toBeTruthy();
      expect(anchor.getAttribute("ng-reflect-router-link")).toBe("/test-url");
    });

    it("should render button menu items", () => {
      const buttonItem = new ButtonMenuItem(
        "test.button",
        () => {},
        "settings",
      );

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      expect(button).toBeTruthy();
    });

    it("should render href menu items", () => {
      const hrefItem = new HrefMenuItem(
        "test.href",
        "https://example.com",
        "open_in_new",
      );

      fixture.componentRef.setInput("menu", [hrefItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector(
        'a[mat-list-item][href="https://example.com"]',
      );
      expect(anchor).toBeTruthy();
    });

    it("should render href menu items as native anchor elements to enable browser navigation", async () => {
      const hrefItem = new HrefMenuItem(
        "test.href",
        "https://example.com",
        "open_in_new",
      );

      fixture.componentRef.setInput("menu", [hrefItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector(
        'a[mat-list-item][href="https://example.com"]',
      );

      // Verify it's a real anchor element (not just mat-list-item)
      expect(anchor.tagName).toBe("A");

      // Verify href is set correctly for native browser navigation
      expect(anchor.getAttribute("href")).toBe("https://example.com");

      // Verify no routerLink is present (which would intercept navigation)
      expect(anchor.hasAttribute("routerLink")).toBe(false);
    });

    it("should render href menu items with download URLs for document downloads", async () => {
      const downloadUrl = "/api/documents/12345/download";
      const hrefItem = new HrefMenuItem(
        "download.document",
        downloadUrl,
        "download",
      );

      fixture.componentRef.setInput("menu", [hrefItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector("a[mat-list-item]");

      // Verify it's a real anchor element
      expect(anchor.tagName).toBe("A");

      // Verify href is set correctly for download
      expect(anchor.getAttribute("href")).toBe(downloadUrl);
    });

    it("should render multiple menu items of different types", () => {
      const menuItems: MenuItem[] = [
        {
          type: MenuItemType.HEADER,
          title: "test.header",
          icon: "home",
        } as MenuItem,
        new LinkMenuItem("test.link", "/test-url", "link"),
        new ButtonMenuItem("test.button", () => {}, "settings"),
        new HrefMenuItem("test.href", "https://example.com", "open_in_new"),
      ];

      fixture.componentRef.setInput("menu", menuItems);
      fixture.detectChanges();

      const headers = fixture.nativeElement.querySelectorAll("h3");
      const links = fixture.nativeElement.querySelectorAll("a[mat-list-item]");
      const buttons = fixture.nativeElement.querySelectorAll(
        "button[mat-list-item]",
      );

      expect(headers.length).toBe(1);
      expect(links.length).toBe(2); // Link + Href
      expect(buttons.length).toBe(1);
    });
  });

  describe("menu item icons", () => {
    it("should display icon for link menu items", async () => {
      const linkItem = new LinkMenuItem("test.link", "/test-url", "dashboard");

      fixture.componentRef.setInput("menu", [linkItem]);
      fixture.detectChanges();

      const icons = await loader.getAllHarnesses(MatIconHarness);
      expect(icons.length).toBeGreaterThan(0);

      const icon = await icons[0].getName();
      expect(icon).toBe("dashboard");
    });

    it("should display icon for button menu items", async () => {
      const buttonItem = new ButtonMenuItem(
        "test.button",
        () => {},
        "settings",
      );

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.detectChanges();

      const icons = await loader.getAllHarnesses(MatIconHarness);
      expect(icons.length).toBeGreaterThan(0);

      const icon = await icons[0].getName();
      expect(icon).toBe("settings");
    });

    it("should display icon for href menu items", async () => {
      const hrefItem = new HrefMenuItem(
        "test.href",
        "https://example.com",
        "open_in_new",
      );

      fixture.componentRef.setInput("menu", [hrefItem]);
      fixture.detectChanges();

      const icons = await loader.getAllHarnesses(MatIconHarness);
      expect(icons.length).toBeGreaterThan(0);

      const icon = await icons[0].getName();
      expect(icon).toBe("open_in_new");
    });

    it("should display header icon when menu is closed", async () => {
      const headerItem: MenuItem = {
        type: MenuItemType.HEADER,
        title: "test.header",
        icon: "folder",
      } as MenuItem;

      component["menuMode"] = MenuMode.CLOSED;
      component["menuState"].set(MenuState.CLOSED);
      fixture.componentRef.setInput("menu", [headerItem]);
      fixture.detectChanges();

      const headerIcons = fixture.nativeElement.querySelectorAll("h3 mat-icon");
      expect(headerIcons.length).toBe(1);
    });

    it("should use default icon for header when no icon provided", async () => {
      const headerItem: MenuItem = {
        type: MenuItemType.HEADER,
        title: "test.header",
      } as MenuItem;

      component["menuMode"] = MenuMode.CLOSED;
      component["menuState"].set(MenuState.CLOSED);
      fixture.componentRef.setInput("menu", [headerItem]);
      fixture.detectChanges();

      const headerIcon = fixture.nativeElement.querySelector("h3 mat-icon");
      expect(headerIcon.textContent.trim()).toBe("more_horiz");
    });
  });

  describe("menu item states", () => {
    it("should disable link menu items when disabled property is true", () => {
      const linkItem = new LinkMenuItem("test.link", "/test-url", "link");
      linkItem.disabled = true;

      fixture.componentRef.setInput("menu", [linkItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector("a[mat-list-item]");
      expect(anchor.classList.contains("mdc-list-item--disabled")).toBe(true);
    });

    it("should disable button menu items when disabled property is true", async () => {
      const buttonItem = new ButtonMenuItem(
        "test.button",
        () => {},
        "settings",
      );
      buttonItem.disabled = true;

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      expect(button.disabled).toBe(true);
    });

    it("should disable href menu items when disabled property is true", () => {
      const hrefItem = new HrefMenuItem(
        "test.href",
        "https://example.com",
        "open_in_new",
      );
      hrefItem.disabled = true;

      fixture.componentRef.setInput("menu", [hrefItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector(
        "a[mat-list-item][href]",
      );
      expect(anchor.classList.contains("mdc-list-item--disabled")).toBe(true);
    });

    it("should activate link menu items when activated property is true", () => {
      const linkItem = new LinkMenuItem("test.link", "/test-url", "link");
      linkItem.activated = true;

      fixture.componentRef.setInput("menu", [linkItem]);
      fixture.detectChanges();

      const anchor = fixture.nativeElement.querySelector("a[mat-list-item]");
      expect(anchor.classList.contains("mdc-list-item--activated")).toBe(true);
    });

    it("should activate button menu items when matching activeItem", () => {
      const buttonItem = new ButtonMenuItem(
        "test.button",
        () => {},
        "settings",
      );

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.componentRef.setInput("activeItem", "test.button");
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      expect(button.classList.contains("mdc-list-item--activated")).toBe(true);
    });
  });

  describe("menu item interactions", () => {
    it("should call button function when clicked", () => {
      const mockFn = jest.fn();
      const buttonItem = new ButtonMenuItem("test.button", mockFn, "settings");

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      button.click();

      expect(mockFn).toHaveBeenCalled();
    });

    it("should not call button function when disabled", () => {
      const mockFn = jest.fn();
      const buttonItem = new ButtonMenuItem("test.button", mockFn, "settings");
      buttonItem.disabled = true;

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      button.click();

      expect(mockFn).not.toHaveBeenCalled();
    });

    it("should emit activeItemChange when button is clicked", () => {
      const activeItemChangeSpy = jest.fn();
      component["activeItemChange"].subscribe(activeItemChangeSpy);

      const buttonItem = new ButtonMenuItem(
        "test.button",
        () => {},
        "settings",
      );

      fixture.componentRef.setInput("menu", [buttonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      button.click();

      expect(activeItemChangeSpy).toHaveBeenCalledWith("test.button");
    });

    it("should handle async button menu items", () => {
      const asyncButtonItem = new AsyncButtonMenuItem(
        "test.async",
        () => of(void 0),
        "cloud",
      );

      fixture.componentRef.setInput("menu", [asyncButtonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      button.click();

      expect(utilServiceMock.setLoading).toHaveBeenCalledWith(true);
    });

    it("should re-enable button after async operation completes", (done) => {
      const asyncButtonItem = new AsyncButtonMenuItem(
        "test.async",
        () => of(void 0),
        "cloud",
      );

      fixture.componentRef.setInput("menu", [asyncButtonItem]);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector(
        "button[mat-list-item]",
      );
      button.click();

      setTimeout(() => {
        expect(asyncButtonItem.disabled).toBe(false);
        expect(utilServiceMock.setLoading).toHaveBeenCalledWith(false);
        done();
      }, 100);
    });
  });

  describe("menu toggle functionality", () => {
    it("should render toggle menu button in footer", () => {
      fixture.componentRef.setInput("menu", []);
      fixture.detectChanges();

      const toggleButton =
        fixture.nativeElement.querySelector("#toggleMenuButon");
      expect(toggleButton).toBeTruthy();
    });

    it("should toggle menu mode from CLOSED to AUTO", () => {
      const modeSpy = jest.fn();
      component["mode"].subscribe(modeSpy);

      component["menuMode"] = MenuMode.CLOSED;
      component.toggleMenu();

      expect(component["menuMode"]).toBe(MenuMode.AUTO);
      expect(modeSpy).toHaveBeenCalledWith("over");
    });

    it("should toggle menu mode from AUTO to OPEN", () => {
      const modeSpy = jest.fn();
      component["mode"].subscribe(modeSpy);

      component["menuMode"] = MenuMode.AUTO;
      component.toggleMenu();

      expect(component["menuMode"]).toBe(MenuMode.OPEN);
      expect(modeSpy).toHaveBeenCalledWith("side");
    });

    it("should toggle menu mode from OPEN to CLOSED", () => {
      const modeSpy = jest.fn();
      component["mode"].subscribe(modeSpy);

      component["menuMode"] = MenuMode.OPEN;
      component.toggleMenu();

      expect(component["menuMode"]).toBe(MenuMode.CLOSED);
      expect(modeSpy).toHaveBeenCalledWith("side");
    });
  });

  describe("menu hover behavior", () => {
    it("should open menu on mouse enter when in AUTO mode", () => {
      component["menuMode"] = MenuMode.AUTO;
      component.mouseEnter();

      expect(component["menuState"]()).toBe(MenuState.OPEN);
    });

    it("should close menu on mouse leave when in AUTO mode", () => {
      component["menuMode"] = MenuMode.AUTO;
      component.mouseLeave();

      expect(component["menuState"]()).toBe(MenuState.CLOSED);
    });

    it("should keep menu open on mouse leave when in OPEN mode", () => {
      component["menuMode"] = MenuMode.OPEN;
      component.mouseLeave();

      expect(component["menuState"]()).toBe(MenuState.OPEN);
    });

    it("should keep menu closed on mouse enter when in CLOSED mode", () => {
      component["menuMode"] = MenuMode.CLOSED;
      component.mouseEnter();

      expect(component["menuState"]()).toBe(MenuState.CLOSED);
    });
  });
});
