package com.doubletuck.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {

  private static final String PROPERTIES_FILE = "application.properties";
  private static AppProperties instance;

  private final Properties props = new Properties();

  private AppProperties() {
    try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      if (in != null) {
        props.load(in);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + PROPERTIES_FILE, e);
    }
  }

  public static AppProperties getInstance() {
    if (instance == null) {
      instance = new AppProperties();
    }
    return instance;
  }

  public String getExportDataDirectory() {
    return props.getProperty("export.data.directory", "data");
  }

  public String getExportTrackingFilename() {
    return props.getProperty("export.tracking-filename", "meet_scores_export_status.csv");
  }
}
