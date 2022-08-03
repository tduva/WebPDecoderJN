# WebPDecoderJN

Decode static or animated WebP images using JNA and native libraries based
on the libwebp library.

Only Windows and Linux 64bit currently supported.

## Requirements

* WebPDecoderJN-*.jar
* [JNA](https://github.com/java-native-access/jna)

## Usage

You can use `WebPDecoder.init()` to extract the native libs for the platform
from the JAR. This needs to be run once before any decoding attempts.

The `WebPDecoder.test()` function can be used to attempt decoding a WebP file
contained in the JAR in order to test if the native libraries can be found and
work properly.

Decode images using the `WebPDecoder.decode(byte[] data)` function and get a
`WebPImage` object containing some metadata and the individual frames.

[Javadocs](https://tduva.github.io/WebPDecoderJN/)

## Test App

The test app can be run to try out if the loading of the native libraries and
the decoding works. If you are not running it headless you can enter your own
URLs and paths to image files and then view the individual frames and some
metadata.

Run the `shadowJar` task in order to pack all the required code in the JAR
(build the `WebPDecoderJN-TestApp-all.jar` variant of the JAR).

## Compiling the Java library

Run `gradlew build` (personally I use `gradlew -Dorg.gradle.java.home="C:/Program Files (x86)/Java/jdk1.8.0_201" %* --console=verbose build` for specifying a specific JDK and
verbose output). This creates various JAR files under `lib/build/libs` and
`test-app/build/libs`. The `-all` variants include all dependencies.

## Compiling the native libraries

Some libraries are already included in compiled form, although you may want
to compile them yourself (or compile it for additional platforms).

The libraries are based on the [WebP project](https://developers.google.com/speed/webp/docs/compiling).

On Windows the `.dll` can be compiled like this using the Native Tools Command
Prompt for VS 2022:

    ..\libwebp-1.2.2>nmake /f Makefile.vc CFG=release-dynamic RTLIBCFG=dynamic OBJDIR=output
    
Otherwise refer to the compiling help of the libwebp library. Both the `libwebp`
and `libwebpdemux` are required.

WebP project license: [BSD 3-Clause](https://github.com/webmproject/libwebp/blob/main/COPYING)

The native library can then be put in `lib/src/main/resources/` in a subfolder
so that JNA can find it in the JAR (refer to JNA for the folder names).
