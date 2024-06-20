package app.organicmaps.widget.placepage;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import app.organicmaps.Framework;

public class AxisValueFormatter extends DefaultValueFormatter
{
  private static final int DEF_DIGITS = 1;
  @NonNull
  private final BarLineChartBase mChart;

  public AxisValueFormatter(@NonNull BarLineChartBase chart)
  {
    super(DEF_DIGITS);
    mChart = chart;
  }

  @Override
  public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler)
  {
    return Framework.nativeFormatAltitude(value);
  }
}
