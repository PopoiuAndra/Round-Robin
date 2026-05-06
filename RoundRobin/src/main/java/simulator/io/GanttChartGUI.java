package simulator.io;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GanttChartGUI extends JFrame implements SimulationEventListener {

    private final Map<Integer, List<ExecutionBlock>> cpuSchedules = new HashMap<>();
    private final List<ExecutionBlock> diskSchedule = new ArrayList<>(); // Disk Line

    private int maxTick = 0;
    private final GanttPanel ganttPanel;

    public GanttChartGUI(int numProcessors) {
        setTitle("OS Simulator - Gantt Chart (CPU + Disk)");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        for (int i = 0; i < numProcessors; i++) {
            cpuSchedules.put(i, new ArrayList<>());
        }
        ganttPanel = new GanttPanel();
    }

    @Override
    public void onLogMessage(String message) {}

    @Override
    public void onCpuExecution(int cpuId, int processId, int tick) {
        List<ExecutionBlock> blocks = cpuSchedules.get(cpuId);
        if (!blocks.isEmpty() && blocks.get(blocks.size() - 1).processId == processId
                && blocks.get(blocks.size() - 1).endTick == tick - 1) {
            blocks.get(blocks.size() - 1).endTick = tick;
        } else {
            blocks.add(new ExecutionBlock(processId, tick, tick));
        }
        if (tick > maxTick) maxTick = tick;
    }

    // Recording Hard Disk execution
    @Override
    public void onDiskExecution(int processId, int tick) {
        if (!diskSchedule.isEmpty() && diskSchedule.get(diskSchedule.size() - 1).processId == processId
                && diskSchedule.get(diskSchedule.size() - 1).endTick == tick - 1) {
            diskSchedule.get(diskSchedule.size() - 1).endTick = tick;
        } else {
            diskSchedule.add(new ExecutionBlock(processId, tick, tick));
        }
        if (tick > maxTick) maxTick = tick;
    }

    public void showChart() {
        JScrollPane scrollPane = new JScrollPane(ganttPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        add(scrollPane);
        setVisible(true);
    }

    private static class ExecutionBlock {
        int processId;
        int startTick;
        int endTick;

        public ExecutionBlock(int processId, int startTick, int endTick) {
            this.processId = processId;
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }

    private class GanttPanel extends JPanel {
        private final Color[] processColors = {
                new Color(220, 53, 69), new Color(40, 167, 69), new Color(0, 123, 255),
                new Color(253, 126, 20), new Color(111, 66, 193), new Color(23, 162, 184),
                new Color(255, 193, 7), new Color(32, 201, 151)
        };

        private final int TICK_WIDTH = 40;
        private final int ROW_HEIGHT = 60;
        private final int PADDING_X = 120;
        private final int PADDING_Y = 70;

        @Override
        public Dimension getPreferredSize() {
            int totalWidth = PADDING_X + (maxTick + 2) * TICK_WIDTH;
            // Added +1 to size for the DISK row
            int totalHeight = PADDING_Y + (cpuSchedules.size() + 1) * (ROW_HEIGHT + 30) + 50;
            return new Dimension(totalWidth, totalHeight);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 1. TIMELINE AXIS
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            int timelineY = PADDING_Y - 20;

            for (int t = 0; t <= maxTick; t++) {
                int xPos = PADDING_X + t * TICK_WIDTH;
                g2d.setColor(new Color(235, 235, 235));
                g2d.drawLine(xPos, timelineY, xPos, getHeight() - 20);
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawLine(xPos, timelineY - 5, xPos, timelineY + 5);
                g2d.drawString(String.valueOf(t), xPos - 5, timelineY - 10);
            }

            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(PADDING_X, timelineY, PADDING_X + (maxTick + 1) * TICK_WIDTH, timelineY);
            g2d.setStroke(new BasicStroke(1));

            // 2. DRAWING THE CPUS
            for (Map.Entry<Integer, List<ExecutionBlock>> entry : cpuSchedules.entrySet()) {
                int cpuId = entry.getKey();
                int yPos = PADDING_Y + cpuId * (ROW_HEIGHT + 30);

                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.setColor(Color.BLACK);
                g2d.drawString("CPU " + cpuId, 15, yPos + ROW_HEIGHT / 2 + 5);

                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawLine(PADDING_X, yPos + ROW_HEIGHT / 2, getWidth(), yPos + ROW_HEIGHT / 2);

                drawBlocks(g2d, entry.getValue(), yPos);
            }

            // 3. DRAWING THE HARD DISK (NEW)
            int diskYPos = PADDING_Y + cpuSchedules.size() * (ROW_HEIGHT + 30);

            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.setColor(new Color(139, 69, 19)); // Brown color for Disk label
            g2d.drawString("HARD DISK", 10, diskYPos + ROW_HEIGHT / 2 + 5);

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawLine(PADDING_X, diskYPos + ROW_HEIGHT / 2, getWidth(), diskYPos + ROW_HEIGHT / 2);

            // Drawing the disk blocks
            for (ExecutionBlock block : diskSchedule) {
                int rectX = PADDING_X + block.startTick * TICK_WIDTH;
                int rectWidth = (block.endTick - block.startTick + 1) * TICK_WIDTH;

                g2d.setColor(new Color(200, 150, 50)); // Bronze/Orange color for Disk Transfer
                g2d.fillRoundRect(rectX, diskYPos, rectWidth, ROW_HEIGHT, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(rectX, diskYPos, rectWidth, ROW_HEIGHT, 15, 15);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 22));
                String label = "P" + block.processId;
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(label);
                g2d.drawString(label, rectX + (rectWidth - textWidth) / 2, diskYPos + ROW_HEIGHT / 2 + 8);
            }
        }

        // Helper method to avoid code repetition when drawing CPU blocks
        private void drawBlocks(Graphics2D g2d, List<ExecutionBlock> blocks, int yPos) {
            for (ExecutionBlock block : blocks) {
                int rectX = PADDING_X + block.startTick * TICK_WIDTH;
                int rectWidth = (block.endTick - block.startTick + 1) * TICK_WIDTH;

                if (block.processId == 0) {
                    g2d.setColor(new Color(80, 80, 80));
                } else {
                    g2d.setColor(processColors[block.processId % processColors.length]);
                }

                g2d.fillRoundRect(rectX, yPos, rectWidth, ROW_HEIGHT, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(rectX, yPos, rectWidth, ROW_HEIGHT, 15, 15);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 22));
                String label = (block.processId == 0) ? "SYS" : "P" + block.processId;
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(label);
                g2d.drawString(label, rectX + (rectWidth - textWidth) / 2, yPos + ROW_HEIGHT / 2 + 8);
            }
        }
    }
}