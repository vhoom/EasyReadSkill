@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ==================== 配置区域 ====================
set "SOURCE_DIR=E:\"
set "TARGET_DIR=C:\Users\aeix\Desktop\新建文件夹"

:: ==================== 文件类型定义（完整版） ====================
set "IMAGE_EXTS=.jpg .jpeg .jfif .pjpeg .png .gif .bmp .webp .svg .ico .tif .tiff .heic .heif .avif .raw .cr2 .nef .arw .dng .psd .ai .eps .exr .hdr .ppm .pgm .pbm .xbm .xpm"

set "VIDEO_EXTS=.mp4 .mkv .avi .mov .wmv .flv .webm .m4v .mpg .mpeg .3gp .rmvb .ts .m2ts .vob .divx .xvid .h264 .h265 .hevc .m4p .ogv .vp8 .vp9 .264 .265 .f4v .swf .qt .dv .mxf .mxg"

set "MUSIC_EXTS=.mp3 .wav .flac .aac .ogg .wma .m4a .alac .ape .opus .aiff .aif .au .mid .midi .amr .dts .ac3 .cda .dff .dsf .mka .mp2 .mpa .ra .rm .shn .tak .tta .voc .wv .aa .aax .m3u .pls"
:: ==============================================================
:: 检查源目录是否存在
if not exist "%SOURCE_DIR%" (
    echo [错误] 源目录不存在: %SOURCE_DIR%
    pause
    exit /b 1
)

:: 创建目标子目录
set "TARGET_IMAGE=%TARGET_DIR%\image"
set "TARGET_VIDEO=%TARGET_DIR%\video"
set "TARGET_MUSIC=%TARGET_DIR%\music"

if not exist "%TARGET_IMAGE%" mkdir "%TARGET_IMAGE%" 2>nul
if not exist "%TARGET_VIDEO%" mkdir "%TARGET_VIDEO%" 2>nul
if not exist "%TARGET_MUSIC%" mkdir "%TARGET_MUSIC%" 2>nul

echo 源目录: %SOURCE_DIR%
echo 目标目录: %TARGET_DIR%
echo.
echo 分类规则:
echo   [图片] -> image\
echo   [视频] -> video\
echo   [音频] -> music\
echo.
echo 正在扫描并复制文件，请稍候...
echo.

set /a IMAGE_COUNT=0
set /a VIDEO_COUNT=0
set /a MUSIC_COUNT=0
set /a SKIP_COUNT=0

:: 递归遍历源目录下的所有文件
for /r "%SOURCE_DIR%" %%F in (*) do (
    set "FILE_EXT=%%~xF"
    set "FILE_NAME=%%~nxF"
    set "FILE_PATH=%%F"
    
    :: 判断文件类型
    set "FILE_TYPE="
    set "TARGET_SUB="
    
    echo !IMAGE_EXTS! | findstr /i " !FILE_EXT! " >nul
    if !errorlevel! equ 0 (
        set "FILE_TYPE=图片"
        set "TARGET_SUB=!TARGET_IMAGE!"
    )
    
    if not defined FILE_TYPE (
        echo !VIDEO_EXTS! | findstr /i " !FILE_EXT! " >nul
        if !errorlevel! equ 0 (
            set "FILE_TYPE=视频"
            set "TARGET_SUB=!TARGET_VIDEO!"
        )
    )
    
    if not defined FILE_TYPE (
        echo !MUSIC_EXTS! | findstr /i " !FILE_EXT! " >nul
        if !errorlevel! equ 0 (
            set "FILE_TYPE=音频"
            set "TARGET_SUB=!TARGET_MUSIC!"
        )
    )
    
    :: 如果是支持的文件类型
    if defined FILE_TYPE (
        set "TARGET_FILE=!TARGET_SUB!\!FILE_NAME!"
        
        :: 检查目标文件是否已存在
        if exist "!TARGET_FILE!" (
            set /a SKIP_COUNT+=1
            echo [跳过] [!FILE_TYPE!] !FILE_NAME! ^(已存在^)
        ) else (
            copy "%%F" "!TARGET_FILE!" >nul 2>nul
            if !errorlevel! equ 0 (
                if "!FILE_TYPE!"=="图片" set /a IMAGE_COUNT+=1
                if "!FILE_TYPE!"=="视频" set /a VIDEO_COUNT+=1
                if "!FILE_TYPE!"=="音频" set /a MUSIC_COUNT+=1
                echo [OK] [!FILE_TYPE!] !FILE_NAME!
            ) else (
                echo [失败] [!FILE_TYPE!] !FILE_NAME!
            )
        )
    )
)

echo.
echo ==========================================
echo 复制完成！统计信息:
echo.
echo   图片: %IMAGE_COUNT% 个 (image\)
echo   视频: %VIDEO_COUNT% 个 (video\)
echo   音频: %MUSIC_COUNT% 个 (music\)
echo   跳过: %SKIP_COUNT% 个 (目标已存在)
echo.
echo 目标目录: %TARGET_DIR%
echo ==========================================
pause
exit /b 0