import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CupidArchery extends JFrame {
    private String girlName;
    private String boyName;

    public CupidArchery(String girlName, String boyName) {
        this.girlName = girlName;
        this.boyName = boyName;

        setTitle("🏹 LovePlay Arena - Cupid Archery 💘");
        setSize(500, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        ArcheryPanel panel = new ArcheryPanel();
        add(panel);
    }

    class ArcheryPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
        private Timer timer;

        private int girlScore = 0;
        private int boyScore = 0;

        private int girlArrows = 5;
        private int boyArrows = 5;
        private boolean isGirlTurn = true;

        // Target (Heart) Properties
        private int targetX = 100;
        private int targetY = 100;
        private int targetSpeed = 4;
        private final int TARGET_SIZE = 50;

        // Bow & Arrow Mechanics
        private int bowX = 250;
        private int bowY = 550;
        private boolean isAiming = false;
        private Point dragPoint;

        private List<Arrow> arrows = new ArrayList<>();

        public ArcheryPanel() {
            setBackground(new Color(255, 235, 240));
            addMouseListener(this);
            addMouseMotionListener(this);

            timer = new Timer(16, this); // ~60 FPS
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Move Target Left and Right
            targetX += targetSpeed;
            if (targetX < 40 || targetX > getWidth() - 90) {
                targetSpeed = -targetSpeed;
            }

            // Update Flying Arrows
            for (int i = 0; i < arrows.size(); i++) {
                Arrow arrow = arrows.get(i);
                if (arrow.active) {
                    arrow.x += arrow.vx;
                    arrow.y += arrow.vy;

                    // Hit Detection with Heart Target
                    if (Math.hypot(arrow.x - (targetX + TARGET_SIZE / 2), arrow.y - (targetY + TARGET_SIZE / 2)) < 35) {
                        arrow.active = false;
                        if (isGirlTurn) girlScore += 10; else boyScore += 10;
                        switchTurn();
                    }

                    // Missed Target (Out of Screen)
                    if (arrow.y < 0 || arrow.x < 0 || arrow.x > getWidth()) {
                        arrow.active = false;
                        switchTurn();
                    }
                }
            }

            repaint();
        }

        private void switchTurn() {
            if (isGirlTurn) {
                girlArrows--;
            } else {
                boyArrows--;
            }

            // Game Over Check
            if (girlArrows <= 0 && boyArrows <= 0) {
                timer.stop();
                String winner;
                if (girlScore > boyScore) winner = girlName + " 👧 Wins! 💕";
                else if (boyScore > girlScore) winner = boyName + " 👦 Wins! 💕";
                else winner = "It's a Tie! Both are Perfect Cupid Archers! 💘";

                JOptionPane.showMessageDialog(this, "🎉 Game Over! 🎉\n" + winner + "\n\n" + girlName + ": " + girlScore + " pts\n" + boyName + ": " + boyScore + " pts", "Cupid Archery Result", JOptionPane.INFORMATION_MESSAGE);
                resetGame();
                timer.start();
                return;
            }

            // Toggle Turn
            if (girlArrows > 0 && boyArrows > 0) {
                isGirlTurn = !isGirlTurn;
            } else if (girlArrows <= 0) {
                isGirlTurn = false;
            } else {
                isGirlTurn = true;
            }
        }

        private void resetGame() {
            girlScore = 0;
            boyScore = 0;
            girlArrows = 5;
            boyArrows = 5;
            isGirlTurn = true;
            arrows.clear();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Top Header Dashboard
            g2d.setColor(new Color(255, 182, 193));
            g2d.fillRect(0, 0, getWidth(), 60);

            g2d.setColor(new Color(110, 40, 130));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2d.drawString(girlName + " 👧: " + girlScore + " pts  (🏹 " + girlArrows + ")", 20, 35);
            g2d.drawString(boyName + " 👦: " + boyScore + " pts  (🏹 " + boyArrows + ")", 280, 35);

            // Draw Heart Target 🎯
            g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 45));
            g2d.drawString("🎯", targetX, targetY + 35);

            // Draw Bow (දුන්න)
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawArc(bowX - 35, bowY - 20, 70, 50, 0, -180);

            // Draw Bow String
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1));
            if (isAiming && dragPoint != null) {
                g2d.drawLine(bowX - 35, bowY, dragPoint.x, dragPoint.y);
                g2d.drawLine(bowX + 35, bowY, dragPoint.x, dragPoint.y);

                // Trajectory Aim Line
                g2d.setColor(new Color(255, 20, 147));
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8}, 0));
                g2d.drawLine(bowX, bowY, bowX + (bowX - dragPoint.x) * 3, bowY + (bowY - dragPoint.y) * 3);
            } else {
                g2d.drawLine(bowX - 35, bowY, bowX + 35, bowY);
            }

            // Draw Arrows
            for (Arrow arrow : arrows) {
                if (arrow.active) {
                    g2d.setColor(new Color(220, 20, 60));
                    g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                    g2d.drawString("💘", (int) arrow.x - 12, (int) arrow.y + 10);
                }
            }

            // Turn Info
            g2d.setColor(new Color(180, 30, 90));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            String currentTurn = isGirlTurn ? girlName + "'s Turn 👧" : boyName + "'s Turn 👦";
            g2d.drawString("Turn: " + currentTurn, 170, 610);
        }

        // --- Mouse Controls for Aiming Bow ---
        @Override
        public void mousePressed(MouseEvent e) {
            if (Math.hypot(e.getX() - bowX, e.getY() - bowY) < 50) {
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
                float vx = (bowX - dragPoint.x) * 0.25f;
                float vy = (bowY - dragPoint.y) * 0.25f;

                arrows.add(new Arrow(bowX, bowY, vx, vy));
                isAiming = false;
            }
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
    }

    class Arrow {
        float x, y;
        float vx, vy;
        boolean active = true;

        public Arrow(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }
}