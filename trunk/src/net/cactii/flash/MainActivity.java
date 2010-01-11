package net.cactii.flash;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

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
	public SuCommand su_command;
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
        su_command = new SuCommand();
        strobing = false;
        strobeperiod = 100;
        mTorchOn = false;
        mTorchThreadRunning = false;
        
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
				if (setFlashOff().equals("Failed")) {
					Toast.makeText(context, "Error setting LED off", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Turned LED off", Toast.LENGTH_SHORT).show();
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
					if (setFlashOn().equals("Failed")) {
						Toast.makeText(context, "Error setting LED on", Toast.LENGTH_LONG).show();
						return;
					}
					Toast.makeText(context, "Turned LED on", Toast.LENGTH_SHORT).show();
				}
			}
        });
        
		torchThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.interrupted()) {
					while (mTorchOn) {
							setFlashFlash();
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								setFlashOff();
							}
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						setFlashOff();
					}
				}
				mTorchOn = false;
			}
		});
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
						setFlashFlash();
						try {
							Thread.sleep(onTime);
						} catch (InterruptedException e) {
							setFlashOff();
						}
						if (strobing)
							setFlashOff();
						try {
							Thread.sleep(strobeperiod);
						} catch (InterruptedException e) {
							setFlashOff();
						}
					}
					strobing = false;
					try {
						Thread.sleep(strobeperiod);
					} catch (InterruptedException e) {
						setFlashOff();
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
				if (setFlashFlash().equals("Failed")) {
					Toast.makeText(context, "Error setting LED flash", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Made LED flash", Toast.LENGTH_SHORT).show();
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
        } else {
        	is_supported = this.su_command.can_su;
        	if (!is_supported)
        		this.openNotRootDialog();
        }
        if (!is_supported) {
        	buttonOff.setEnabled(false);
        	buttonOn.setEnabled(false);
        	buttonFlash.setEnabled(false);
        	buttonStrobe.setEnabled(false);
        	slider.setEnabled(false);
        } else {
        	su_command.Run("chmod 666 /dev/msm_camera/config0");
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
	    	setFlashOff();
    	}
    	this.mTorchOn = false;
    	this.torchThread.interrupt();
		this.mPrefsEditor.putInt("strobeperiod", this.strobeperiod);
    	closeFlash();
    	this.mPrefsEditor.commit();
    	super.onPause();
    }
    
    public void onResume() {
    	openFlash();
    	super.onResume();
    }
    
    // These functions are defined in the native libflash library.
    public native String  openFlash();
    public native String  setFlashOff();
    public native String  setFlashOn();
    public native String  setFlashFlash();
    public native String  closeFlash();

    
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
   	
   	/*
   	 * This class handles 'su' functionality. Ensures that the command can be run, then
   	 * handles run/pipe/return flow.
   	 */
   	private class SuCommand {
   		public boolean can_su;
   		public String su_bin_file;
   		
   		public SuCommand() {
   			this.can_su = true;
   			this.su_bin_file = "/system/xbin/su";
   			if (this.Run("echo"))
   				return;
   			this.su_bin_file = "/system/bin/su";
   			if (this.Run("echo"))
   				return;
   			this.su_bin_file = "";
   			this.can_su = false;	
   		}
   	    public boolean Run(String command) {
   	    	DataOutputStream os = null;
   	    	try {
   				Process process = Runtime.getRuntime().exec(this.su_bin_file);
   				os = new DataOutputStream(process.getOutputStream());
   				os.writeBytes(command + "\n");
   				os.flush();
   				os.writeBytes("exit\n");
   				os.flush();
   				process.waitFor();
   				return true;
   			} catch (IOException e) {
   				e.printStackTrace();
   				Toast.makeText(context, "Error running: " + command, Toast.LENGTH_LONG).show();
   			} catch (InterruptedException e) {
   				e.printStackTrace();
   				Toast.makeText(context, "Error running: " + command, Toast.LENGTH_LONG).show();
   			}
   	    	return false;
   	    }	
   	}
   	
   	// Load libflash once on app startup.
   	static {
   		System.loadLibrary("flash");
   	}
}
