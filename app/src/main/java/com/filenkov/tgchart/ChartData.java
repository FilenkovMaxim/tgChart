package com.filenkov.tgchart;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ChartData {
  private static final String TYPE_LINE = "line";
  private static final String TYPE_X = "x";

  int graphsCount;
  int valuesCount;
  long[] timeline;
  int[][] graphs;
  int[] maximums; // max values for every graph
  int[] colors;
  String[] names;

  ChartData(JSONObject json) throws JSONException {
    JSONObject types = json.getJSONObject("types");
    JSONObject jNames = json.getJSONObject("names");
    JSONObject jColors = json.getJSONObject("colors");

    graphsCount = jNames.length();
    colors = new int[graphsCount];
    names = new String[graphsCount];

    // set graphs, names and colors
    JSONArray columns = json.getJSONArray("columns");
    valuesCount = columns.getJSONArray(0).length() - 1; // -1 because first item contains label.
    graphs = new int[graphsCount][valuesCount];
    maximums = new int[graphsCount];
    timeline = new long[valuesCount];

    int skip = 0; // when we detect timeline array in columns we need to skip it and should use i-1
    for (int i = 0; i < graphsCount + 1; i++) { // +1 because one item contains timeline
      JSONArray array = columns.getJSONArray(i);
      String label = array.getString(0);

      if (TYPE_X.equals(types.getString(label))) {
        // it's a timeline, fill times
        for (int j = 0; j < valuesCount; j++) {
          timeline[j] = array.getLong(j + 1);
        }
        skip++;

      } else if (TYPE_LINE.equals(types.getString(label))) {

        for (int j = 0; j < valuesCount; j++) {
          int value = array.getInt(j + 1);
          graphs[i - skip][j] = value;
          // find max values
          if (value > maximums[i - skip]) {
            maximums[i - skip] = value;
          }
        }

        colors[i - skip] = Color.parseColor(jColors.getString(label));
        names[i - skip] = jNames.getString(label);

      } else {
        skip++;
      }
    }
  }
}
