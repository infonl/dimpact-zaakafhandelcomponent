import { Pipe, PipeTransform } from "@angular/core";
import { FileFormat } from "src/app/informatie-objecten/model/file-format";

@Pipe({
  name: "mimetypeToExtension",
  standalone: true,
})
export class MimetypeToExtensionPipe implements PipeTransform {
  fileFormatExtesions = {
    [FileFormat["PDF"]]: ".pdf",
    [FileFormat["JPEG"]]: ".jpg/.jpeg",
    [FileFormat["BMP"]]: ".bmp",
    [FileFormat["GIF"]]: ".gif",
    [FileFormat["PNG"]]: ".png",
    [FileFormat["TEXT"]]: ".txt",
    [FileFormat["XLSX"]]: ".xlsx",
    [FileFormat["XLS"]]: ".xls",
    [FileFormat["PPTX"]]: ".pptx",
    [FileFormat["PPT"]]: ".ppt",
    [FileFormat["DOCX"]]: ".docx",
    [FileFormat["DOC"]]: ".doc",
    [FileFormat["ODT"]]: ".odt",
    [FileFormat["VSD"]]: ".vsd",
    [FileFormat["RTF"]]: ".rtf",
  };

  transform(mimetype: string): string {
    const isMimetypeSupported = Object.keys(this.fileFormatExtesions).includes(
      mimetype as FileFormat,
    );
    if (!isMimetypeSupported) {
      console.log(`Unsupported mimetype: ${mimetype}`);
      return mimetype;
    }
    return this.fileFormatExtesions[mimetype];
  }
}
