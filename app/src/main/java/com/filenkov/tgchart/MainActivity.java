package com.filenkov.tgchart;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {
  private boolean nightMode = false;
  private ChartView[] chartViews;
  private LinearLayout toolbar;
  private LinearLayout mainLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    String getPackageName = getPackageName();
    mainLayout = findViewById(R.id.main);
    toolbar = findViewById(R.id.toolbar);

    chartViews = new ChartView[5];
    try {
      JSONArray json = loadJSONFromAsset(this, "chart_data.json");
      for (int i = 0; i < 5; i++) {
        chartViews[i] = findViewById(getResources().getIdentifier("chart" + i, "id", getPackageName));
        if (nightMode) {
          chartViews[i].setNightMode(false);
        } else {
          chartViews[i].setDayMode(false);
        }
        chartViews[i].setChartData(new ChartData(json.getJSONObject(i)));
      }
    } catch (IOException e) {
      Toast.makeText(this, "Error while reading graphs", Toast.LENGTH_LONG).show();
    } catch (JSONException e) {
      Toast.makeText(this, "Error while parsing graphs to json", Toast.LENGTH_LONG).show();
    }

    findViewById(R.id.nightmode).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (nightMode) {
          nightMode = false;
          mainLayout.setBackgroundColor(getResources().getColor(R.color.dayBgColor));
          toolbar.setBackgroundColor(getResources().getColor(R.color.dayToolbarBgColor));
          for (int i = 0; i < 5; i++) {
            chartViews[i].setDayMode(true);
          }
        } else {
          nightMode = true;
          mainLayout.setBackgroundColor(getResources().getColor(R.color.nightBgColor));
          toolbar.setBackgroundColor(getResources().getColor(R.color.nightToolbarBgColor));
          for (int i = 0; i < 5; i++) {
            chartViews[i].setNightMode(true);
          }
        }
      }
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    for (int i = 0; i < 5; i++) {
      ((ChartView) findViewById(getResources().getIdentifier("chart" + i, "id", getPackageName())))
          .onDestroy();
    }
  }

  /**
   * @param context  context.
   * @param filename json file name.
   * @return JSON array.
   */
  public JSONArray loadJSONFromAsset(final Context context, final String filename) throws IOException, JSONException {
    InputStream is = context.getAssets().open(filename);
    int size = is.available();
    byte[] buffer = new byte[size];
    is.read(buffer);
    is.close();
    return new JSONArray(new String(buffer, "UTF-8"));
  }
}
