package net.cactii.flash;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {

	public static final boolean BRIGHT = false;
	
	public TorchWidgetProvider mWidgetProvider;
	
	public FlashDevice device;
    // Off button
	private Button buttonOff;
	// On button
	private Button buttonOn;
	// Strobe toggle
	private ToggleButton buttonStrobe;
	// Is the strobe running?
	public Boolean strobing;
	// Flash button
	private Button buttonFlash;
	// Thread to handle strobing
	public Thread strobeThread;
	public boolean mStrobeThreadRunning;
	
	public Thread torchThread;
	public boolean mTorchThreadRunning;
	public boolean mTorchOn;
	// Strobe frequency slider.
	public SeekBar slider;
	// Period of strobe, in milliseconds
	public int strobeperiod;
	// Strobe has timed out
	public boolean mTimedOut;
	private Context context;
	// Label showing strobe frequency
	public TextView strobeLabel;
	// Represents a 'su' instance
	public Su su_command;
	// Preferences
	public SharedPreferences mPrefs;
	public SharedPreferences.Editor mPrefsEditor = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this.getApplicationContext();
        buttonOff = (Button) findViewById(R.id.buttonOff);
        buttonOn = (Button) findViewById(R.id.buttonOn);
        buttonStrobe = (ToggleButton) findViewById(R.id.buttonStrobe);
        strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        slider = (SeekBar) findViewById(R.id.slider);
        
        buttonFlash = (Button) findViewById(R.id.buttonFlash);
        su_command = new Su();
        strobing = false;
        strobeperiod = 100;
        mTorchOn = false;
        mTorchThreadRunning = false;
        
        mWidgetProvider = TorchWidgetProvider.getInstance();
        device = FlashDevice.getInstance();
        
        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();
        
        // Turn LED off.
        buttonOff.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				strobing = false;
				mTorchOn = false;
				buttonStrobe.setChecked(false);
				if (device.FlashOff().equals("Failed")) {
					Toast.makeText(context, "Error setting LED off", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Turned LED off", Toast.LENGTH_SHORT).show();
				updateWidget();
			}
        });
        
        // Turn LED on
        buttonOn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				strobing = false;
				buttonStrobe.setChecked(false);
				if (BRIGHT) {
					mTorchOn = true;
				} else {
					if (device.FlashOn().equals("Failed")) {
						Toast.makeText(context, "Error setting LED on", Toast.LENGTH_LONG).show();
						return;
					}
					Toast.makeText(context, "Turned LED on", Toast.LENGTH_SHORT).show();
				}
				updateWidget();
			}
        });
        
		torchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.interrupted()) {
					while (mTorchOn) {
						FlashDevice.setFlashFlash();
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							FlashDevice.setFlashOff();
						}
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						FlashDevice.setFlashOff();
					}
				}
				mTorchOn = false;
			}
		});
		if (BRIGHT)
			torchThread.start();
		
		strobeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.interrupted()) {
				    mTimedOut = false;
					int onTime;
					long startTime = System.currentTimeMillis();
					while (strobing) {
					  
					  // Stop strobe if on for more than 25 seconds. Otherwise the
					  // LED lifetime may be reduced.
					  if (System.currentTimeMillis() - startTime > 25000) {
					    strobing = false;
					    mTimedOut = true;
					    buttonStrobe.post(new Runnable() {
		                  @Override
		                  public void run() {
		                    buttonStrobe.setChecked(false);
		                  }
					    });
					  }
					    onTime = strobeperiod/4;
					    FlashDevice.setFlashFlash();
						try {
							Thread.sleep(onTime);
						} catch (InterruptedException e) {
							device.FlashOff();
						}
						if (strobing)
							device.FlashOff();
						try {
							Thread.sleep(strobeperiod);
						} catch (InterruptedException e) {
							device.FlashOff();
						}
					}
					strobing = false;
					try {
						Thread.sleep(strobeperiod);
					} catch (InterruptedException e) {
						device.FlashOff();
					}
				}
			}
		});
		strobeThread.start();
        
        // Handle LED strobe function.
        buttonStrobe.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton b, boolean checked) {
				if (!checked) {
					if (strobeThread != null) {
						strobing = false;
						if (mTimedOut) {
						  Toast.makeText(MainActivity.this, "Stopping strobe to save LED", Toast.LENGTH_SHORT).show();
						  mTimedOut = false;
						}
					}
					updateWidget();
					return;
				}
				strobing = true;
				mTorchOn = false;
			}
        });
        
        // Strobe frequency slider bar handling
        setProgressBarVisibility(true);
        slider.setHorizontalScrollBarEnabled(true);
        slider.setProgress(200 - this.mPrefs.getInt("strobeperiod", 100));
        strobeperiod = this.mPrefs.getInt("strobeperiod", 100);
        strobeLabel.setText("Strobe frequency: " + 500/strobeperiod + "Hz");
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				strobeperiod = 201 - progress;
				if (strobeperiod < 10)
					strobeperiod = 10;
				strobeLabel.setText("Strobe frequency: " + 500/strobeperiod + "Hz");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
        	
        });
        
        // FLASH button.
        buttonFlash.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				strobing = false;
				buttonStrobe.setChecked(false);
				if (FlashDevice.setFlashFlash().equals("Failed")) {
					Toast.makeText(context, "Error setting LED flash", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Made LED flash", Toast.LENGTH_SHORT).show();
				updateWidget();
			}
        });
        
        // Show the about dialog, the first time the user runs the app.
        if (!this.mPrefs.getBoolean("aboutSeen", false)) {
          this.openAboutDialog();
          this.mPrefsEditor.putBoolean("aboutSeen", true);
        }
        
        boolean is_supported = true;
        
        if (new File("/dev/msm_camera/config0").exists() == false) {
        	Toast.makeText(context, "Only Nexus One is supported, sorry!", Toast.LENGTH_LONG).show();
        	is_supported = false;
        }

        if (!device.open) {
        	Log.d("Torch", "Cant open flash RW");
        	is_supported = this.su_command.can_su;
        	if (!is_supported)
        		this.openNotRootDialog();
        	else
        		su_command.Run("chmod 666 /dev/msm_camera/config0");
        }
        if (!is_supported) {
        	buttonOff.setEnabled(false);
        	buttonOn.setEnabled(false);
        	buttonFlash.setEnabled(false);
        	buttonStrobe.setEnabled(false);
        	slider.setEnabled(false);
        } else {
        	device.Open();
        	if (!device.open) {
        		Toast.makeText(context, "Cannot access LED device, sorry!", Toast.LENGTH_LONG).show();
        	}
        }
    }
    
    public void onPause() {
    	if (strobing) {
	    	strobing = false;
			buttonStrobe.setChecked(false);
	    	if (strobeThread != null) {
		    	try {
					strobeThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	device.FlashOff();
    	}
		this.mPrefsEditor.putInt("strobeperiod", this.strobeperiod);
		device.Close();
    	this.mPrefsEditor.commit();
    	this.updateWidget();
    	super.onPause();
    }
    
    public void onDestroy() {
    	device.Open();
    	FlashDevice.setFlashOff();
    	device.Close();
    	this.updateWidget();
    	super.onDestroy();
    }
    
    public void onResume() {
    	device.Open();
    	this.updateWidget();
    	super.onResume();
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean supRetVal = super.onCreateOptionsMenu(menu);
    	SubMenu setup = menu.addSubMenu(0, 0, 0, "About Torch");
    	return supRetVal;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	boolean supRetVal = super.onOptionsItemSelected(menuItem);
	    this.openAboutDialog();
    	return supRetVal;
    }  
    
    
   	private void openAboutDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.aboutview, null);     
		new AlertDialog.Builder(MainActivity.this)
        .setTitle("About")
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Log.d(MSG_TAG, "Close pressed");
                }
        })
        .show();  		
   	}
   	
   	private void openNotRootDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.norootview, null); 
		new AlertDialog.Builder(MainActivity.this)
        .setTitle("Not Root!")
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.finish();
                }
        })
        .setNeutralButton("Override", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // nothing
                }
        })
        .show();
   	}
   	
   	public void updateWidget() {
   		this.mWidgetProvider.updateState(context);
   	}
   	
}
