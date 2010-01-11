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
	// Strobe frequency slider.
	public SeekBar slider;
	// Period of strobe, in milliseconds
	public int strobeperiod;
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
        
        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();
        
        // Turn LED off.
        buttonOff.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				strobing = false;
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
				if (setFlashOn().equals("Failed")) {
					Toast.makeText(context, "Error setting LED on", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Turned LED on", Toast.LENGTH_SHORT).show();
			}
        });
        
        // Handle LED strobe function.
        buttonStrobe.setOnCheckedChangeListener(new OnCheckedChangeListener() {
          public boolean mTimedOut;
			public void onCheckedChanged(CompoundButton b, boolean checked) {
				if (!checked) {
					if (strobeThread != null) {
						strobing = false;
						strobeThread.interrupt();
						if (mTimedOut) {
						  Toast.makeText(MainActivity.this, "Stopping strobe to save LED", Toast.LENGTH_SHORT).show();
						  mTimedOut = false;
						}
					}
					return;
				}
				strobeThread = new Thread(new Runnable() {
					@Override
					public void run() {
					    mTimedOut = false;
						strobing = true;
						int onTime;
						long startTime = System.currentTimeMillis();
						while (strobing && !Thread.interrupted()) {
						  
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
					}
				});
				strobeThread.start();
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
				strobeperiod = 200 - progress;
				strobeLabel.setText("Strobe frequency: " + 500/strobeperiod + "Hz");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
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
        } else 
        	is_supported = this.su_command.can_su;
        
        if (!is_supported) {
        	buttonOff.setEnabled(false);
        	buttonOn.setEnabled(false);
        	buttonFlash.setEnabled(false);
        } else {
        	su_command.Run("chmod 666 /dev/msm_camera/config0");
        }
    }
    
    public void onPause() {
    	strobing = false;
		buttonStrobe.setChecked(false);
		this.mPrefsEditor.putInt("strobeperiod", this.strobeperiod);
    	if (strobeThread != null) {
	    	try {
				strobeThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	setFlashOff();
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
   	
   	/*
   	 * This class handles 'su' functionality. Ensures that the command can be run, then
   	 * handles run/pipe/return flow.
   	 */
   	private class SuCommand {
   		public boolean can_su;
   		public String su_bin_file;
   		
   		public SuCommand() {
   			this.can_su = true;
   	        File su_bin = new File("/system/xbin/su");
   	        if (su_bin.exists() == false) {
   	        	su_bin = new File("/system/bin/su");
   	        	if (su_bin.exists() == false) {
   	        		Toast.makeText(context, "Cant find 'su' binary!", Toast.LENGTH_LONG).show();
   	        		this.can_su = false;
   	        	} else
   	        		this.su_bin_file = "/system/bin/su";
   	        } else
   	        	this.su_bin_file = "/system/xbin/su";
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
