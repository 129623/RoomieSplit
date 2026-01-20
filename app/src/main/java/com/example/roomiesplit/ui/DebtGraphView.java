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
        android.graphics.Bitmap avatarBitmap;

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
        // Use context colors for Dark Mode support
        int primaryColor = androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.primary);
        int textColor = androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.app_text_primary);
        int surfaceColor = androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.app_surface);
        int borderColor = androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.app_text_secondary);

        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(primaryColor);
        nodePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE); // Text inside Green Node stays White
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setColor(borderColor);
        edgePaint.setStrokeWidth(5f);
        edgePaint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(borderColor);
        arrowPaint.setStyle(Paint.Style.FILL);
    }

    public void updateNodeAvatar(String nodeId, android.graphics.Bitmap bitmap) {
        for (Node n : nodes) {
            if (n.id.equals(nodeId)) {
                n.avatarBitmap = bitmap;
                invalidate();
                return;
            }
        }
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
        int nodeRadius = 60; // Increased radius for Avatar

        // Layout calc ...
        // (Assuming layout logic stays mostly same, just updating draw loop)
        double angleStep = 2 * Math.PI / nodes.size();
        for (int i = 0; i < nodes.size(); i++) {
            double angle = i * angleStep - Math.PI / 2;
            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));
            nodePositions.put(nodes.get(i).id, new PointF(x, y));
        }

        // Draw Edges first
        // ... (Keep edge drawing logic) ...
        Map<String, Integer> pairCount = new HashMap<>();
        for (Edge edge : edges) {
            PointF start = nodePositions.get(edge.fromId);
            PointF end = nodePositions.get(edge.toId);
            if (start != null && end != null) {
                drawCurvedArrow(canvas, start.x, start.y, end.x, end.y, nodeRadius, edge.amount);
            }
        }

        // Draw Nodes
        for (Node node : nodes) {
            PointF pos = nodePositions.get(node.id);

            if (node.avatarBitmap != null) {
                // Draw Avatar Circle
                // Create a circular bitmap shader or clip
                canvas.save();
                Path circlePath = new Path();
                circlePath.addCircle(pos.x, pos.y, nodeRadius, Path.Direction.CW);
                canvas.clipPath(circlePath);

                android.graphics.Rect src = new android.graphics.Rect(0, 0, node.avatarBitmap.getWidth(),
                        node.avatarBitmap.getHeight());
                android.graphics.RectF dst = new android.graphics.RectF(pos.x - nodeRadius, pos.y - nodeRadius,
                        pos.x + nodeRadius, pos.y + nodeRadius);
                canvas.drawBitmap(node.avatarBitmap, src, dst, null);
                canvas.restore();

                // Optional: Draw border
                Paint borderP = new Paint(Paint.ANTI_ALIAS_FLAG);
                borderP.setStyle(Paint.Style.STROKE);
                borderP.setStrokeWidth(4f);
                borderP.setColor(nodePaint.getColor());
                canvas.drawCircle(pos.x, pos.y, nodeRadius, borderP);
            } else {
                // Default Green Circle
                nodePaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(),
                        com.example.roomiesplit.R.color.primary));
                canvas.drawCircle(pos.x, pos.y, nodeRadius, nodePaint);

                String label = node.name.length() > 0 ? node.name.substring(0, 1) : "?";
                canvas.drawText(label, pos.x, pos.y + 12, textPaint);
            }

            // Name Label below node
            Paint namePaint = new Paint(textPaint);
            namePaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(),
                    com.example.roomiesplit.R.color.app_text_primary));
            namePaint.setTextSize(28f); // Larger text
            canvas.drawText(node.name, pos.x, pos.y + nodeRadius + 40, namePaint);
        }
    }

    private String getPairKey(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void drawCurvedArrow(Canvas canvas, float x1, float y1, float x2, float y2, int nodeRadius, String label) {
        // ... (Keep math) ...
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        float angleOffset = (float) (Math.PI / 8.0);
        float startX = x1 + (float) (Math.cos(angle + angleOffset) * nodeRadius);
        float startY = y1 + (float) (Math.sin(angle + angleOffset) * nodeRadius);
        float endX = x2 + (float) (Math.cos(angle + Math.PI - angleOffset) * nodeRadius);
        float endY = y2 + (float) (Math.sin(angle + Math.PI - angleOffset) * nodeRadius);

        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2;
        float dx = endX - startX;
        float dy = endY - startY;
        float norm = (float) Math.sqrt(dx * dx + dy * dy);
        float curveOffset = 60f;
        float perpX = -dy / norm * curveOffset;
        float perpY = dx / norm * curveOffset;
        float controlX = midX + perpX;
        float controlY = midY + perpY;

        Path path = new Path();
        path.moveTo(startX, startY);
        path.quadTo(controlX, controlY, endX, endY);
        canvas.drawPath(path, edgePaint);

        // Arrowhead
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

        // Label Pill
        float curveMidX = 0.25f * startX + 0.5f * controlX + 0.25f * endX;
        float curveMidY = 0.25f * startY + 0.5f * controlY + 0.25f * endY;

        Paint labelPaint = new Paint(textPaint);
        labelPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.app_text_primary));
        labelPaint.setTextSize(28f);
        labelPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float textW = labelPaint.measureText(label);
        float textH = metrics.descent - metrics.ascent;
        float padding = 12f;
        float halfW = textW / 2 + padding;
        float halfH = textH / 2 + padding;

        android.graphics.RectF bgRect = new android.graphics.RectF(curveMidX - halfW, curveMidY - halfH,
                curveMidX + halfW, curveMidY + halfH);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.app_surface));
        bgPaint.setStyle(Paint.Style.FILL);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(),
                com.example.roomiesplit.R.color.app_text_secondary));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        canvas.drawRoundRect(bgRect, 16, 16, bgPaint);
        canvas.drawRoundRect(bgRect, 16, 16, borderPaint);

        float yOffset = (metrics.descent + metrics.ascent) / 2;
        canvas.drawText(label, curveMidX, curveMidY - yOffset, labelPaint);
    }
}
