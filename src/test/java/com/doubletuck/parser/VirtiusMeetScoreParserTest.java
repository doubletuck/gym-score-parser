package com.doubletuck.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for VirtiusMeetScoreParser focusing on the TSV-to-CSV conversion logic.
 */
class VirtiusMeetScoreParserTest {

  @TempDir
  Path tempDir;

  private VirtiusMeetScoreParser parser;

  @BeforeEach
  void setUp() {
    parser = new VirtiusMeetScoreParser(tempDir, List.of());
  }

  // --- escapeCsv ---

  @Test
  void escapeCsv_plainValue_returnedAsIs() {
    assertThat(parser.escapeCsv("hello")).isEqualTo("hello");
  }

  @Test
  void escapeCsv_nullValue_returnsEmptyString() {
    assertThat(parser.escapeCsv(null)).isEqualTo("");
  }

  @Test
  void escapeCsv_emptyString_returnsEmptyString() {
    assertThat(parser.escapeCsv("")).isEqualTo("");
  }

  @Test
  void escapeCsv_valueWithComma_wrapsInQuotes() {
    assertThat(parser.escapeCsv("Smith, John")).isEqualTo("\"Smith, John\"");
  }

  @Test
  void escapeCsv_valueWithDoubleQuote_escapesAndWraps() {
    assertThat(parser.escapeCsv("say \"hello\"")).isEqualTo("\"say \"\"hello\"\"\"");
  }

  @Test
  void escapeCsv_valueWithNewline_wrapsInQuotes() {
    assertThat(parser.escapeCsv("line1\nline2")).isEqualTo("\"line1\nline2\"");
  }

  @Test
  void escapeCsv_valueWithCommaAndQuote_escapesQuoteAndWraps() {
    assertThat(parser.escapeCsv("a,\"b\"")).isEqualTo("\"a,\"\"b\"\"\"");
  }

  // --- writeTsvAsCsv ---

  @Test
  void writeTsvAsCsv_simpleValues_producesCorrectCsv() throws IOException {
    String tsv = "Name\tScore\tRank\nAlice\t9.5\t1";
    Path output = tempDir.resolve("out.csv");
    parser.writeTsvAsCsv(tsv, output);

    String csv = Files.readString(output);
    assertThat(csv).isEqualTo("Name,Score,Rank\nAlice,9.5,1\n");
  }

  @Test
  void writeTsvAsCsv_valueWithComma_isQuoted() throws IOException {
    String tsv = "Name\tNote\nSmith, John\tOK";
    Path output = tempDir.resolve("out.csv");
    parser.writeTsvAsCsv(tsv, output);

    String csv = Files.readString(output);
    assertThat(csv).contains("\"Smith, John\"");
  }

  @Test
  void writeTsvAsCsv_valueWithDoubleQuote_isEscaped() throws IOException {
    String tsv = "Name\tNote\nAlice\tsay \"hi\"";
    Path output = tempDir.resolve("out.csv");
    parser.writeTsvAsCsv(tsv, output);

    String csv = Files.readString(output);
    assertThat(csv).contains("\"say \"\"hi\"\"\"");
  }

  @Test
  void writeTsvAsCsv_rowWithFewerColumnsThanHeader_isPaddedWithEmptyValues() throws IOException {
    String tsv = "A\tB\tC\nonly_one";
    Path output = tempDir.resolve("out.csv");
    parser.writeTsvAsCsv(tsv, output);

    String csv = Files.readString(output);
    assertThat(csv).contains("only_one,,");
  }

  @Test
  void writeTsvAsCsv_multipleRows_allWritten() throws IOException {
    String tsv = "Name\tScore\nAlice\t9.5\nBob\t8.8\nCarol\t9.1";
    Path output = tempDir.resolve("out.csv");
    parser.writeTsvAsCsv(tsv, output);

    List<String> lines = Files.readAllLines(output);
    assertThat(lines).hasSize(4);
  }

  @Test
  void writeTsvAsCsv_emptyTsv_throwsIllegalArgumentException() {
    Path output = tempDir.resolve("out.csv");
    assertThatThrownBy(() -> parser.writeTsvAsCsv("", output))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void writeTsvAsCsv_overwritesExistingFile() throws IOException {
    Path output = tempDir.resolve("out.csv");
    Files.writeString(output, "old content");
    parser.writeTsvAsCsv("A\tB\n1\t2", output);
    String csv = Files.readString(output);
    assertThat(csv).doesNotContain("old content");
    assertThat(csv).startsWith("A,B");
  }
}
