package net.cactii.flash;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class TorchWidgetProvider extends AppWidgetProvider {
	
	public FlashDevice device;
	private static TorchWidgetProvider sInstance;
	
	private Su su;
	
	static final ComponentName THIS_APPWIDGET =
		new ComponentName("net.cactii.flash",
				"net.cactii.flash.TorchWidgetProvider");
	
	static synchronized TorchWidgetProvider getInstance() {
		if (sInstance == null)
			sInstance = new TorchWidgetProvider();
		return sInstance;
	}
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            
            Su su = new Su();
            if (!su.can_su) {
            	// If 'su' is not available, clicking the widget will bring
            	// up the main activity where the user will be notified.
	            Intent intent = new Intent(context, MainActivity.class);
	            PendingIntent pendingIntent = PendingIntent.getActivity(context,
	            		0, intent, 0);
	            views.setOnClickPendingIntent(R.id.btn, pendingIntent);
            } else {
            	device = FlashDevice.getInstance();
    			if (!device.open) {
    				device.Open();
    				if (!device.open) {
    					su.Run("chmod 666 /dev/msm_camera/config0");
    				}
    			}
	            views.setOnClickPendingIntent(R.id.btn, 
	            		getLaunchPendingIntent(context,
	            				appWidgetId, 0));
            }
			this.updateState(context);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
	}
	private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId,
			int buttonId) {
		Intent launchIntent = new Intent();
		launchIntent.setClass(context, TorchWidgetProvider.class);
		launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		launchIntent.setData(Uri.parse("custom:" + buttonId));
		PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
				launchIntent, 0 /* no flags */);
		return pi;
	}
	
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
			Uri data = intent.getData();
			int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
			device = FlashDevice.getInstance();
			if (buttonId == 0) {	
				if (!device.open)
					device.Open();
				if (device.on)
					device.FlashOff();
				else
					device.FlashOn();

				device.Close();
			}
			this.updateState(context);
		}
	}
	
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
