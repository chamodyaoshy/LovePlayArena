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

        setTitle("LovePlay Arena - Couple Carrom");
        setSize(520, 665);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        CarromBoardPanel boardPanel = new CarromBoardPanel();
        add(boardPanel);
    }

    class CarromBoardPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
        private Timer timer;

        private int girlScore = 0;
        private int boyScore = 0;
        private boolean isGirlTurn = true;

        // Turn outcome flags after a shot finishes
        private boolean ownCoinPocketedThisTurn = false;
        private boolean opponentCoinPocketedThisTurn = false;
        private boolean queenPocketedThisTurn = false;
        private boolean strikerPocketedThisTurn = false;

        // Queen cover tracking
        private boolean queenNeedsCover = false;
        private boolean queenOwnerIsGirl = true;

        // Coins pocketed while Gold Queen is still on board (must pot & cover Gold first)
        private List<Coin> pendingGirlCoinsBeforeGold = new ArrayList<>();
        private List<Coin> pendingBoyCoinsBeforeGold = new ArrayList<>();

        private boolean gameOver = false;
        private boolean winnerDialogShown = false;
        private String statusMessage = "";

        // Board boundaries: surface is [35 .. 465], baseline is [90 .. 410]
        private final int BOARD_MIN = 35;
        private final int BOARD_MAX = 465;
        private final int BASELINE_LEFT = 90;
        private final int BASELINE_RIGHT = 410;
        private final int TOP_BASELINE_Y = 120;
        private final int BOTTOM_BASELINE_Y = 380;

        // Striker
        private float strikerX = 250, strikerY = TOP_BASELINE_Y;
        private float strikerVX = 0, strikerVY = 0;
        private final int STRIKER_RADIUS = 16;
        private boolean isAiming = false;
        private boolean isSlidingStriker = false;
        private boolean turnSwitchPending = false;
        private Point dragPoint;

        // Coins
        private List<Coin> coins = new ArrayList<>();

        public CarromBoardPanel() {
            setBackground(new Color(255, 240, 246));
            setFocusable(true);
            addMouseListener(this);
            addMouseMotionListener(this);
            addKeyListener(this);

            initBoard();

            timer = new Timer(16, this); // ~60 FPS Physics
            timer.start();
        }

        private void initBoard() {
            coins.clear();
            pendingGirlCoinsBeforeGold.clear();
            pendingBoyCoinsBeforeGold.clear();

            // Center Queen (Gold)
            coins.add(new Coin(250, 250, "QUEEN"));

            // Girl Coins (Pink) & Boy Coins (Blue) surrounding center
            double angleStep = Math.PI / 3;
            for (int i = 0; i < 6; i++) {
                double angle = i * angleStep;
                float x = (float) (250 + 35 * Math.cos(angle));
                float y = (float) (250 + 35 * Math.sin(angle));
                String type = (i % 2 == 0) ? "GIRL" : "BOY";
                coins.add(new Coin(x, y, type));
            }

            girlScore = 0;
            boyScore = 0;
            isGirlTurn = true;
            ownCoinPocketedThisTurn = false;
            opponentCoinPocketedThisTurn = false;
            queenPocketedThisTurn = false;
            strikerPocketedThisTurn = false;
            queenNeedsCover = false;
            gameOver = false;
            winnerDialogShown = false;
            statusMessage = "";
            turnSwitchPending = false;
            resetStriker();
        }

        private Coin getQueenCoin() {
            for (Coin c : coins) {
                if ("QUEEN".equals(c.type)) {
                    return c;
                }
            }
            return null;
        }

        private boolean isQueenStillOnBoard() {
            Coin queen = getQueenCoin();
            return queen != null && !queen.inPocket;
        }

        private boolean canPlaceStrikerAt(float tx, float ty) {
            for (Coin c : coins) {
                if (!c.inPocket) {
                    float minDist = STRIKER_RADIUS + c.radius + 1.5f;
                    if (Math.hypot(tx - c.x, ty - c.y) < minDist) {
                        return false;
                    }
                }
            }
            return true;
        }

        private float resolveValidStrikerX(float desiredX, float baseY, float currentFallbackX) {
            float clamped = clampStrikerX(desiredX);
            if (canPlaceStrikerAt(clamped, baseY)) {
                return clamped;
            }

            float minX = BASELINE_LEFT + STRIKER_RADIUS;
            float maxX = BASELINE_RIGHT - STRIKER_RADIUS;

            // Search outward left/right from desiredX for closest valid spot on baseline
            for (float offset = 2f; offset <= (maxX - minX); offset += 2f) {
                float rightCandidate = clamped + offset;
                if (rightCandidate <= maxX && canPlaceStrikerAt(rightCandidate, baseY)) {
                    return rightCandidate;
                }
                float leftCandidate = clamped - offset;
                if (leftCandidate >= minX && canPlaceStrikerAt(leftCandidate, baseY)) {
                    return leftCandidate;
                }
            }

            return canPlaceStrikerAt(currentFallbackX, baseY) ? currentFallbackX : clamped;
        }

        private void placeCoinAtFreeSpot(Coin target, float preferredX, float preferredY) {
            float tx = preferredX;
            float ty = preferredY;
            float minDist = target.radius * 2f + 2f;

            boolean blocked = false;
            for (Coin other : coins) {
                if (other != target && !other.inPocket) {
                    if (Math.hypot(other.x - tx, other.y - ty) < minDist) {
                        blocked = true;
                        break;
                    }
                }
            }

            if (blocked) {
                for (int r = 10; r <= 80; r += 6) {
                    for (int deg = 0; deg < 360; deg += 20) {
                        double rad = Math.toRadians(deg);
                        float cx = (float) (preferredX + r * Math.cos(rad));
                        float cy = (float) (preferredY + r * Math.sin(rad));
                        if (cx < BOARD_MIN + target.radius || cx > BOARD_MAX - target.radius) continue;
                        if (cy < BOARD_MIN + target.radius || cy > BOARD_MAX - target.radius) continue;

                        boolean free = true;
                        for (Coin other : coins) {
                            if (other != target && !other.inPocket) {
                                if (Math.hypot(other.x - cx, other.y - cy) < minDist) {
                                    free = false;
                                    break;
                                }
                            }
                        }
                        if (free) {
                            tx = cx;
                            ty = cy;
                            blocked = false;
                            break;
                        }
                    }
                    if (!blocked) break;
                }
            }

            target.x = tx;
            target.y = ty;
            target.vx = 0;
            target.vy = 0;
            target.inPocket = false;
        }

        private void returnQueenToCenter() {
            Coin queen = getQueenCoin();
            if (queen == null) return;
            placeCoinAtFreeSpot(queen, 250f, 250f);
        }

        private void restorePendingCoins(List<Coin> pendingList, float baseY) {
            for (Coin c : pendingList) {
                placeCoinAtFreeSpot(c, 250f, baseY);
            }
            pendingList.clear();
        }

        private void confirmPendingCoins(boolean forGirl) {
            List<Coin> list = forGirl ? pendingGirlCoinsBeforeGold : pendingBoyCoinsBeforeGold;
            for (Coin c : list) {
                if (c.inPocket) {
                    if (forGirl) girlScore += 10;
                    else boyScore += 10;
                }
            }
            list.clear();
        }

        private float getCurrentBaselineY() {
            return isGirlTurn ? TOP_BASELINE_Y : BOTTOM_BASELINE_Y;
        }

        private float clampStrikerX(float x) {
            float minX = BASELINE_LEFT + STRIKER_RADIUS;
            float maxX = BASELINE_RIGHT - STRIKER_RADIUS;
            return Math.max(minX, Math.min(maxX, x));
        }

        private boolean isNearCurrentBaseline(int mx, int my) {
            float baseY = getCurrentBaselineY();
            return Math.abs(my - baseY) <= 14 && mx >= BASELINE_LEFT && mx <= BASELINE_RIGHT;
        }

        private void resetStriker() {
            strikerPocketedThisTurn = false;
            strikerVX = 0;
            strikerVY = 0;
            float baseY = getCurrentBaselineY();
            strikerX = resolveValidStrikerX(250f, baseY, 250f);
            strikerY = baseY;
            isAiming = false;
            isSlidingStriker = false;
            turnSwitchPending = false;
        }

        private boolean isAnyPieceMoving() {
            if (!strikerPocketedThisTurn && Math.hypot(strikerVX, strikerVY) > 0.15) return true;
            for (Coin c : coins) {
                if (!c.inPocket && Math.hypot(c.vx, c.vy) > 0.15) {
                    return true;
                }
            }
            return false;
        }

        private boolean areAllCoinsPocketed() {
            for (Coin c : coins) {
                if (!c.inPocket) {
                    return false;
                }
            }
            return true;
        }

        private void checkStrikerPocket() {
            if (strikerPocketedThisTurn) return;
            int[][] pockets = {{47, 47}, {452, 47}, {47, 452}, {452, 452}};
            for (int[] p : pockets) {
                if (Math.hypot(strikerX - p[0], strikerY - p[1]) < 23) {
                    strikerPocketedThisTurn = true;
                    strikerVX = 0;
                    strikerVY = 0;
                    strikerX = -100;
                    strikerY = -100;
                    if (isGirlTurn) girlScore = Math.max(0, girlScore - 10);
                    else boyScore = Math.max(0, boyScore - 10);
                    break;
                }
            }
        }

        private void showGameOverDialog() {
            if (winnerDialogShown) return;
            winnerDialogShown = true;

            String resultTitle;
            String resultBody;

            if (girlScore > boyScore) {
                resultTitle = "Winner: " + girlName + "!";
                resultBody = girlName + " wins with " + girlScore + " points vs " + boyName + "'s " + boyScore + " points!";
            } else if (boyScore > girlScore) {
                resultTitle = "Winner: " + boyName + "!";
                resultBody = boyName + " wins with " + boyScore + " points vs " + girlName + "'s " + girlScore + " points!";
            } else {
                resultTitle = "It's a Tie!";
                resultBody = "Both " + girlName + " and " + boyName + " scored " + girlScore + " points!";
            }

            statusMessage = resultTitle;
            repaint();

            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showOptionDialog(
                        CoupleCarrom.this,
                        resultBody + "\n\nDo you want to play another round?",
                        "Game Over - LovePlay Arena",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        new Object[]{"Play Again", "Close"},
                        "Play Again"
                );

                if (choice == JOptionPane.YES_OPTION) {
                    initBoard();
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // 1. Update Striker Position & Friction
            if (!strikerPocketedThisTurn) {
                strikerX += strikerVX;
                strikerY += strikerVY;
                strikerVX *= 0.985f;
                strikerVY *= 0.985f;

                if (Math.abs(strikerVX) < 0.08f) strikerVX = 0;
                if (Math.abs(strikerVY) < 0.08f) strikerVY = 0;

                float minStriker = BOARD_MIN + STRIKER_RADIUS;
                float maxStriker = BOARD_MAX - STRIKER_RADIUS;

                if (strikerX < minStriker) {
                    strikerX = minStriker;
                    strikerVX = Math.abs(strikerVX) * 0.85f;
                } else if (strikerX > maxStriker) {
                    strikerX = maxStriker;
                    strikerVX = -Math.abs(strikerVX) * 0.85f;
                }

                if (strikerY < minStriker) {
                    strikerY = minStriker;
                    strikerVY = Math.abs(strikerVY) * 0.85f;
                } else if (strikerY > maxStriker) {
                    strikerY = maxStriker;
                    strikerVY = -Math.abs(strikerVY) * 0.85f;
                }

                checkStrikerPocket();
            }

            // 2. Update Coins Position, Bounds & Striker Collision
            for (Coin coin : coins) {
                if (coin.inPocket) continue;

                coin.x += coin.vx;
                coin.y += coin.vy;
                coin.vx *= 0.985f;
                coin.vy *= 0.985f;

                if (Math.abs(coin.vx) < 0.08f) coin.vx = 0;
                if (Math.abs(coin.vy) < 0.08f) coin.vy = 0;

                float minCoin = BOARD_MIN + coin.radius;
                float maxCoin = BOARD_MAX - coin.radius;

                if (coin.x < minCoin) {
                    coin.x = minCoin;
                    coin.vx = Math.abs(coin.vx) * 0.85f;
                } else if (coin.x > maxCoin) {
                    coin.x = maxCoin;
                    coin.vx = -Math.abs(coin.vx) * 0.85f;
                }

                if (coin.y < minCoin) {
                    coin.y = minCoin;
                    coin.vy = Math.abs(coin.vy) * 0.85f;
                } else if (coin.y > maxCoin) {
                    coin.y = maxCoin;
                    coin.vy = -Math.abs(coin.vy) * 0.85f;
                }

                if (!strikerPocketedThisTurn) {
                    float dx = coin.x - strikerX;
                    float dy = coin.y - strikerY;
                    float dist = (float) Math.hypot(dx, dy);
                    float minDist = STRIKER_RADIUS + coin.radius;

                    if (dist > 0.001f && dist < minDist) {
                        float nx = dx / dist;
                        float ny = dy / dist;
                        float overlap = minDist - dist;

                        strikerX -= nx * overlap * 0.4f;
                        strikerY -= ny * overlap * 0.4f;
                        coin.x += nx * overlap * 0.6f;
                        coin.y += ny * overlap * 0.6f;

                        float rvx = coin.vx - strikerVX;
                        float rvy = coin.vy - strikerVY;
                        float velAlongNormal = rvx * nx + rvy * ny;

                        if (velAlongNormal < 0) {
                            float restitution = 0.85f;
                            float impulse = -(1 + restitution) * velAlongNormal;
                            float totalMass = 2.0f;

                            coin.vx += (impulse / totalMass) * nx * 1.3f;
                            coin.vy += (impulse / totalMass) * ny * 1.3f;
                            strikerVX -= (impulse / totalMass) * nx * 0.7f;
                            strikerVY -= (impulse / totalMass) * ny * 0.7f;
                        }
                    }
                }

                checkPocket(coin);
            }

            // 3. Coin vs Coin Collision
            for (int i = 0; i < coins.size(); i++) {
                Coin a = coins.get(i);
                if (a.inPocket) continue;

                for (int j = i + 1; j < coins.size(); j++) {
                    Coin b = coins.get(j);
                    if (b.inPocket) continue;

                    float dx = b.x - a.x;
                    float dy = b.y - a.y;
                    float dist = (float) Math.hypot(dx, dy);
                    float minDist = a.radius + b.radius;

                    if (dist > 0.001f && dist < minDist) {
                        float nx = dx / dist;
                        float ny = dy / dist;
                        float overlap = (minDist - dist) * 0.5f;

                        a.x -= nx * overlap;
                        a.y -= ny * overlap;
                        b.x += nx * overlap;
                        b.y += ny * overlap;

                        float rvx = b.vx - a.vx;
                        float rvy = b.vy - a.vy;
                        float velAlongNormal = rvx * nx + rvy * ny;

                        if (velAlongNormal < 0) {
                            float impulse = -0.9f * velAlongNormal * 0.5f;
                            a.vx -= impulse * nx;
                            a.vy -= impulse * ny;
                            b.vx += impulse * nx;
                            b.vy += impulse * ny;
                        }
                    }
                }
            }

            // 4. Resolve rules & turns after all pieces stop
            if (turnSwitchPending && !isAnyPieceMoving()) {
                turnSwitchPending = false;
                String currentPlayer = isGirlTurn ? girlName : boyName;

                if (strikerPocketedThisTurn) {
                    if (queenNeedsCover) {
                        queenNeedsCover = false;
                        if (queenOwnerIsGirl) girlScore = Math.max(0, girlScore - 30);
                        else boyScore = Math.max(0, boyScore - 30);
                        returnQueenToCenter();
                    }
                    restorePendingCoins(pendingGirlCoinsBeforeGold, TOP_BASELINE_Y);
                    restorePendingCoins(pendingBoyCoinsBeforeGold, BOTTOM_BASELINE_Y);
                    isGirlTurn = !isGirlTurn;
                    String nextPlayer = isGirlTurn ? girlName : boyName;
                    statusMessage = "Striker foul (-10 pts)! Turn passed to " + nextPlayer + ".";
                } else if (queenNeedsCover && isGirlTurn == queenOwnerIsGirl) {
                    if (ownCoinPocketedThisTurn) {
                        queenNeedsCover = false;
                        confirmPendingCoins(queenOwnerIsGirl);
                        statusMessage = "Gold covered! Your coins now count! Extra chance for " + currentPlayer + "!";
                        if (opponentCoinPocketedThisTurn) {
                            isGirlTurn = !isGirlTurn;
                            String nextPlayer = isGirlTurn ? girlName : boyName;
                            statusMessage += " (Opp coin +10 to " + nextPlayer + ")";
                        }
                    } else {
                        queenNeedsCover = false;
                        if (queenOwnerIsGirl) girlScore = Math.max(0, girlScore - 30);
                        else boyScore = Math.max(0, boyScore - 30);
                        returnQueenToCenter();
                        restorePendingCoins(pendingGirlCoinsBeforeGold, TOP_BASELINE_Y);
                        restorePendingCoins(pendingBoyCoinsBeforeGold, BOTTOM_BASELINE_Y);
                        isGirlTurn = !isGirlTurn;
                        statusMessage = "No cover! Gold & coins returned. Turn passed.";
                    }
                } else if (queenPocketedThisTurn) {
                    if (ownCoinPocketedThisTurn) {
                        queenNeedsCover = false;
                        confirmPendingCoins(isGirlTurn);
                        statusMessage = "Gold (+30) & own coin pocketed! Extra chance for " + currentPlayer + "!";
                    } else {
                        queenNeedsCover = true;
                        queenOwnerIsGirl = isGirlTurn;
                        statusMessage = "Gold (+30)! Pocket your own coin now to cover Gold!";
                    }
                } else if (opponentCoinPocketedThisTurn) {
                    if (isQueenStillOnBoard()) {
                        restorePendingCoins(pendingGirlCoinsBeforeGold, TOP_BASELINE_Y);
                        restorePendingCoins(pendingBoyCoinsBeforeGold, BOTTOM_BASELINE_Y);
                    }
                    isGirlTurn = !isGirlTurn;
                    String nextPlayer = isGirlTurn ? girlName : boyName;
                    statusMessage = "Opponent coin pocketed! +10 to " + nextPlayer + " & turn passed.";
                } else if (ownCoinPocketedThisTurn) {
                    if (isQueenStillOnBoard()) {
                        statusMessage = "Pot Gold first! Your coin stays until Gold is potted & covered.";
                        isGirlTurn = !isGirlTurn;
                    } else {
                        statusMessage = "+10 pts! " + currentPlayer + " gets an extra chance!";
                    }
                } else {
                    if (isQueenStillOnBoard()) {
                        restorePendingCoins(pendingGirlCoinsBeforeGold, TOP_BASELINE_Y);
                        restorePendingCoins(pendingBoyCoinsBeforeGold, BOTTOM_BASELINE_Y);
                    }
                    isGirlTurn = !isGirlTurn;
                    statusMessage = "";
                }

                if (areAllCoinsPocketed() && queenNeedsCover) {
                    queenNeedsCover = false;
                    if (queenOwnerIsGirl) girlScore = Math.max(0, girlScore - 30);
                    else boyScore = Math.max(0, boyScore - 30);
                    returnQueenToCenter();
                    statusMessage = "Cover Gold before final coin! Gold back to center.";
                }

                if (areAllCoinsPocketed()) {
                    gameOver = true;
                    strikerVX = 0;
                    strikerVY = 0;
                    showGameOverDialog();
                } else {
                    resetStriker();
                }
            }

            repaint();
        }

        private void checkPocket(Coin coin) {
            int[][] pockets = {{47, 47}, {452, 47}, {47, 452}, {452, 452}};
            for (int[] p : pockets) {
                if (Math.hypot(coin.x - p[0], coin.y - p[1]) < 24) {
                    coin.inPocket = true;
                    coin.vx = 0;
                    coin.vy = 0;

                    if (coin.type.equals("GIRL")) {
                        if (!isQueenStillOnBoard() || queenPocketedThisTurn || (queenNeedsCover && queenOwnerIsGirl)) {
                            girlScore += 10;
                        } else {
                            if (!pendingGirlCoinsBeforeGold.contains(coin)) {
                                pendingGirlCoinsBeforeGold.add(coin);
                            }
                        }
                        if (isGirlTurn) {
                            ownCoinPocketedThisTurn = true;
                        } else {
                            opponentCoinPocketedThisTurn = true;
                        }
                    } else if (coin.type.equals("BOY")) {
                        if (!isQueenStillOnBoard() || queenPocketedThisTurn || (queenNeedsCover && !queenOwnerIsGirl)) {
                            boyScore += 10;
                        } else {
                            if (!pendingBoyCoinsBeforeGold.contains(coin)) {
                                pendingBoyCoinsBeforeGold.add(coin);
                            }
                        }
                        if (!isGirlTurn) {
                            ownCoinPocketedThisTurn = true;
                        } else {
                            opponentCoinPocketedThisTurn = true;
                        }
                    } else if (coin.type.equals("QUEEN")) {
                        queenPocketedThisTurn = true;
                        if (isGirlTurn) girlScore += 30;
                        else boyScore += 30;
                    }
                    break;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 1. Soft Romantic Background
            g2d.setPaint(new GradientPaint(0, 0, new Color(255, 240, 246), 0, getHeight(), new Color(245, 235, 255)));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 2. Outer Wooden Frame with Subtle Shadow & Gold Trim
            g2d.setColor(new Color(90, 45, 65, 45));
            g2d.fillRoundRect(18, 20, 470, 470, 26, 26);

            g2d.setColor(new Color(176, 102, 82));
            g2d.fillRoundRect(15, 15, 470, 470, 24, 24);

            g2d.setColor(new Color(235, 185, 120));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(20, 20, 460, 460, 20, 20);

            // 3. Carrom Playing Surface (Cream Blush)
            g2d.setColor(new Color(255, 248, 245));
            g2d.fillRoundRect(35, 35, 430, 430, 14, 14);

            // Subtle Inner Border Line
            g2d.setColor(new Color(240, 195, 210));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(42, 42, 416, 416, 10, 10);

            // 4. Center Couple Medallion
            int cx = 250, cy = 250;
            g2d.setColor(new Color(255, 192, 203, 130));
            g2d.fillOval(cx - 56, cy - 56, 112, 112);

            g2d.setColor(new Color(236, 72, 153));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(cx - 56, cy - 56, 112, 112);

            g2d.setColor(new Color(212, 175, 55));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(cx - 42, cy - 42, 84, 84);

            // 8 Delicate Rays around Center
            g2d.setColor(new Color(244, 114, 182, 170));
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI / 4);
                int x2 = (int) (cx + Math.cos(a) * 54);
                int y2 = (int) (cy + Math.sin(a) * 54);
                g2d.drawLine(cx, cy, x2, y2);
            }

            // Center Heart Emblem
            g2d.setColor(new Color(255, 248, 245));
            g2d.fillOval(cx - 16, cy - 16, 32, 32);
            g2d.setColor(new Color(225, 29, 72));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2d.drawString("<3", cx - 9, cy + 6);

            // 5. Baselines with End Circles
            g2d.setStroke(new BasicStroke(2f));

            // Top Baseline (Girl - Pink)
            g2d.setColor(new Color(244, 114, 182));
            g2d.drawLine(BASELINE_LEFT, TOP_BASELINE_Y, BASELINE_RIGHT, TOP_BASELINE_Y);
            g2d.fillOval(BASELINE_LEFT - 5, TOP_BASELINE_Y - 5, 10, 10);
            g2d.fillOval(BASELINE_RIGHT - 5, TOP_BASELINE_Y - 5, 10, 10);

            // Bottom Baseline (Boy - Blue)
            g2d.setColor(new Color(96, 165, 250));
            g2d.drawLine(BASELINE_LEFT, BOTTOM_BASELINE_Y, BASELINE_RIGHT, BOTTOM_BASELINE_Y);
            g2d.fillOval(BASELINE_LEFT - 5, BOTTOM_BASELINE_Y - 5, 10, 10);
            g2d.fillOval(BASELINE_RIGHT - 5, BOTTOM_BASELINE_Y - 5, 10, 10);

            // 6. 4 Corner Pockets with Gold Rim
            int[][] pocketCenters = {{47, 47}, {452, 47}, {47, 452}, {452, 452}};
            for (int[] p : pocketCenters) {
                g2d.setColor(new Color(212, 175, 55));
                g2d.fillOval(p[0] - 20, p[1] - 20, 40, 40);

                g2d.setColor(new Color(38, 28, 36));
                g2d.fillOval(p[0] - 17, p[1] - 17, 34, 34);
            }

            // 7. Draw Coins
            for (Coin coin : coins) {
                if (!coin.inPocket) {
                    if (coin.type.equals("GIRL")) {
                        g2d.setColor(new Color(255, 105, 180));
                    } else if (coin.type.equals("BOY")) {
                        g2d.setColor(new Color(79, 134, 247));
                    } else {
                        g2d.setColor(new Color(255, 215, 0));
                    }

                    g2d.fillOval((int) coin.x - coin.radius, (int) coin.y - coin.radius, coin.radius * 2, coin.radius * 2);

                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(1.8f));
                    g2d.drawOval((int) coin.x - coin.radius, (int) coin.y - coin.radius, coin.radius * 2, coin.radius * 2);

                    g2d.setStroke(new BasicStroke(1.2f));
                    g2d.drawOval((int) coin.x - coin.radius + 4, (int) coin.y - coin.radius + 4, (coin.radius - 4) * 2, (coin.radius - 4) * 2);
                }
            }

            // 8. Draw Striker
            if (!gameOver && !strikerPocketedThisTurn) {
                g2d.setColor(new Color(168, 85, 247));
                g2d.fillOval((int) strikerX - STRIKER_RADIUS, (int) strikerY - STRIKER_RADIUS, STRIKER_RADIUS * 2, STRIKER_RADIUS * 2);

                g2d.setColor(new Color(255, 228, 240));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawOval((int) strikerX - STRIKER_RADIUS, (int) strikerY - STRIKER_RADIUS, STRIKER_RADIUS * 2, STRIKER_RADIUS * 2);

                g2d.setColor(Color.WHITE);
                g2d.fillOval((int) strikerX - 5, (int) strikerY - 5, 10, 10);
            }

            // 9. Draw Aim Trajectory Line
            if (!gameOver && !strikerPocketedThisTurn && isAiming && dragPoint != null) {
                int targetX = (int) (strikerX + (strikerX - dragPoint.x));
                int targetY = (int) (strikerY + (strikerY - dragPoint.y));

                g2d.setColor(new Color(236, 72, 153));
                g2d.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8, 6}, 0));
                g2d.drawLine((int) strikerX, (int) strikerY, targetX, targetY);
                g2d.fillOval(targetX - 5, targetY - 5, 10, 10);
            }

            // 10. Scoreboard Card & Turn Status
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(20, 496, 460, 120, 16, 16);
            g2d.setColor(new Color(244, 180, 205));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(20, 496, 460, 120, 16, 16);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2d.setColor(new Color(219, 39, 119));
            g2d.drawString(girlName + " (Pink): " + girlScore + " pts", 35, 522);

            g2d.setColor(new Color(37, 99, 235));
            g2d.drawString(boyName + " (Blue): " + boyScore + " pts", 265, 522);

            g2d.setColor(new Color(88, 28, 135));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            String turnText = gameOver
                    ? "Match Finished!"
                    : (isGirlTurn ? ("Turn: " + girlName + " -> Pocket Pink") : ("Turn: " + boyName + " -> Pocket Blue"));
            g2d.drawString(turnText, 125, 550);

            if (!statusMessage.isEmpty()) {
                g2d.setColor(new Color(190, 24, 93));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2d.drawString(statusMessage, 42, 574);
            }

            g2d.setColor(new Color(107, 114, 128));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.drawString("Striker cannot be placed over a coin on the baseline", 112, 598);
        }

        // --- Mouse Click / Drag Mechanics for Baseline Move, Sliding & Shooting ---
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            if (gameOver || turnSwitchPending || isAnyPieceMoving() || strikerPocketedThisTurn) return;

            int mx = e.getX();
            int my = e.getY();

            if (Math.hypot(mx - strikerX, my - strikerY) < STRIKER_RADIUS + 14) {
                isSlidingStriker = true;
                isAiming = false;
                dragPoint = e.getPoint();
                repaint();
            } else if (isNearCurrentBaseline(mx, my)) {
                float baseY = getCurrentBaselineY();
                float nextX = resolveValidStrikerX(mx, baseY, strikerX);
                if (nextX != strikerX || canPlaceStrikerAt(strikerX, baseY)) {
                    strikerX = nextX;
                    strikerY = baseY;
                    isSlidingStriker = true;
                    isAiming = false;
                    dragPoint = e.getPoint();
                    repaint();
                } else {
                    statusMessage = "Cannot place striker over a coin on the baseline!";
                    repaint();
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (gameOver || turnSwitchPending || isAnyPieceMoving() || strikerPocketedThisTurn) return;

            if (isSlidingStriker) {
                float baseY = getCurrentBaselineY();
                float pullDistY = Math.abs(e.getY() - baseY);
                if (pullDistY > 18) {
                    isSlidingStriker = false;
                    isAiming = true;
                    dragPoint = e.getPoint();
                } else {
                    strikerX = resolveValidStrikerX(e.getX(), baseY, strikerX);
                    strikerY = baseY;
                }
                repaint();
            } else if (isAiming) {
                dragPoint = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!gameOver && isAiming && dragPoint != null) {
                float powerX = (strikerX - dragPoint.x) * 0.22f;
                float powerY = (strikerY - dragPoint.y) * 0.22f;

                float maxSpeed = 16.0f;
                float speed = (float) Math.hypot(powerX, powerY);
                if (speed > maxSpeed) {
                    powerX = (powerX / speed) * maxSpeed;
                    powerY = (powerY / speed) * maxSpeed;
                }

                ownCoinPocketedThisTurn = false;
                opponentCoinPocketedThisTurn = false;
                queenPocketedThisTurn = false;
                strikerPocketedThisTurn = false;
                statusMessage = "";
                strikerVX = powerX;
                strikerVY = powerY;
                isAiming = false;
                turnSwitchPending = true;
            }
            isSlidingStriker = false;
            dragPoint = null;
        }

        // --- Keyboard Left / Right Sliding ---
        @Override
        public void keyPressed(KeyEvent e) {
            if (gameOver || turnSwitchPending || isAnyPieceMoving() || isAiming || strikerPocketedThisTurn) return;

            float step = 10.0f;
            float baseY = getCurrentBaselineY();

            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                float targetX = clampStrikerX(strikerX - step);
                if (canPlaceStrikerAt(targetX, baseY)) {
                    strikerX = targetX;
                    strikerY = baseY;
                    repaint();
                } else {
                    // Step past blocking coin if free space is just beyond it
                    float shifted = resolveValidStrikerX(targetX - step, baseY, strikerX);
                    if (canPlaceStrikerAt(shifted, baseY)) {
                        strikerX = shifted;
                        strikerY = baseY;
                        repaint();
                    }
                }
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                float targetX = clampStrikerX(strikerX + step);
                if (canPlaceStrikerAt(targetX, baseY)) {
                    strikerX = targetX;
                    strikerY = baseY;
                    repaint();
                } else {
                    float shifted = resolveValidStrikerX(targetX + step, baseY, strikerX);
                    if (canPlaceStrikerAt(shifted, baseY)) {
                        strikerX = shifted;
                        strikerY = baseY;
                        repaint();
                    }
                }
            }
        }

        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}
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

        public Coin(float x, float y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }
}