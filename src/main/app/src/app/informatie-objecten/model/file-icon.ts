/*
 * SPDX-FileCopyrightText: 2022 - 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class FileIcon {
  // Generic files
  private static readonly pdf = new FileIcon(
    "pdf",
    "picture_as_pdf",
    "darkred",
  );
  private static readonly txt = new FileIcon("txt", "text_snippet");
  // Office documents
  private static readonly doc = new FileIcon("doc", "description", "blue");
  private static readonly docx = new FileIcon("docx", "description", "blue");
  private static readonly ods = new FileIcon("ods", "description", "blue");
  private static readonly odt = new FileIcon("odt", "description", "blue");
  private static readonly ppt = new FileIcon("ppt", "slideshow", "red");
  private static readonly pptx = new FileIcon("pptx", "slideshow", "red");
  private static readonly rtf = new FileIcon("rtf", "description");
  private static readonly vsd = new FileIcon("vsd", "architecture", "orange");
  private static readonly xls = new FileIcon("xls", "table", "green");
  private static readonly xlsx = new FileIcon("xlsx", "table", "green");
  // Image files
  private static readonly bmp = new FileIcon("bmp", "image");
  private static readonly gif = new FileIcon("gif", "image");
  private static readonly jpeg = new FileIcon("jpeg", "image");
  private static readonly jpg = new FileIcon("jpg", "image");
  private static readonly png = new FileIcon("png", "image");
  // Video files
  private static readonly avi = new FileIcon("avi", "video_file");
  private static readonly flv = new FileIcon("flv", "video_file");
  private static readonly mkv = new FileIcon("mkv", "video_file");
  private static readonly mov = new FileIcon("mov", "video_file");
  private static readonly mp4 = new FileIcon("mp4", "video_file");
  private static readonly mpeg = new FileIcon("mpeg", "video_file");
  private static readonly wmv = new FileIcon("wmv", "video_file");
  // Other files
  private static readonly msg = new FileIcon("msg", "chat");
  private static readonly unknown = new FileIcon("unknown", "unknown_document");

  public static readonly fileIcons = [
    FileIcon.pdf,
    FileIcon.txt,
    FileIcon.doc,
    FileIcon.docx,
    FileIcon.ods,
    FileIcon.odt,
    FileIcon.ppt,
    FileIcon.pptx,
    FileIcon.rtf,
    FileIcon.vsd,
    FileIcon.xls,
    FileIcon.xlsx,
    FileIcon.bmp,
    FileIcon.gif,
    FileIcon.jpeg,
    FileIcon.jpg,
    FileIcon.png,
    FileIcon.avi,
    FileIcon.flv,
    FileIcon.mkv,
    FileIcon.mov,
    FileIcon.mp4,
    FileIcon.mpeg,
    FileIcon.wmv,
    FileIcon.msg,
  ].sort((fileIconA, fileIconB) => fileIconA.compare(fileIconB));

  public constructor(
    public readonly type: string,
    public readonly icon: string,
    public readonly color?: string,
  ) {}

  getBestandsextensie(): string {
    return "." + this.type.toLowerCase();
  }

  compare(other: FileIcon) {
    return this.type.localeCompare(other.type);
  }

  static getIconByBestandsnaam(bestandsnaam?: unknown) {
    const fileType = String(bestandsnaam).split(".").pop();
    const fileIcon = FileIcon.fileIcons.find(({ type }) => type === fileType);
    return fileIcon ?? FileIcon.unknown;
  }
}
