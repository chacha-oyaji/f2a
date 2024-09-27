package net.dialectech.f2aApplication;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {
	static CUIController controller = null;

	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("UIController.fxml"));
			BorderPane root = (BorderPane) fxmlLoader.load();
			// BorderPane root =
			// (BorderPane)FXMLLoader.load(getClass().getResource("Sample.fxml"));
			Scene scene = new Scene(root, 680, 460);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			primaryStage.setScene(scene);
			primaryStage.setTitle("F2A Generator by JI1BXM");
			primaryStage.show();

			controller = fxmlLoader.getController();

			CComCenter comMem = CComCenter.getInstance();
			CToneGenerator tg1 = new CToneGenerator();
			comMem.addToneGenerator(tg1);
			tg1.setOnSucceeded((value) -> {
				System.out.println("TG1 : CSendReceiveController Secceeded! " + value.getEventType().toString());
				tg1.restart();
			});
			tg1.setOnCancelled((value) -> {
				System.out.println("TG1 : CSendReceiveController Cancelled! and Restarts.");
				tg1.restart();
			});
			tg1.start();

			CSendReceiveController srController = new CSendReceiveController(controller);
			comMem.setSendReceiveController(srController);
			srController.setOnSucceeded((value) -> {
				System.out.println("CSendReceiveController Secceeded! " + value.getEventType().toString());
				srController.restart();
			});
			srController.setOnCancelled((value) -> {
				System.out.println("CSendReceiveController Cancelled! and Restarts.");
				srController.restart();
			});
			srController.start();

			CKeyHandler keyHandler = new CKeyHandler();
			comMem.setKeyHandler(keyHandler);
			keyHandler.setOnSucceeded((value) -> {
				System.out.println("CKeyHandler Secceeded! " + value.getEventType().toString());
				keyHandler.restart();
			});
			keyHandler.setOnCancelled((value) -> {
				System.out.println("CKeyHandler Cancelled! and Restarts.");
				keyHandler.restart();
			});
			keyHandler.start();

			CToneGenerator tg2 = new CToneGenerator();
			comMem.addToneGenerator(tg2);
			tg2.setOnSucceeded((value) -> {
				System.out.println("TG2 : CSendReceiveController Secceeded! " + value.getEventType().toString());
				tg2.restart();
			});
			tg2.setOnCancelled((value) -> {
				System.out.println("TG2 : CSendReceiveController Cancelled! and Restarts.");
				tg2.restart();
			});
			tg2.start();
			/*
			*/

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
