import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class TicTacToe extends JFrame {
    private String girlName;
    private String boyName;
    private boolean isGirlTurn = true; 

    private JButton[][] board = new JButton[3][3];
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private int girlScore = 0;
    private int boyScore = 0;

    private static final String GIRL_ICON = "👧";
    private static final String BOY_ICON = "👦";

    public TicTacToe(String girlName, String boyName) {
        this.girlName = girlName;
        this.boyName = boyName;

        setTitle("💖 LovePlay Arena - Tic-Tac-Toe 💖");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        BackgroundPanel mainPanel = new BackgroundPanel();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top Panel
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setOpaque(false);

        statusLabel = new JLabel(girlName + "'s Turn 💖 (" + GIRL_ICON + ")", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        statusLabel.setForeground(new Color(255, 105, 180));

        scoreLabel = new JLabel(girlName + " " + GIRL_ICON + " : " + girlScore + "  |  " + boyName + " " + BOY_ICON + " : " + boyScore, SwingConstants.CENTER);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        scoreLabel.setForeground(new Color(110, 40, 130));

        topPanel.add(statusLabel);
        topPanel.add(scoreLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Game Grid
        JPanel gridPanel = new JPanel(new GridLayout(3, 3, 12, 12));
        gridPanel.setOpaque(false);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = createCuteButton();
                final int row = r;
                final int col = c;
                board[r][c].addActionListener(e -> handleCellClick(row, col));
                gridPanel.add(board[r][c]);
            }
        }
        mainPanel.add(gridPanel, BorderLayout.CENTER);

        // Bottom Reset Button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomPanel.setOpaque(false);

        JButton restartBtn = new JButton("🔄 Restart Game");
        restartBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        restartBtn.setBackground(new Color(255, 182, 193));
        restartBtn.setFocusPainted(false);
        restartBtn.addActionListener(e -> resetGame());

        bottomPanel.add(restartBtn);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JButton createCuteButton() {
        JButton btn = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 220));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 30, 30));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void handleCellClick(int row, int col) {
        if (!board[row][col].getText().equals("")) return;

        if (isGirlTurn) {
            board[row][col].setText(GIRL_ICON);
        } else {
            board[row][col].setText(BOY_ICON);
        }

        if (checkWin()) {
            String winner = isGirlTurn ? girlName : boyName;
            if (isGirlTurn) girlScore++; else boyScore++;
            updateScore();
            showWinnerDialog(winner + " Wins! 💕 " + (isGirlTurn ? GIRL_ICON : BOY_ICON));
            disableBoard();
        } else if (isBoardFull()) {
            JOptionPane.showMessageDialog(this, "It's a Tie! 💕 Both of you are made for each other!", "Game Draw", JOptionPane.INFORMATION_MESSAGE);
        } else {
            isGirlTurn = !isGirlTurn;
            statusLabel.setText((isGirlTurn ? girlName : boyName) + "'s Turn 💕 (" + (isGirlTurn ? GIRL_ICON : BOY_ICON) + ")");
            statusLabel.setForeground(isGirlTurn ? new Color(255, 105, 180) : new Color(70, 130, 180));
        }
    }

    private boolean checkWin() {
        for (int i = 0; i < 3; i++) {
            if (!board[i][0].getText().equals("") && board[i][0].getText().equals(board[i][1].getText()) && board[i][1].getText().equals(board[i][2].getText())) return true;
            if (!board[0][i].getText().equals("") && board[0][i].getText().equals(board[1][i].getText()) && board[1][i].getText().equals(board[2][i].getText())) return true;
        }
        if (!board[0][0].getText().equals("") && board[0][0].getText().equals(board[1][1].getText()) && board[1][1].getText().equals(board[2][2].getText())) return true;
        if (!board[0][2].getText().equals("") && board[0][2].getText().equals(board[1][1].getText()) && board[1][1].getText().equals(board[2][0].getText())) return true;

        return false;
    }

    private boolean isBoardFull() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c].getText().equals("")) return false;
            }
        }
        return true;
    }

    private void disableBoard() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c].setEnabled(false);
            }
        }
    }

    private void resetGame() {
        isGirlTurn = true;
        statusLabel.setText(girlName + "'s Turn 💖 (" + GIRL_ICON + ")");
        statusLabel.setForeground(new Color(255, 105, 180));

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c].setText("");
                board[r][c].setEnabled(true);
            }
        }
    }

    private void updateScore() {
        scoreLabel.setText(girlName + " " + GIRL_ICON + " : " + girlScore + "  |  " + boyName + " " + BOY_ICON + " : " + boyScore);
    }

    private void showWinnerDialog(String message) {
        JDialog winDialog = new JDialog(this, "🎉 Winner! 🎉", true);
        winDialog.setSize(350, 200);
        winDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 230, 240), getWidth(), getHeight(), new Color(255, 192, 203));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new BorderLayout());

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(new Color(180, 30, 90));

        JButton okBtn = new JButton("Play Again 💖");
        okBtn.setBackground(new Color(255, 105, 180));
        okBtn.setForeground(Color.WHITE);
        okBtn.addActionListener(e -> {
            winDialog.dispose();
            resetGame();
        });

        panel.add(label, BorderLayout.CENTER);
        JPanel btnP = new JPanel(); btnP.setOpaque(false); btnP.add(okBtn);
        panel.add(btnP, BorderLayout.SOUTH);

        winDialog.add(panel);
        winDialog.setVisible(true);
    }

    class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, new Color(255, 209, 220), getWidth(), getHeight(), new Color(225, 200, 255));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}