for /f "delims=" %%i in ('dir /s /b /o:n /ad ^| findstr /r "build"') do rd /s /q %%i
rem for /f "delims=" %%i in ('dir /s /b /o:n /ad ^| findstr /r ".gradle"') do rd /s /q %%i
rem for /f "delims=" %%i in ('dir /s /b /o:n /ad ^| findstr /r ".idea"') do rd /s /q %%i