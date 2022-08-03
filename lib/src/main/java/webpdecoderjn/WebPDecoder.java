
package webpdecoderjn;

import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Decode a WebP image using native libraries.
 * 
 * <p>
 * The native libraries need to be made available before any decoding attempts.
 * Both the {@code libwebp} and {@code libwebpdemux} are required. For some
 * platforms they are packed in the JAR and can be extracted using the
 * {@link #init()} function, which must be run before any of the decode or test
 * functions. You can also place the libraries somewhere they can be found,
 * which may be platform dependent, especially in regards to the dependency of
 * {@code libwebpdemux} on {@code libwebp} works (e.g. on Windows it may have to
 * be in the same folder with the correct name).
 * 
 * <p>
 * The functions using the native libraries may throw an
 * {@code UnsatisfiedLinkError}. Since this is an error it is recommended to
 * catch it explicitly instead of catching {@code Error} or {@code Throwable}.
 *
 * @author tduva
 */
public class WebPDecoder {
    
    private static final Logger LOGGER = Logger.getLogger(WebPDecoder.class.getName());
    
    //==========================
    // Library Loading
    //==========================
    private static boolean initialized = false;
    private static volatile String libPath;
    private static volatile String libPathDemux;

    /**
     * This function is intended to be used before decoding is attempted (or
     * {@link #test()} is used). Extracts the platform dependent libraries from
     * the JAR to a temp folder chosen by JNA.
     * 
     * <p>
     * On Windows the files are renamed back to the original file name (the
     * "libwebp.dll" file appears to be expected in the same folder as the
     * "libwebpdemux.dll"). If the files are found on the classpath outside the
     * JAR (if they have been placed there) likely no renaming will have to be
     * done.
     *
     * <p>
     * When this function succeeds, it will explicitly look for the libwebpdemux
     * file (full path) when loading the library.
     *
     * <p>
     * This function is not required if both libraries (libwebp and
     * libwebpdemux) are made available differently, as long as they can both be
     * found, in the same folder.
     *
     * @throws IOException When extracting a library fails, in which case it may
     * not be possible to decode images
     */
    public static synchronized void init() throws IOException {
        if (!initialized) {
            libPath = extractLib("libwebp").toString();
            libPathDemux = extractLib("libwebpdemux").toString();
            initialized = true;
        }
    }
    
    private static LibWebP libWebPInstance;
    private static LibWebPDemux LibWebDemuxInstance;
    
    private static synchronized LibWebPDemux libDemux() {
        if (LibWebDemuxInstance == null) {
            /**
             * The demux lib depends on the main lib, so that one should be
             * loaded first, if necessary. As it is coded, this should happen
             * anyway, but just in case it is changed.
             * 
             * On Linux loading the lib seems to help in order for it to be
             * found. On Windows the dll seemed to be required in the same
             * folder with the correct name, although not sure if that is always
             * the case.
             */
            libMain();
            LibWebDemuxInstance = Native.load(libPathDemux != null ? libPathDemux : "libwebpdemux", LibWebPDemux.class);
        }
        return LibWebDemuxInstance;
    }
    
    private static synchronized LibWebP libMain() {
        if (libWebPInstance == null) {
            libWebPInstance = Native.load(libPath != null ? libPath : "libwebp", LibWebP.class);
        }
        return libWebPInstance;
    }
    
    /**
     * Extract the library from the JAR and rename it to the correct name, since
     * otherwise the dependency on libwebp.dll doesn't seem to be resolved.
     *
     * @param name
     * @return
     * @throws IOException
     */
    private static Path extractLib(String name) throws IOException {
        Path path = Native.extractFromResourcePath(name).toPath();
        String targetName = makeTargetName(path, name);
        if (path.getFileName().toString().equals(targetName) || Platform.isLinux()) {
            // If the file is found on the classpath outside JAR, no renaming
            return path;
        }
        // Put in subfolder per platform, in case both x86 and x64 JRE are used
        Path newPath = path.resolveSibling(
                String.format("%s/%s", Platform.RESOURCE_PREFIX, targetName));
        Files.createDirectories(newPath.getParent());
        try {
            if (Objects.equals(System.getProperty("jna.debug_load"), "true")) {
                LOGGER.info(String.format("Move: %s -> %s", path, newPath));
            }
            Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception ex) {
            // The same file may exist, but e.g. be in use (no overwriting)
            if (!filesEqual(path, newPath)) {
                // If not the correct file, it's an error
                throw ex;
            }
        }
        return newPath;
    }
    
    /**
     * Makes the name the file should have in the end (e.g. libwebp.dll). The
     * name input is normally just "libwebp", but in case that changes this can
     * also handle formats such as "/libwebp.dll".
     *
     * It takes the file extension (if any) from the path JNA created, but uses
     * the original name.
     *
     * @param path The path JNA created or found
     * @param name The name that was searched for
     * @return
     */
    private static String makeTargetName(Path path, String name) {
        String ext = getFileExtension(path.getFileName().toString());
        name = Paths.get(name).getFileName().toString();
        if (!name.endsWith(ext)) {
            name += ext;
        }
        return name;
    }
    
    //==========================
    // Decoding
    //==========================
    /**
     * Same as {@link #testEx()} but instead of throwing an exception it only
     * return {@code true} or {@code false}.
     * 
     * @return {@code true} if everything seems ok, {@code false} otherwise
     */
    public static boolean test() {
        try {
            testEx();
            return true;
        }
        catch (IOException | UnsatisfiedLinkError ex) {
            // Just catch everything, any exception or error counts as a failure
            return false;
        }
    }
    
    /**
     * Decode a test image included in the JAR to check if the native libraries
     * are properly loaded and if everything seems to work.
     * 
     * @throws IOException If loading the test image fails
     * @throws WebPDecoderException If the decoder encounters an error
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     * libraries (note that this is an error, not an exception)
     */
    public static void testEx() throws IOException, WebPDecoderException,
                                                    UnsatisfiedLinkError {
        URL url = WebPDecoder.class.getResource("/image/test.webp");
        WebPImage image = decode(getBytesFromURL(url));
        boolean expectedResult = image.canvasWidth == 16 && image.canvasHeight == 16
                && image.frameCount == 2 && image.frames.size() == 2
                && image.loopCount == 1 && image.frames.get(0).delay == 480
                && image.frames.get(1).delay == 1280;
        if (!expectedResult) {
            throw new WebPDecoderException("Unexpected decode result");
        }
    }
    
    /**
     * Decode a WebP image based on the url in the given String.
     * 
     * @param url The url
     * @return A decoded WebPImage
     * @throws IOException When loading the data from the url fails
     * @throws WebPDecoderException When the decoder encounters an issue (e.g.
     * if it's not a valid WebP file)
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     * libraries (note that this is an error, not an exception)
     */
    public static WebPImage decodeUrl(String url) throws IOException,
                                                         WebPDecoderException,
                                                         UnsatisfiedLinkError {
        byte[] rawData = getBytesFromURL(new URL(url));
        return decode(rawData);
    }
    
    /**
     * Decode a WebP image.
     * 
     * @param rawData The raw bytes of the image
     * @return A decoded WebPImage
     * @throws WebPDecoderException When the decoder encounters an issue (e.g.
     * if it's not a valid WebP file)
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     * libraries (note that this is an error, not an exception)
     */
    public static WebPImage decode(byte[] rawData) throws WebPDecoderException,
                                                          UnsatisfiedLinkError {
        List<WebPImageFrame> frames = new ArrayList<>();
        Pointer bytes = null;
        Pointer decoder = null;
        LibWebPDemux.WebPAnimInfo info;
        try {
            bytes = libMain().WebPMalloc(rawData.length);
            bytes.write(0, rawData, 0, rawData.length);
            
            LibWebPDemux.WebPData data = new LibWebPDemux.WebPData();
            data.bytes = bytes;
            data.length = new Size_T(rawData.length);
            
            decoder = libDemux().WebPAnimDecoderNewInternal(data, null, LibWebPDemux.WEBP_DEMUX_ABI_VERSION);
            if (decoder == null) {
                throw new WebPDecoderException("Failed creating decoder, invalid image?");
            }

            info = new LibWebPDemux.WebPAnimInfo();
            if (libDemux().WebPAnimDecoderGetInfo(decoder, info) == 0) {
                throw new WebPDecoderException("Failed getting decoder info");
            }
            
            int prevTimestamp = 0;
            while (libDemux().WebPAnimDecoderHasMoreFrames(decoder) == 1) {
                PointerByReference buf = new PointerByReference();
                IntByReference timestamp = new IntByReference();
                
                if (libDemux().WebPAnimDecoderGetNext(decoder, buf, timestamp) == 0) {
                    throw new WebPDecoderException("Error decoding next frame");
                }
                
                int delay = timestamp.getValue() - prevTimestamp;
                prevTimestamp = timestamp.getValue();
                
                BufferedImage image = createImage(buf.getValue(), info.canvas_width, info.canvas_height);
                frames.add(new WebPImageFrame(image, timestamp.getValue(), delay));
            }
        }
        finally {
            if (decoder != null) {
                libDemux().WebPAnimDecoderDelete(decoder);
            }
            if (bytes != null) {
                libMain().WebPFree(bytes);
            }
        }
        return new WebPImage(frames, info.canvas_width, info.canvas_height,
                info.loop_count, Color.BLACK, info.frame_count);
    }
    
    private static BufferedImage createImage(Pointer pixelData, int width, int height) {
        if (pixelData != null) {
            int[] pixels = pixelData.getIntArray(0, width * height);

            ColorModel colorModel = new DirectColorModel(32, 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000);

            SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
            DataBufferInt db = new DataBufferInt(pixels, width * height);
            WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, db, null);

            return new BufferedImage(colorModel, raster, false, new Hashtable<Object, Object>());
        }
        return null;
    }
    
    public static class WebPDecoderException extends IOException {

        private static final long serialVersionUID = 1L;

        public WebPDecoderException(String message) {
            super(message);
        }

    }
    
    //==========================
    // Decoded Image Classes
    //==========================
    /**
     * A decoded image containing the individual frames (for static images just
     * one) and some meta info.
     */
    public static class WebPImage {
        
        public final List<WebPImageFrame> frames;
        public final int canvasWidth;
        public final int canvasHeight;
        public final int loopCount;
        public final Color bgColor;
        public final int frameCount;
        
        private WebPImage(List<WebPImageFrame> frames, int canvasWidth, int canvasHeight,
                          int loopCount, Color bgColor, int frameCount) {
            this.frames = frames;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.frameCount = frameCount;
            this.loopCount = loopCount;
            this.bgColor = bgColor;
        }
        
        @Override
        public String toString() {
            return String.format("%d x %d / %d loops / %d frames %s",
                    canvasWidth, canvasHeight, loopCount, frameCount, frames);
        }
        
    }
    
    /**
     * A single frame of a decoded image.
     */
    public static class WebPImageFrame {
        
        /**
         * The image.
         */
        public final BufferedImage img;
        
        /**
         * Counted from the start of the animation until when to show the frame
         * (in ms).
         */
        public final int timestamp;
        
        /**
         * How long to show the frame (in ms).
         */
        public final int delay;
        
        private WebPImageFrame(BufferedImage img, int timestamp, int delay) {
            this.img = img;
            this.timestamp = timestamp;
            this.delay = delay;
        }
        
        @Override
        public String toString() {
            return String.valueOf(delay);
        }
        
    }
    
    //==========================
    // libwebp
    //==========================
    private interface LibWebP extends Library {
        
        /*
        [webp/types.h]
            // Allocates 'size' bytes of memory. Returns NULL upon error. Memory
            // must be deallocated by calling WebPFree(). This function is made available
            // by the core 'libwebp' library.
            WEBP_EXTERN void* WebPMalloc(size_t size);
        */
        public Pointer WebPMalloc(int size);
        
        /*
        [webp/types.h]
            // Releases memory returned by the WebPDecode*() functions (from decode.h).
            WEBP_EXTERN void WebPFree(void* ptr);
        */
        public void WebPFree(Pointer pointer);
    }
    
    //==========================
    // libwebpdemux
    //==========================
    private interface LibWebPDemux extends Library {
        
        static final int WEBP_DEMUX_ABI_VERSION = 0x0107;
        
        /*
        [webp/demux.h]
            // Internal, version-checked, entry point.
            WEBP_EXTERN WebPAnimDecoder* WebPAnimDecoderNewInternal(
                const WebPData*, const WebPAnimDecoderOptions*, int);

            // Creates and initializes a WebPAnimDecoder object.
            // Parameters:
            //   webp_data - (in) WebP bitstream. This should remain unchanged during the
            //                    lifetime of the output WebPAnimDecoder object.
            //   dec_options - (in) decoding options. Can be passed NULL to choose
            //                      reasonable defaults (in particular, color mode MODE_RGBA
            //                      will be picked).
            // Returns:
            //   A pointer to the newly created WebPAnimDecoder object, or NULL in case of
            //   parsing error, invalid option or memory error.
            static WEBP_INLINE WebPAnimDecoder* WebPAnimDecoderNew(
                const WebPData* webp_data, const WebPAnimDecoderOptions* dec_options) {
              return WebPAnimDecoderNewInternal(webp_data, dec_options,
                                                WEBP_DEMUX_ABI_VERSION);
            }
        */
        public Pointer WebPAnimDecoderNewInternal(WebPData webp_data, Structure dec_options, int version);
        
        /*
        [webp/mux_types.h]
            // Data type used to describe 'raw' data, e.g., chunk data
            // (ICC profile, metadata) and WebP compressed image data.
            // 'bytes' memory must be allocated using WebPMalloc() and such.
            struct WebPData {
              const uint8_t* bytes;
              size_t size;
            };
        */
        @Structure.FieldOrder({ "bytes", "length" })
        public static class WebPData extends Structure {
            public Pointer bytes;
            public Size_T length;
        }
        
        /*
        [webp/demux.h]
            // Get global information about the animation.
            // Parameters:
            //   dec - (in) decoder instance to get information from.
            //   info - (out) global information fetched from the animation.
            // Returns:
            //   True on success.
            WEBP_EXTERN int WebPAnimDecoderGetInfo(const WebPAnimDecoder* dec,
                                                   WebPAnimInfo* info);
        */
        public int WebPAnimDecoderGetInfo(Pointer dec, WebPAnimInfo info);
        
        /*
        [webp/demux.h]
            // Global information about the animation..
            struct WebPAnimInfo {
              uint32_t canvas_width;
              uint32_t canvas_height;
              uint32_t loop_count;
              uint32_t bgcolor;
              uint32_t frame_count;
              uint32_t pad[4];   // padding for later use
            };
        */
        @Structure.FieldOrder({ "canvas_width", "canvas_height", "loop_count", "bgcolor", "frame_count", "pad" })
        public static class WebPAnimInfo extends Structure {
            public int canvas_width;
            public int canvas_height;
            public int loop_count;
            public int bgcolor;
            public int frame_count;
            public int[] pad = new int[4];
        }
        
        /*
        [webp/demux.h]
            // Check if there are more frames left to decode.
            // Parameters:
            //   dec - (in) decoder instance to be checked.
            // Returns:
            //   True if 'dec' is not NULL and some frames are yet to be decoded.
            //   Otherwise, returns false.
            WEBP_EXTERN int WebPAnimDecoderHasMoreFrames(const WebPAnimDecoder* dec);
        */
        public int WebPAnimDecoderHasMoreFrames(Pointer dec);
        
        /*
        [webp/demux.h]
            // Fetch the next frame from 'dec' based on options supplied to
            // WebPAnimDecoderNew(). This will be a fully reconstructed canvas of size
            // 'canvasWidth * 4 * canvasHeight', and not just the frame sub-rectangle. The
            // returned buffer 'buf' is valid only until the next call to
            // WebPAnimDecoderGetNext(), WebPAnimDecoderReset() or WebPAnimDecoderDelete().
            // Parameters:
            //   dec - (in/out) decoder instance from which the next frame is to be fetched.
            //   buf - (out) decoded frame.
            //   timestamp - (out) timestamp of the frame in milliseconds.
            // Returns:
            //   False if any of the arguments are NULL, or if there is a parsing or
            //   decoding error, or if there are no more frames. Otherwise, returns true.
            WEBP_EXTERN int WebPAnimDecoderGetNext(WebPAnimDecoder* dec,
                                                   uint8_t** buf, int* timestamp);
        */
        public int WebPAnimDecoderGetNext(Pointer dec, PointerByReference buf, IntByReference timestamp);
        
        /*
        [webp/demux.h]
            // Deletes the WebPAnimDecoder object.
            // Parameters:
            //   dec - (in/out) decoder instance to be deleted
            WEBP_EXTERN void WebPAnimDecoderDelete(WebPAnimDecoder* dec);
        */
        public void WebPAnimDecoderDelete(Pointer dec);
    }
    
    /**
     * Used internally, public for JNA.
     */
    public static class Size_T extends IntegerType {
        
        private static final long serialVersionUID = 1L;
        
        public static final Size_T ZERO = new Size_T();
        
        public Size_T() {
            this(0);
        }
        
        public Size_T(long value) {
            super(Native.SIZE_T_SIZE, value, true);
        }
    }
    
    //==========================
    // General Helpers
    //==========================
    /**
     * Read all bytes from the given URL.
     * 
     * @param url The URL
     * @return A byte array
     * @throws IOException If loading the bytes fails
     */
    public static byte[] getBytesFromURL(URL url) throws IOException {
        URLConnection c = url.openConnection();
        try (InputStream input = c.getInputStream()) {
            byte[] imageData = readAllBytes(input);
            return imageData;
        }
    }
    
    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer, 0, buffer.length)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }
    
    /**
     * Get the extension of the given file name, if any.
     *
     * @param name
     * @return The extension (including ".") or an empty String
     */
    private static String getFileExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index < 1 || name.length() == index + 1) {
            return "";
        }
        return name.substring(index);
    }
    
    /**
     * Check if the given paths are equal, either in path or just content.
     * 
     * @param path1
     * @param path2
     * @return 
     */
    private static boolean filesEqual(Path path1, Path path2) {
        try {
            if (!Files.isRegularFile(path1) || !Files.isRegularFile(path2)) {
                return false;
            }
            if (Files.isSameFile(path1, path2)) {
                return true;
            }
            return findByteDifferencePos(path1, path2) == -1;
        }
        catch (IOException ex) {
            return false;
        }
    }
    
    private static long findByteDifferencePos(Path path1, Path path2) throws IOException {
        try ( BufferedInputStream f1 = new BufferedInputStream(new FileInputStream(path1.toFile())); 
                BufferedInputStream f2 = new BufferedInputStream(new FileInputStream(path2.toFile()))) {

            int ch = 0;
            long pos = 1;
            while ((ch = f1.read()) != -1) {
                if (ch != f2.read()) {
                    return pos;
                }
                pos++;
            }
            if (f2.read() == -1) {
                return -1;
            }
            else {
                return pos;
            }
        }
    }
    
}
