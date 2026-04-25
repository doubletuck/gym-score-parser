package com.doubletuck;

import com.doubletuck.command.BulkExportScoresCommand;
import com.doubletuck.command.ExportScoresCommand;
import com.doubletuck.command.GenerateTrackingFileCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "gym-score-parser", description = "CLI for exporting gym meet scores from Virtius.", subcommands = {
    BulkExportScoresCommand.class,
    ExportScoresCommand.class,
    GenerateTrackingFileCommand.class }, mixinStandardHelpOptions = true)
public class GymScoreParserApplication {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new GymScoreParserApplication()).execute(args);
    System.exit(exitCode);
  }
}