# Compile for Windows

All commands are entered in the "x64 Native Tools Command Prompt for VS 2022"
(or the x86 variant). It may also work other ways, but that's how it's been
tested.

## Step 1: Compile static libraries

Download and compile libwebp static libraries, as per the
[documentation](https://developers.google.com/speed/webp/docs/compiling).

    nmake /f Makefile.vc CFG=release-static RTLIBCFG=static OBJDIR=output
    
For some reason the output doesn't appear to have header files, but they can be
taken from elsewhere (e.g. from the precompiled packages from the
[downloads repository](https://storage.googleapis.com/downloads.webmproject.org/releases/webp/index.html)).

## Step 2: Compile dynamic library

For compiling the folder structure should look like this:

    libwebp_animdecoder.c
    makefile
    include\webp\decode.h
    include\webp\demux.h
    include\webp\types.h
    lib\libwebp.lib
    lib\libwebpdemux.lib
    
Run `nmake` to create the library.

## Other

* `dumpbin /exports <file>` - View what functions the file exports

