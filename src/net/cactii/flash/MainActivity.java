package net.cactii.flash;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Bundle;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
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

	private Button buttonOff;
	private Button buttonOn;
	private ToggleButton buttonStrobe;
	public Boolean strobing;
	private Button buttonFlash;
	public Thread strobeThread;
	public SeekBar slider;
	public int strobeperiod;
	private Context context;
	public TextView strobeLabel;
	public SuCommand su_command;
		
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
        
        buttonStrobe.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton b, boolean checked) {
				if (!checked) {
					if (strobeThread != null) {
						strobing = false;
						strobeThread.interrupt();
					}
					return;
				}
				strobeThread = new Thread(new Runnable() {
					@Override
					public void run() {
						strobing = true;
						while (strobing && !Thread.interrupted()) {
							setFlashOn();
							try {
								Thread.sleep(strobeperiod);
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
        
        setProgressBarVisibility(true);
        slider.setHorizontalScrollBarEnabled(true);
        slider.setProgress(100);
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
    	super.onPause();
    }
    
    public void onResume() {
    	openFlash();
    	super.onResume();
    }
    
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
   	static {
   		System.loadLibrary("flash");
   	}
}
