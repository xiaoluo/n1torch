package net.cactii.flash;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class WidgetOptionsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
  
  public int mAppWidgetId;
  public Context mContext;
  public SeekBarPreference mStrobeFrequency;
  public SharedPreferences mPreferences;
  
  private Su mSu;
  
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

          editor.putBoolean("widget_strobe_" + mAppWidgetId,
                  mPreferences.getBoolean("widget_strobe", false));
          editor.putInt("widget_strobe_freq_" + mAppWidgetId,
              500/mPreferences.getInt("widget_strobe_freq", 5));
          editor.putBoolean("widget_bright_" + mAppWidgetId,
                  mPreferences.getBoolean("widget_bright", false));
          editor.commit();
          AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
          Intent resultValue = new Intent();
          resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
          setResult(RESULT_OK, resultValue);
          finish();
          return false;
        }
        
      });
      
      this.mSu = new Su();
      if (!this.mSu.can_su) {
        if (!Build.VERSION.RELEASE.equals("2.2")) {
          this.openNotRootDialog();
        }
        mBrightPref.setEnabled(false);
      }
      if (Build.VERSION.SDK_INT > 10)
        this.openNoWidgetDialog();
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
  
  private void openNotRootDialog() {
    LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.norootview, null); 
    new AlertDialog.Builder(WidgetOptionsActivity.this)
        .setTitle("Not Root!")
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        WidgetOptionsActivity.this.finish();
                }
        })
        .setNeutralButton("Ignore", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // nothing
                }
        })
        .show();
    }
  
  private void openNoWidgetDialog() {
    LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.nowidgetview, null); 
    new AlertDialog.Builder(WidgetOptionsActivity.this)
        .setTitle("No Widgets in ICS!")
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        WidgetOptionsActivity.this.finish();
                }
        })
        .setNeutralButton("Ignore", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // nothing
                }
        })
        .show();
    }
  
}
