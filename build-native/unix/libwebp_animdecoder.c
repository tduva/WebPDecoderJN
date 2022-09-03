#include <stdio.h>
#include "include/webp/decode.h"
#include "include/webp/demux.h"

int main() {
	// Fake calls so the functions are exported
	WebPGetDecoderVersion();
	WebPGetDemuxVersion();
	WebPData* data;
	WebPAnimDecoderOptions dec_options;
	WebPAnimDecoder* dec = WebPAnimDecoderNew(data, &dec_options);
	WebPAnimInfo* info;
	WebPAnimDecoderGetInfo(dec, info);
	WebPAnimDecoderHasMoreFrames(dec);
	uint8_t** buf;
	int* timestamp;
	WebPAnimDecoderGetNext(dec, buf, timestamp);
	WebPAnimDecoderDelete(dec);
	WebPMalloc(30);
	WebPFree(dec);
}
