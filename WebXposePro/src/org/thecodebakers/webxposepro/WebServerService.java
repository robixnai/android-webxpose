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
 * The over the GPL V 3 there is the following addition: 
 * - The authors may create another version of the program which will be distributed
 *   as proprietary software.
 * 
 * @author Cleuton Sampaio e Francisco Rogrigues - thecodebakers@gmail.com
 */
package org.thecodebakers.webxposepro;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

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
	String titulo;
	int porta;
	int portaAdm;
	String webRoot;
	ServerSocket serverSocket;
	ServerSocket serverAdmSocket;
	Thread mainProcessThread;
	Thread adminProcessThread;
	final String TAG = "WebXpose-";
	String serverName;
	static final Map<Integer,String> httpStatusCodes = new HashMap<Integer, String>();
	static Service selfRef;

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
			titulo = extras.getString("sitetitle");
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
					Log.i(TAG, res.getString(R.string.onstartok));
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
					request.servico = (WebServerService) WebServerService.selfRef;
					request.decodeRequest(linha);
					/*				
					BufferedWriter bw = new BufferedWriter(
							new OutputStreamWriter(
							cs.getOutputStream()));
					
					bw.write(request.httpResponse);
					bw.flush();
					cs.close();
					*/
					Log.d("WebServerService", ">>> beginning transmission of response: " + request.contentLength);
					DataOutputStream dos = new DataOutputStream(cs.getOutputStream());
					for (long inx = 0; inx < request.responseBytes.length; inx++) {
						//Log.d("WebServerService", ">>> byte " + inx + ": " + ((char) request.responseBytes[(int) inx]));
						dos.write(request.responseBytes[(int) inx]); 
					}
					dos.flush();
					dos.close();
					Log.d("WebServerService", ">>> ending transmission of response");
				}
				catch(IOException ioe) {
					Log.d("WebService","#### IOEXCEPTION: " + ioe.getMessage());
				}
				catch(Exception ex) {
					Log.d("WebService","####!!!! EXCEPTION: " + ex.getMessage());
				}
			}
			
		}
		
		class HttpRequest {
			String  queryString;
			String  httpMethod;
			String  desiredFileDir;
			boolean isDirectoryListing;
			String  contentType;
			long     contentLength;
			String  httpResponseHeader;
			String  responseContent;
			byte [] responseBytes;
			String	httpResponse;
			String  httpHeader;
			int     httpStatus;
			WebServerService servico;
			
			void assembleHttpHeader() {
				String hdr = "HTTP/1.0 ";
				hdr += httpStatusCodes.get(this.httpStatus) + "\r\n";
				hdr += "Server: " + serverName + "\r\n";
				hdr += "Content-Type: " + this.contentType + "\r\n";
				if (this.isDirectoryListing) {
					this.contentLength = this.responseContent.length();
				}
				hdr += "Content-length: " + this.contentLength + "\r\n";
				hdr += "Connection: close\r\n";
				this.httpHeader = hdr + "\r\n";
				Log.d("WebServerService", this.httpHeader);
			}
			
			void decodeRequest (String line) {

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
				
				if (this.httpMethod.equalsIgnoreCase("GET")) {
					if (this.isDirectoryListing) {
						assembleHttpHeader();
						this.httpResponse = this.httpHeader + this.responseContent;
						this.responseBytes = this.httpResponse.getBytes();
					}
					else {
						// obtem binário do arquivo
						byte [] dados = getFileBytes();
						assembleHttpHeader();
						this.responseBytes = new byte[this.httpHeader.getBytes().length + dados.length];
						int pos = 0;
						byte [] resposta = this.httpHeader.getBytes();
						for (int ind = 0; ind < resposta.length; ind++) {
							this.responseBytes[pos] = resposta[ind];
							pos++;
						}

						for (int ind = 0; ind < dados.length; ind++) {
							this.responseBytes[pos] = dados[ind];
							pos++;
						}
						
					}
				}
			}

			private byte [] getFileBytes() {
				// Monta a resposta binária
				byte [] saida = null;
				try {
					FileInputStream fis = new FileInputStream(this.desiredFileDir);
					saida = new byte[(int) this.contentLength];
					Log.d("WebServerService", "@@@ Getting file bytes. Total: " + this.contentLength);
					int pos = 0;
					while (true) {
						int lido = fis.read();
				        if (lido == -1) {
				            break; 
				        }
						saida[pos] = (byte) lido;
						pos++;
				    }
					fis.close();
					this.httpStatus = WebServerService.HTTP_OK;
					Log.d("WebServerService", "@@@ Finish getting bytes.");
				} catch (FileNotFoundException e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(((WebServerService) WebServerService.selfRef).getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
					this.httpStatus = WebServerService.INTERNAL_SERVER_ERROR;
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(((WebServerService) WebServerService.selfRef).getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
					this.httpStatus = WebServerService.INTERNAL_SERVER_ERROR;
				}
				return saida;
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
						// É um arquivo
						this.getFileType(this.desiredFileDir);
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
				htmlText.append("</div><br/><b><a href='http://www.thecodebakers.org'>The Code Bakers</a></b></body></html>");
				return htmlText.toString();
			}

			private String beginHTML(String titpage) {
				StringBuffer htmlText = new StringBuffer();
				Resources res = WebServerService.selfRef.getResources();
				String tit = "";
				if (titulo != null && titulo.length() > 0) {
					tit = "<h3>" + titulo + "</h3>";
				}
				htmlText.append("<html><head><title>" + titpage + "</title>");
				htmlText.append("<style>body {font-family:'Arial', Sans-serif; font-size: 2em;}</style>");
				htmlText.append("<script type=\"text/javascript\">");
				htmlText.append("<!-- \r\n");
				htmlText.append("var dsi=2;\r\n");
				htmlText.append("var tamanhos = ['50%','80%','100%','150%','200%','300%','400%'];\r\n");
				htmlText.append("function fontAdjust(p) {\r\n");
				htmlText.append("if (p == \"grow\") {\r\n");
				htmlText.append("if (dsi<7) {dsi = dsi + 1}; \r\n");
				htmlText.append("}\r\n");
				htmlText.append("else {\r\n");
				htmlText.append("if (dsi > 0) {\r\n");
				htmlText.append("dsi = dsi - 1; \r\n");
				htmlText.append("}\r\n");
				htmlText.append("}\r\n");
				htmlText.append("document.getElementById('principal').style.fontSize = tamanhos[dsi];\r\n");
				htmlText.append("}\r\n");
				htmlText.append(" -->\r\n");
				htmlText.append("</script>\r\n");
				htmlText.append("</head><body>");
				htmlText.append(tit);
				htmlText.append("<br/>" + res.getString(R.string.genBy) + " " + res.getString(R.string.app_name));
				htmlText.append("<br/> <input type=\"button\" onclick=\"fontAdjust('grow');\" value=\"" + res.getString(R.string.txtAumentar) + "\"/>\r\n");
				htmlText.append(" <input type=\"button\" onclick=\"fontAdjust('snall');\" value=\"" + res.getString(R.string.txtDiminuir) + "\"/>\r\n");
				htmlText.append("<br/><div id='principal'>");
				return htmlText.toString();
			}

			private String getDirList(String queryString2) {
				Resources res = WebServerService.selfRef.getResources();
				StringBuffer listing = new StringBuffer();
				String realPath = servico.webRoot + (queryString2.charAt(0) == '/' ? queryString2 : ("/" + queryString2));
				String titulo = res.getString(R.string.dirListing)  + " " + realPath;
				File desiredFile = new File(realPath);
				listing.append(this.beginHTML(titulo));
				
				// Loop principal
				File [] conteudo = desiredFile.listFiles();
				listing.append("<ul>");
				if (desiredFile.getAbsolutePath().length() > servico.webRoot.length()) {
					listing.append("<li><a href='"
							+ removeSdcard(getParent(desiredFile)) + "'>" + "["+ res.getString(R.string.voltar) + "]" + "</a>");
					listing.append("</li>");
				}
				for (int x = 0; x < conteudo.length; x++) {
					if (!conteudo[x].isHidden()) {
						listing.append("<li><a href='"
								+ removeSdcard(conteudo[x].getAbsolutePath()) + "'>" + removeSdcard(conteudo[x].getPath()) + "</a>");
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
				// Retirar o webroot
				String webRoot = servico.webRoot;
				int webRootLen = webRoot.length();
				
				if (path.length() > webRoot.length()) {
					return path.substring(webRootLen);
				}
				else {
					return "/";
				}
			}
			
			private boolean isDirectory(String queryString2) {
				boolean resultado = false;
				String realPath = servico.webRoot + (queryString2.charAt(0) == '/' ? queryString2 : ("/" + queryString2));
				File desiredFile = new File(realPath);
				if (desiredFile.isDirectory()) {
					resultado = true;
					this.desiredFileDir = desiredFile.getAbsolutePath();
					this.isDirectoryListing = true;
				}
				else {
					this.contentLength = desiredFile.length();
				}
				return resultado;
			}

			private boolean fileExists(String queryString2) {
				boolean resultado = false;
				String realPath = servico.webRoot + (queryString2.charAt(0) == '/' ? queryString2 : ("/" + queryString2));
				File desiredFile = new File(realPath);
				if (desiredFile.exists()) {
					resultado = true;
					this.isDirectoryListing = false;
					this.desiredFileDir = desiredFile.getAbsolutePath();
				}
				return resultado;
			}
			
			private void getFileType(String path) {
				//"image/jpeg", "image/gif", "image/png", "text/html", "text/plain", "application/zip", "application/pdf" , "application/octet-stream"
				this.contentType = "application/octet-stream";
				int lastDot = path.lastIndexOf('.');
				if (lastDot > 0) {
					// Tem extensão
					if (lastDot < path.length()) {
						// o ponto não é o último caracter
						String extensao = path.substring(lastDot);
						if (extensao.equalsIgnoreCase(".JPEG") || extensao.equalsIgnoreCase(".JPG")) {
							this.contentType = "image/jpeg";
							return;
						}
						if (extensao.equalsIgnoreCase(".GIF")) {
							this.contentType = "image/gif";
							return;
						}
						if (extensao.equalsIgnoreCase(".PNG")) {
							this.contentType = "image/png";
							return;
						}
						if (extensao.equalsIgnoreCase(".HTML") || extensao.equalsIgnoreCase(".HTM")) {
							this.contentType = "text/html";
							return;
						}
						if (extensao.equalsIgnoreCase(".TXT")) {
							this.contentType = "text/plain";
							return;
						}
						if (extensao.equalsIgnoreCase(".ZIP")) {
							this.contentType = "application/zip";
							return;
						}
						if (extensao.equalsIgnoreCase(".PDF")) {
							this.contentType = "application/pdf";
							return;
						}
						
					}
				}
			}
		}
	}
}
