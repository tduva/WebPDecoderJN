# Compile for Mac / Linux

## Step 1: Compile static libraries

Download and compile libwebp static libraries, as per the
[documentation](https://developers.google.com/speed/webp/docs/compiling). The
"Preparing the Platform" doesn't seem required for just building WebP decoding
libraries.

I've used this command to build (replace `<dir>` with a directory where the
files will placed to override the default where it installs it somewhere on the
system):

    ./configure --prefix=<dir>
    make
    make install

On Mac ARM64, cross-compiling for Mac x86-64:

    ./configure --prefix=<dir> --build `./config.guess` --host x86_64-apple-darwin CFLAGS='-arch x86_64 -O2 -g'

If in the next step it complains about `fPic`, you may need to recompile this
step with the flag set:

    ./configure --prefix=<dir> CFLAGS='-fPIC -O2 -g'

## Step 2: Compile dynamic library

For compiling the folder structure should look like this (this could be the
prefix `<dir>` specified above):

    libwebp_animdecoder.c
    makefile
    include\webp\decode.h
    include\webp\demux.h
    include\webp\types.h
    lib\libwebp.lib
    lib\libwebpdemux.lib
    
Run `make` to create the library. When cross-compiling for Mac x86-64 use
`make cross=1`. Optionally `make strip` can be used to remove debug info.

JNA searches for a library prefixed with `lib` on some platforms, but since the
library itself is called `libwebp_animdecoder` it's called `liblibwebp_animdecoder`
on Mac/Linux.

## Other

* `file <file>` - For general info about the file (e.g. check the arch)
* `objdump -x <file>` - View info (especially at the top) on what it depends and
  a lot of stuff I don't understand (on Mac `otool` might be useful)
* `nm -D <file>` / `nm -g <file>` - View what functions the file exports

