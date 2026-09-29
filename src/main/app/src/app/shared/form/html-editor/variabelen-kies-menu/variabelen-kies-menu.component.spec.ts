/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { Editor } from "ngx-editor";
import { fromPartial } from "src/test-helpers";
import { VariabelenKiesMenuComponent } from "./variabelen-kies-menu.component";

const makeEditor = () => {
  const exec = jest.fn();
  const insertText = jest.fn().mockReturnValue({ exec });
  const editor = fromPartial<Editor>({ commands: { insertText } });

  return { editor, insertText, exec };
};

describe(VariabelenKiesMenuComponent.name, () => {
  const user = userEvent.setup();

  const setup = async (variabelen = ["fakeNaam", "fakeAdres"]) => {
    const { editor, insertText, exec } = makeEditor();

    const { fixture } = await render(VariabelenKiesMenuComponent, {
      inputs: { editor, variabelen },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
    });

    return { fixture, insertText, exec };
  };

  const openMenu = () =>
    user.click(screen.getByRole("button", { name: "variabelen" }));

  it("shows a closed menu until the button is clicked", async () => {
    await setup();

    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("offers one item per variabele, labelled with the variabele and its description", async () => {
    await setup();

    await openMenu();

    const [naamItem, adresItem, ...otherItems] =
      screen.getAllByRole("menuitem");
    expect(naamItem).toHaveAccessibleName(
      "fakeNaam: mailtemplate.variabele.fakeNaam",
    );
    expect(adresItem).toHaveAccessibleName(
      "fakeAdres: mailtemplate.variabele.fakeAdres",
    );
    expect(otherItems).toHaveLength(0);
  });

  it("offers no items when there are no variabelen", async () => {
    await setup([]);

    await openMenu();

    expect(screen.queryAllByRole("menuitem")).toHaveLength(0);
  });

  it("inserts the chosen variabele between curly braces into the editor", async () => {
    const { insertText, exec } = await setup();

    await openMenu();
    await user.click(screen.getByRole("menuitem", { name: /^fakeAdres:/ }));

    expect(insertText).toHaveBeenCalledTimes(1);
    expect(insertText).toHaveBeenCalledWith("{fakeAdres}");
    expect(exec).toHaveBeenCalledTimes(1);
  });

  it("offers the new variabelen once the variabelen change", async () => {
    const { fixture } = await setup();

    fixture.componentRef.setInput("variabelen", ["fakeZaaknummer"]);
    fixture.detectChanges();
    await openMenu();

    expect(screen.getAllByRole("menuitem")).toHaveLength(1);
    expect(
      screen.getByRole("menuitem", { name: /^fakeZaaknummer:/ }),
    ).toBeInTheDocument();
  });

  it("inserts into the new editor once the editor changes", async () => {
    const { fixture, insertText } = await setup();
    const otherEditor = makeEditor();

    fixture.componentRef.setInput("editor", otherEditor.editor);
    fixture.detectChanges();
    await openMenu();
    await user.click(screen.getByRole("menuitem", { name: /^fakeNaam:/ }));

    expect(otherEditor.insertText).toHaveBeenCalledWith("{fakeNaam}");
    expect(otherEditor.exec).toHaveBeenCalledTimes(1);
    expect(insertText).not.toHaveBeenCalled();
  });
});
