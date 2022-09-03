
package webpdecoderjn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import webpdecoderjn.WebPDecoder.WebPImage;
import webpdecoderjn.WebPDecoder.WebPImageFrame;

/**
 * Simple program to test WebP decoding. Loads the libs and performs the test
 * method, then allows custom images to be loaded and decoded, outputting
 * various debug info in the GUI and to the console. If ran in a headless
 * environment only the test method will be performed and no GUI is opened.
 * 
 * @author tduva
 */
public class App {
    
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    
    public static void main(String[] args) {
        Logging.installSingleLineLog();
        System.setProperty("jna.debug_load", "true");
        System.setProperty("jna.nosys", "true");
        System.setProperty("jna.platform.library.path", "");
        System.setProperty("jna.librarby.path", "");
        // Some servers may reject some default Java user agents
        System.setProperty("http.agent", "WebP Decoder Test");
        LOGGER.info(Logging.systemInfo());
        String url = null;
        if (args.length > 0) {
            url = args[0];
        }
        int reps = 0;
        if (args.length > 1) {
            reps = Integer.parseInt(args[1]);
        }
        guiTest(url, reps);
    }
    
    private static void guiTest(String url, int reps) {
        SwingUtilities.invokeLater(() -> {
            //--------------------------
            // "Guard clauses"
            //--------------------------
            try {
                WebPDecoder.init(true);
                WebPDecoder.testEx();
                LOGGER.info("Test decoding ok.");
            }
            catch (Exception | UnsatisfiedLinkError ex) {
                LOGGER.warning("Decoder doesn't work: "+ex);
                String message = String.format("Decoder doesn't work [%s/%s]",
                        System.getProperty("os.name"), WebPDecoder.getArch());
                showError(message, ex);
                return;
            }
            
            //--------------------------
            // No GUI performance test
            //--------------------------
            if (url != null && reps > 0) {
                try {
                    getImage(url, reps);
                }
                catch (IOException ex) {
                    LOGGER.warning("Error loading image: "+ex);
                }
            }
            
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            
            //--------------------------
            // Continue and show GUI
            //--------------------------
            JFrame frame = new JFrame();
            frame.setTitle(String.format("Test WebPDecoder [%s/%s]",
                    System.getProperty("os.name"), WebPDecoder.getArch()));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel inputPanel = new JPanel();
            JTextArea input = new JTextArea(3, 50);
            input.setText(url != null ? url : "https://www.gstatic.com/webp/gallery/4.sm.webp");
            JButton inputButton = new JButton("Load");
            inputPanel.add(new JScrollPane(input));
            inputPanel.add(inputButton);
            
            JLabel label = new JLabel("Repetitions: ");
            label.setToolTipText("Repeat loading the images this many times to test performance.");
            inputPanel.add(label);
            
            JComboBox<Integer> repSelection = new JComboBox<>(new Integer[]{1, 10, 20, 50, 100, 500, 1000});
            label.setLabelFor(repSelection);
            inputPanel.add(repSelection);

            // For custom test
//            JButton testButton = new JButton("Test");
//            testButton.addActionListener(e -> test());
//            inputPanel.add(testButton);
            
            JPanel outputPanel = new JPanel();
            outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
            outputPanel.add(new JLabel("<html><body style='padding:5'>Test decoding ok. WebP decoding appears to work."
                    + "<br /><br />For further testing enter URL or file path above (or several, one per line)."));

            inputButton.addActionListener(e -> {
                //--------------------------
                // Update images
                //--------------------------
                outputPanel.removeAll();

                String[] lines = input.getText().split("\n");
                for (String line : lines) {
                    line = line.trim();
                    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    try {
                        ImageResult result = getImage(line, (Integer) repSelection.getSelectedItem());
                        WebPImage image = result.image;
                        panel.add(new JLabel(makeImageInfo(result)));
                        for (WebPImageFrame f : image.frames) {
                            panel.add(new JLabel(String.format("%d", f.delay), new ImageIcon(f.img), SwingConstants.LEFT));
                        }
                    }
                    catch (Exception ex) {
                        panel.add(new JLabel("Error: " + ex));
                    }
                    catch (UnsatisfiedLinkError ex) {
                        panel.add(new JLabel("Native library not found"));
                    }
                    outputPanel.add(panel);
                    outputPanel.add(Box.createVerticalStrut(20));
                }
                // Adjust size and position
                frame.pack();
                Rectangle bounds = frame.getGraphicsConfiguration().getBounds();
                frame.setSize(Math.min(frame.getWidth(), (int)(bounds.width * 0.8)),
                        Math.min(frame.getHeight(), (int)(bounds.height * 0.8)));
                frame.setLocationRelativeTo(null);
            });

            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(new JScrollPane(outputPanel), BorderLayout.CENTER);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    private static String makeImageInfo(ImageResult result) {
        WebPImage image = result.image;
        return String.format("<html><body>%d x %d<br />frames: %d<br />loops: %d<br />bg: %d/%d/%d/%d<br />%dms load",
                image.canvasWidth, image.canvasHeight,
                image.frameCount, image.loopCount,
                image.bgColor.getRed(), image.bgColor.getGreen(), image.bgColor.getBlue(), image.bgColor.getAlpha(),
                result.loadTime);
    }
    
    /**
     * Get the image from a line. Looks for a url or otherwise tries a path.
     * Fairly primitive, but should do for this.
     * 
     * @param line
     * @return
     * @throws Exception 
     */
    private static ImageResult getImage(String line, int rep) throws IOException {
        if (rep < 1) {
            rep = 1;
        }
        LOGGER.info(String.format("Decoding %s (%dx)", line, rep));
        byte[] data = getBytesFromLine(line);
        long start = System.currentTimeMillis();
        WebPImage image = null;
        for (int i = 0; i < rep; i++) {
            image = WebPDecoder.decode(data);
        }
        long duration = System.currentTimeMillis() - start;
        LOGGER.info(String.format("Decoding took %dms", duration));
        return new ImageResult(image, duration);
    }
    
    private static byte[] getBytesFromLine(String line) throws IOException {
        URL url;
        line = line.replace("\"", "");
        if (line.startsWith("http")) {
            url = new URL(line);
        }
        else {
            url = Paths.get(line).toUri().toURL();
        }
        return WebPDecoder.getBytesFromURL(url);
    }
    
    private static void showError(String message, Throwable ex) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message + ": " + addLinebreaks(ex.toString(), 100, false));
        }
    }
    
    /**
     * Adds linebreaks to the input, in place of existing space characters, so
     * that each resulting line has the given maximum length. If there is no
     * space character where needed a line may be longer. The added linebreaks
     * don't count into the maximum line length.
     *
     * @param input The intput to modify
     * @param maxLineLength The maximum line length in number of characters
     * @param html If true, a "&lt;br /&gt;" will be added instead of a \n
     * @return 
     */
    public static String addLinebreaks(String input, int maxLineLength, boolean html) {
        if (input == null || input.length() <= maxLineLength) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder b = new StringBuilder();
        int lineLength = 0;
        for (int i=0;i<words.length;i++) {
            String word = words[i];
            if (b.length() > 0
                    && lineLength + word.length() > maxLineLength) {
                if (html) {
                    b.append("<br />");
                } else {
                    b.append("\n");
                }
                lineLength = 0;
            } else if (b.length() > 0) {
                b.append(" ");
                lineLength++;
            }
            b.append(word);
            lineLength += word.length();
        }
        return b.toString();
    }
    
    // For custom test
//    private static Object temp;
//    
//    private static void test() {
//        try {
//            long start = System.currentTimeMillis();
//            byte[] data = WebPDecoder.getBytesFromURL(new URL("file:///C:\\Users\\sb\\Downloads\\gwsreg2x.png"));
//            for (int i=0;i<1000;i++) {
//                try {
//                    temp = WebPDecoder.decode(data);
//                }
//                catch (Exception ex) {
////                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//            System.out.println(System.currentTimeMillis() - start);
//            temp = null;
//        }
//        catch (MalformedURLException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        catch (IOException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
    private static class ImageResult {
        
        public final WebPImage image;
        public final long loadTime;
        
        public ImageResult(WebPImage image, long loadTime) {
            this.image = image;
            this.loadTime = loadTime;
        }
        
    }
    
}
