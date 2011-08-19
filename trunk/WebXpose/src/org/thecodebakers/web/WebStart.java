/*
 * Copyright (C) 2011 The Code Bakers
 * Authors: Cleuton Sampaio e Francisco Rodrigues
 * e-mail: thecodebakers@gmail.com
 * Project: http://code.google.com/p/android-webxpose
 * Site: http://www.thecodebakers.org
 *
 * Licensed under the GNU GPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://gplv3.fsf.org/
 * 
 * There is one adition/change over the standard GPL V3: The Authors 
 * may create another product based upon the same code, without following
 * GPL terms. The new product can be proprietary.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Cleuton Sampaio e Francisco Rogrigues - thecodebakers@gmail.com
 */

package org.thecodebakers.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import org.thecodebakers.web.service.WebServerService;

import android.app.Activity;
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

public class WebStart extends Activity {
    /** Called when the activity is first created. */
	
	private String ipAddress;
	private EditText defaultPort;
	private EditText defaultPath;
	private TextView txtIpAddress;
	private Button btnStartServer;
	private final String TAG = "WebXposeGUI";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Resources res = getResources();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnStartServer = (Button) this.findViewById(R.id.btnStart);
        defaultPort = (EditText) this.findViewById(R.id.porta);
        defaultPath = (EditText) this.findViewById(R.id.sharedFolder);
        txtIpAddress = (TextView) this.findViewById(R.id.ipAddress);
        checkNetwork();
        
    }
    
    private void checkNetwork() {
    	Resources res = getResources();
    	StringBuffer ipa = new StringBuffer();
    	ConnectivityManager conMgr =  (ConnectivityManager)getSystemService(this.getApplicationContext().CONNECTIVITY_SERVICE);
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
      								ipa.append("\n");
      								
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
    	int portaInfo = Integer.parseInt(defaultPort.getText().toString(),10);
    	if (portaInfo < 1024) {
    		Toast.makeText(getApplicationContext(), res.getString(R.string.rootport), Toast.LENGTH_LONG).show();
    	}
    	else {
    		if (verifyPath(defaultPath.getText().toString())) {
               	Intent intent = new Intent(this, WebServerService.class);
               	intent.putExtra("ipaddress", txtIpAddress.getText().toString());
               	intent.putExtra("port", defaultPort.getText().toString());
               	intent.putExtra("sharedfolder", defaultPath.getText().toString());
               	startService(intent);   
           		this.btnStartServer.setEnabled(false);
               	Toast.makeText(getApplicationContext(), res.getString(R.string.startCompleted), Toast.LENGTH_LONG).show();
    		}
    	}
    }
    

    
    private boolean verifyPath(String path) {
    	Resources res = getResources();
    	boolean returnCode = false;
    	if (path.length() < 7) {
    		Toast.makeText(getApplicationContext(), res.getString(R.string.insecurePath), Toast.LENGTH_LONG).show();
    		return false;
    	}
    	if (path.length() > 7) {
    		if (!path.substring(0, 8).equals("/sdcard/")) {
        		Toast.makeText(getApplicationContext(), res.getString(R.string.insecurePath), Toast.LENGTH_LONG).show();
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
    		Toast.makeText(getApplicationContext(), res.getString(R.string.invalidPath), Toast.LENGTH_LONG).show();
    		return false;
    	}
    	if (!(new File(path)).exists()) {
    		Toast.makeText(getApplicationContext(), res.getString(R.string.folderNotFound), Toast.LENGTH_LONG).show();
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
}