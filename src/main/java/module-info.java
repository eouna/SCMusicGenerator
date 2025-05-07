module com.eouna.scmusicgenerator {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires javafx.media;
  requires org.controlsfx.controls;
  requires org.kordamp.bootstrapfx.core;
  requires org.apache.commons.lang3;
  requires org.apache.commons.io;
  requires java.compiler;
  requires java.management;
  requires java.base;
  requires org.kordamp.ikonli.core;
  requires org.kordamp.ikonli.fontawesome;
  requires org.kordamp.ikonli.javafx;
  requires org.slf4j;
  requires maven.model;
  requires plexus.utils;
  requires java.desktop;
  requires TarsosDSP.core;
  requires TarsosDSP.jvm;
  requires org.apache.httpcomponents.client5.httpclient5;
  requires org.apache.httpcomponents.core5.httpcore5;

  opens com.eouna.scmusicgenerator.ui.controllers to
      javafx.fxml,
      javafx.base,
      javafx.controls,
      javafx.graphics;
  opens com.eouna.scmusicgenerator to
      java.base;

  exports com.eouna.scmusicgenerator;
}
