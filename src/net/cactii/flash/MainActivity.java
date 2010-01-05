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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button buttonOff;
	private Button buttonOn;
	private Button buttonFlash;
	private Context context;
	public SuCommand su_command;
	
	private static final String FLASH = "/system/bin/toolbox ioctl -l 1 -a 1 /dev/msm_camera/config0 0x40046d16 ";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this.getApplicationContext();
        buttonOff = (Button) findViewById(R.id.buttonOff);
        buttonOn = (Button) findViewById(R.id.buttonOn);
        buttonFlash = (Button) findViewById(R.id.buttonFlash);
        su_command = new SuCommand();
        
        buttonOff.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (su_command.Run(FLASH + " 0") == false) {
					Toast.makeText(context, "Error setting LED off", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Turned LED off", Toast.LENGTH_SHORT).show();
			}
        });
        
        buttonOn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (su_command.Run(FLASH + " 1") == false) {
					Toast.makeText(context, "Error setting LED on", Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(context, "Turned LED on", Toast.LENGTH_SHORT).show();
			}
        });
        
        buttonFlash.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (su_command.Run(FLASH + " 2") == false) {
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
        }
        
        is_supported = this.su_command.can_su;
         
    	if (new File("/system/bin/ioctl").exists() == false) {
    		Toast.makeText(context, "Cant find 'ioctl' binary, sorry!", Toast.LENGTH_LONG).show();
    		is_supported = false;
    	}
        
        if (!is_supported) {
        	buttonOff.setEnabled(false);
        	buttonOn.setEnabled(false);
        	buttonFlash.setEnabled(false);
        }
    }
    
    public void onStop() {
    	su_command.Run(FLASH + " off");
    	super.onStop();
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
}
