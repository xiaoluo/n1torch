package net.cactii.flash;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class TorchWidgetProvider extends AppWidgetProvider {

  public FlashDevice device;
  private static TorchWidgetProvider sInstance;
  public boolean mTorchOn;
  public Thread brightThread;
  public SharedPreferences mPrefs;

  private Su su;

  static final ComponentName THIS_APPWIDGET = new ComponentName("net.cactii.flash",
      "net.cactii.flash.TorchWidgetProvider");

  static synchronized TorchWidgetProvider getInstance() {
    if (sInstance == null) {
      sInstance = new TorchWidgetProvider();
    }
    return sInstance;
  }

  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int N = appWidgetIds.length;
    for (int i = 0; i < N; i++) {
      int appWidgetId = appWidgetIds[i];
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

      views.setOnClickPendingIntent(R.id.btn, getLaunchPendingIntent(context, appWidgetId, 0));

      this.updateState(context);
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId, int buttonId) {
    Intent launchIntent = new Intent();
    Log.d("TorchWidget", "WIdget id: " + appWidgetId);
    launchIntent.setClass(context, TorchWidgetProvider.class);
    launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
    launchIntent.setData(Uri.parse("custom:" + appWidgetId + "/" + buttonId));
    PendingIntent pi = PendingIntent
        .getBroadcast(context, 0 /* no requestCode */, launchIntent, 0 /*
                                                                        * no
                                                                        * flags
                                                                        */);
    return pi;
  }

  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
      Uri data = intent.getData();
      int buttonId;
      int widgetId;
      buttonId = Integer.parseInt(data.getSchemeSpecificPart().split("/")[0]);
      widgetId = Integer.parseInt(data.getSchemeSpecificPart().split("/")[1]);

      device = FlashDevice.getInstance();
      Log.d("TorchWidget", "Button Id is: " + widgetId);
      if (buttonId == 0) {
        Intent pendingIntent;
        Bundle extras = intent.getExtras();
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null)
          appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
              AppWidgetManager.INVALID_APPWIDGET_ID);
        if (device.Writable() && mPrefs.getBoolean("widget_bright", false)) {
          pendingIntent = new Intent(context, RootTorchService.class);
        } else {
          pendingIntent = new Intent(context, TorchService.class);
        }
        if (mPrefs.getBoolean("widget_strobe", false)) {
          pendingIntent.putExtra("strobe", true);
          pendingIntent.putExtra("period", mPrefs.getInt("widget_strobe_freq", 200));
        }
        if (this.TorchServiceRunning(context)) {
          context.stopService(pendingIntent);
        } else {
          context.startService(pendingIntent);
        }
      }
      this.updateState(context);
    }
  }

  private boolean TorchServiceRunning(Context context) {
    ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

    List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

    if (!(svcList.size() > 0))
      return false;
    for (int i = 0; i < svcList.size(); i++) {
      RunningServiceInfo serviceInfo = svcList.get(i);
      ComponentName serviceName = serviceInfo.service;
      if (serviceName.getClassName().endsWith(".TorchService")
          || serviceName.getClassName().endsWith(".RootTorchService"))
        return true;
    }
    return false;
  }

  public void updateState(Context context) {
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

    if (this.TorchServiceRunning(context)) {
      views.setImageViewResource(R.id.img_torch, R.drawable.icon);
    } else {
      views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
    }
    final AppWidgetManager gm = AppWidgetManager.getInstance(context);
    gm.updateAppWidget(THIS_APPWIDGET, views);
  }
}
