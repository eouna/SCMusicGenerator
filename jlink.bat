@echo off
if exist "%~dp0\jre" (
  rmdir /s /q "%~dp0\jre"
  echo delete old jre success
)
jlink.exe --module-path "%JAVA_HOME%/jmods" --strip-debug --add-modules java.base,java.desktop,java.instrument,java.management,java.naming,java.scripting,javafx.media,javafx.base,javafx.graphics,javafx.fxml,javafx.controls,jdk.jfr,jdk.unsupported --compress=2 --no-header-files --no-man-pages --output jre
echo build simple runtime env success
