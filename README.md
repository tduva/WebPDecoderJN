# WebPDecoderJN

Decode static or animated WebP images into individual frames (and metadata),
using JNA and native libraries based on the libwebp library.

Includes native libraries for (must fit the JRE it is run with):

* Windows x86, x86-64
* Linux x86-64
* Mac x86-64, arm64

## Requirements

You need the `WebPDecoderJN-*.jar` file as well as [JNA](https://github.com/java-native-access/jna).

The `-all` variant contains JNA already, however if you are already using JNA in
your project you should probably choose the regular variant to avoid conflicts.

## Usage

You can use `WebPDecoder.init()` to extract the native libs for the platform
from the JAR. This needs to be run once before any decoding attempts. If it is
not used JNA will take care of looking for the library, which may still end up
with extracting it from the JAR if it is not found in the system. Using `init()`
just ensures that it is loaded from the JAR.

The `WebPDecoder.test()` function can be used to attempt decoding a WebP file
contained in the JAR in order to test if the native libraries work properly.

Decode images using the `WebPDecoder.decode(byte[] data)` function and get a
`WebPImage` object containing some metadata and the individual frames.

If your goal is to display the image in Swing this is outside the scope of this
project, however [this](https://github.com/chatty/chatty/blob/master/src/chatty/util/gif/ListAnimatedImage.java)
may give you a starting point. From what I understand you need an ImageProducer
that provides the frames and then turn that into an Image object.

[Javadocs](https://tduva.github.io/WebPDecoderJN/)

## Test App

The test app can be run to try out if the loading of the native libraries and
the decoding works. If you are not running it headless you can enter your own
URLs and paths to image files and then view the individual frames and some
metadata.

The native library is extracted from the JAR, unless it is found in the same
directory as the JAR that contains the WebPDecoder class.

Optionally parameters can be provided:

    java -jar WebPDecoderJN-TestApp-1.2-all.jar [URL] [decodeCount]
    
* The URL will be filled as default value into the GUI.
* If a decodeCount greater than 0 is provided, the URL will also be decoded that
  many times (before the GUI opens), which can act as a simple performance test,
  even in headless mode.

## Compiling the Java library

Run `gradlew build` (personally I use something like `gradlew -Dorg.gradle.java.home="C:/Program Files (x86)/Java/jdk1.8.0_201" build --console=verbose` for
specifying a specific JDK and verbose output). This creates various JAR files
under `lib/build/libs` and `test-app/build/libs`. The `-all` variants include
all dependencies.

## Compiling the native libraries

Some libraries are already included in compiled form, although you may want
to compile them yourself (or compile it for additional platforms).

The libraries are based on the [WebP project](https://developers.google.com/speed/webp/docs/compiling).
To decode animated WebP images normally both the `libwebp` and `libwebpdemux`
dynamic libraries would be required. However for this project the required
functions are built into a single dynamic library `libwebp_animdecoder` using
the provided C "program" (really just a file for the linker to include the
required code/exported functions).

See the `build-native` folder for platform-specific documentation. If you want
to build for a new platform, you may need to adjust the instructions to fit your
needs.

WebP library project license: [BSD 3-Clause](https://github.com/webmproject/libwebp/blob/main/COPYING)

The native library can then be put in `lib/src/main/resources/` in a subfolder
so that JNA can find it in the JAR (refer to JNA for the folder names).
