package net.cactii.flash;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class WidgetOptionsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
  
  public int mAppWidgetId;
  public Context mContext;
  public SeekBarPreference mStrobeFrequency;
  public SharedPreferences mPreferences;
  
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.layout.optionsview); 
      this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      this.mContext = this;
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      if (extras != null) {
          mAppWidgetId = extras.getInt(
                  AppWidgetManager.EXTRA_APPWIDGET_ID, 
                  AppWidgetManager.INVALID_APPWIDGET_ID);
          Log.d("TorchOptions", "Widget id: " + mAppWidgetId);
      }
      
      CheckBoxPreference mBrightPref = (CheckBoxPreference)findPreference("widget_bright");
      mBrightPref.setChecked(false);
      
      CheckBoxPreference mStrobePref = (CheckBoxPreference)findPreference("widget_strobe");
      mStrobePref.setChecked(false);

      mStrobeFrequency = (SeekBarPreference)findPreference("widget_strobe_freq");
      mStrobeFrequency.setEnabled(false);

      getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
      Preference mSave = (Preference)findPreference("saveSettings");
      mSave.setOnPreferenceClickListener(new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
          Editor editor = mPreferences.edit();

          editor.putInt("widget_strobe_freq",
              500/mPreferences.getInt("widget_strobe_freq", 5));
          editor.commit();
          AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
          Intent resultValue = new Intent();
          resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
          setResult(RESULT_OK, resultValue);
          finish();
          return false;
        }
        
      });
      
  }

  
  public void onPause() {

    super.onPause();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals("widget_strobe")) {
      this.mStrobeFrequency.setEnabled(sharedPreferences.getBoolean("widget_strobe", false));
    }
    
  }
  
}
