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

        // Draw Edges with Curvature
        // Map to count edges between node pairs to determine curve amount
        Map<String, Integer> pairCount = new HashMap<>();

        for (Edge edge : edges) {
            PointF start = nodePositions.get(edge.fromId);
            PointF end = nodePositions.get(edge.toId);
            if (start != null && end != null) {
                // Determine if there is a reverse edge or multiple edges
                String key = getPairKey(edge.fromId, edge.toId);
                // Simple logic: always curve right relative to direction
                // If A->B, curve "right" looks convex
                // If B->A, curve "right" (which is left from A's perspective) looks distinct
                drawCurvedArrow(canvas, start.x, start.y, end.x, end.y, nodeRadius, edge.amount);
            }
        }

        // Draw Nodes (Overlay on top of edges)
        for (Node node : nodes) {
            PointF pos = nodePositions.get(node.id);
            nodePaint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawCircle(pos.x, pos.y, nodeRadius, nodePaint);

            String label = node.name.length() > 0 ? node.name.substring(0, 1) : "?";
            canvas.drawText(label, pos.x, pos.y + 12, textPaint);

            Paint namePaint = new Paint(textPaint);
            namePaint.setColor(Color.BLACK);
            namePaint.setTextSize(24f);
            canvas.drawText(node.name, pos.x, pos.y + nodeRadius + 30, namePaint);
        }
    }

    private String getPairKey(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void drawCurvedArrow(Canvas canvas, float x1, float y1, float x2, float y2, int nodeRadius, String label) {
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        float dist = (float) Math.hypot(x2 - x1, y2 - y1);

        // Offset anchors to avoid head-tail overlap on bidirectional edges
        // Shift start and end "right" relative to direction
        float angleOffset = (float) (Math.PI / 8.0); // 22.5 degrees

        // Adjusted start point on node circle
        float startX = x1 + (float) (Math.cos(angle + angleOffset) * nodeRadius);
        float startY = y1 + (float) (Math.sin(angle + angleOffset) * nodeRadius);

        // Adjusted end point on node circle (incoming side)
        // angle + PI is the "back" direction. Subtract offset to shift "Right" relative
        // to incoming vector?
        // Let's trace: angle=0 (Right). angle+PI=180 (Left).
        // We want landing to be "Down" (Right relative to A->B).
        // Down corresponds to angle > 180 (e.g. 190).
        // So angle + PI + offset?
        // Wait, previously I reasoned:
        // A->B curves Right (Down).
        // We want end point to be on Down side of B.
        // B center (10,0). Points on B: (10 + R cos(phi), 0 + R sin(phi)).
        // Down side has sin(phi) > 0.
        // phi roughly 180.
        // sin(180+x) = -sin(x) (Negative -> Up).
        // sin(180-x) = sin(x) (Positive -> Down).
        // So we want phi = PI - offset.
        // So angle + PI - angleOffset.
        float endX = x2 + (float) (Math.cos(angle + Math.PI - angleOffset) * nodeRadius);
        float endY = y2 + (float) (Math.sin(angle + Math.PI - angleOffset) * nodeRadius);

        // Control point for quadratic bezier
        // Offset perpendicular to the line
        // Midpoint
        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2;

        // Perpendicular vector (-dy, dx) normalized * offset
        // Curve amount
        float curveOffset = 60f; // Amount of curve

        float dx = endX - startX;
        float dy = endY - startY;
        float norm = (float) Math.sqrt(dx * dx + dy * dy);

        float perpX = -dy / norm * curveOffset;
        float perpY = dx / norm * curveOffset;

        float controlX = midX + perpX;
        float controlY = midY + perpY;

        Path path = new Path();
        path.moveTo(startX, startY);
        path.quadTo(controlX, controlY, endX, endY);
        canvas.drawPath(path, edgePaint);

        // Arrowhead at the end, oriented along the tangent of the curve at t=1
        // Derivative of Quad Bezier: B'(t) = 2(1-t)(P1-P0) + 2t(P2-P1)
        // At t=1: B'(1) = 2(P2-P1) where P2 is end, P1 is control
        // Vector is P2 - P1 = (endX - controlX, endY - controlY)
        float tangentAngle = (float) Math.atan2(endY - controlY, endX - controlX);

        float arrowSize = 25f;
        Path arrowPath = new Path();
        arrowPath.moveTo(endX, endY);
        arrowPath.lineTo(endX - arrowSize * (float) Math.cos(tangentAngle - Math.PI / 6),
                endY - arrowSize * (float) Math.sin(tangentAngle - Math.PI / 6));
        arrowPath.lineTo(endX - arrowSize * (float) Math.cos(tangentAngle + Math.PI / 6),
                endY - arrowSize * (float) Math.sin(tangentAngle + Math.PI / 6));
        arrowPath.close();
        canvas.drawPath(arrowPath, arrowPaint);

        // Label on the curve (approx at t=0.5)
        float curveMidX = 0.25f * startX + 0.5f * controlX + 0.25f * endX;
        float curveMidY = 0.25f * startY + 0.5f * controlY + 0.25f * endY;

        Paint labelPaint = new Paint(textPaint);
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(28f);
        labelPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // Measure text for background
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float textW = labelPaint.measureText(label);
        float textH = metrics.descent - metrics.ascent;

        float padding = 10f;
        float halfW = textW / 2 + padding;
        float halfH = textH / 2 + padding;

        android.graphics.RectF bgRect = new android.graphics.RectF(
                curveMidX - halfW,
                curveMidY - halfH,
                curveMidX + halfW,
                curveMidY + halfH);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        // Add a subtle border to the background pill
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.LTGRAY);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        canvas.drawRoundRect(bgRect, 10, 10, bgPaint);
        canvas.drawRoundRect(bgRect, 10, 10, borderPaint);

        // Vertically center text
        float yOffset = (metrics.descent + metrics.ascent) / 2;
        canvas.drawText(label, curveMidX, curveMidY - yOffset, labelPaint);
    }
}
