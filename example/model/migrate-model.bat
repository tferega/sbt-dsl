@echo off
setlocal
pushd "%~dp0\lib"

set PACKAGE=org.example
set MODULE=storage

java ^
  -jar dsl-clc.jar ^
  download ^
  dsl=..\dsl ^
  namespace=%PACKAGE% ^
  revenj.java=..\..\lib\%MODULE%-revenj.jar ^
  settings=manual-json ^
  "postgres=localhost:5432/%MODULE%_db?user=%MODULE%_user&password=%MODULE%_pass" ^
  sql=..\sql ^
  apply
