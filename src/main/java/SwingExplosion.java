import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Conway's game of life is a cellular automaton devised by the
 * mathematician John Conway.
 */
public class SwingExplosion extends JFrame implements ActionListener, KeyListener {
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1100, 850);

    private JMenuBar mb_menu;
    private JMenu m_game, m_settings;
    private JMenuItem mi_game_autofill, mi_game_play, mi_game_stop, mi_settings_browse_image, mi_settings_prop_speed, mi_settings_fog_height;

    private int i_movesPerSecond = 60;

    private int max_iter = 200;
    private double blast_increment = 0.6;
    private int R = 0;
    private double max_R = (R + max_iter * blast_increment);

    private GameBoard gb_gameBoard;
    private Thread game;
    private BufferedImage img;
    private BufferedImage cloud_img;
    private Raster rst;
    private BufferedImage backgroundImage;
    private Image boomImage;
    private Image crosshairImage;
    private BufferedImage boomBuffer;
    private final Random rand = new Random(48);

    private File backgroundFile;

    private BufferedImage backBuffer;

    double fogHeight = 1.0;
    double propagationSpeed = 1.0;

    public static void main(String[] args) {
        JFrame game = new SwingExplosion();
        game.setResizable(false);
        game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.setTitle("Explosions");
        game.setSize(DEFAULT_WINDOW_SIZE);
        game.setMinimumSize(DEFAULT_WINDOW_SIZE);
        game.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - game.getWidth()) / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height - game.getHeight()) / 2);
        game.setVisible(true);
    }

    public SwingExplosion() {
        mb_menu = new JMenuBar();
        setJMenuBar(mb_menu);
        m_game = new JMenu("Game");
        m_settings = new JMenu("Settings");
        mb_menu.add(m_game);
        mb_menu.add(m_settings);
        mi_game_play = new JMenuItem("Play");
        mi_game_play.addActionListener(this);
        mi_game_stop = new JMenuItem("Stop");
        mi_game_stop.setEnabled(false);
        mi_game_stop.addActionListener(this);
        mi_settings_browse_image = new JMenuItem("Browse for background");
        mi_settings_browse_image.addActionListener(this);
        mi_settings_fog_height = new JMenuItem("Change explosion fog height");
        mi_settings_fog_height.addActionListener(this);
        mi_settings_prop_speed = new JMenuItem("Set explosion propagation speed");
        mi_settings_prop_speed.addActionListener(this);
        m_game.add(new JSeparator());
        m_game.add(mi_game_play);
        m_game.add(mi_game_stop);
        m_settings.add(mi_settings_browse_image);
        m_settings.add(mi_settings_prop_speed);
        m_settings.add(mi_settings_fog_height);

        gb_gameBoard = new GameBoard();
        add(gb_gameBoard);
        addKeyListener(this);
    }

    public void setGameBeingPlayed(boolean isBeingPlayed) {
        if (isBeingPlayed) {
            mi_game_play.setEnabled(false);
            mi_game_stop.setEnabled(true);
            game = new Thread(gb_gameBoard);
            game.start();
        } else {
            mi_game_play.setEnabled(true);
            mi_game_stop.setEnabled(false);
            game.interrupt();
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(mi_settings_fog_height)) {
            final JFrame f_fog = new JFrame();
            f_fog.setTitle("Normal Distribution Height");
            f_fog.setSize(400, 80);
            f_fog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - f_fog.getWidth()) / 2,
                    (Toolkit.getDefaultToolkit().getScreenSize().height - f_fog.getHeight()) / 2);
            f_fog.setResizable(false);
            JPanel p_fog = new JPanel();
            p_fog.setOpaque(false);
            f_fog.add(p_fog);
            p_fog.add(new JLabel("Please select normal distribution height"));
            Object[] percentageOptions = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
            final JComboBox cb_percent = new JComboBox(percentageOptions);
            cb_percent.setSelectedItem(fogHeight);
            p_fog.add(cb_percent);
            f_fog.setVisible(true);
            cb_percent.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fogHeight = (double) cb_percent.getSelectedItem();
                }
            });
        } if (ae.getSource().equals(mi_settings_prop_speed)) {
            final JFrame f_prop = new JFrame();
            f_prop.setTitle("Propagation Speed");
            f_prop.setSize(400, 80);
            f_prop.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - f_prop.getWidth()) / 2,
                    (Toolkit.getDefaultToolkit().getScreenSize().height - f_prop.getHeight()) / 2);
            f_prop.setResizable(false);
            JPanel p_prop = new JPanel();
            p_prop.setOpaque(false);
            f_prop.add(p_prop);
            p_prop.add(new JLabel("Please select propagation speed"));
            Object[] percentageOptions = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
            final JComboBox cb_percent = new JComboBox(percentageOptions);
            cb_percent.setSelectedItem(propagationSpeed);
            p_prop.add(cb_percent);
            f_prop.setVisible(true);
            cb_percent.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    propagationSpeed = (double) cb_percent.getSelectedItem();
                }
            });
        } else if (ae.getSource().equals(mi_game_play)) {
            setGameBeingPlayed(true);
        } else if (ae.getSource().equals(mi_game_stop)) {
            setGameBeingPlayed(false);
        } else if (ae.getSource().equals(mi_settings_browse_image)) {
            browseImage();
        }
    }

    private void browseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new ImageFileFilter());
        int res = fc.showOpenDialog(null);
        try {
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                backgroundFile = file;
                repaint();
            } // Oops!
            else {
                JOptionPane.showMessageDialog(null,
                        "You must select one image to be the reference.", "Aborting...",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception iOException) {
        }
    }

    public class ImageFileFilter extends FileFilter implements java.io.FileFilter {
        public boolean accept(File f) {
            if (f.getName().toLowerCase().endsWith(".jpeg")) return true;
            if (f.getName().toLowerCase().endsWith(".jpg")) return true;
            if (f.getName().toLowerCase().endsWith(".png")) return true;
            if (f.getName().toLowerCase().endsWith(".bmp")) return true;
            if (f.isDirectory()) return true;
            return false;
        }

        public String getDescription() {
            return "JPEG files";
        }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F5) {
            gb_gameBoard.addPoint(rand.nextInt(gb_gameBoard.getWidth()), rand.nextInt(gb_gameBoard.getHeight()));
        }
        if (mi_game_play.isEnabled()) {
            setGameBeingPlayed(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private class GameBoard extends JPanel implements ComponentListener, MouseListener, MouseMotionListener, Runnable {
        private class Blast {
            private int x;
            private int y;
            private double r;

            Blast(int x, int y, double r) {
                this.x = x;
                this.y = y;
                this.r = r;
            }

            public int getX() {
                return x;
            }

            public void setX(int x) {
                this.x = x;
            }

            public int getY() {
                return y;
            }

            public void setY(int y) {
                this.y = y;
            }

            public int getR() {
                return (int) r;
            }

            public void setR(double r) {
                this.r = r;
            }

            public void addR(double r) {
                this.r += r;
            }
        }

        private Dimension d_gameBoardSize = null;
        private final ConcurrentLinkedQueue<Blast> blasts = new ConcurrentLinkedQueue<>();

        public GameBoard() {
            addComponentListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            boomImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("cotton.png")).getScaledInstance(80, 80, 0);
            crosshairImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("crosshair.png")).getScaledInstance(50, 50, 0);

            ImageIcon ic = new ImageIcon(getClass().getResource("background.png"));
            backgroundImage = new BufferedImage(ic.getIconWidth(), ic.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g1 = backgroundImage.getGraphics();
            ic.paintIcon(null, g1, 0, 0);
            g1.dispose();
        }

        public void addPoint(int x, int y) {
            blasts.add(new Blast(x, y, R));
            repaint();
        }

        public void addPoint(MouseEvent me) {
            int x = me.getPoint().x;
            int y = me.getPoint().y;
            if ((x >= 0) && (x < d_gameBoardSize.width) && (y >= 0) && (y < d_gameBoardSize.height)) {
                addPoint(x, y);
            }
        }

        public void resetBoard() {
            blasts.clear();
        }

        public void randomlyFillBoard(double percent) {
            for (int i = 0; i < d_gameBoardSize.width; i++) {
                for (int j = 0; j < d_gameBoardSize.height; j++) {
                    if (Math.random() * 100 < percent) {
                        addPoint(i, j);
                    }
                }
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paintComponents(g);
            try {
                if (img == null) {
                    img = new BufferedImage(d_gameBoardSize.width, d_gameBoardSize.height, BufferedImage.TYPE_INT_ARGB);
                    rst = img.copyData(null);

                    cloud_img = new BufferedImage(d_gameBoardSize.width, d_gameBoardSize.height, BufferedImage.TYPE_INT_ARGB);
                    boomBuffer = new BufferedImage(d_gameBoardSize.width, d_gameBoardSize.height, BufferedImage.TYPE_INT_ARGB);
                } else {
                    img.setData(rst);
                    boomBuffer.setData(rst);
                }

                try {
                    if (backgroundFile != null) {
                        backgroundImage = ImageIO.read(backgroundFile);
                        gb_gameBoard.setBounds(0, 0, backgroundImage.getWidth(), backgroundImage.getHeight());
                        gb_gameBoard.setSize(backgroundImage.getWidth(), backgroundImage.getHeight());
                    }
                } catch (IOException io) {

                }

                g.drawImage(backgroundImage, 0, 0, this);

                for (Blast current : blasts) {
                    if (current.getR() <= 0) {
                        g.drawImage(crosshairImage, current.getX() - crosshairImage.getWidth(null) / 2, current.getY() - crosshairImage.getHeight(null) / 2, null);
                    } else {
                        Graphics gboom = boomBuffer.getGraphics();
                        gboom.drawImage(boomImage, current.getX() - boomImage.getWidth(null) / 2, current.getY() - boomImage.getHeight(null) / 2, null);

                        int left = current.getX() - current.getR();
                        int right = current.getX() + current.getR();
                        for (int i = left; i <= right; i++) {
                            if (i < 0 || i >= img.getWidth()) continue;

                            int top = current.getY() - current.getR();
                            int bottom = current.getY() + current.getR();
                            for (int j = top; j <= bottom; j++) {
                                if (j < 0 || j >= img.getHeight()) continue;

                                double hypotR = Math.hypot(i - current.getX(), j - current.getY());

                                if (hypotR <= current.getR()) {
                                    double z = Math.hypot(hypotR, current.getR());
                                    double p = 20 * current.getR() / (z + 20 * current.getR()); // This adds curviture to the gas cloud

                                    int di = current.getX() - i;
                                    int dj = current.getY() - j;

                                    double newdi = p * di;
                                    double newdj = p * dj;

                                    int newi = current.getX() - (int) newdi;
                                    int newj = current.getY() - (int) newdj;

                                    double baseline = 1;
                                    double radiusPercent = (current.getR() + 0.01 * max_R) / max_R; // this adds 0.01 to max value of the ratio, to trim that frame later
                                    double alphaPercent = (1 - Math.pow(2 * radiusPercent - 1, 2));
                                    double alphaCloudPercent = (1 - Math.pow(2 * radiusPercent - 1, 4));

                                    /* Trim values above 1 because of some Swing bug */
                                    if (radiusPercent <= 1) {
                                        if (0 <= i && i < d_gameBoardSize.getWidth() && 0 <= j && j < d_gameBoardSize.getHeight()) {
                                            setCloudPixel(cloud_img, i, j, backgroundImage, alphaCloudPercent, current.getX(), current.getY(), current.getR());
                                        }
                                        if (hypotR <= current.getR() - 2) {
                                            if (0 <= i && i < d_gameBoardSize.getWidth() && 0 <= j && j < d_gameBoardSize.getHeight()) {
                                                if (0 <= newi && newi < d_gameBoardSize.getWidth() && 0 <= newj && newj < d_gameBoardSize.getHeight()) {
                                                    setShockwavePixel(boomBuffer, img, i, j, backgroundImage, cloud_img, newi, newj, baseline * alphaPercent);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            gboom.dispose();
                        }
                    }
                }
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g.drawImage(img, 0, 0, null);
                g.dispose();
            } catch (ConcurrentModificationException cme) {
                cme.printStackTrace();
            }
        }


        private void setCloudPixel(BufferedImage cloudBuffer, int i, int j, BufferedImage newBuffer, double alpha, int x, int y, int r) {
            double a = Math.exp(-Math.pow(Math.pow(2 * (i - x) / (double) r, 2) + Math.pow(2 * (j - y) / (double) r, 2) - 2, 2));
            int ah = (int) (fogHeight * 255 / 2);
            int white = argb(ah, 255, 255, 255);

            int oldArgb = cloudBuffer.getRGB(i, j);

            if (oldArgb == 0) {
                cloudBuffer.setRGB(i, j, setAlphaPercent(setAlphaPercent(white, a), alpha));
            } else {

                int newArgb = mergeColors(cloudBuffer.getRGB(i, j), setAlphaPercent(white, a));

                cloudBuffer.setRGB(i, j, setAlphaPercent(newArgb, alpha));
            }
        }

        private void setShockwavePixel(BufferedImage boomBuffer, BufferedImage shockwaveBuffer, int i, int j, BufferedImage newBuffer, BufferedImage cloudBuffer, int newi, int newj, double alpha) {
            int oldArgb = shockwaveBuffer.getRGB(i, j);

            // Random noise emulates the particles of gas
            int noisyinew = noisyIndex(newi, 0, newBuffer.getWidth() - 1);
            int noisyjnew = noisyIndex(newj, 0, newBuffer.getHeight() - 1);

            int w = Math.min(newBuffer.getWidth(), cloudBuffer.getWidth());
            int h = Math.min(newBuffer.getHeight(), cloudBuffer.getHeight());

            if (oldArgb == 0) {
                int argb = mergeColors(boomBuffer.getRGB(noisyinew, noisyjnew), mergeBufferPixels(newBuffer, cloudBuffer, noisyinew, noisyjnew));
                shockwaveBuffer.setRGB(i, j, setAlphaPercent(argb, alpha));
            } else {
                // Random noise emulates the particles of gas
                int noisyiold = noisyIndex(i, 0, shockwaveBuffer.getWidth() - 1);
                int noisyjold = noisyIndex(j, 0, shockwaveBuffer.getHeight() - 1);

                int noisyOldArgb = gauss(noisyiold, noisyjold, shockwaveBuffer);
                int newArgb = mergeBufferPixels(newBuffer, cloudBuffer, noisyinew, noisyjnew);

                shockwaveBuffer.setRGB(i, j, setAlphaPercent(mergeColors(boomBuffer.getRGB(noisyinew, noisyjnew), avg2(noisyOldArgb, newArgb)), alpha));
            }
        }


        private int noisyIndex(int index, int minIndex, int maxIndex) {
            int randi = rand.nextInt(2) - 1;
            //if (randi < -1) randi = 0;
            int noisyi = index + randi;

            if (noisyi < minIndex || noisyi > maxIndex) {
                noisyi = index;
            }

            return noisyi;
        }

        int mergeColors(int backgroundColor, int foregroundColor) {
            float ab = getAlpha(backgroundColor) / 255f;
            float af = getAlpha(foregroundColor) / 255f;

            if (ab == 0 && af == 0) {
                return 0;
            }

            float ap = ab + af;

            float factorb = ab / ap;
            float factorf = af / ap;

            int br = ((backgroundColor >> 16) & 255);
            int bg = ((backgroundColor >> 8) & 255);
            int bb = (backgroundColor & 255);

            int fr = ((foregroundColor >> 16) & 255);
            int fg = ((foregroundColor >> 8) & 255);
            int fb = (foregroundColor & 255);

            int a = (int) (getAlpha(backgroundColor) * factorb + getAlpha(foregroundColor) * factorf);
            int r = (int) (br * factorb + fr * factorf);
            int g = (int) (bg * factorb + fg * factorf);
            int b = (int) (bb * factorb + fb * factorf);
            return (((int) a & 0xFF) << 24) |
                    (((int) r & 0xFF) << 16) |
                    (((int) g & 0xFF) << 8) |
                    (((int) b & 0xFF));
        }

        int mergeBufferPixels(BufferedImage b1, BufferedImage b2, int i, int j) {
            int w = Math.min(b1.getWidth(), b2.getWidth());
            int h = Math.min(b1.getHeight(), b2.getHeight());
            if (0 <= i && i < w && 0 <= j && j < h) {
                return mergeColors(b1.getRGB(i, j), b2.getRGB(i, j));
            }
            return 0;
        }

            private int gauss ( int i, int j, BufferedImage bi){
                double a = 0;
                double r = 0;
                double g = 0;
                double b = 0;

                double[][] gauss2d = {
                        {1d / 16, 1d / 8, 1d / 16},
                        {1d / 8, 1d / 4, 1d / 8},
                        {1d / 16, 1d / 8, 1d / 16}
                };

                for (int x = 0; x <= 2; x++) {
                    for (int y = 0; y <= 2; y++) {
                        int xx = i - 1 + x;
                        int yy = j - 1 + y;

                        if (0 < xx & xx < bi.getWidth() && 0 < yy && yy < bi.getHeight()) {
                            int argb = bi.getRGB(xx, yy);
                            double k = gauss2d[x][y];
                            a += k * ((argb >> 24) & 255);
                            r += k * ((argb >> 16) & 255);
                            g += k * ((argb >> 8) & 255);
                            b += k * (argb & 255);
                        }
                    }
                }

                return (((int) a & 0xFF) << 24) |
                        (((int) r & 0xFF) << 16) |
                        (((int) g & 0xFF) << 8) |
                        (((int) b & 0xFF));
            }

            private int avg2 ( int argb1, int argb2){
                double a = 0;
                double r = 0;
                double g = 0;
                double b = 0;

                a += ((argb1 >> 24) & 255);
                r += ((argb1 >> 16) & 255);
                g += ((argb1 >> 8) & 255);
                b += (argb1 & 255);

                a += ((argb2 >> 24) & 255);
                r += ((argb2 >> 16) & 255);
                g += ((argb2 >> 8) & 255);
                b += (argb2 & 255);

                return (((int) a / 2 & 0xFF) << 24) |
                        (((int) r / 2 & 0xFF) << 16) |
                        (((int) g / 2 & 0xFF) << 8) |
                        (((int) b / 2 & 0xFF));
            }

            int argb ( int a, int r, int g, int b){
                return (((int) a & 0xFF) << 24) |
                        (((int) r & 0xFF) << 16) |
                        (((int) g & 0xFF) << 8) |
                        (((int) b & 0xFF));
            }

            int setAlpha ( int argb, int alpha){
                int a = (alpha & 255);
                int r = ((argb >> 16) & 255);
                int g = ((argb >> 8) & 255);
                int b = (argb & 255);

                return (((int) a & 0xFF) << 24) |
                        (((int) r & 0xFF) << 16) |
                        (((int) g & 0xFF) << 8) |
                        (((int) b & 0xFF));
            }

            int setAlphaPercent ( int argb, double percent){
                int a = ((argb >> 24) & 255);
                double alpha = a * percent;

                return setAlpha(argb, (int) alpha);
            }

            int getAlpha ( int argb){
                return (argb >> 24) & 255;
            }

            @Override
            public void componentResized (ComponentEvent e){
                d_gameBoardSize = new Dimension(getWidth(), getHeight());
            }

            @Override
            public void componentMoved (ComponentEvent e){
            }

            @Override
            public void componentShown (ComponentEvent e){
            }

            @Override
            public void componentHidden (ComponentEvent e){
            }

            @Override
            public void mouseClicked (MouseEvent e){
            }

            @Override
            public void mousePressed (MouseEvent e){
            }

            @Override
            public void mouseReleased (MouseEvent e){
                addPoint(e);
            }

            @Override
            public void mouseEntered (MouseEvent e){
            }

            @Override
            public void mouseExited (MouseEvent e){
            }

            @Override
            public void mouseDragged (MouseEvent e){
                addPoint(e);
            }

            @Override
            public void mouseMoved (MouseEvent e){
            }

            @Override
            public void run () {
                for (Blast current : blasts) {
                    if (current.getR() <= max_R) {
                        current.addR(blast_increment);
                    } else {
                        blasts.remove(current);
                    }
                }
                repaint();
                try {
                    int moves = (i_movesPerSecond + ((int) propagationSpeed * i_movesPerSecond)) / 2;
                    Thread.sleep(1000 / moves);
                    run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }