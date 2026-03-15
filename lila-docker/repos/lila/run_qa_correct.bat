@echo off
setlocal enabledelayedexpansion

:: f.env 파일에서 환경 변수를 읽어와 설정합니다.
for /f "usebackq tokens=1,2 delims==" %%A in ("settings.env") do (
    set "key=%%A"
    set "value=%%B"
    :: 공백 제거
    set "key=!key: =!"
    set "value=!value: =!"
    if not "!key!"=="" (
        set "!key!=!value!"
    )
)

:: QA 도구를 실행합니다.
sbt "llm/testOnly lila.llm.analysis.BookmakerThesisQaRunner"
