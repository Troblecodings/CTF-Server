package io.github.troblecodings.ctf_server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

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
public class ServerApp extends Application implements Runnable {

	public static LoggerFile LOGGER;
	private static ServerSocket sslserver;
	public static HashMap<Socket, PrintWriter> sockets = new HashMap<>();
	public static ExecutorService service;
	public static Path path_plan = Paths.get("game_plans");
	public static Path path_history = Paths.get("match_history");
	private static Path path_log = Paths.get("logs");
	public static Image ICON = new Image(ServerApp.class.getResourceAsStream("Icon.png"));
	public static String SERVER_PW, MOTD;
	public static int MINS = 2;
	public static TextArea CONSOLE = new TextArea();
	public static GridPane root;
	public static FileServer server;
	private static String address;
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		Date date = new Date();
		if (!Files.exists(path_log))
			Files.createDirectory(path_log);
		LOGGER = new LoggerFile(new FileOutputStream(new File(path_log.toFile(),
				"log-" + date.getMonth() + "-" + date.getDay() + "-" + (1900 + date.getYear()) + "-" + date.getHours()
						+ "-" + date.getMinutes() + "-" + date.getSeconds() + ".log")));
		if (!Files.exists(path_plan))
			Files.createDirectory(path_plan);
		if (!Files.exists(path_history))
			Files.createDirectory(path_history);
		if (!Files.exists(SocketInput.STRIKES))
			Files.createFile(SocketInput.STRIKES);
		if (!Files.exists(SocketInput.BANS))
			Files.createFile(SocketInput.BANS);
		try(final DatagramSocket socket = new DatagramSocket()){
			  socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			  LOGGER.println(address = socket.getLocalAddress().getHostAddress().toString());
		}
		server = new FileServer(Paths.get("app.apk"), "/app/");
		launch(args);
	}

	public static void sendToAll(String nm) {
		sockets.forEach((sk, wr) -> {
			wr.println(nm);
			wr.println();
			wr.flush();
			LOGGER.println("Sendet " + sk + " " + nm);
		});
	}
	
	private void showQR(String str, String tit,int y)
	{
		Stage qrcode = new Stage();
		qrcode.setTitle(tit);
		qrcode.getIcons().add(ServerApp.ICON);
		qrcode.setX(0);
		qrcode.setY(250 * y);
		qrcode.setResizable(false);
		qrcode.initModality(Modality.NONE);
		StackPane qrpane = new StackPane();
		QRCodeWriter qrwr = new QRCodeWriter();
		BitMatrix matrix = null;
		try {
			matrix = qrwr.encode(str, BarcodeFormat.QR_CODE, 200, 200);
		} catch (WriterException e) {
			e.printStackTrace();
		}
		WritableImage image = new WritableImage(200, 200);
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 200; j++) {
				if(matrix.get(i, j))
				image.getPixelWriter().setColor(i, j, Color.BLACK);
			}
		}
		qrpane.getChildren().add(new ImageView(image));
		qrcode.setScene(new Scene(qrpane, 300, 200));
		qrcode.show();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void start(Stage primaryStage) throws Exception {
		Thread th = new Thread(this);
		LOGGER.println("Set networking config!");
			
		showQR("http://" + address + ":333/app/", "App download", 0);
		
		Dialog<Pair<String, String>> config = new Dialog<>();
		config.setTitle("Server config!");
		config.initModality(Modality.NONE);
		GridPane configpane = new GridPane();

		configpane.add(new Label("Port"), 0, 0);
		configpane.add(new Label("Password"), 0, 1);
		configpane.add(new Label("Time in min"), 0, 2);
		configpane.add(new Label("Motd"), 0, 3);

		Numberfield port = new Numberfield("555");
		Numberfield time = new Numberfield("2");
		TextField motd = new TextField("Welcome to the server!%nWait for match!");
		PasswordField pw = new PasswordField();
		
		configpane.add(port, 1, 0);
		configpane.add(pw, 1, 1);
		configpane.add(time, 1, 2);
		configpane.add(motd, 1, 3);
		configpane.setVgap(15);
		configpane.setHgap(15);
		configpane.setPadding(new Insets(15));

		config.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
		config.getDialogPane().setContent(configpane);
		config.setResultConverter(tp -> {
			if (tp == ButtonType.APPLY)
				return new Pair<String, String>(port.getText(), pw.getText());
			if(tp == ButtonType.CANCEL) {
				LOGGER.println("ERROR! Not configured! Exiting!");
				System.exit(-1);
			}
			return null;
		});
		((Stage) config.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
		config.showAndWait().ifPresent(str -> {
			try {
				PORT = Integer.valueOf(str.getKey());
				MINS = Integer.valueOf(time.getText());
				MOTD = motd.getText();
			} catch (Throwable t) {
				LOGGER.println("Wrong port configuration!");
			}
			LOGGER.println("Running on port " + PORT);
			if (str.getValue().isEmpty()) {
				LOGGER.println("ATTENTION! No admin password given!");
			} else {
				SERVER_PW = str.getValue();
			}
			// Starting server
			th.start();
		});
		
		showQR(address + "\n" + PORT + "\n" + SERVER_PW + "\n0", "Match 0 setup", 1);
		showQR(address + "\n" + PORT + "\n" + SERVER_PW + "\n1", "Match 1 setup", 2);

		root = new GridPane();
		Scene sc = new Scene(root, 1000, 650);
		primaryStage.setScene(sc);
		primaryStage.setTitle("CTF Server App");
		primaryStage.setResizable(false);
		primaryStage.getIcons().add(ICON);
		primaryStage.setOnCloseRequest(ev -> {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setHeaderText("Shutdown?");
			alert.setContentText("Would your really like to shutdown the server?");
			alert.setTitle("Close?");
			alert.getButtonTypes().addAll(ButtonType.CANCEL);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
			Optional<ButtonType> btn = alert.showAndWait();
			if (btn.isPresent() && btn.get() == ButtonType.OK) {
				try {
					server.stop();
					th.stop();
					ServerApp.sslserver.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.exit(0);
			} else {
				ev.consume();
			}
		});
		root.setVgap(15);
		root.setHgap(15);

		GridPane players = new GridPane();
		players.setHgap(15);
		players.setVgap(15);
		players.setPrefSize(485, 285);
		players.setPadding(new Insets(15));
		for (int y = 1; y < 5; y++) {
			players.add(new Label("Player " + y), 0, y);
		}
		for (int x = 1; x < 3; x++) {
			players.add(new Label("Team " + (x == 1 ? "Red" : "Blue")), x, 0);
			for (int y = 1; y < 5; y++) {
				players.add(new PlayerField(x == 1, y), x, y);
			}
		}

		Button setup = new Button("New");
		setup.setOnAction(ev -> {
			Dialog<Pair<String, String>> alert = new Dialog<>();
			alert.setTitle("Create new match");
			GridPane pane = new GridPane();

			pane.add(new Label("Red teamname"), 0, 0);
			pane.add(new Label("Blue teamname"), 0, 1);

			TextField red = new TextField("teamname");
			TextField blue = new TextField("teamname");
			pane.add(red, 1, 0);
			pane.add(blue, 1, 1);
			pane.setVgap(15);
			pane.setHgap(15);
			pane.setPadding(new Insets(15));
			ButtonType type = new ButtonType("Create");
			alert.getDialogPane().getButtonTypes().addAll(type, ButtonType.CANCEL);
			alert.getDialogPane().setContent(pane);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
			alert.setResultConverter(tp -> {
				if (tp == type)
					return new Pair<String, String>(red.getText(), blue.getText());
				return null;
			});
			alert.showAndWait().ifPresent(pr -> {
				JSONObject obj = new JSONObject();
				JSONObject jred = new JSONObject();
				jred.put("name", pr.getKey());
				JSONArray rarray = new JSONArray();
				players.getChildren().filtered(pd -> {
					return pd instanceof PlayerField && ((PlayerField) pd).isRed();
				}).forEach(nxt -> {
					rarray.put(((PlayerField) nxt).getText());
				});
				jred.put("players", rarray);
				JSONObject jblue = new JSONObject();
				JSONArray barray = new JSONArray();
				players.getChildren().filtered(pd -> {
					return pd instanceof PlayerField && !((PlayerField) pd).isRed();
				}).forEach(nxt -> {
					barray.put(((PlayerField) nxt).getText());
				});
				jblue.put("players", barray);
				jblue.put("name", pr.getValue());

				obj.put("1", jred);
				obj.put("2", jblue);
				try {
					PrintWriter writer = new PrintWriter(
							new File(path_plan.toFile(), pr.getKey() + " vs " + pr.getValue() + ".json"));
					obj.write(writer);
					writer.flush();
					writer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			;
		});
		players.add(setup, 0, 5);

		root.add(players, 0, 0);
		MatchPane matchpane = new MatchPane(0);
		matchpane.setHgap(15);
		matchpane.setVgap(15);
		matchpane.setPrefSize(485, 285);
		matchpane.setPadding(new Insets(15));
		root.add(matchpane, 0, 1);

		MatchPane matchpane2 = new MatchPane(1);
		matchpane2.setHgap(15);
		matchpane2.setVgap(15);
		matchpane2.setPrefSize(485, 285);
		matchpane2.setPadding(new Insets(15));
		root.add(matchpane2, 1, 1);
		
		CONSOLE.setEditable(false);
		root.add(CONSOLE, 1, 0);

		primaryStage.show();
	}

	private static int PORT = 555;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			sslserver = new ServerSocket(PORT);
			service = Executors.newCachedThreadPool();
			LOGGER.println("Started server!");
			while (true) {
				Socket sk = sslserver.accept();
				LOGGER.println(sk + " connected to server");
				sockets.put(sk, new PrintWriter(sk.getOutputStream()));
				service.submit(new SocketInput(sk));
			}
		} catch (Exception e) {
			e.printStackTrace(LOGGER);
		}
	}

}
