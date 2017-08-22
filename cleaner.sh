#!/usr/bin/env bash
#for /f "delims=" %%i in ('dir /s /b /o:n /ad ^| findstr /r "build"') do rd /s /q %%i" for Windows
# clean all build dir

#find . -name 'build' -delete
find . -name 'build' -exec rm -R '{}' \;