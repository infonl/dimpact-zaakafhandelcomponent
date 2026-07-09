/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class FormioFormulierContent {
  filename: string;
  content: string;

  constructor(filename: string, content: string) {
    this.filename = filename;
    this.content = content;
  }
}
