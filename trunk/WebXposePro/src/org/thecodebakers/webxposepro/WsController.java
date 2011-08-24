/*
 * Copyright (C) 2011 The Code Bakers
 * Authors: Cleuton Sampaio e Francisco Rodrigues
 * e-mail: thecodebakers@gmail.com
 * Project: http://http://code.google.com/p/tcbnewswidget
 * Site: http://www.thecodebakers.org
 *
 * Licensed under the GNU GPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://gplv3.fsf.org/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Cleuton Sampaio e Francisco Rogrigues - thecodebakers@gmail.com
 */
package org.thecodebakers.webxposepro;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WsController extends Activity {
	private EditText defaultPort;
	private EditText defaultAdminPort;
	private EditText defaultPath;
	private EditText siteTitle;
	private TextView txtIpAddress;
	private Button btnStartServer;
	private Button btnStopServer;
	private final String TAG = "WebXposeGUI";
	private WsController selfRef;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        selfRef = this;
        if (firstTime()) {
        	Intent i = new Intent(this.getApplicationContext(),WsFirstTime.class);
        	this.startActivityForResult(i, 100);
        }
        else {
        	completeStart();
        }
        
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Resources res = getResources();
    	if (requestCode == 100) {
    		if (resultCode == Activity.RESULT_OK) {
            	completeStart();
    		}
    		else {
    	        new AlertDialog.Builder(this).setMessage(res.getString(R.string.userNotAgree))
    	        .setNeutralButton(res.getString(R.string.voltar), new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int which) {
    	            	selfRef.finish();
    	            } }).show(); 
    		}
    	}
	}



	private void completeStart() {
    	Resources res = getResources();
        btnStartServer = (Button) this.findViewById(R.id.btnStart);
        btnStopServer = (Button) this.findViewById(R.id.btnStop);
        defaultPort = (EditText) this.findViewById(R.id.porta);
        defaultAdminPort = (EditText) this.findViewById(R.id.portaadm);
        siteTitle = (EditText) this.findViewById(R.id.txtNomeCel);
        int adminPort = Integer.parseInt(defaultAdminPort.getText().toString());
        defaultPath = (EditText) this.findViewById(R.id.sharedFolder);
        txtIpAddress = (TextView) this.findViewById(R.id.ipAddress);
        
        checkNetwork();
        if (checkAdminPort(adminPort)) {
        	btnStopServer.setEnabled(true);
        	btnStartServer.setEnabled(false);
        	toggleFields();
        	Toast.makeText(getApplicationContext(), res.getString(R.string.serverIsStarted), Toast.LENGTH_LONG).show();
        }
        else {
        	Toast.makeText(getApplicationContext(), res.getString(R.string.serverNotStarted), Toast.LENGTH_LONG).show();
        }
    	
    }

	private void toggleFields() {
		this.siteTitle.setEnabled(!this.siteTitle.isEnabled());
		this.defaultPort.setEnabled(!this.defaultPort.isEnabled());
		this.defaultAdminPort.setEnabled(!this.defaultAdminPort.isEnabled());
		this.defaultPath.setEnabled(!this.defaultPath.isEnabled());
	}
	
	private boolean firstTime() {
		Resources res = this.getResources();
		return this.getSharedPreferences(res.getString(R.string.WXPROprefs), MODE_PRIVATE).getBoolean("first_time", true);
	}

	private void checkNetwork() {
    	Resources res = getResources();
    	StringBuffer ipa = new StringBuffer();
    	this.getApplicationContext();
		ConnectivityManager conMgr =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo info = conMgr.getActiveNetworkInfo();
    	if (info != null) {
        	if (info.isAvailable()) {
        		if (info.isConnected()) {
      				try {
      					Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
      					while (nis.hasMoreElements()) {
      						NetworkInterface ni = nis.nextElement();
      						Enumeration<InetAddress> ipads = ni.getInetAddresses();
      						while (ipads.hasMoreElements()) {
      							InetAddress ipad = ipads.nextElement();
      							if (!ipad.isLoopbackAddress()) {
      								ipa.append(ipad.getHostAddress().toString());
      								if (ipad.isSiteLocalAddress()) {
      									ipa.append(" (LOCAL)");
      								}
      							}
      						}
      						if (ipa.length() > 0) {
      							txtIpAddress.setText(ipa.toString());
    							this.btnStartServer.setEnabled(true);  							
      						}
      					}
        			} catch (SocketException ex) {
        				Log.e(TAG, ex.toString());
        				Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
        			}
        		}
        		else {
        			Log.e(TAG, res.getString(R.string.actConNotConnected));
        			Toast.makeText(getApplicationContext(), res.getString(R.string.actConNotConnected), Toast.LENGTH_LONG).show();
        		}
        	}
        	else {
        		Log.e(TAG, res.getString(R.string.actConNotAvailable));
        		Toast.makeText(getApplicationContext(), res.getString(R.string.actConNotAvailable), Toast.LENGTH_LONG).show();
        	}    		
    	}
    	else {
    		Log.e(TAG, res.getString(R.string.actConNotAvailableNull));
    		Toast.makeText(getApplicationContext(), res.getString(R.string.actConNotAvailableNull), Toast.LENGTH_LONG).show();
    	}

    }
    
    public void start(View view) {
    	Resources res = getResources();

    	if (validadeForm()) {
			Intent intent = new Intent(this, WebServerService.class);
			intent.putExtra("sitetitle", siteTitle.getText().toString());
			intent.putExtra("ipaddress", txtIpAddress.getText().toString());
			intent.putExtra("port", defaultPort.getText().toString());
			intent.putExtra("adminPort", defaultAdminPort.getText().toString());
			intent.putExtra("sharedfolder", defaultPath.getText().toString());
			startService(intent);   
      		this.btnStopServer.setEnabled(true);
       		this.btnStartServer.setEnabled(false);
       		toggleFields();
           	Toast.makeText(getApplicationContext(), res.getString(R.string.startCompleted), Toast.LENGTH_LONG).show();
    	}
    	else {
    		Toast.makeText(getApplicationContext(), res.getString(R.string.validationProblems), Toast.LENGTH_LONG).show();
    	}
    }
    
    public void stop(View view) {
    	Resources res = getResources();
    	EditText txtPortaAdm = (EditText) this.findViewById(R.id.portaadm);
    	int portaAdm = Integer.parseInt(txtPortaAdm.getText().toString());
    	this.requestStop(portaAdm);
    	this.btnStopServer.setEnabled(false);
    	this.btnStartServer.setEnabled(true);
    	toggleFields();
    	Toast.makeText(getApplicationContext(), res.getString(R.string.stopCompleted), Toast.LENGTH_LONG).show();
    }
    
    private boolean verifyPath(String path) {
    	boolean returnCode = false;
    	if (path.length() < 7) {
    		return false;
    	}
    	if (path.length() > 7) {
    		if (!path.substring(0, 8).equals("/sdcard/")) {
        		return false;
    		}
    	}
    	if (path.equals("/sdcard")) {
    		returnCode = true;
    	}
    	if (path.matches("[a-zA-Z0-9/]+")) {
    		returnCode = true;
    	}
    	else {
    		return false;
    	}
    	if (!(new File(path)).exists()) {
    		return false;
    	}
    	return returnCode;
    }
    
    private boolean checkAdminPort(int adminPort) {
    	StringBuffer bigBuf = new StringBuffer();
    	boolean resultado = false;
    	try {
    		InputStream strm = new URL("http://localhost:" + adminPort + "/").openStream();
    		BufferedReader rdr = new BufferedReader(new InputStreamReader(strm));
    		String thisLine = rdr.readLine();
    		while (thisLine != null) {
    			bigBuf = bigBuf.append(thisLine);
    			thisLine = rdr.readLine();
    		}
    		rdr.close();
    		resultado = true;
    		} catch (Exception e) {
    		}
    	return resultado;    	
    }
    
    private void requestStop(int adminPort) {
    	StringBuffer bigBuf = new StringBuffer();
    	try {
    		InputStream strm = new URL("http://localhost:" + adminPort + "/shutdown.cgi").openStream();
    		BufferedReader rdr = new BufferedReader(new InputStreamReader(strm));
    		String thisLine = rdr.readLine();
    		while (thisLine != null) {
    			bigBuf = bigBuf.append(thisLine);
    			thisLine = rdr.readLine();
    		}
    		rdr.close();
    		} catch (Exception e) {
    		}
    }

    private boolean validadeForm() {
    	Resources res = this.getResources();
    	boolean resultado = true;
    	boolean erroportas = false;
   		String siteName = this.siteTitle.getText().toString();
   		int porta = 0;
   		int admin = 0;
   		if (siteName.indexOf('<') >= 0 || siteName.indexOf('>') >= 0) {
   			resultado = false;
   			this.siteTitle.setError(res.getString(R.string.invalidTitle));
   		}
   		try {
   	   		porta = Integer.parseInt(this.defaultPort.getText().toString());
   			if (porta <= 1024) {
   				resultado = false;
   				erroportas = true;
   	   			this.defaultPort.setError(res.getString(R.string.invalidPort));
   			}
   		}
   		catch (NumberFormatException nex) {
				resultado = false;
				erroportas = true;
   	   			this.defaultPort.setError(res.getString(R.string.invalidPort));
   		}
   		try {
   	   		admin = Integer.parseInt(this.defaultAdminPort.getText().toString());
   			if (admin <= 1024) {
   				resultado = false;
   				erroportas = true;
   	   			this.defaultAdminPort.setError(res.getString(R.string.invalidPort));
   			}
   		}
   		catch (NumberFormatException nex) {
				resultado = false;
				erroportas = true;
   	   			this.defaultAdminPort.setError(res.getString(R.string.invalidPort));
   		}
   		
   		if (!erroportas && (porta == admin)) {
			resultado = false;
  			this.defaultPort.setError(res.getString(R.string.invalidEqualPorts));
   		}
   		
   		String sharedFolder = this.defaultPath.getText().toString();
   		if (!verifyPath(sharedFolder)) {
   			resultado = false;
   			this.defaultPath.setError(res.getString(R.string.insecurePath));
   		}

    	return resultado;
    }
}