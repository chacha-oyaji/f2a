package net.dialectech.f2aApplication;

import java.net.URL;
import java.security.Provider.Service;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import lombok.Setter;

import com.fazecast.jSerialComm.SerialPort;

public class CUIController {

	private int intData;
	CComMemory comMem = CComMemory.getInstance();
	CToneGenerator toneGenerator = new CToneGenerator();
	@Setter
	private boolean waitForStartKeyOn = false; // キースタートまで待たせる

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML // fx:id="btnSendReceive"
	private Button btnSendReceive; // Value injected by FXMLLoader

	@FXML // fx:id="cbBreakIn"
	@Getter
	private CheckBox cbBreakIn; // Value injected by FXMLLoader

	@FXML // fx:id="dbUsbPort"
	private ChoiceBox<String> dbAudioPort; // Value injected by FXMLLoader

	@FXML // fx:id="dbComPort"
	private ChoiceBox<String> dbComPort; // Value injected by FXMLLoader

	@FXML // fx:id="sbAfVolume"
	public ScrollBar sbAfVolume; // Value injected by FXMLLoader

	@FXML // fx:id="sbMonitorVolume"
	public ScrollBar sbMonitorVolume; // Value injected by FXMLLoader

	@FXML // fx:id="sbAtackDelay"
	public ScrollBar sbAtackDelay; // Value injected by FXMLLoader

	@FXML // fx:id="sbReleaseDelay"
	public ScrollBar sbReleaseDelay; // Value injected by FXMLLoader

	@FXML // fx:id="sbToneFrequency"
	public ScrollBar sbToneFrequency; // Value injected by FXMLLoader

	@FXML
	public ChoiceBox<String> dbTransmitterDestination;

	@FXML
	private Label idStatusMessage;

	@FXML
	private Label lblAtackDelay;

	@FXML
	private Label lblMicOutputVolume;

	@FXML
	private Label lblMonitorVolume;

	@FXML
	private Label lblReleaseDelay;

	@FXML
	private Label lblToneFrequency;

	@FXML
	void onSettlementChanged(MouseEvent event) {
		System.out.print("*");
	}

	@FXML
	void onKeyPressed(KeyEvent event) {
		String target = dbAudioPort.getValue();
		Mixer mixer = comMem.mixerMap.get(target);
		// Mixerが選択されていない場合はデフォルトにする
		if (mixer == null)
			mixer = AudioSystem.getMixer(null);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// まずはモニター用トーンを即時発信開始
					if (comMem.isKeydown() == false) {
						comMem.setKeyDownDetected(true);
					}
					comMem.setKeydown(true);

					// 次に、送信音用トーンをAtack-Delay時間後に発信開始
					Thread.sleep((int) sbAtackDelay.getValue());
					if (comMem.isLateKeydown() == false) {
						comMem.setLateKeyDownDetected(true);
					}
					comMem.setLateKeydown(true);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}).start();
	}

	@FXML
	void onKeyReleased(KeyEvent event) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// まずはモニター用トーンを即時停止
					comMem.setKeydown(false);
					comMem.setKeyDownDetected(false);

					// 次に、送信音用トーンを(Atack-Delay時間-50mS)後に停止
					int releaseDelay = (((int) sbAtackDelay.getValue()) - 100) < 0 ? 0
							: (int) sbAtackDelay.getValue() - 100;
					Thread.sleep(releaseDelay);
					comMem.setLateKeydown(false);
					comMem.setLateKeyDownDetected(false);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}).start();
	}

	@FXML
	void onBtnSendReceiveClicked(MouseEvent event) {
		if (comMem.isPtt()) {
			comMem.setPtt(false);
		} else {
			comMem.setPtt(true);
		}
		dispSendReceive();
	}

	public void dispSendReceive() {
		// TODO 自動生成されたメソッド・スタブ
		if (dbComPort.getValue() == null) {
			idStatusMessage.setText("COM PORT NOT SPECIFIED.");
			return;
		}
		if (dbTransmitterDestination.getValue() == null) {
			idStatusMessage.setText("RIG NOT SPECIFIED.");
			return;
		}
		if (comMem.isPtt()) {
			btnSendReceive.setStyle("-fx-background-color: #fcc;");
			idStatusMessage.setText("SENDING・・・");

		} else {
			btnSendReceive.setStyle(null);
			idStatusMessage.setText("RECEIVING・・・");
		}
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		assert btnSendReceive != null
				: "fx:id=\"btnSendReceive\" was not injected: check your FXML file 'Sample.fxml'.";
		assert cbBreakIn != null : "fx:id=\"cbBreakIn\" was not injected: check your FXML file 'Sample.fxml'.";
		assert dbAudioPort != null : "fx:id=\"dbUsbPort\" was not injected: check your FXML file 'Sample.fxml'.";
		assert dbComPort != null : "fx:id=\"dbComPort\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbAfVolume != null : "fx:id=\"sbAfVolume\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbAtackDelay != null : "fx:id=\"sbAtackDelay\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbReleaseDelay != null
				: "fx:id=\"sbReleaseDelay\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbToneFrequency != null
				: "fx:id=\"sbToneFrequency\" was not injected: check your FXML file 'Sample.fxml'.";

		sbAfVolume.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblMicOutputVolume.setText(String.valueOf((int) new_Val.doubleValue()) + " %");
			comMem.setMicVolume(new_Val.doubleValue());
			if (comMem != null && comMem.getCwManager() != null)
				comMem.getCwManager().cancel();
		});
		sbMonitorVolume.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblMonitorVolume.setText(String.valueOf((int) new_Val.doubleValue()) + " %");
			comMem.setMonitorVolume(new_Val.doubleValue());
			if (comMem != null && comMem.getCwManager() != null)
				comMem.getCwManager().cancel();
		});
		sbAtackDelay.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblAtackDelay.setText(String.valueOf((int) new_Val.doubleValue()) + " mS");
		});
		sbToneFrequency.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblToneFrequency.setText(String.valueOf((int) new_Val.doubleValue()) + " Hz");
		});
		dbAudioPort.valueProperty().addListener((ov, old_val, new_Val) -> {
			System.out.println("CHANGED TO " + new_Val);
			comMem.setSelectedMixer(comMem.getMixerMap().get(new_Val));
			if (comMem != null && comMem.getCwManager() != null)
				comMem.getCwManager().cancel();
		});
		sbToneFrequency.valueProperty().addListener((ov, old_val, new_Val) -> {
			double newData = (((int) new_Val.doubleValue()) / 10) * 10.0;
			sbToneFrequency.setValue(newData);
			if (comMem != null && comMem.getCwManager() != null)
				comMem.getCwManager().cancel();
		});
		sbReleaseDelay.valueProperty().addListener((ov, old_val, new_Val) -> {
			double newData = (((int) new_Val.doubleValue()) / 100) * 100.0;
			sbReleaseDelay.setValue(newData);
			lblReleaseDelay.setText(String.valueOf((int) newData) + " mS");
			comMem.setReleaseDelay((long) new_Val.doubleValue());
		});

		sbAfVolume.setMax(100.0);
		sbAfVolume.setMin(10.0);
		sbAfVolume.setValue(30.0);

		sbMonitorVolume.setMax(100.0);
		sbMonitorVolume.setMin(10.0);
		sbMonitorVolume.setValue(30.0);

		sbAtackDelay.setMax(1000.0);
		sbAtackDelay.setMin(0.0);
		sbAtackDelay.setValue(20.0);

		sbReleaseDelay.setMax(5000.0);
		sbReleaseDelay.setMin(500.0);
		sbReleaseDelay.setValue(2000.0);

		sbToneFrequency.setMax(2000.0);
		sbToneFrequency.setMin(400.0);
		sbToneFrequency.setValue(700.0);

		dbAudioPort.getItems().addAll(comMem.getAudioDeviceNameList());

		SerialPort[] ports = SerialPort.getCommPorts();
		LinkedList<String> comPortList = new LinkedList<String>();
		for (SerialPort port : ports) {
			// COMポートの名前を表示
			comPortList.add(port.getSystemPortName());
		}
		dbComPort.getItems().addAll(comPortList);

		dbTransmitterDestination.getItems().addAll(comMem.getRigList());
	}

	public String selectedComPort() {
		return dbComPort.getValue();
	}

	public String selectedAudioChannel() {
		return dbAudioPort.getValue();
	}

	public String selectedRig() {
		return dbTransmitterDestination.getValue();
	}

}
