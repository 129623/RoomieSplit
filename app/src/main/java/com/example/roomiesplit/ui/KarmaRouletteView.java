package com.example.roomiesplit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KarmaRouletteView extends View {

    private Paint paint;
    private Paint textPaint;
    private RectF rectF;
    private List<RouletteEntry> entries = new ArrayList<>();
    private float totalWeight = 0;
    private int[] colors = {
            Color.parseColor("#FF5252"), // Red
            Color.parseColor("#448AFF"), // Blue
            Color.parseColor("#69F0AE"), // Green
            Color.parseColor("#FFD740"), // Yellow
            Color.parseColor("#E040FB") // Purple
    };

    public static class RouletteEntry {
        String name;
        float weight;

        public RouletteEntry(String name, float weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    public KarmaRouletteView(Context context) {
        super(context);
        init();
    }

    public KarmaRouletteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
    }

    public void setEntries(List<RouletteEntry> entries) {
        this.entries = entries;
        calculateTotalWeight();
        invalidate();
    }

    private void calculateTotalWeight() {
        totalWeight = 0;
        for (RouletteEntry entry : entries) {
            totalWeight += entry.weight;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (entries.isEmpty())
            return;

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        int padding = 50;

        float left = (width - size) / 2f + padding;
        float top = (height - size) / 2f + padding;
        float right = left + size - 2 * padding;
        float bottom = top + size - 2 * padding;

        rectF.set(left, top, right, bottom);

        float startAngle = 0;
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = (size - 2 * padding) / 2f;

        for (int i = 0; i < entries.size(); i++) {
            RouletteEntry entry = entries.get(i);
            float sweepAngle = (entry.weight / totalWeight) * 360f;

            paint.setColor(colors[i % colors.length]);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            // Draw Text
            float textAngle = startAngle + sweepAngle / 2;
            double rad = Math.toRadians(textAngle);
            float labelRadius = radius * 0.6f;
            float x = centerX + (float) (labelRadius * Math.cos(rad));
            float y = centerY + (float) (labelRadius * Math.sin(rad));

            canvas.drawText(entry.name, x, y + 15, textPaint); // +15 for approx vertical alignment

            startAngle += sweepAngle;
        }
    }

    // Simple helper to simulate "spinning" by just randomly picking one based on
    // weights
    public RouletteEntry spin() {
        if (entries.isEmpty())
            return null;

        float random = new Random().nextFloat() * totalWeight;
        float current = 0;
        for (RouletteEntry entry : entries) {
            current += entry.weight;
            if (random <= current) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }
}
