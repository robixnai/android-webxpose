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
import java.util.HashMap;
import java.util.Map;

import org.thecodebakers.web.R;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class WebServerService extends Service {

	// Propriedades
	private int porta;
	private int portaAdm;
	private String webRoot = "/sdcard";
	private ServerSocket serverSocket;
	private ServerSocket serverAdmSocket;
	private Thread mainProcessThread;
	private Thread adminProcessThread;
	private final String TAG = "WebXpose-";
	private String serverName;
	private static final Map<Integer,String> httpStatusCodes = new HashMap<Integer, String>();
	private static Service selfRef;

	static final int HTTP_OK = 200;
	static final int BAD_REQUEST = 400;
	static final int FORBIDDEN = 403;
	static final int NOT_FOUND = 404;
	static final int INTERNAL_SERVER_ERROR = 500;
	static final int NOT_IMPLEMENTENT = 501;
	static {
		httpStatusCodes.put(200, "200 OK");
		httpStatusCodes.put(400, "400 Bad Request");
		httpStatusCodes.put(403, "403 Forbidden");
		httpStatusCodes.put(404, "404 Not Found");
		httpStatusCodes.put(500, "500 Internal Server Error");
		httpStatusCodes.put(501, "501 Not Implemented");
	}
	
	public synchronized void stopEverything() {
		try {
			this.serverSocket.close();
			this.serverAdmSocket.close();
			this.mainProcessThread.interrupt();
			this.adminProcessThread.interrupt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public IBinder onBind(Intent arg0) {

		return null;
	}

	public WebServerService() {
		WebServerService.selfRef = this;
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Resources res = this.getResources();
		try {
			PackageManager manager = this.getApplicationContext().getPackageManager();
			PackageInfo info = manager.getPackageInfo(
					this.getApplicationContext().getPackageName(), 0);
			serverName = TAG + R.string.serverName + info.versionName;
			Log.i(TAG, res.getString(R.string.iniciando));
			Bundle extras = intent.getExtras();
			porta = Integer.parseInt(extras.getString("port"));
			portaAdm = Integer.parseInt(extras.getString("adminPort"));
			webRoot = extras.getString("sharedfolder");
			Log.i(TAG, extras.getString("ipaddress") + ":" + porta + "/" + webRoot);
			try {
				serverSocket = new ServerSocket(porta);
				try {
					serverAdmSocket = new ServerSocket(portaAdm);
					ProcessWebRequests procReq = new ProcessWebRequests(serverSocket);
					mainProcessThread = new Thread(procReq);
					mainProcessThread.start();
					ProcessAdminRequests procAdm = new ProcessAdminRequests(serverAdmSocket);
					adminProcessThread = new Thread(procAdm);
					adminProcessThread.start();
					Log.i(TAG, res.getString(org.thecodebakers.web.R.string.onstartok));
					Toast.makeText(this.getApplicationContext(), R.string.serviceStarted, Toast.LENGTH_LONG);
					return START_STICKY;
				}
				catch (Exception ex) {
					Log.e(TAG, res.getString(R.string.errorOpeningAdminPort));
					Toast.makeText(this.getApplicationContext(), R.string.errorOpeningAdminPort, Toast.LENGTH_LONG);
				}
			}
			catch (Exception ex) {
				Log.e(TAG, res.getString(R.string.errorOpeningPort));
				Toast.makeText(this.getApplicationContext(), R.string.errorOpeningPort, Toast.LENGTH_LONG);
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Exception: " + e.getMessage());
		}
		return START_NOT_STICKY;
	}

	class ProcessAdminRequests implements Runnable {
		// Um Thread para acompanhar os requests administrativos
		
		ServerSocket srvSock;
		
		ProcessAdminRequests (ServerSocket srvSock) {
			this.srvSock = srvSock;
		}
		
		public void run() {
			while (true) {
				Socket s = null;
				try {
                    s = srvSock.accept();
                    if(processa(s)) {
                    	// Request to stop received:
                    	WebServerService webServer = (WebServerService) WebServerService.selfRef;
                    	webServer.stopEverything();
                    	Log.i("WebService", "Processing stop...");
                    }
                    s.close();					
				}
				catch(IOException ioe) {
					System.out.println(ioe.getMessage());
					break;
				}
			}				
		}
		
		private boolean processa(Socket s) {
			boolean resultado = false;
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(
						s.getInputStream()));
				
				// Ler o que veio no request, o querystring. Só lemos uma única linha
				String linha = br.readLine();
				Log.d("WebService","@@@ Admin request: " + linha);
				if (linha.indexOf("shutdown.cgi") >= 0) {
					Log.i("WebService","@@@@@@@Shutdown");
					WebServerService.selfRef.stopSelf();
					resultado = true;
				}
				BufferedWriter bw = new BufferedWriter(
						new OutputStreamWriter(
						s.getOutputStream()));
				String texto = "<html><head><title>Envio de resposta</title>" + 
					"</head><body><h1>Request " + linha + " processado.</h1></body></html>";
				bw.write("HTTP/1.0 200 OK\r\n");
				bw.write("Content-Type: text/html\r\n" );
				bw.write("Content-Length: " + texto.length() + "\r\n\r\n");
				bw.write(texto);
				bw.flush();
				s.close();				
			}
			catch (IOException ioe) {
				resultado = true;
				Log.e("WebService", ioe.getMessage());
			}
			return resultado;
		}
	}
	
	class ProcessWebRequests implements Runnable {
		// Ele cria um Thread em separado, só para rodar o loop de espera do servidor
		
		private ServerSocket srvSock;
		
		public ProcessWebRequests(ServerSocket srvSock) {
			this.srvSock = srvSock;
		}
		

		public void run() {
			// Loop principal
			Thread clientThread;
			while (true) {
				try {
					ProcessaHTTP processaHTTP = new ProcessaHTTP(srvSock);
					clientThread = new Thread(processaHTTP);
					clientThread.start();
				}
				catch(IOException ioe) {
					System.out.println(ioe.getMessage());

					break;
				}
			}			
		}

		class ProcessaHTTP implements Runnable {
			// Esta classe vai processar cada request HTTP em um thread separado
			
			private Socket cs;
			
			ProcessaHTTP(ServerSocket ss) throws IOException {
				this.cs = ss.accept();
			}
			
			public void run() {
				// Tratamento de cada requisição
				/*
				 * Esta é uma implementação muito simpes de 
				 * Nosso servidor somente lida com GET e HEAD.
				 * Não lemos nada do socket cliente, apenas a query string.
				 * Não enviamos nada para o cliente, apenas o arquivo que ele pediu, ou então um erro.
				 * É permitido listar diretório.
				 */
				
				try {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(
							cs.getInputStream()));
					
					// Ler o que veio no request, o querystring. Só lemos uma única linha
					String linha = br.readLine();

					Log.d("WebService",linha);

									
					BufferedWriter bw = new BufferedWriter(
							new OutputStreamWriter(
							cs.getOutputStream()));
					String texto = "<html><head><title>Envio de resposta</title>" + 
						"</head><body><h1>Esta &eacute; uma p&aacute;gina HTML gerada " + 
						" por uma aplica&ccedil;&atilde;o Java SE</h1></body></html>";
					bw.write("HTTP/1.0 200 OK\r\n");
					bw.write("Content-Type: text/html\r\n" );
					bw.write("Content-Length: " + texto.length() + "\r\n\r\n");
					bw.write(texto);
					bw.flush();
					cs.close();
				}
				catch(IOException ioe) {
					Log.d("WebService",ioe.getMessage());

				}
			}
			
		}
		
		class HttpRequest {
			String  queryString;
			String  httpMethod;
			String  desiredFileDir;
			boolean isDirectoryListing;
			String  contentType;
			int     contentLength;
			
			int decodeRequest (String line) {
				/*
				 * O formato do querystring deve ser:
				 * 
				 * 0....5...10...15...20...25
				 * get /dir/teste.dat HTTP/1.0
				 * 
				 * Não há suporte para variáveis querystring e nem extra-path
				 * Se o path terminar em "/", ele assume que é para listar diretório
				 */
				int returnCode = WebServerService.HTTP_OK;
				if (line.length() < 5) {
					returnCode = WebServerService.BAD_REQUEST;
				}
				else {
					httpMethod = line.substring(0, 4).toUpperCase().trim();
					if (!httpMethod.equals("GET") && !httpMethod.equals("HEAD")) {
						returnCode = WebServerService.NOT_IMPLEMENTENT;
					}
					else {
						int tamQS = line.length();
						int posNextSpace = line.indexOf(' ', 4);
						if (posNextSpace <= 0) {
							returnCode = WebServerService.BAD_REQUEST;
						}
						else {
							queryString = line.substring(4, posNextSpace);
						}
					}
				}
				return returnCode;
			}
			
		}
		
	}

}
