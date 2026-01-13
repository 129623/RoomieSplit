package com.example.roomiesplit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;
import java.util.ArrayList;

public class WeekBarChartView extends View {

    private List<Double> dataPoints = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;

    public WeekBarChartView(Context context) {
        this(context, null);
    }

    public WeekBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0xFF4CAF50); // Green
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.LTGRAY);
        axisPaint.setStrokeWidth(2f);
    }

    public void setData(List<Double> data, List<String> dayLabels) {
        this.dataPoints = data;
        this.labels = dayLabels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints == null || dataPoints.isEmpty())
            return;

        float width = getWidth();
        float height = getHeight();
        float padding = 40f;
        float bottomPadding = 60f; // For labels
        float chartHeight = height - bottomPadding - padding;

        // Find max value
        double max = 0;
        for (Double d : dataPoints) {
            if (d > max)
                max = d;
        }
        if (max == 0)
            max = 1; // Avoid divide by zero

        float barWidth = (width - 2 * padding) / dataPoints.size() * 0.6f;
        float spacing = (width - 2 * padding) / dataPoints.size();

        // Draw Axis Line
        canvas.drawLine(padding, height - bottomPadding, width - padding, height - bottomPadding, axisPaint);

        for (int i = 0; i < dataPoints.size(); i++) {
            double val = dataPoints.get(i);
            float barH = (float) ((val / max) * chartHeight);

            float left = padding + (i * spacing) + (spacing - barWidth) / 2;
            float top = (height - bottomPadding) - barH;
            float right = left + barWidth;
            float bottom = height - bottomPadding;

            // Draw Bar
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRect(rect, barPaint);

            // Draw Label
            if (labels != null && i < labels.size()) {
                canvas.drawText(labels.get(i), left + barWidth / 2, height - 15, textPaint);
            }

            // Draw Value on Top
            if (val > 0) {
                canvas.drawText(String.valueOf((int) val), left + barWidth / 2, top - 10, textPaint);
            }
        }
    }
}
