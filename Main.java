package standalone;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * Run this file: right-click → Run As → Java Application.
 * No other setup needed.
 */
public class Main extends JFrame {

    private static final long serialVersionUID = 1L;

    // ── colours ───────────────────────────────────────────────────────────────
    private static final Color BG          = new Color(0x0F172A);
    private static final Color CELL_EMPTY  = new Color(0x1E293B);
    private static final Color CELL_BORDER = new Color(0x2D3F55);
    private static final Color FOOD_TINT   = new Color(0x1A3A1A);
    private static final Color TEXT_COLOR  = Color.WHITE;

    // ── UI ────────────────────────────────────────────────────────────────────
    private final Canvas    canvas;
    private final JButton   stepBtn;
    private final JButton   autoBtn;
    private final JButton   resetBtn;
    private final JSlider   speedSlider;
    private final JTextArea logArea;
    private final JLabel    statsLabel;

    // ── simulation ────────────────────────────────────────────────────────────
    private final Simulation sim;
    private Timer   autoTimer;
    private boolean running = false;

    public Main() {
        super("Predator-Prey Simulation");
        sim = new Simulation(new Random());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));
        getContentPane().setBackground(BG);

        // ── buttons ───────────────────────────────────────────────────────────
        stepBtn  = makeButton("⏭  Step",  new Color(0x3B82F6));
        autoBtn  = makeButton("▶  Auto",  new Color(0x22C55E));
        resetBtn = makeButton("↺  Reset", new Color(0xF59E0B));

        JLabel speedLbl = new JLabel("Speed:");
        speedLbl.setForeground(TEXT_COLOR);
        speedSlider = new JSlider(20, 1000, 200);
        speedSlider.setInverted(true);
        speedSlider.setPreferredSize(new Dimension(120, 26));
        speedSlider.setBackground(new Color(0x1E293B));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setBackground(new Color(0x1E293B));
        controls.setBorder(new TitledBorder(
                BorderFactory.createLineBorder(new Color(0x334155)),
                "Controls", TitledBorder.LEFT, TitledBorder.TOP, null, TEXT_COLOR));
        controls.add(stepBtn);
        controls.add(autoBtn);
        controls.add(resetBtn);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(speedLbl);
        controls.add(speedSlider);
        controls.add(Box.createHorizontalStrut(16));
        controls.add(makeLegend());

        // ── canvas ────────────────────────────────────────────────────────────
        canvas = new Canvas();
        canvas.setBackground(BG);
        canvas.setPreferredSize(new Dimension(720, 660));

        // ── stats ─────────────────────────────────────────────────────────────
        statsLabel = new JLabel(" ");
        statsLabel.setForeground(TEXT_COLOR);
        statsLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statsLabel.setOpaque(true);
        statsLabel.setBackground(BG);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // ── log ───────────────────────────────────────────────────────────────
        logArea = new JTextArea(5, 40);
        logArea.setEditable(false);
        logArea.setBackground(new Color(0x0D1B2A));
        logArea.setForeground(new Color(0xA0C4FF));
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder(
                BorderFactory.createLineBorder(new Color(0x334155)),
                "Event log", TitledBorder.LEFT, TitledBorder.TOP, null, TEXT_COLOR));
        logScroll.setPreferredSize(new Dimension(720, 120));

        // ── bottom panel ──────────────────────────────────────────────────────
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);
        bottom.add(statsLabel, BorderLayout.NORTH);
        bottom.add(logScroll,  BorderLayout.CENTER);

        add(controls, BorderLayout.NORTH);
        add(canvas,   BorderLayout.CENTER);
        add(bottom,   BorderLayout.SOUTH);

        // ── timer ─────────────────────────────────────────────────────────────
        autoTimer = new Timer(speedSlider.getValue(), e -> doStep());
        speedSlider.addChangeListener(e -> autoTimer.setDelay(speedSlider.getValue()));

        // ── button actions ────────────────────────────────────────────────────
        stepBtn.addActionListener(e -> { stopAuto(); doStep(); });

        autoBtn.addActionListener(e -> {
            if (running) stopAuto(); else startAuto();
        });

        resetBtn.addActionListener(e -> {
            stopAuto();
            sim.reset();
            logArea.setText("");
            refresh();
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        refresh();
    }

    // ── step ──────────────────────────────────────────────────────────────────

    private void doStep() {
        if (!sim.isAlive()) {
            stopAuto();
            log("=== Simulation ended at step " + sim.getStep() + " ===");
            return;
        }
        sim.step();
        List<String> events = sim.getLastEvents();
        if (!events.isEmpty()) {
            StringBuilder sb = new StringBuilder("[Step " + sim.getStep() + "]\n");
            for (String e : events) sb.append("  ").append(e).append('\n');
            log(sb.toString().trim());
        }
        refresh();
    }

    private void startAuto() {
        running = true;
        autoBtn.setText("⏸  Pause");
        autoBtn.setBackground(new Color(0xEF4444));
        stepBtn.setEnabled(false);
        autoTimer.start();
    }

    private void stopAuto() {
        running = false;
        autoTimer.stop();
        autoBtn.setText("▶  Auto");
        autoBtn.setBackground(new Color(0x22C55E));
        stepBtn.setEnabled(true);
    }

    private void refresh() {
        canvas.repaint();
        statsLabel.setText(String.format(
                "  Step: %d  |  Predators: %d  |  Prey: %d  " +
                "|  Catches: %d  |  Eats: %d  |  Births: %d  |  Deaths: %d",
                sim.getStep(), sim.alivePredators(), sim.alivePrey(),
                sim.getCatches(), sim.getEats(), sim.getBirths(), sim.getDeaths()));
    }

    private void log(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ── hex canvas ────────────────────────────────────────────────────────────

    class Canvas extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            HexGrid grid = sim.getGrid();
            if (grid == null) return;

            // Compute hex size to fill the panel neatly
            int margin  = 20;
            int R       = Simulation.GRID_RADIUS;
            double sz1  = (getWidth()  - 2.0 * margin) / ((2.0 * R + 1) * 1.5);
            double sz2  = (getHeight() - 2.0 * margin) / ((2.0 * R + 1) * Math.sqrt(3));
            int hexSize = Math.max((int) Math.min(sz1, sz2), 4);
            int cx = getWidth()  / 2;
            int cy = getHeight() / 2;

            // Draw cells
            for (HexCell cell : grid.getAllCells()) {
                int px = (int) Math.round(cx + hexSize * 1.5 * cell.getQ());
                int py = (int) Math.round(cy + hexSize * Math.sqrt(3)
                                               * (cell.getR() + cell.getQ() * 0.5));
                Polygon hex = hexPolygon(px, py, hexSize);
                g2.setColor(cellColor(cell));
                g2.fillPolygon(hex);
                g2.setColor(CELL_BORDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawPolygon(hex);
            }

            // Draw energy bars on agent cells
            if (hexSize >= 10) {
                drawBars(g2, sim.getPredators(), cx, cy, hexSize,
                         Simulation.PREDATOR_INIT_ENERGY, new Color(0xF87171));
                drawBars(g2, sim.getPrey(),      cx, cy, hexSize,
                         Simulation.PREY_INIT_ENERGY,     new Color(0x4ADE80));
            }

            // Ended message
            if (!sim.isAlive()) {
                g2.setColor(new Color(0xFBBF24));
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
                g2.drawString("Simulation ended — press Reset", margin + 8, margin + 22);
            }
        }

        private void drawBars(Graphics2D g2, List<Agent> agents,
                              int cx, int cy, int hexSize,
                              double maxE, Color fullColor) {
            for (Agent a : agents) {
                if (!a.isAlive()) continue;
                int px = (int) Math.round(cx + hexSize * 1.5 * a.getQ());
                int py = (int) Math.round(cy + hexSize * Math.sqrt(3)
                                               * (a.getR() + a.getQ() * 0.5));
                int   bw    = hexSize;
                int   bh    = Math.max(3, hexSize / 8);
                int   bx    = px - bw / 2;
                int   by    = py - hexSize + 2;
                float ratio = (float) Math.min(a.getEnergy() / maxE, 1.0);
                g2.setColor(new Color(0x1E293B));
                g2.fillRect(bx, by, bw, bh);
                g2.setColor(dim(fullColor, ratio));
                g2.fillRect(bx, by, (int)(bw * ratio), bh);
            }
        }

        private Color cellColor(HexCell cell) {
            switch (cell.getState()) {
                case PREDATOR: {
                    Agent a = agentAt(sim.getPredators(), cell.getQ(), cell.getR());
                    float r = a != null
                            ? (float)(a.getEnergy() / Simulation.PREDATOR_INIT_ENERGY) : 1f;
                    return dim(new Color(0xF87171), Math.max(0.3f, r));
                }
                case PREY: {
                    Agent a = agentAt(sim.getPrey(), cell.getQ(), cell.getR());
                    float r = a != null
                            ? (float)(a.getEnergy() / Simulation.PREY_INIT_ENERGY) : 1f;
                    return dim(new Color(0x4ADE80), Math.max(0.3f, r));
                }
                default: {
                    float f = (float) cell.getFood() / HexCell.FOOD_MAX;
                    if (f < 0.01f) return CELL_EMPTY;
                    return blend(CELL_EMPTY, FOOD_TINT, f * 0.85f);
                }
            }
        }

        private Agent agentAt(List<Agent> list, int q, int r) {
            for (Agent a : list)
                if (a.isAlive() && a.getQ() == q && a.getR() == r) return a;
            return null;
        }

        private Color dim(Color c, float t) {
            t = Math.max(0f, Math.min(1f, t));
            return new Color((int)(c.getRed()*t), (int)(c.getGreen()*t), (int)(c.getBlue()*t));
        }

        private Color blend(Color a, Color b, float t) {
            t = Math.max(0f, Math.min(1f, t));
            return new Color(
                    (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                    (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
        }

        private Polygon hexPolygon(int cx, int cy, int size) {
            Polygon p = new Polygon();
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(60 * i);
                p.addPoint((int) Math.round(cx + size * Math.cos(a)),
                           (int) Math.round(cy + size * Math.sin(a)));
            }
            return p;
        }
    }

    // ── legend ────────────────────────────────────────────────────────────────

    private JPanel makeLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setBackground(new Color(0x1E293B));
        p.add(dot(new Color(0xF87171), "Predator"));
        p.add(dot(new Color(0x4ADE80), "Prey"));
        p.add(dot(new Color(0x2D6A2D), "Food"));
        p.add(dot(CELL_EMPTY,           "Empty"));
        return p;
    }

    private JPanel dot(Color color, String label) {
        JPanel swatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(color);
                g.fillRoundRect(0, 3, 13, 13, 3, 3);
            }
        };
        swatch.setPreferredSize(new Dimension(14, 20));
        swatch.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT_COLOR);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        JPanel pair = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        pair.setBackground(new Color(0x1E293B));
        pair.add(swatch); pair.add(lbl);
        return pair;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 28));
        return b;
    }

    // ── entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
