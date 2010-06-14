package net.cactii.flash;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class RootTorchService extends Service {

  private FlashDevice mDevice;
  public Thread mStrobeThread;
  
  public TimerTask mTorchTask;
  public Timer mTorchTimer;
  
  public TimerTask mStrobeTask;
  public Timer mStrobeTimer;
  
  private NotificationManager mNotificationManager;
  private Notification mNotification;
  
  public boolean mTorchOn;
  
  public void onCreate() {
    String ns = Context.NOTIFICATION_SERVICE;
    this.mNotificationManager = (NotificationManager) getSystemService(ns);
    
    this.mTorchTask = new TimerTask() {
      public void run() {
         FlashDevice.setFlashFlash();
      }
    };
    this.mTorchTimer = new Timer();
    
    this.mStrobeTask = new TimerTask() {
      public int mCounter = 4;
      public boolean mOn;
      
      public void run() {
        if (!this.mOn) {
          if (this.mCounter-- < 1) {
            FlashDevice.setFlashFlash();
            this.mOn = true;
          }
        } else {
          mDevice.FlashOff();
          this.mCounter = 4;
          this.mOn = false;
        }
      }
    };
    this.mStrobeTimer = new Timer();
    
  }
  
  public int onStartCommand(Intent intent, int flags, int startId) {
    
    this.mDevice = new FlashDevice();
    this.mDevice.Open();
    
    if (intent.getBooleanExtra("strobe", false))
      this.mStrobeTimer.schedule(this.mStrobeTask, 0, 20);
    else
      this.mTorchTimer.schedule(this.mTorchTask, 0, 200);
    
    this.mNotification = new Notification(R.drawable.notification_icon,
        "Torch on", System.currentTimeMillis());
    this.mNotification.setLatestEventInfo(this, "Torch on",
        "Torch currently on",
        PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
    
    this.mNotificationManager.notify(0, this.mNotification);
    
    startForeground(0, this.mNotification);
    return START_STICKY;
  }
  
  public void onDestroy() {
    this.mNotificationManager.cancelAll();
    stopForeground(true);
    this.mTorchTimer.cancel();
    this.mStrobeTimer.cancel();
    this.mDevice.FlashOff();
    this.mDevice.Close();
  }
  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

}
