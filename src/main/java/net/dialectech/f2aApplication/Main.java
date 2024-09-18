package net.dialectech.f2aApplication;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
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

			CCwManager cwm = new CCwManager(controller);
			CComMemory comMem = CComMemory.getInstance();
			comMem.setCwManager(cwm);
			cwm.setOnSucceeded((value) -> {
				System.out.println("CCwManager Secceeded! " + value.getEventType().toString());
				cwm.restart();
			});
			cwm.setOnCancelled((value) -> {
				System.out.println("CCwManager Cancelled! and Restarts.");
				cwm.restart();
			});
			cwm.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		launch(args);
	}
}
