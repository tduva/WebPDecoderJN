# Run "make cross=1" to use this
ifdef cross
	TARGET = -target x86_64-apple-darwin
endif

# Try to detect Mac
OUTFILE_EXT = .so
ifeq ($(shell uname -s),Darwin)
	OUTFILE_EXT = .dylib
endif

LIBNAME = libwebp_animdecoder
OUTFILE = lib$(LIBNAME)$(OUTFILE_EXT)
INFILE = $(LIBNAME)
LIBWEBP = lib/libwebp.a lib/libwebpdemux.a

# Main section
$(OUTFILE): $(INFILE).o $(LIBWEBP)
	gcc $(TARGET) -shared -o $(OUTFILE) $(INFILE).o $(LIBWEBP)
$(INFILE).o : $(INFILE).c
	gcc $(TARGET) -c -g $(INFILE).c

.PHONY: strip
strip: $(OUTFILE)
	strip --strip-debug $(OUTFILE)
