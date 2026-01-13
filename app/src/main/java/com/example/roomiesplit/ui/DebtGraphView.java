package com.example.roomiesplit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebtGraphView extends View {

    private Paint nodePaint;
    private Paint textPaint;
    private Paint edgePaint;
    private Paint arrowPaint;

    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private Map<String, PointF> nodePositions = new HashMap<>();

    public static class Node {
        String id;
        String name;

        public Node(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class Edge {
        String fromId;
        String toId;
        String amount;

        public Edge(String fromId, String toId, String amount) {
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
        }
    }

    public DebtGraphView(Context context) {
        super(context);
        init();
    }

    public DebtGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(Color.parseColor("#4CAF50")); // Green nodes
        nodePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setColor(Color.GRAY);
        edgePaint.setStrokeWidth(5f);
        edgePaint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.GRAY);
        arrowPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (nodes.isEmpty())
            return;

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;
        int nodeRadius = 50;

        // Calculate Node Positions (Circular Layout)
        double angleStep = 2 * Math.PI / nodes.size();
        for (int i = 0; i < nodes.size(); i++) {
            double angle = i * angleStep - Math.PI / 2; // Start from top
            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));
            nodePositions.put(nodes.get(i).id, new PointF(x, y));
        }

        // Draw Edges
        for (Edge edge : edges) {
            PointF start = nodePositions.get(edge.fromId);
            PointF end = nodePositions.get(edge.toId);
            if (start != null && end != null) {
                drawArrow(canvas, start.x, start.y, end.x, end.y, nodeRadius, edge.amount);
            }
        }

        // Draw Nodes
        for (Node node : nodes) {
            PointF pos = nodePositions.get(node.id);
            nodePaint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawCircle(pos.x, pos.y, nodeRadius, nodePaint);

            // Draw Name (truncated to first char for simplicity in circle)
            String label = node.name.length() > 0 ? node.name.substring(0, 1) : "?";
            canvas.drawText(label, pos.x, pos.y + 12, textPaint);

            // Draw full name below
            Paint namePaint = new Paint(textPaint);
            namePaint.setColor(Color.BLACK);
            namePaint.setTextSize(24f);
            canvas.drawText(node.name, pos.x, pos.y + nodeRadius + 30, namePaint);
        }
    }

    private void drawArrow(Canvas canvas, float x1, float y1, float x2, float y2, int nodeRadius, String label) {
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        float dist = (float) Math.hypot(x2 - x1, y2 - y1);

        // Adjust start and end to be outside the node circle
        float startX = x1 + (float) (Math.cos(angle) * nodeRadius);
        float startY = y1 + (float) (Math.sin(angle) * nodeRadius);
        float endX = x2 - (float) (Math.cos(angle) * nodeRadius);
        float endY = y2 - (float) (Math.sin(angle) * nodeRadius);

        canvas.drawLine(startX, startY, endX, endY, edgePaint);

        // Arrowhead
        float arrowSize = 25f;
        Path path = new Path();
        path.moveTo(endX, endY);
        path.lineTo(endX - arrowSize * (float) Math.cos(angle - Math.PI / 6),
                endY - arrowSize * (float) Math.sin(angle - Math.PI / 6));
        path.lineTo(endX - arrowSize * (float) Math.cos(angle + Math.PI / 6),
                endY - arrowSize * (float) Math.sin(angle + Math.PI / 6));
        path.close();
        canvas.drawPath(path, arrowPaint);

        // Label (Amount)
        Paint labelPaint = new Paint(textPaint);
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(24f);

        // Midpoint with offset
        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2;
        canvas.drawText(label, midX, midY - 10, labelPaint);
    }
}
