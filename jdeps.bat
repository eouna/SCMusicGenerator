@echo off
jdeps --module-path target --print-module-deps --ignore-missing-deps --recursive .\target\SCMusicGenerator-1.0.2.jar
