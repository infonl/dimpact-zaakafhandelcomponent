/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import {
  FileFormat,
  FileFormatExtensionMapping,
} from "src/app/informatie-objecten/model/file-format";

@Pipe({
  name: "mimetypeToExtension",
  standalone: true,
})
export class MimetypeToExtensionPipe implements PipeTransform {
  fileFormatExtesions = FileFormatExtensionMapping;

  transform(mimetype: string): string {
    const isMimetypeSupported = Object.keys(this.fileFormatExtesions).includes(
      mimetype as FileFormat,
    );
    if (!isMimetypeSupported) {
      return mimetype;
    }
    return this.fileFormatExtesions[mimetype];
  }
}
