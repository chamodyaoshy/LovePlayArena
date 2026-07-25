import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CoupleCarrom extends JFrame {
    private String girlName;
    private String boyName;

    public CoupleCarrom(String girlName, String boyName) {
        this.girlName = girlName;
        this.boyName = boyName;

        setTitle("🥏 LovePlay Arena - Couple Carrom 👑");
        setSize(550, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        CarromBoardPanel boardPanel = new CarromBoardPanel();
        add(boardPanel);
    }

    class CarromBoardPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
        private Timer timer;

        private int girlScore = 0;
        private int boyScore = 0;
        private boolean isGirlTurn = true;

        // Striker
        private float strikerX = 250, strikerY = 480;
        private float strikerVX = 0, strikerVY = 0;
        private final int STRIKER_RADIUS = 16;
        private boolean isAiming = false;
        private Point dragPoint;

        // Coins
        private List<Coin> coins = new ArrayList<>();

        public CarromBoardPanel() {
            setBackground(new Color(250, 235, 240));
            addMouseListener(this);
            addMouseMotionListener(this);

            initBoard();

            timer = new Timer(16, this); // ~60 FPS Physics
            timer.start();
        }

        private void initBoard() {
            coins.clear();
            // Center Queen (👑)
            coins.add(new Coin(250, 250, 0, "QUEEN"));

            // Girl Coins (Pink) & Boy Coins (Blue) surrounding center
            double angleStep = Math.PI / 3;
            for (int i = 0; i < 6; i++) {
                double angle = i * angleStep;
                int x = (int) (250 + 35 * Math.cos(angle));
                int y = (int) (250 + 35 * Math.sin(angle));
                String type = (i % 2 == 0) ? "GIRL" : "BOY";
                coins.add(new Coin(x, y, 0, type));
            }

            resetStriker();
        }

        private void resetStriker() {
            strikerVX = 0;
            strikerVY = 0;
            strikerX = 250;
            strikerY = isGirlTurn ? 120 : 480; // Top line for Girl, Bottom for Boy
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Update Striker Physics
            strikerX += strikerVX;
            strikerY += strikerVY;
            strikerVX *= 0.98f; // Friction
            strikerVY *= 0.98f;

            if (Math.abs(strikerVX) < 0.1) strikerVX = 0;
            if (Math.abs(strikerVY) < 0.1) strikerVY = 0;

            // Bounce Striker off Board Borders
            if (strikerX < 45 || strikerX > 455) strikerVX = -strikerVX;
            if (strikerY < 45 || strikerY > 455) strikerVY = -strikerVY;

            // Update Coins Physics & Collisions
            for (Coin coin : coins) {
                if (!coin.inPocket) {
                    coin.x += coin.vx;
                    coin.y += coin.vy;
                    coin.vx *= 0.98f;
                    coin.vy *= 0.98f;

                    if (coin.x < 45 || coin.x > 455) coin.vx = -coin.vx;
                    if (coin.y < 45 || coin.y > 455) coin.vy = -coin.vy;

                    // Striker vs Coin Collision
                    float dx = coin.x - strikerX;
                    float dy = coin.y - strikerY;
                    float dist = (float) Math.hypot(dx, dy);
                    if (dist < STRIKER_RADIUS + coin.radius) {
                        float angle = (float) Math.atan2(dy, dx);
                        coin.vx = (float) (Math.cos(angle) * 8);
                        coin.vy = (float) (Math.sin(angle) * 8);
                    }

                    // Check Pockets (4 Corners)
                    checkPocket(coin);
                }
            }

            repaint();
        }

        private void checkPocket(Coin coin) {
            int[][] pockets = {{45, 45}, {455, 45}, {45, 455}, {455, 455}};
            for (int[] p : pockets) {
                if (Math.hypot(coin.x - p[0], coin.y - p[1]) < 25) {
                    coin.inPocket = true;
                    if (coin.type.equals("GIRL")) girlScore += 10;
                    else if (coin.type.equals("BOY")) boyScore += 10;
                    else if (coin.type.equals("QUEEN")) {
                        if (isGirlTurn) girlScore += 30; else boyScore += 30;
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Carrom Outer Wooden Frame
            g2d.setColor(new Color(210, 140, 100));
            g2d.fillRoundRect(15, 15, 470, 470, 20, 20);

            // Carrom Playing Surface
            g2d.setColor(new Color(255, 245, 238));
            g2d.fillRect(35, 35, 430, 430);

            // 4 Corner Pockets (Holes)
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillOval(30, 30, 35, 35);
            g2d.fillOval(435, 30, 35, 35);
            g2d.fillOval(30, 435, 35, 35);
            g2d.fillOval(435, 435, 35, 35);

            // Center Circle
            g2d.setColor(new Color(255, 182, 193));
            g2d.drawOval(215, 215, 70, 70);

            // Baseline Indicators
            g2d.setColor(new Color(200, 150, 170));
            g2d.drawLine(90, 120, 410, 120); // Top Baseline
            g2d.drawLine(90, 380, 410, 380); // Bottom Baseline

            // Draw Coins
            for (Coin coin : coins) {
                if (!coin.inPocket) {
                    if (coin.type.equals("GIRL")) g2d.setColor(new Color(255, 105, 180)); // Pink
                    else if (coin.type.equals("BOY")) g2d.setColor(new Color(100, 149, 237)); // Blue
                    else g2d.setColor(new Color(255, 215, 0)); // Queen Gold

                    g2d.fillOval((int) coin.x - coin.radius, (int) coin.y - coin.radius, coin.radius * 2, coin.radius * 2);
                    g2d.setColor(Color.WHITE);
                    g2d.drawOval((int) coin.x - coin.radius, (int) coin.y - coin.radius, coin.radius * 2, coin.radius * 2);
                }
            }

            // Draw Striker
            g2d.setColor(new Color(186, 85, 211));
            g2d.fillOval((int) strikerX - STRIKER_RADIUS, (int) strikerY - STRIKER_RADIUS, STRIKER_RADIUS * 2, STRIKER_RADIUS * 2);
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int) strikerX - STRIKER_RADIUS, (int) strikerY - STRIKER_RADIUS, STRIKER_RADIUS * 2, STRIKER_RADIUS * 2);

            // Draw Aim Line when Dragging Mouse
            if (isAiming && dragPoint != null) {
                g2d.setColor(new Color(255, 20, 147));
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{9}, 0));
                g2d.drawLine((int) strikerX, (int) strikerY, (int) strikerX + ((int) strikerX - dragPoint.x), (int) strikerY + ((int) strikerY - dragPoint.y));
            }

            // Scoreboard & Turn Display
            g2d.setColor(new Color(110, 40, 130));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.drawString(girlName + " 👧 (Pink): " + girlScore + " pts", 35, 520);
            g2d.drawString(boyName + " 👦 (Blue): " + boyScore + " pts", 280, 520);

            String turnText = (isGirlTurn ? girlName + " 👧 (Top Turn)" : boyName + " 👦 (Bottom Turn)");
            g2d.drawString("Current Turn: " + turnText, 140, 555);
        }

        // --- Mouse Drag Mechanics for Aiming & Shooting ---
        @Override
        public void mousePressed(MouseEvent e) {
            if (Math.hypot(e.getX() - strikerX, e.getY() - strikerY) < STRIKER_RADIUS + 10) {
                isAiming = true;
                dragPoint = e.getPoint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (isAiming) {
                dragPoint = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isAiming && dragPoint != null) {
                strikerVX = (strikerX - dragPoint.x) * 0.25f;
                strikerVY = (strikerY - dragPoint.y) * 0.25f;
                isAiming = false;

                // Switch Turns after shooting
                Timer turnTimer = new Timer(3000, evt -> {
                    isGirlTurn = !isGirlTurn;
                    resetStriker();
                });
                turnTimer.setRepeats(false);
                turnTimer.start();
            }
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
    }

    class Coin {
        float x, y;
        float vx = 0, vy = 0;
        int radius = 12;
        String type;
        boolean inPocket = false;

        public Coin(float x, float y, float speed, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }
}