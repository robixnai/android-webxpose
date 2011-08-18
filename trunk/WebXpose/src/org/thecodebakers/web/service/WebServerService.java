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
import java.io.File;
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
			serverName = TAG + res.getString(R.string.serverName) + " v. " + info.versionName;
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
				
				try {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(
							cs.getInputStream()));
					
					// Ler o que veio no request, o querystring. Só lemos uma única linha
					String linha = br.readLine();

					Log.d("WebService",linha);
					
					HttpRequest request = new HttpRequest();
					request.decodeRequest(linha);
									
					BufferedWriter bw = new BufferedWriter(
							new OutputStreamWriter(
							cs.getOutputStream()));
					
					bw.write(request.httpResponse);
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
			String  httpResponseHeader;
			String  responseContent;
			String	httpResponse;
			String  httpHeader;
			int     httpStatus;
			
			void assembleHttpHeader() {
				String hdr = "HTTP/1.0 ";
				hdr += httpStatusCodes.get(this.httpStatus) + "\r\n";
				hdr += "Server: " + serverName + "\r\n";
				hdr += "Content-Type: " + this.contentType + "\r\n";
				this.contentLength = this.responseContent.length();
				hdr += "Content-length: " + this.contentLength + "\r\n";
				this.httpHeader = hdr + "\r\n";
			}
			
			void decodeRequest (String line) {
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
					this.contentType = "text/plain";
					this.responseContent = "The request is mal formed";
					this.httpStatus = WebServerService.BAD_REQUEST;
				}
				else {
					httpMethod = line.substring(0, 4).toUpperCase().trim();
					if (!httpMethod.equals("GET") && !httpMethod.equals("HEAD")) {
						this.responseContent = "Method not implemented.";
						this.contentType = "text/plain";
						this.httpStatus = WebServerService.NOT_IMPLEMENTENT;
					}
					else {
						int tamQS = line.length();
						int posNextSpace = line.indexOf(' ', 4);
						if (posNextSpace <= 0) {
							this.contentType = "text/plain";
							this.responseContent = "The request is mal formed";
							this.httpStatus = WebServerService.BAD_REQUEST;
						}
						else {
							queryString = line.substring(4, posNextSpace);
							getResponse(queryString);
						}
					}
				}
				
				// Create the response
				assembleHttpHeader();
				this.httpResponse = this.httpHeader + "\r\n" + this.responseContent;
				Log.d("WebServerService", this.httpResponse);
			}

			private void getResponse(String queryString2) {
				Resources res = WebServerService.selfRef.getResources();
				String titulo = res.getString(R.string.dirListing)  + " " + queryString2;
				if (fileExists(queryString)) {
					if (isDirectory(queryString)) {
						this.responseContent = getDirList(queryString);
						this.contentType = "text/html";
						this.httpStatus = WebServerService.HTTP_OK;
					}
					else {
						// é um arquivo... a fazer
					}
				}
				else {
					this.responseContent = this.beginHTML(titulo) + "<p>" + res.getString(R.string.pathNotFound) + queryString2 + "</p>" + this.endHTML();
					this.contentType = "text/html";
					this.httpStatus = WebServerService.HTTP_OK;
				}
				
			}

			private String endHTML() {
				StringBuffer htmlText = new StringBuffer();
				Resources res = WebServerService.selfRef.getResources();
				htmlText.append("<br/><b><a href='http://www.thecodebakers.org'>The Code Bakers</a></b></body></html>");
				return htmlText.toString();
			}

			private String beginHTML(String titulo) {
				StringBuffer htmlText = new StringBuffer();
				Resources res = WebServerService.selfRef.getResources();
				htmlText.append("<html><head><title>" + titulo + "</title>");
				htmlText.append("<style>body {font-family:'Arial', Sans-serif; font-size: 1.5em;}</style>");
				htmlText.append("<script type=\"text/javascript\">");
				htmlText.append("<!-- \r\n");
				htmlText.append("var dsi=1.5;\r\n");
				htmlText.append("function fontAdjust(p) {\r\n");
				htmlText.append("if (p == \"grow\") {\r\n");
				htmlText.append("dsi = dsi + 0.5;\r\n");
				htmlText.append("}\r\n");
				htmlText.append("else {\r\n");
				htmlText.append("if (dsi > 1) {\r\n");
				htmlText.append("dsi = dsi - 0.5;\r\n");
				htmlText.append("}\r\n");
				htmlText.append("}\r\n");
				htmlText.append("document.body.style.fontSize=dsi + 'em';\r\n");
				htmlText.append("}\r\n");
				htmlText.append(" -->\r\n");
				htmlText.append("</script>\r\n");
				htmlText.append("</head><body>");
				htmlText.append("<br/>" + res.getString(R.string.genBy) + " " + res.getString(R.string.app_name));
				htmlText.append("&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"javascript:fontAdjust('grow');\" style=\"font-size: 2em\">+A</a>");
				htmlText.append("&nbsp;&nbsp;&nbsp;<a href=\"javascript:fontAdjust('small');\" style=\"font-size: 1.5em\">-A</a>");
				htmlText.append("<br/>");
				return htmlText.toString();
			}

			private String getDirList(String queryString2) {
				Resources res = WebServerService.selfRef.getResources();
				StringBuffer listing = new StringBuffer();
				String realPath = "/sdcard" + (queryString2.charAt(0) == '/' ? queryString2 : ("/" + queryString2));
				String titulo = res.getString(R.string.dirListing)  + " " + realPath;
				File desiredFile = new File(realPath);
				listing.append(this.beginHTML(titulo));
				
				// Loop principal
				File [] conteudo = desiredFile.listFiles();
				listing.append("<br/><b>" + desiredFile.getAbsolutePath() + ":</b>");
				listing.append("<ul>");
				if (desiredFile.getAbsolutePath().length() > 7) {
					listing.append("<li><a href='"
							+ removeSdcard(getParent(desiredFile)) + "'>" + "["+ res.getString(R.string.voltar) + "]" + "</a>");
					listing.append("</li>");
				}
				for (int x = 0; x < conteudo.length; x++) {
					if (!conteudo[x].isHidden()) {
						listing.append("<li><a href='"
								+ removeSdcard(conteudo[x].getAbsolutePath()) + "'>" + conteudo[x].getPath() + "</a>");
						if (conteudo[x].isDirectory()) {
							listing.append(" (DIR)");
						}
						listing.append("</li>");
					}
				}
				listing.append("</ul>");
				listing.append(this.endHTML());
				return listing.toString();
			}

			private String getParent(File desiredFile) {
				String retorno = "";
				Log.d("WebServerService", "@@@ " + desiredFile.getAbsolutePath());
				int posLast = desiredFile.getAbsolutePath().lastIndexOf('/');
				retorno = desiredFile.getAbsolutePath().substring(0, posLast);
				Log.d("WebServerService", "@@@ " + retorno);
				return retorno;
			}

			private String removeSdcard(String path) {
				// /sdcard
				if (path.length() > 7) {
					return path.substring(7);
				}
				else {
					return "/";
				}
			}
			
			private boolean isDirectory(String queryString2) {
				boolean resultado = false;
				String realPath = "/sdcard" + (queryString2.charAt(0) == '/' ? queryString2 : ("/" + queryString2));
				File desiredFile = new File(realPath);
				if (desiredFile.isDirectory()) {
					resultado = true;
				}
				return resultado;
			}

			private boolean fileExists(String queryString2) {
				boolean resultado = false;
				String realPath = "/sdcard" + (queryString2.charAt(0) == '/' ? queryString2 : ("/" + queryString2));
				File desiredFile = new File(realPath);
				if (desiredFile.exists()) {
					resultado = true;
				}
				return resultado;
			}
			
		}
		
	}

}
