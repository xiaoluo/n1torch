package net.cactii.flash;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

// TODO: Get this working properly.

public class SmlTorchWidgetProvider extends TorchWidgetProvider {
	private static SmlTorchWidgetProvider sInstance;

	static final ComponentName THIS_APPWIDGET =
		new ComponentName("net.cactii.flash",
				"net.cactii.flash.SmlTorchWidgetProvider");
	
	static synchronized SmlTorchWidgetProvider getInstance() {
		if (sInstance == null)
			sInstance = new SmlTorchWidgetProvider();
		return sInstance;
	}
	
	private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId,
			int buttonId) {
		Intent launchIntent = new Intent();
		launchIntent.setClass(context, SmlTorchWidgetProvider.class);
		launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		launchIntent.setData(Uri.parse("custom:" + buttonId));
		PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
				launchIntent, 0 /* no flags */);
		return pi;
	}
	
	@Override
	public void updateState(Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
		device = FlashDevice.getInstance();
		if (device.on) {
			views.setImageViewResource(R.id.img_torch, R.drawable.icon);
		} else {
			views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
		}
		final AppWidgetManager gm = AppWidgetManager.getInstance(context);
		gm.updateAppWidget(THIS_APPWIDGET, views);
	}
}
