LIBNAME = libwebp_animdecoder
OUTFILE = $(LIBNAME).dll
INFILE = $(LIBNAME)
LIBWEBP = lib/libwebp.lib lib/libwebpdemux.lib

$(OUTFILE) : $(INFILE).obj
	link /nologo /DLL /OUT:$(OUTFILE) $(INFILE).obj $(LIBWEBP)

$(INFILE).obj : $(INFILE).c
	cl /nologo /c $(INFILE).c
