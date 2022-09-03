#include <stdio.h>
#include "include/webp/decode.h"
#include "include/webp/demux.h"

#ifdef _WIN64
#pragma comment(linker, "/export:WebPMalloc")
#pragma comment(linker, "/export:WebPFree")
#pragma comment(linker, "/export:WebPAnimDecoderNewInternal")
#pragma comment(linker, "/export:WebPAnimDecoderGetInfo")
#pragma comment(linker, "/export:WebPAnimDecoderHasMoreFrames")
#pragma comment(linker, "/export:WebPAnimDecoderDelete")
#pragma comment(linker, "/export:WebPAnimDecoderGetNext")
#else
#pragma comment(linker, "/export:_WebPMalloc")
#pragma comment(linker, "/export:_WebPFree")
#pragma comment(linker, "/export:_WebPAnimDecoderNewInternal")
#pragma comment(linker, "/export:_WebPAnimDecoderGetInfo")
#pragma comment(linker, "/export:_WebPAnimDecoderHasMoreFrames")
#pragma comment(linker, "/export:_WebPAnimDecoderDelete")
#pragma comment(linker, "/export:_WebPAnimDecoderGetNext")
#endif
