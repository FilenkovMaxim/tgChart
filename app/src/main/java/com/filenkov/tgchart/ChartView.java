package com.filenkov.tgchart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Chart view.
 * Contains two parts: main chart and compact bar for scrolling and zooming.
 */
public class ChartView extends View {
  private static final int INVISIBLE = 0;
  private static final int VISIBLE = 255;
  private static final String NO_DATA_MESSAGE = "Please, select graph to display";
  /**
   * Count of level lines.
   */
  private static final int LEVELS_COUNT = 5;
  private SimpleDateFormat valueboxDateFormat = new SimpleDateFormat("ccc, MMM d", Locale.getDefault());
  private SimpleDateFormat timelineDateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
  private int backgroundColor;
  private int textColor;
  private final float density;
  /**
   * View padding from both sides.
   */
  private float paddingX;

  private float chartHeight;
  private float currentScaleY;
  private float newScaleY;
  private float scaleStep;
  private float chartLineWeight;
  private int chartMax;
  private int oldChartMax;
  private int chartCurrentIndex;
  private float chartXstep;
  private float valueRadius;
  private String[] dates;

  /**
   * All height of bar space (with frame).
   */
  private float barHeight;
  /**
   * Height of space for graph (without frame).
   */
  private float barGraphHeight;
  private float barLineWeight;
  private int barMax;
  private float barXstep;

  private int selectionStart = 0;
  private int selectionEnd = 1;
  private float selectionStartX = 0;
  private float selectionEndX = 0;
  private float selectionMinWidth = 0;
  private float selectionBorderHeight;
  private float selectionBorderWidth;
  private float selectionBorderSensitivity;
  private float checkBoxSize;
  private float checkboxSensitivity;

  int[] graphsAlpha;
  float[] valuesWidthes;
  float[] namesWidthes;

  private RectF chartBounds;
  private RectF barBounds;

  private ChartData chart;
  private boolean[] displayedGraphs;
  private Path[] chartPathes;
  private Path zeroLinePath;
  private Path timelinePath;
  private Path[] barPathes;
  private Path selectionPath;
  private Path nonSelectionPath;

  private Paint[] chartPaints;
  private Paint valuePaint;
  private Paint valueBoxPaint;
  private Paint valueBoxBorderPaint;
  private Paint valueCirclePaint;
  private Paint valueLinePaint;
  private Paint namesPaint;
  private Paint zeroLinePaint;
  private Paint levelsPaint;
  private Paint timesPaint;
  private Paint backgroundPaint;
  private Paint[] barPaints;
  /**
   * Frame on bar region showed current selection.
   */
  private Paint selectionPaint;
  private Paint nonSelectionPaint;
  private Paint checkboxPaint;
  private float timelineY;
  private int optimalDatesCount;
  /**
   * Checkboxes left top corner Y.
   */
  private float[] checkboxesY;

  private boolean isNightMode = false;
  /**
   * Indicate if user is touching chart region now.
   */
  private boolean chartTouchMode = false;
  /**
   * Indicate user drags selection left edge to change size of selection.
   */
  private boolean selectionChangeLeftMode = false;
  /**
   * Indicate user drags selection right edge to change size of selection.
   */
  private boolean selectionChangeRightMode = false;
  /**
   * Indicate user drags selection to change position of selection.
   */
  private boolean selectionMoveMode = false;
  /**
   * Touch X on selection move mode started.
   */
  private float selectionMoveX;
  /**
   * Indicate if user is touching checkbox.
   */
  private boolean checkBoxTouchMode = false;
  private int checkBoxTouchedIndex = -1;

  public ChartView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    density = context.getResources().getDisplayMetrics().density;
    init();
  }

  private void init() {
    paddingX = density * 16;
    chartHeight = density * 275.33f; // 826px
    chartLineWeight = density * 2f; // 6px
    valueRadius = density * 5; // 15px

    barHeight = density * 38f; // 114px
    barLineWeight = density * 0.66f; // 2px

    selectionBorderHeight = density * 1f; // 3px
    selectionBorderWidth = density * 4f; // 12px
    selectionBorderSensitivity = density * 20f; // 12px

    checkBoxSize = density * 18;
    checkboxSensitivity = density * 12;

    barGraphHeight = barHeight - 4 * selectionBorderHeight;

    zeroLinePath = new Path();
    zeroLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    zeroLinePaint.setStyle(Paint.Style.STROKE);
    zeroLinePaint.setStrokeWidth(density * 0.66f); // 2px

    levelsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    levelsPaint.setStyle(Paint.Style.STROKE);
    levelsPaint.setStrokeWidth(density * 0.66f); // 2px

    timesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    timesPaint.setStyle(Paint.Style.STROKE);
    timesPaint.setStrokeWidth(density * 0.66f); // 2px
    timesPaint.setTextSize(density * 8);
    timesPaint.setColor(0xffb2bbc1);

    selectionPath = new Path();
    selectionPaint = new Paint();
    selectionPaint.setStyle(Paint.Style.FILL_AND_STROKE);

    nonSelectionPath = new Path();
    nonSelectionPaint = new Paint();
    nonSelectionPaint.setStyle(Paint.Style.FILL_AND_STROKE);

    checkboxPaint = new Paint();
    checkboxPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    checkboxPaint.setStrokeWidth(density * 2);

    valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    valueBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    valueBoxPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    valueBoxPaint.setShadowLayer(10, 32, 2, 0xcc000000);
    valueBoxBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    valueBoxBorderPaint.setStyle(Paint.Style.STROKE);
    valueBoxBorderPaint.setStrokeWidth(density * 1);
    valueBoxBorderPaint.setColor(0xffe5ebef);
    valueLinePaint = new Paint();
    valueLinePaint.setStyle(Paint.Style.STROKE);
    valueLinePaint.setStrokeWidth(density * 2);
    valueCirclePaint = new Paint();
    valueCirclePaint.setStyle(Paint.Style.FILL);

    namesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    namesPaint.setTextSize(density * 12);
    namesPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

    backgroundPaint = new Paint();
    backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
  }

  /**
   * Apply "Day Mode".
   *
   * @param invalidate true means redraw is needed.
   */
  public void setDayMode(final boolean invalidate) {
    isNightMode = false;
    backgroundColor = 0xffffffff;
    textColor = 0xff000000;
    zeroLinePaint.setColor(0xffe7e8e9);
    levelsPaint.setColor(0xfff1f1f2);
    selectionPaint.setColor(0xccdbe7f0);
    nonSelectionPaint.setColor(0x99dbe7f0);
    valueBoxPaint.setColor(0xffffffff);
    valueLinePaint.setColor(0xffe5ebef);
    valueCirclePaint.setColor(0xffffffff);
    namesPaint.setColor(textColor);
    timesPaint.setColor(0xffb2bbc1);
    backgroundPaint.setColor(backgroundColor);
    if (invalidate) {
      invalidate();
    }
  }

  /**
   * Apply "Night Mode".
   *
   * @param invalidate true means redraw is needed.
   */
  public void setNightMode(final boolean invalidate) {
    isNightMode = true;
    backgroundColor = 0xff1d2733;
    textColor = 0xffffffff;
    zeroLinePaint.setColor(0xff0b131e);
    levelsPaint.setColor(0xff404853);
    selectionPaint.setColor(0xcc2b4256);
    nonSelectionPaint.setColor(0x9919232e);
    valueBoxPaint.setColor(0xff202b38);
    valueLinePaint.setColor(0xff131c26);
    valueCirclePaint.setColor(0xff1d2733);
    namesPaint.setColor(textColor);
    timesPaint.setColor(0xff8698b0);
    backgroundPaint.setColor(backgroundColor);
    if (invalidate) {
      invalidate();
    }
  }

  /**
   * Set chart graphs.
   *
   * @param data ChartData object.
   */
  public void setChartData(ChartData data) {
    chart = data;
    selectionStart = chart.valuesCount - 31;
    selectionEnd = chart.valuesCount - 1;
    displayedGraphs = new boolean[chart.graphsCount];
    graphsAlpha = new int[chart.graphsCount];
    for (int i = 0; i < chart.graphsCount; i++) {
      displayedGraphs[i] = true;
      graphsAlpha[i] = VISIBLE;
    }
    // read dates
    dates = new String[chart.valuesCount];
    for (int j = 0; j < chart.valuesCount; j++) {
      dates[j] = timelineDateFormat.format(new Date(chart.timeline[j]));
    }
    onGraphsDisplayedChanged();
  }

  /**
   * Recalculates global maximum and minimum.
   */
  private void onGraphsDisplayedChanged() {
    barMax = Integer.MIN_VALUE;
    for (int i = 0; i < chart.graphsCount; i++) {
      if (graphsAlpha[i] > INVISIBLE && chart.maximums[i] > barMax) {
        barMax = chart.maximums[i];
      }
    }
  }

  @Override
  protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (w != oldw || h != oldh) {
      chartBounds = new RectF(paddingX, 0, w - paddingX, chartHeight);

      barBounds = new RectF(paddingX, chartBounds.bottom + density * 33,
          w - paddingX, chartBounds.bottom + density * 33 + barHeight);
      barXstep = (barBounds.right - barBounds.left) / (chart.valuesCount - 1);

      selectionMinWidth = Math.max(barXstep * 2.1f, selectionBorderWidth * 2.1f);

      timelineY = chartBounds.bottom + density * 16;
      timelinePath = new Path();
      timelinePath.addRect(0, timelineY - density * 10, paddingX, timelineY + density * 5, Path.Direction.CW);
      timelinePath.addRect(chartBounds.right, timelineY - density * 10, w, timelineY + density * 5, Path.Direction.CW);

      // calculate optimal dates count on timeline
      float dateWidth = timesPaint.measureText(timelineDateFormat.format(new Date(1550793600000L))); // width of Dec 22
      optimalDatesCount = (int) (chartBounds.width() / (dateWidth * 2));

      valuesWidthes = new float[chart.graphsCount];
      namesWidthes = new float[chart.graphsCount];
      namesWidthes[0] = valuePaint.measureText(chart.names[0]);
      checkboxesY = new float[chart.graphsCount];
      checkboxesY[0] = barBounds.bottom + density * 24; // first checkbox position.
      for (int i = 1; i < chart.graphsCount; i++) {
        checkboxesY[i] = checkboxesY[i - 1] + checkBoxSize + density * 32;
        namesWidthes[i] = valuePaint.measureText(chart.names[i]);
      }

      scaleStep = barMax > 0 ? 0.2f * chartHeight / barMax : 1;
      onSelectionChanged();
      currentScaleY = newScaleY;
      prepareDrawStatic();
      prepareDrawDynamic();
    }
  }

  /**
   * Calculate x coordinate on a bar for i-position.
   *
   * @param i position in graph data array.
   * @return x coordinate.
   */
  private float barX(final int i) {
    return barBounds.left + i * barXstep;
  }

  /**
   * Calculate i-position in the graph data array by x coordinate.
   *
   * @param x coordinate.
   * @return position in the graph data array.
   */
  private int barI(final float x) {
    return Math.max(0, Math.min(Math.round((x - barBounds.left) / barXstep), chart.valuesCount - 1));
  }

  /**
   * Calculate y coordinate on a bar for graph value.
   *
   * @param value graph value.
   * @return y coordinate.
   */
  private float barY(final int value) {
    return barBounds.bottom - value * barGraphHeight / barMax;
  }

  /**
   * Perform operations which should be done once in the view life.
   */
  private void prepareDrawStatic() {
    zeroLinePath.reset();
    zeroLinePath.moveTo(chartBounds.left, chartBounds.bottom);
    zeroLinePath.lineTo(chartBounds.right, chartBounds.bottom);

    barPathes = new Path[chart.graphsCount];
    barPaints = new Paint[chart.graphsCount];
    chartPaints = new Paint[chart.graphsCount];
    for (int i = 0; i < chart.graphsCount; i++) {
      barPathes[i] = new Path();
      barPathes[i].moveTo(barX(0), barY(chart.graphs[i][0]));
      for (int j = 0; j < chart.valuesCount; j++) {
        barPathes[i].lineTo(barX(j), barY(chart.graphs[i][j]));
      }

      barPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
      barPaints[i].setColor(chart.colors[i]);
      barPaints[i].setStyle(Paint.Style.STROKE);
      barPaints[i].setStrokeWidth(barLineWeight);

      chartPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
      chartPaints[i].setColor(chart.colors[i]);
      chartPaints[i].setStyle(Paint.Style.STROKE);
      chartPaints[i].setStrokeWidth(chartLineWeight);
      chartPaints[i].setAlpha(graphsAlpha[i]);
    }
  }

  private void onSelectionChanged() {
    oldChartMax = chartMax;
    chartXstep = (chartBounds.right - chartBounds.left) / (selectionEnd - selectionStart);
    selectionStartX = barX(selectionStart);
    selectionEndX = barX(selectionEnd);

    buildSelectionPathes();

    if (selectionStart == 0 && selectionEnd == chart.valuesCount) { // selected all region
      chartMax = barMax;
      newScaleY = chartMax != 0 ? chartHeight / chartMax : currentScaleY;
      return;
    }

    chartMax = Integer.MIN_VALUE;
    for (int i = 0; i < chart.graphsCount; i++) {
      if (displayedGraphs[i]) {
        for (int j = selectionStart; j < selectionEnd; j++) {
          if (chart.graphs[i][j] > chartMax) chartMax = chart.graphs[i][j];
        }
      }
    }
    newScaleY = chartMax != 0 ? chartHeight / chartMax : currentScaleY;
  }

  /**
   * Build pathes for selection frame and out of frame space cover.
   */
  private void buildSelectionPathes() {
    selectionPath.reset();
    selectionPath.addRect(new RectF(selectionStartX, barBounds.top,
        selectionStartX + selectionBorderWidth, barBounds.bottom), Path.Direction.CW);
    selectionPath.addRect(new RectF(selectionStartX + selectionBorderWidth, barBounds.top,
        selectionEndX - selectionBorderWidth, barBounds.top + selectionBorderHeight), Path.Direction.CW);
    selectionPath.addRect(new RectF(selectionEndX - selectionBorderWidth, barBounds.top,
        selectionEndX, barBounds.bottom), Path.Direction.CW);
    selectionPath.addRect(new RectF(selectionStartX + selectionBorderWidth, barBounds.bottom - selectionBorderHeight,
        selectionEndX - selectionBorderWidth, barBounds.bottom), Path.Direction.CW);
    // Path for selection frame out space
    nonSelectionPath.reset();
    nonSelectionPath.addRect(new RectF(barBounds.left, barBounds.top, selectionStartX, barBounds.bottom), Path.Direction.CW);
    nonSelectionPath.addRect(new RectF(selectionEndX, barBounds.top, barBounds.right, barBounds.bottom), Path.Direction.CW);
  }

  /**
   * Calculate x coordinate on a chart for i-position.
   *
   * @param i position in graph data array.
   * @return x coordinate.
   */
  private float chartX(final int i) {
    return chartBounds.left + (i - selectionStart) * chartXstep;
  }

  /**
   * Calculate i-position in the graph data array by x coordinate on chart region.
   *
   * @param x coordinate.
   * @return position in the graph data array.
   */
  private int chartI(final float x) {
    return Math.max(selectionStart, Math.min(Math.round((x - chartBounds.left) / chartXstep) + selectionStart, selectionEnd));
  }

  /**
   * Calculate y coordinate on a chart for graph value.
   *
   * @param value graph value.
   * @return y coordinate.
   */
  private float chartY(final int value) {
    return chartBounds.bottom - value * currentScaleY;
  }

  /**
   * Perform operations which should be done on every change.
   */
  private void prepareDrawDynamic() {
    chartPathes = new Path[chart.graphsCount];
    for (int i = 0; i < chart.graphsCount; i++) {
      chartPathes[i] = new Path();
      chartPathes[i].moveTo(chartX(selectionStart), chartY(chart.graphs[i][selectionStart]));
      for (int j = selectionStart; j <= selectionEnd; j++) {
        chartPathes[i].lineTo(chartX(j), chartY(chart.graphs[i][j]));
      }
    }
  }

  private long lastDrawTimeNs = 0;

  @Override
  protected void onDraw(Canvas canvas) {
    long drawStartTime = System.nanoTime();
    super.onDraw(canvas);
    canvas.drawColor(backgroundColor);
    canvas.drawPath(zeroLinePath, zeroLinePaint);

    int displayedGraphsCount = 0;
    for (int i = 0; i < chart.graphsCount; i++) {
      if (displayedGraphs[i]) {
        displayedGraphsCount++;
      }
    }

    if (displayedGraphsCount > 0) {
      // draw levels
      int alpha = Math.round(VISIBLE - VISIBLE * Math.abs(newScaleY - currentScaleY) / newScaleY);
      int paddingTop = Math.round(density * 19 / currentScaleY);
      drawLevels(canvas, chartMax - paddingTop, alpha);
      if (currentScaleY != newScaleY) {
        drawLevels(canvas, oldChartMax, VISIBLE - alpha);
      }

      for (int i = 0; i < chart.graphsCount; i++) {
        if (graphsAlpha[i] > INVISIBLE) {
          chartPaints[i].setAlpha(graphsAlpha[i]);
          canvas.drawPath(chartPathes[i], chartPaints[i]);
          canvas.drawPath(barPathes[i], barPaints[i]);
        }
        drawLabel(canvas, i);
      }
    } else {
      // no data for out
      float textWidth = namesPaint.measureText(NO_DATA_MESSAGE);
      canvas.drawText(NO_DATA_MESSAGE, chartBounds.centerX() - textWidth / 2, chartBounds.centerY(), namesPaint);
      for (int i = 0; i < chart.graphsCount; i++) {
        drawLabel(canvas, i);
      }
    }

    // draw dates
    drawDates(canvas);

    canvas.drawPath(selectionPath, selectionPaint);
    canvas.drawPath(nonSelectionPath, nonSelectionPaint);

    if (chartTouchMode) {
      drawValueBox(canvas);
    }

    lastDrawTimeNs = System.nanoTime() - drawStartTime;
//    if (lastDrawTimeNs > animationIntervalMs * 1_000_000) {
//      Log.e("ChartView", "onDraw() overrun: " + (float) lastDrawTimeNs / 1_000_000);
//    } else {
//      Log.e("ChartView", "onDraw() finished: " + (float) lastDrawTimeNs / 1_000_000);
//    }
  }

  /**
   * Draw levels and lines.
   *
   * @param canvas canvas.
   * @param max    max value (top line).
   * @param alpha  opacity 0..255.
   */
  private void drawLevels(final Canvas canvas, final int max, final int alpha) {
    levelsPaint.setAlpha(alpha);
    timesPaint.setAlpha(alpha);
    float l;
    float y;
    for (int i = 0; i < LEVELS_COUNT; i++) {
      l = max - (float) (i * max) / LEVELS_COUNT;
      y = chartBounds.bottom - l * currentScaleY;
      canvas.drawLine(chartBounds.left, y, chartBounds.right, y, levelsPaint);
      canvas.drawText(String.valueOf(Math.round(l)), chartBounds.left, y - density * 5, timesPaint);
    }
  }

  /**
   * Draw dates.
   *
   * @param canvas canvas.
   */
  private void drawDates(final Canvas canvas) {
    int selectionSize = selectionEnd - selectionStart;
    int stepPower = (int) (Math.log((double) (selectionSize) / optimalDatesCount) / Math.log(2));
    stepPower = Math.max(1, stepPower);
    int step = (int) Math.pow(2, stepPower);
    int start = Math.max(0, (selectionStart / step) * step - step);
    int end = Math.min((selectionEnd / step) * step + step, (chart.valuesCount / step) * step);

    int alpha = (int) (VISIBLE * (1 - (float) (selectionSize - step * optimalDatesCount) / (step * optimalDatesCount)));
    for (int i = start; i < end; i += step) {
      timesPaint.setAlpha(i % (step * 2) == 0 ? VISIBLE : alpha);
      canvas.drawText(dates[i], chartX(i), timelineY, timesPaint);
    }

    canvas.drawPath(timelinePath, backgroundPaint); // clip outsides
  }

  /**
   * Draw checkbox with graph label (name).
   *
   * @param canvas canvas.
   * @param i      index of graph.
   */
  private void drawLabel(final Canvas canvas, final int i) {
    drawCheckbox(canvas, checkboxesY[i], (float) (VISIBLE - graphsAlpha[i]) / VISIBLE, chart.colors[i]);
    // 2.185f is measured on picture
    canvas.drawText(chart.names[i],
        paddingX + checkBoxSize + density * 22, checkboxesY[i] + checkBoxSize * 0.8f,
        namesPaint);
    if (i < chart.graphsCount - 1) {
      canvas.drawLine(paddingX + checkBoxSize + density * 22,
          checkboxesY[i] + checkBoxSize + density * 16,
          barBounds.right,
          checkboxesY[i] + checkBoxSize + density * 16, zeroLinePaint);
    }
  }

  /**
   * Draw checkbox.
   *
   * @param canvas      canvas.
   * @param y           y-position of top left corner.
   * @param filledWhite % filled of white.
   * @param color       color of checkbox.
   */
  private void drawCheckbox(final Canvas canvas, final float y, final float filledWhite, final int color) {
    checkboxPaint.setColor(color);
    canvas.drawRoundRect(new RectF(paddingX, y,
            paddingX + checkBoxSize, y + checkBoxSize),
        checkBoxSize / 10, checkBoxSize / 10, checkboxPaint);
    checkboxPaint.setColor(0xffffffff);
    canvas.drawLine(paddingX + density * 3, y + density * 9.33f,
        paddingX + density * 7, y + density * 14.33f, checkboxPaint);
    canvas.drawLine(paddingX + density * 7, y + density * 14.33f,
        paddingX + density * 15.33f, y + density * 4.66f, checkboxPaint);

    if (filledWhite > 0) {
      checkboxPaint.setColor(backgroundColor);
      float w = checkBoxSize - filledWhite * (checkBoxSize - density * 2);
      if (2 * w < checkBoxSize) {
        canvas.drawRoundRect(new RectF(paddingX + w, y + w,
                paddingX + checkBoxSize - w, y + checkBoxSize - w),
            (checkBoxSize - 2 * w) / 10, (checkBoxSize - 2 * w) / 10, checkboxPaint);
      }
    }
  }

  /**
   * Draw value box.
   *
   * @param canvas canvas.
   */
  private void drawValueBox(final Canvas canvas) {
    float x = chartX(chartCurrentIndex);
    // need to know width of box
    String date = valueboxDateFormat.format(new Date(chart.timeline[chartCurrentIndex]));
    float p = density * 11.66f; // left padding
    float w = p; // start from left padding
    int valuesCount = 0;
    valuePaint.setTextSize(density * 11);
    for (int i = 0; i < chart.graphsCount; i++) {
      if (displayedGraphs[i]) {
        valuesCount++;
        valuesWidthes[i] = Math.max(0,
            valuePaint.measureText(String.valueOf(chart.graphs[i][chartCurrentIndex])));
        w += valuesWidthes[i];
      }
    }

    if (valuesCount < 1) {
      return;
    }
    // add margins and right padding
    w += (valuesCount - 1) * density * 10 + density * 12.66f;
    w = Math.max(w, valuePaint.measureText(date) + density * 12.66f);

    float h = density * 62;
    canvas.drawLine(x, h, x, chartBounds.bottom, valueLinePaint);

    valuePaint.setTextSize(density * 11);

    for (int i = 0; i < chart.graphsCount; i++) {
      if (graphsAlpha[i] > INVISIBLE) {
        float y = chartY(chart.graphs[i][chartCurrentIndex]);
        canvas.drawCircle(x, y, valueRadius, valueCirclePaint);
        canvas.drawCircle(x, y, valueRadius, chartPaints[i]);
      }
    }

    float left = Math.min(x - paddingX + 6, chartBounds.right + paddingX - w - 6);
    canvas.drawRoundRect(new RectF(left, 0, left + w, h), density * 5, density * 5, valueBoxPaint);
    if (!isNightMode) {
      canvas.drawRoundRect(new RectF(left, 0, left + w, h), density * 5, density * 5, valueBoxBorderPaint);
    }

    left += p;
    valuePaint.setTextSize(density * 9);
    valuePaint.setColor(textColor);
    canvas.drawText(date, left, density * 17, valuePaint);

    for (int i = 0; i < chart.graphsCount; i++) {
      if (displayedGraphs[i]) {
        valuePaint.setColor(chart.colors[i]);
        valuePaint.setTextSize(density * 11);
        canvas.drawText(String.valueOf(chart.graphs[i][chartCurrentIndex]), left, density * 41, valuePaint);
        valuePaint.setTextSize(density * 8);
        canvas.drawText(chart.names[i], left, density * 54, valuePaint);
        left += valuesWidthes[i] + density * 10;
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(final MotionEvent event) {
    float touchEventX = event.getX();
    float touchEventY = event.getY();
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (touchEventY >= chartBounds.top && touchEventY <= chartBounds.bottom) {
          // this is touch on chart region
          chartTouchMode = true;
          chartCurrentIndex = chartI(touchEventX);
          invalidate();
          return true;

        } else if (touchEventY >= barBounds.top && touchEventY <= barBounds.bottom) {
          // this is touch on bar region

          if (touchEventX > selectionStartX + selectionBorderSensitivity
              && touchEventX < selectionEndX - selectionBorderSensitivity) {
            selectionMoveX = touchEventX;
            selectionMoveMode = true;

          } else if (touchEventX >= selectionStartX - selectionBorderSensitivity
              && touchEventX <= selectionStartX + selectionBorderSensitivity) {
            selectionChangeLeftMode = true;

          } else if (touchEventX >= selectionEndX - selectionBorderSensitivity
              && touchEventX <= selectionEndX + selectionBorderSensitivity) {
            selectionChangeRightMode = true;

          }
          return true;

        } else {
          // checkbox click?
          for (int i = 0; i < chart.graphsCount; i++) {
            if (touchEventY >= checkboxesY[i] - checkboxSensitivity
                && touchEventY <= checkboxesY[i] + checkBoxSize + checkboxSensitivity
                && touchEventX >= paddingX - checkboxSensitivity
                && touchEventX <= paddingX + checkBoxSize + checkboxSensitivity
            ) {
              checkBoxTouchedIndex = i;
              checkBoxTouchMode = true;
              return true;
            }
          }
          return super.onTouchEvent(event);
        }

      case MotionEvent.ACTION_MOVE:
        if (chartTouchMode) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          chartCurrentIndex = chartI(touchEventX);
          invalidate();

        } else if (selectionMoveMode) {
          float delta = touchEventX - selectionMoveX;
          if (Math.abs(delta) < barXstep) {
            return true;
          }

          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          if (selectionStartX + delta > barBounds.left && selectionEndX + delta < barBounds.right) {
            selectionStart = barI(selectionStartX + delta);
            selectionEnd = barI(selectionEndX + delta);
            applyNewValuesWithAnimation();
          }
          selectionMoveX = touchEventX;

        } else if (selectionChangeLeftMode && touchEventX < selectionEndX - selectionMinWidth) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          selectionStart = barI(touchEventX);
          applyNewValuesWithAnimation();

        } else if (selectionChangeRightMode && touchEventX > selectionStartX + selectionMinWidth) {
          if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
          }
          selectionEnd = barI(touchEventX);
          applyNewValuesWithAnimation();
        }

        return true;

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        chartTouchMode = false;
        selectionChangeLeftMode = false;
        selectionChangeRightMode = false;
        selectionMoveMode = false;
        if (checkBoxTouchMode && event.getAction() == MotionEvent.ACTION_UP) {
          displayedGraphs[checkBoxTouchedIndex] = !displayedGraphs[checkBoxTouchedIndex];
        }
        checkBoxTouchMode = false;
        applyNewValuesWithAnimation();
        return super.onTouchEvent(event);
      default:
        return super.onTouchEvent(event);
    }
  }

  private void applyNewValuesWithAnimation() {
    onSelectionChanged();
    prepareDrawDynamic();
    invalidate();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(alphaChangeRunnable, Math.max(0, Math.round(animationIntervalMs - (float) lastDrawTimeNs / 1_000_000)));
  }

  /**
   * Animation interval.
   */
  private int animationIntervalMs = 4;
  /**
   * Handler for animation run.
   */
  private Handler handler = new Handler();
  /**
   * Alpha animation runnable.
   */
  private Runnable alphaChangeRunnable = new Runnable() {
    @Override
    public void run() {
      boolean repeat = false;
      for (int i = 0; i < chart.graphsCount; i++) {
        if (displayedGraphs[i] && graphsAlpha[i] != VISIBLE) {
          graphsAlpha[i] = Math.min(graphsAlpha[i] + 10, VISIBLE);
          repeat = true;
        } else if (!displayedGraphs[i] && graphsAlpha[i] != INVISIBLE) {
          graphsAlpha[i] = Math.max(graphsAlpha[i] - 10, INVISIBLE);
          repeat = true;
        }
      }

      if (currentScaleY < newScaleY) {
        currentScaleY = Math.min(currentScaleY + scaleStep, newScaleY);
        prepareDrawDynamic();
        repeat = true;
      } else if (currentScaleY > newScaleY) {
        currentScaleY = Math.max(currentScaleY - scaleStep, newScaleY);
        prepareDrawDynamic();
        repeat = true;
      }

      invalidate();
      if (repeat) {
        handler.postDelayed(this, Math.max(0, Math.round(animationIntervalMs - (float) lastDrawTimeNs / 1_000_000)));
      }
    }
  };

  /**
   * Called on MainActivity.onDestroy()
   */
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    float height = density * 350 // height of region from top to bar bottom
        + density * 50 * chart.graphsCount;
    setMeasuredDimension(width, (int) height);
  }
}
