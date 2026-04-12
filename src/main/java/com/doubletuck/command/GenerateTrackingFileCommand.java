package com.doubletuck.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "generate-tracking-file",
    description = "Generates a CSV file that lists all the meets on the Virtius home page.",
    mixinStandardHelpOptions = true
)
public class GenerateTrackingFileCommand implements Runnable {

  @Option(names = "--export-directory",
      description = "Directory where the tracking file is written. Defaults to the export.data.directory value in application.properties.")
  private String exportDirectory;

  @Option(names = "--export-tracking-filename",
      description = "Name of the tracking file. Defaults to the export.tracking-filename value in application.properties.")
  private String exportTrackingFilename;

  @Option(names = "--overwrite-tracking-file",
      description = "If present, an existing tracking file is overwritten and all data within it is cleared. Otherwise, the file is updated with new information.")
  private boolean overwriteTrackingFile;

  @Override
  public void run() {
    throw new UnsupportedOperationException("generate-tracking-file is not yet implemented.");
  }
}