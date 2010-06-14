package net.cactii.flash;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class TorchService extends Service {

  private Camera mCamera;
  private Camera.Parameters mParams;
  
  private NotificationManager mNotificationManager;
  private Notification mNotification;
  
  public TimerTask mStrobeTask;
  public Timer mStrobeTimer;
  
  public Handler mHandler;
  private IntentReceiver mReceiver;
  
  public void onCreate() {
    String ns = Context.NOTIFICATION_SERVICE;
    this.mNotificationManager = (NotificationManager) getSystemService(ns);
    
    this.mStrobeTask = new TimerTask() {
      public int mCounter = 4;
      public boolean mOn;
      
      public void run() {
        if (!this.mOn) {
          if (this.mCounter-- < 1) {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(params);
            this.mOn = true;
          }
        } else {
          Camera.Parameters params = mCamera.getParameters();
          params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
          mCamera.setParameters(params);
          this.mCounter = 4;
          this.mOn = false;
        }
      }
    };
    this.mStrobeTimer = new Timer();
    
    this.mHandler = new Handler() {
      
    };
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    try {
      this.mCamera = Camera.open();
    } catch (RuntimeException e) {
      
    }

    if (intent != null && intent.getBooleanExtra("strobe", false))
      this.mStrobeTimer.schedule(this.mStrobeTask, 0,
          intent.getIntExtra("period", 200)/4);
    else {
      this.mParams = mCamera.getParameters();
      this.mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
      this.mCamera.setParameters(this.mParams);
    }
    
    this.mReceiver = new IntentReceiver();
    registerReceiver(this.mReceiver, new IntentFilter("net.cactii.flash.SET_STROBE"));
    
    this.mNotification = new Notification(R.drawable.notification_icon,
        "Torch on", System.currentTimeMillis());
    this.mNotification.setLatestEventInfo(this, "Torch on",
        "Torch currently on",
        PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
    
    this.mNotificationManager.notify(0, this.mNotification);
    
    startForeground(0, this.mNotification);
    return START_STICKY;
  }
  
  public void Reshedule(int period) {
    this.mStrobeTimer.cancel();
    this.mStrobeTimer.schedule(this.mStrobeTask, 0, period/4);
  }
  
  public void onDestroy() {
    this.mStrobeTimer.cancel();
    this.mCamera.release();
    this.mNotificationManager.cancelAll();
    this.unregisterReceiver(this.mReceiver);
    stopForeground(true);
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

  public class IntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
      mHandler.post(new Runnable() {

        @Override
        public void run() {
          Reshedule(intent.getIntExtra("period", 200));
        }
        
      });
    }
  }
}
