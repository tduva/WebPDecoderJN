
package webpdecoderjn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
        guiTest();
    }
    
    private static void guiTest() {
        SwingUtilities.invokeLater(() -> {
            //--------------------------
            // "Guard clauses"
            //--------------------------
            try {
                WebPDecoder.init();
            }
            catch (Exception | UnsatisfiedLinkError ex) {
                LOGGER.warning("Error extracting native libs: "+ex);
                showError("Error extracting native libs", ex);
                return;
            }
            
            try {
                WebPDecoder.testEx();
                LOGGER.info("Test decoding ok.");
            }
            catch (Exception | UnsatisfiedLinkError ex) {
                LOGGER.warning("Decoder doesn't work: "+ex);
                showError("Decoder doesn't work", ex);
                return;
            }
            
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            
            //--------------------------
            // Continue and show GUI
            //--------------------------
            JFrame frame = new JFrame();
            frame.setTitle("Test WebPDecoder");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel inputPanel = new JPanel();
            JTextArea input = new JTextArea(3, 50);
            input.setText("https://www.gstatic.com/webp/gallery/4.sm.webp");
            JButton inputButton = new JButton("Load");
            inputPanel.add(new JScrollPane(input));
            inputPanel.add(inputButton);

            // For custom test
//            JButton testButton = new JButton("Test");
//            testButton.addActionListener(e -> test());
//            inputPanel.add(testButton);
            
            JPanel outputPanel = new JPanel();
            outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
            outputPanel.add(new JLabel("Test decoding ok. Enter URL or file path above (or several) to further test WebP decoding."));

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
                        WebPImage image = getImage(line);
                        panel.add(new JLabel(makeImageInfo(image)));
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
    
    private static String makeImageInfo(WebPImage image) {
        return String.format("<html><body>%d x %d<br />frames: %d<br />loops: %d<br />bg: %d/%d/%d/%d",
                image.canvasWidth, image.canvasHeight,
                image.frameCount, image.loopCount,
                image.bgColor.getRed(), image.bgColor.getGreen(), image.bgColor.getBlue(), image.bgColor.getAlpha());
    }
    
    /**
     * Get the image from a line. Looks for a url or otherwise tries a path.
     * Fairly primitive, but should do for this.
     * 
     * @param line
     * @return
     * @throws Exception 
     */
    private static WebPImage getImage(String line) throws Exception {
        URL url;
        line = line.replace("\"", "");
        if (line.startsWith("http")) {
            url = new URL(line);
        }
        else {
            url = Paths.get(line).toUri().toURL();
        }
        return WebPDecoder.decode(WebPDecoder.getBytesFromURL(url));
    }
    
//    private static void showImages(WebPImage... images) {
//        SwingUtilities.invokeLater(() -> {
//            JFrame frame = new JFrame();
//            frame.setLayout(new FlowLayout());
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            for (WebPImage image : images) {
//                for (WebPImageFrame f : image.frames) {
//                    frame.add(new JLabel(new ImageIcon(f.img)), BorderLayout.CENTER);
//                }
//            }
//            frame.pack();
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });
//        
//    }
    
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
//    private static Object blah;
//    
//    private static void test() {
//        try {
//            long start = System.currentTimeMillis();
//            byte[] data = WebPDecoder.getBytesFromURL(new URL("file:///C:\\Users\\sb\\Downloads\\gwsreg2x.png"));
//            for (int i=0;i<1000;i++) {
//                try {
//                    blah = WebPDecoder.decode(data);
//                }
//                catch (Exception ex) {
////                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//            System.out.println(System.currentTimeMillis() - start);
//            blah = null;
//        }
//        catch (MalformedURLException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        catch (IOException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
}
