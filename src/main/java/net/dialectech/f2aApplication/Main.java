package net.dialectech.f2aApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Properties;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {
	static CUIController controller = null;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			// Propertyの方を先に処理

			// ここからjavaFX特有

			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("UIController.fxml"));
			BorderPane root = (BorderPane) fxmlLoader.load();
			// BorderPane root =
			// (BorderPane)FXMLLoader.load(getClass().getResource("Sample.fxml"));
			Scene scene = new Scene(root, 680, 450);
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
			 * アイコン表示
			*/
			Image icon = new Image(this.getClass().getResourceAsStream("/net/dialectech/f2aApplication/BXMLAB.bmp"));
			primaryStage.getIcons().add(icon);
			Properties prop = new Properties();
			String propertyFileName = System.getProperty("user.home") + File.separator + "." + System.getProperty("user.name") + File.separator + "f2A.properties" ;
			System.out.println(propertyFileName);
			try (InputStream fs = new FileInputStream(propertyFileName);) {
				prop.loadFromXML(fs);
				controller.setSelectedComPort4KeyCDC( prop.getProperty("selectedComPort4KeyCDC")); 
				controller.setSelectedAudioChannel(prop.getProperty("selectedAudioChannel")) ;
				controller.setSelectedComPort4Rig(prop.getProperty("selectedComPort4Rig")) ;
				controller.setSelectedRig(prop.getProperty("selectedRig")) ;
				controller.setSelectedMonitorPort(prop.getProperty("selectedMonitorPort")) ;
				controller.setSelectedToneEffectSelection(prop.getProperty("selectedToneEffectSelection")) ;
				controller.setSelectedPrimaryToneSelection(prop.getProperty("selectedPrimaryToneSelection")) ;
				
				if (prop.getProperty("atackDelay") != "")
					controller.setSelectedAtackDelay(Double.parseDouble(prop.getProperty("atackDelay"))) ;
				if (prop.getProperty("releaseDelay") != "")
					controller.setSelectedReleaseDelay(Double.parseDouble(prop.getProperty("releaseDelay"))) ;
				if (prop.getProperty("monitorVolume") != "")
					controller.setSelectedMonitorVolume(Double.parseDouble(prop.getProperty("monitorVolume"))) ;
				if (prop.getProperty("micVolume") != "")
					controller.setSelectedMicVolume(Double.parseDouble(prop.getProperty("micVolume"))) ;
				if (prop.getProperty("toneFrequency") != "")
					controller.setSelectedFreeToneFrequency(Double.parseDouble(prop.getProperty("toneFrequency"))) ;
			}
			catch (Exception e) {
				
			}
			
			primaryStage.setOnCloseRequest(event -> {
				tg1.closeToneGenerator();
				tg2.closeToneGenerator();
				// Propertiesを記録して終了
				Properties props = new Properties();
				String propertyFileName4Output = System.getProperty("user.home") + File.separator + "." + System.getProperty("user.name") + File.separator + "f2A.properties" ;
				try {
					// Fileがなかったのときのことを考慮して、touchしておく。
					touch(Objects.requireNonNull(new File(propertyFileName4Output), "file").toPath()) ;
					
					// あとは、各設定パラメータを永続化。
					try (OutputStream fs = new FileOutputStream(propertyFileName);){
						String data = controller.selectedComPort4KeyCDC();
						props.setProperty("selectedComPort4KeyCDC", rejectNull(data));
						props.setProperty("selectedAudioChannel",rejectNull(controller.selectedAudioChannel())) ;
						props.setProperty("selectedComPort4Rig",rejectNull(controller.selectedComPort4Rig())) ;
						props.setProperty("selectedRig",rejectNull(controller.selectedRig())) ;
						props.setProperty("selectedToneEffectSelection",rejectNull(controller.selectedToneEffectSelection())) ;
						props.setProperty("atackDelay",String.valueOf(controller.sbAtackDelay.getValue())) ;
						props.setProperty("releaseDelay",String.valueOf(controller.sbReleaseDelay.getValue())) ;
						props.setProperty("monitorVolume",String.valueOf(controller.sbMonitorVolume.getValue())) ;
						props.setProperty("micVolume",String.valueOf(controller.sbMicVolume.getValue())) ;
						props.setProperty("toneFrequency",String.valueOf(controller.sbToneFrequency.getValue())) ;
						props.setProperty("selectedMonitorPort", rejectNull(controller.selectedMonitorPort()));
						props.setProperty("selectedPrimaryToneSelection",rejectNull(controller.selectedPrimaryToneSelection())) ;
						props.storeToXML(fs, "F2A Generator properties");
						
					} catch (Exception e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}

			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String rejectNull(String data) {
		if (data==null)
			return "";
		return data ;
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	
	//以下、４つの関数は、FileUtilsのうちtouch()を利用するのに必要な部分だけを抽出したもの。もちろん、FileUtilsを引っ張り込んでも可。
    private static Path getParent(final Path path) {
        return path == null ? null : path.getParent();
    }

    private static Path readIfSymbolicLink(final Path path) throws IOException {
        return path != null ? Files.isSymbolicLink(path) ? Files.readSymbolicLink(path) : path : null;
    }

    public static Path createParentDirectories(final Path path, final LinkOption linkOption, final FileAttribute<?>... attrs) throws IOException {
        Path parent = getParent(path);
        parent = linkOption == LinkOption.NOFOLLOW_LINKS ? parent : readIfSymbolicLink(parent);
        if (parent == null) {
            return null;
        }
        final boolean exists = linkOption == null ? Files.exists(parent) : Files.exists(parent, linkOption);
        return exists ? parent : Files.createDirectories(parent, attrs);
    }
    
    public static Path touch(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.exists(file)) {
            createParentDirectories(file, LinkOption.NOFOLLOW_LINKS);
            Files.createFile(file);
        } else {
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
        }
        return file;
    }

    
}
