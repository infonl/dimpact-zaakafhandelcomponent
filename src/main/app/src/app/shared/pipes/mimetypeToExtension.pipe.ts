/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import {
  FileFormat,
  FileFormatExtensionMapping,
} from "src/app/informatie-objecten/model/file-format";

@Pipe({
  name: "mimetypeToExtension",
})
export class MimetypeToExtensionPipe implements PipeTransform {
  fileFormatExtesions = FileFormatExtensionMapping;

  transform(mimetype: string): string {
    if (!this.isSupportedMimeType(mimetype)) {
      return mimetype;
    }
    return this.fileFormatExtesions[mimetype];
  }

  private isSupportedMimeType(mimetype: string): mimetype is FileFormat {
    return Object.keys(this.fileFormatExtesions).includes(mimetype);
  }
}
