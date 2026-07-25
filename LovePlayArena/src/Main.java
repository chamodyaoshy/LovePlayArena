import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends JFrame {
    private String girlName = "Princess";
    private String boyName = "Prince";

    private JLabel welcomeLabel;
    private BackgroundPanel mainPanel;
    private Clip bgMusicClip;

    public Main() {
        askPlayerNames();

        setTitle("💖 LovePlay Arena - Couple Games Hub 💖");
        setSize(520, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        mainPanel = new BackgroundPanel();
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Header
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("✨ LovePlay Arena ✨", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        titleLabel.setForeground(new Color(255, 20, 147));

        welcomeLabel = new JLabel("Welcome, " + girlName + " 👧 & " + boyName + " 👦", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        welcomeLabel.setForeground(new Color(110, 40, 130));

        topPanel.add(titleLabel);
        topPanel.add(welcomeLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Games Grid
        JPanel gamesGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        gamesGrid.setOpaque(false);

        JButton ticTacToeBtn = createGameCard("❌ Tic-Tac-Toe ⭕", "Classic Love Battle 💖");
        JButton footballBtn = createGameCard("⚽ Mini Football", "Heart-Goal Battle 🥅");
        JButton carromBtn = createGameCard("🥏 Couple Carrom", "Sweet Queen Strike 👑");
        JButton archeryBtn = createGameCard("🏹 Cupid Archery", "Shoot My Heart 💘");

        // --- Games Launching Events ---
        ticTacToeBtn.addActionListener(e -> {
            TicTacToe game = new TicTacToe(girlName, boyName);
            game.setVisible(true);
        });

        footballBtn.addActionListener(e -> {
            MiniFootball game = new MiniFootball(girlName, boyName);
            game.setVisible(true);
        });

        carromBtn.addActionListener(e -> {
            CoupleCarrom game = new CoupleCarrom(girlName, boyName);
            game.setVisible(true);
        });

        archeryBtn.addActionListener(e -> {
            CupidArchery game = new CupidArchery(girlName, boyName);
            game.setVisible(true);
        });

        gamesGrid.add(ticTacToeBtn);
        gamesGrid.add(footballBtn);
        gamesGrid.add(carromBtn);
        gamesGrid.add(archeryBtn);

        mainPanel.add(gamesGrid, BorderLayout.CENTER);

        // Bottom Controls
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        bottomPanel.setOpaque(false);

        JButton changeNamesBtn = createStyledButton("✏️ Change Names", new Color(255, 182, 193));
        changeNamesBtn.addActionListener(e -> {
            askPlayerNames();
            welcomeLabel.setText("Welcome, " + girlName + " 👧 & " + boyName + " 👦");
        });

        bottomPanel.add(changeNamesBtn);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        mainPanel.startAnimation();
        playBackgroundMusic("background_music.wav");
    }

    private void askPlayerNames() {
        JTextField gField = new JTextField(girlName);
        JTextField bField = new JTextField(boyName);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.add(new JLabel("👧 Girl's Name:"));
        inputPanel.add(gField);
        inputPanel.add(new JLabel("👦 Boy's Name:"));
        inputPanel.add(bField);

        int option = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "💖 LovePlay Arena - Enter Names 💖",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            if (!gField.getText().trim().isEmpty()) girlName = gField.getText().trim();
            if (!bField.getText().trim().isEmpty()) boyName = bField.getText().trim();
        }
    }

    private JButton createGameCard(String title, String subtitle) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 215));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 25, 25));
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setLayout(new GridLayout(2, 1));
        JLabel titleL = new JLabel(title, SwingConstants.CENTER);
        titleL.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleL.setForeground(new Color(200, 30, 100));

        JLabel subL = new JLabel(subtitle, SwingConstants.CENTER);
        subL.setFont(new Font("SansSerif", Font.ITALIC, 11));
        subL.setForeground(Color.GRAY);

        btn.add(titleL);
        btn.add(subL);

        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.DARK_GRAY);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private void playBackgroundMusic(String filepath) {
        try {
            File musicPath = new File(filepath);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                bgMusicClip = AudioSystem.getClip();
                bgMusicClip.open(audioInput);
                bgMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgMusicClip.start();
            }
        } catch (Exception ex) {
            System.out.println("Audio error: " + ex.getMessage());
        }
    }

    class BackgroundPanel extends JPanel {
        private List<HeartParticle> hearts = new ArrayList<>();
        private Timer animationTimer;
        private Random random = new Random();

        public BackgroundPanel() {
            for (int i = 0; i < 30; i++) {
                hearts.add(new HeartParticle(random.nextInt(500), random.nextInt(650)));
            }

            animationTimer = new Timer(30, e -> {
                for (HeartParticle h : hearts) {
                    h.update();
                }
                repaint();
            });
        }

        public void startAnimation() {
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(0, 0, new Color(255, 209, 220), getWidth(), getHeight(), new Color(225, 200, 255));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            for (HeartParticle h : hearts) {
                g2d.setColor(new Color(255, 105, 180, h.alpha));
                g2d.drawString(h.symbol, h.x, h.y);
            }
        }

        class HeartParticle {
            int x, y;
            int speed;
            int alpha;
            String symbol;
            String[] symbols = {"💖", "💕", "💗", "🌸", "✨"};

            public HeartParticle(int startX, int startY) {
                this.x = startX;
                this.y = startY;
                this.speed = random.nextInt(2) + 1;
                this.alpha = random.nextInt(150) + 100;
                this.symbol = symbols[random.nextInt(symbols.length)];
            }

            public void update() {
                y -= speed;
                if (y < -20) {
                    y = 650;
                    x = random.nextInt(500);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }
}