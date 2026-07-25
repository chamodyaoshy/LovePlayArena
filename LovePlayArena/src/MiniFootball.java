import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MiniFootball extends JFrame {
    private String girlName;
    private String boyName;

    private int girlScore = 0;
    private int boyScore = 0;

    public MiniFootball(String girlName, String boyName) {
        this.girlName = girlName;
        this.boyName = boyName;

        setTitle("⚽ LovePlay Arena - Mini Football ⚽");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        FootballPanel gamePanel = new FootballPanel();
        add(gamePanel);

        // Key Listener for Paddle Movement
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gamePanel.handleKeyPress(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                gamePanel.handleKeyRelease(e.getKeyCode());
            }
        });
    }

    class FootballPanel extends JPanel implements ActionListener {
        private Timer timer;

        // Ball Properties
        private int ballX = 240, ballY = 300;
        private int ballXSpeed = 3, ballYSpeed = 4;
        private final int BALL_SIZE = 20;

        // Player Paddles (Girl - Top, Boy - Bottom)
        private int girlPaddleX = 200;
        private int boyPaddleX = 200;
        private final int PADDLE_WIDTH = 90;
        private final int PADDLE_HEIGHT = 15;

        // Key Press States
        private boolean girlLeft = false, girlRight = false;
        private boolean boyLeft = false, boyRight = false;

        public FootballPanel() {
            setBackground(new Color(255, 230, 242));
            timer = new Timer(15, this); // ~60 FPS
            timer.start();
        }

        public void handleKeyPress(int keyCode) {
            // Girl Controls: A / D
            if (keyCode == KeyEvent.VK_A) girlLeft = true;
            if (keyCode == KeyEvent.VK_D) girlRight = true;

            // Boy Controls: Left / Right Arrow
            if (keyCode == KeyEvent.VK_LEFT) boyLeft = true;
            if (keyCode == KeyEvent.VK_RIGHT) boyRight = true;
        }

        public void handleKeyRelease(int keyCode) {
            if (keyCode == KeyEvent.VK_A) girlLeft = false;
            if (keyCode == KeyEvent.VK_D) girlRight = false;

            if (keyCode == KeyEvent.VK_LEFT) boyLeft = false;
            if (keyCode == KeyEvent.VK_RIGHT) boyRight = false;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Move Paddles
            if (girlLeft && girlPaddleX > 10) girlPaddleX -= 6;
            if (girlRight && girlPaddleX < getWidth() - PADDLE_WIDTH - 10) girlPaddleX += 6;

            if (boyLeft && boyPaddleX > 10) boyPaddleX -= 6;
            if (boyRight && boyPaddleX < getWidth() - PADDLE_WIDTH - 10) boyPaddleX += 6;

            // Move Ball
            ballX += ballXSpeed;
            ballY += ballYSpeed;

            // Ball Collision with Left / Right Walls
            if (ballX <= 0 || ballX >= getWidth() - BALL_SIZE - 15) {
                ballXSpeed = -ballXSpeed;
            }

            // Ball Collision with Girl's Paddle (Top)
            if (ballY <= 50 && ballX + BALL_SIZE >= girlPaddleX && ballX <= girlPaddleX + PADDLE_WIDTH) {
                ballYSpeed = Math.abs(ballYSpeed); // Bounce Down
            }

            // Ball Collision with Boy's Paddle (Bottom)
            if (ballY >= getHeight() - 90 && ballX + BALL_SIZE >= boyPaddleX && ballX <= boyPaddleX + PADDLE_WIDTH) {
                ballYSpeed = -Math.abs(ballYSpeed); // Bounce Up
            }

            // Goal Checking
            if (ballY < 10) { // Boy Scores!
                boyScore++;
                resetBall();
                checkWinner();
            } else if (ballY > getHeight() - 50) { // Girl Scores!
                girlScore++;
                resetBall();
                checkWinner();
            }

            repaint();
        }

        private void resetBall() {
            ballX = getWidth() / 2 - 10;
            ballY = getHeight() / 2 - 10;
            ballYSpeed = (ballYSpeed > 0) ? -4 : 4;
        }

        private void checkWinner() {
            if (girlScore >= 3 || boyScore >= 3) {
                timer.stop();
                String winner = (girlScore >= 3) ? girlName + " 👧" : boyName + " 👦";
                JOptionPane.showMessageDialog(this, "🎉 Goal Master! " + winner + " Wins the Match! 💕", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                girlScore = 0;
                boyScore = 0;
                timer.start();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Field Pitch (Pink Turf)
            g2d.setColor(new Color(255, 204, 229));
            g2d.fillRoundRect(15, 15, getWidth() - 30, getHeight() - 65, 30, 30);

            // Field Center Line & Circle
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(15, getHeight() / 2, getWidth() - 15, getHeight() / 2);
            g2d.drawOval(getWidth() / 2 - 40, getHeight() / 2 - 40, 80, 80);

            // Top Paddle (Girl 👧)
            g2d.setColor(new Color(255, 105, 180));
            g2d.fillRoundRect(girlPaddleX, 35, PADDLE_WIDTH, PADDLE_HEIGHT, 15, 15);

            // Bottom Paddle (Boy 👦)
            g2d.setColor(new Color(70, 130, 180));
            g2d.fillRoundRect(boyPaddleX, getHeight() - 85, PADDLE_WIDTH, PADDLE_HEIGHT, 15, 15);

            // Ball (Glowing Heart Ball)
            g2d.setColor(new Color(220, 20, 60));
            g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
            g2d.drawString("💖", ballX, ballY);

            // Score Display
            g2d.setColor(new Color(110, 40, 130));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2d.drawString(girlName + " (A/D): " + girlScore, 30, 30);
            g2d.drawString(boyName + " (←/→): " + boyScore, 30, getHeight() - 40);
        }
    }
}