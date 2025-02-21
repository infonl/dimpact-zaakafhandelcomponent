/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  DebugElement,
  OnChanges,
  SimpleChange,
  SimpleChanges,
} from "@angular/core";
import { By } from "@angular/platform-browser";
import {ComponentFixture} from "@angular/core/testing";

export function updateComponentInputs<T extends OnChanges>(
  component: T,
  changes: Partial<T>,
  firstChange = false,
) {
  const simpleChanges: SimpleChanges = {};

  Object.keys(changes).forEach((changeKey) => {
    component[changeKey] = changes[changeKey];
    simpleChanges[changeKey] = new SimpleChange(
      null,
      changes[changeKey],
      firstChange,
    );
  });
  component.ngOnChanges(simpleChanges);
}

export function queryByText<T>(
  fixture: ComponentFixture<T>,
  selector: ValidHTMLTags,
  text: string,
): DebugElement | null {
  const elements = fixture.debugElement.queryAll(By.css(selector));
  return (
    elements.find(({ nativeElement }) =>
      nativeElement.textContent.includes(text),
    )
  );
}

type ValidHTMLTags = 'div' | 'span' | 'p' | 'a' | 'ul' | 'li' | 'table' | 'tr' | 'td' | 'th' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'button' | 'input' | 'textarea' | 'select' | 'option' | 'form' | 'label' | 'img' | 'nav' | 'header' | 'footer' | 'section' | 'article' | 'aside' | 'main' | 'figure' | 'figcaption' | 'blockquote' | 'code' | 'pre' | 'video' | 'audio' | 'canvas' | 'svg' | 'iframe' | 'details' | 'summary' | 'mark' | 'progress' | 'meter' | 'time' | 'output' | 'abbr' | 'address' | 'b' | 'bdi' | 'bdo' | 'cite' | 'data' | 'dfn' | 'em' | 'i' | 'kbd' | 'q' | 's' | 'samp' | 'small' | 'strong' | 'sub' | 'sup' | 'u' | 'var' | 'wbr';
