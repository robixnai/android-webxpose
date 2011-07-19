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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Cleuton Sampaio e Francisco Rogrigues - thecodebakers@gmail.com
 */
package org.thecodebakers.web.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;

public class WebServerService extends Service {

	// Propriedades
	private int porta = 8080;
	private String WebRoot = "/sdcard/webroot";
	private ServerSocket serverSocket;
	private Thread mainProcessThread;
	private final String TAG = "WebXpose-";
	
	@Override
	public IBinder onBind(Intent arg0) {

		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Resources res = this.getResources();
		try {
			serverSocket = new ServerSocket(porta);
			ProcessWebRequests procReq = new ProcessWebRequests(serverSocket);
			mainProcessThread = new Thread(procReq);
			mainProcessThread.start();
		} catch (IOException e) {
			Log.e(TAG+"onStartCommand", "Exception: " + e.getMessage());
		}
		Log.i(TAG+"onStartCommand", res.getString(org.thecodebakers.web.R.string.onstartok));
		return START_STICKY;
		
	}

	class ProcessWebRequests implements Runnable {
		
		private ServerSocket srvSock;
		public ProcessWebRequests(ServerSocket srvSock) {
			this.srvSock = srvSock;
		}
		
		@Override
		public void run() {
			// Loop principal
			Socket s = null;
			while (true) {
				try {
					s = srvSock.accept();
					processa(s);
					s.close();
				}
				catch(IOException ioe) {
					System.out.println(ioe.getMessage());
				}
			}			
		}

		void processa(Socket s) {
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(
						s.getInputStream()));
				String linha = br.readLine();

				System.out.println(linha);

				BufferedWriter bw = new BufferedWriter(
						new OutputStreamWriter(
						s.getOutputStream()));
				String texto = "<html><head><title>Envio de resposta</title>" + 
					"</head><body><h1>Esta &eacute; uma p&aacute;gina HTML gerada " + 
					" por uma aplica&ccedil;&atilde;o Java SE</h1></body></html>";
				bw.write("HTTP/1.0 200 OK\r\n");
				bw.write("Content-Type: text/html\r\n" );
				bw.write("Content-Length: " + texto.length() + "\r\n\r\n");
				bw.write(texto);
				bw.flush();
			}
			catch(IOException ioe) {
					System.out.println(ioe.getMessage());
			}
		}
		
	}

}
