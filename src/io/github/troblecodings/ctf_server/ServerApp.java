package io.github.troblecodings.ctf_server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.Application;
import javafx.stage.Stage;

/*-*****************************************************************************
 * Copyright 2018 MrTroble
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

/**
 * @author MrTroble
 *
 */
public class ServerApp extends Application implements Runnable{

	public static LoggerFile LOGGER;
	private static ServerSocket sslserver;
	public static ArrayList<Socket> sockets = new ArrayList<>();
	public static ExecutorService service;
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		Date date = new Date();
		LOGGER = new LoggerFile(new FileOutputStream(
				new File("log-" + date.getMonth() + "-" + date.getDay() + "-" + (1900 + date.getYear()) + "-"
						+ date.getHours() + "-" + date.getMinutes() + "-" + date.getSeconds() + ".log")));		
		launch(args);
	}
	
	public static void sendToAll(String nm) throws Exception {
		Iterator<Socket> it = sockets.iterator();
		while(it.hasNext()) {
			Socket sk = it.next();
			PrintWriter wr = new PrintWriter(sk.getOutputStream());
			wr.println(nm);
			wr.flush();
			LOGGER.println(sk + " sended to: " + nm);
		}
	}

	/* (non-Javadoc)
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		//Starting server
		Thread th = new Thread(this);
		th.start();
		
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run(){
		try {
			sslserver = new ServerSocket(555);
			service =  Executors.newCachedThreadPool();
			LOGGER.println("Started server!");
			while (true) {
				Socket sk = sslserver.accept();
				LOGGER.println(sk + " connected to server");
				sockets.add(sk);
				service.submit(new SocketInput(sk));
			}
		} catch (Exception e) {
			e.printStackTrace(LOGGER);
		}
	}

}