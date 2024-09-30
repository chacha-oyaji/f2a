package net.dialectech.f2aApplication;

import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
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
	CComCenter comCenter = CComCenter.getInstance();
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

	@FXML // fx:id="dbComPort4Rig"
	private ChoiceBox<String> dbComPort4Rig; // Value injected by FXMLLoader

	@FXML // fx:id="dbComPort4KeyCDC"
	private ChoiceBox<String> dbComPort4KeyCDC;

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
	private ChoiceBox<String> dbPrimaryToneSelection;

	@FXML
	public ChoiceBox<String> dbTransmitterDestination;

    @FXML
    private ChoiceBox<String> dbToneEffectSelection;
    
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

	public void dispSendReceive() {
		if (dbComPort4Rig.getValue() == null) {
			idStatusMessage.setText("COM PORT NOT SPECIFIED.");
			return;
		}
		if (dbTransmitterDestination.getValue() == null) {
			idStatusMessage.setText("RIG NOT SPECIFIED.");
			return;
		}
		if (comCenter.isPtt()) {
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
		assert dbComPort4Rig != null : "fx:id=\"dbComPort\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbAfVolume != null : "fx:id=\"sbAfVolume\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbAtackDelay != null : "fx:id=\"sbAtackDelay\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbReleaseDelay != null
				: "fx:id=\"sbReleaseDelay\" was not injected: check your FXML file 'Sample.fxml'.";
		assert sbToneFrequency != null
				: "fx:id=\"sbToneFrequency\" was not injected: check your FXML file 'Sample.fxml'.";

		sbAfVolume.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblMicOutputVolume.setText(String.valueOf((int) new_Val.doubleValue()) + " %");
			comCenter.setMicVolume(new_Val.doubleValue());
			comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
//			if (comCenter != null && comCenter.getSendReceiveController() != null)
//				comCenter.getSendReceiveController().cancel();
		});
		sbMonitorVolume.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblMonitorVolume.setText(String.valueOf((int) new_Val.doubleValue()) + " %");
			comCenter.setMonitorVolume(new_Val.doubleValue());
			comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
//			if (comCenter != null && comCenter.getSendReceiveController() != null)
//				comCenter.getSendReceiveController().cancel();
		});
		sbAtackDelay.valueProperty().addListener((ov, old_val, new_Val) -> {
			lblAtackDelay.setText(String.valueOf((int) new_Val.doubleValue()) + " mS");
			comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
		});

		cbBreakIn.selectedProperty().addListener((ov, old_val, new_Val) -> {
			comCenter.setBreakInMode(new_Val.booleanValue());
		});

		dbAudioPort.valueProperty().addListener((ov, old_val, new_Val) -> {
			System.out.println("CHANGED TO " + new_Val);
			String target = dbAudioPort.getValue();
			Mixer mixer = comCenter.mixerMap.get(target);
			if (mixer == null)
				mixer = AudioSystem.getMixer(null);
			comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
			// comCenter.getToneGeneratorArray().get(1).setMixer(mixer);
//			if (comCenter != null && comCenter.getSendReceiveController() != null)
//				comCenter.getSendReceiveController().cancel();
		});

		dbComPort4KeyCDC.valueProperty().addListener((ov, old_val, new_Val) -> {
			System.out.println("KEY PORT is CHANGED TO " + new_Val);
			String target = dbComPort4KeyCDC.getValue();

			comCenter.reOpenKeyHandler(new_Val);
		});

		sbToneFrequency.valueProperty().addListener((ov, old_val, new_Val) -> {
			int primaryFreqData ;
			if (dbPrimaryToneSelection.getValue()==null) {
				primaryFreqData = -1; 
			}
			else {
				primaryFreqData = comCenter.frequencyMap.get(dbPrimaryToneSelection.getValue());
			}
			// このスライドバーを使えるのは、primaryFreqDataが「10Hz毎」になっているときのみ
			if (primaryFreqData == -1) {
				double newData = (((int) new_Val.doubleValue()) / 10) * 10.0;
				sbToneFrequency.setValue(newData);
				lblToneFrequency.setText(String.valueOf((int) newData) + " Hz");
				comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
						sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
			}
		});
		sbAtackDelay.valueProperty().addListener((ov, old_val, new_Val) -> {
			double newData = (((int) new_Val.doubleValue()) / 100) * 100.0;
			sbAtackDelay.setValue(newData);
			lblAtackDelay.setText(String.valueOf((int) newData) + " mS");
			comCenter.setAtackDelay((long) new_Val.doubleValue());
			comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
		});
		sbReleaseDelay.valueProperty().addListener((ov, old_val, new_Val) -> {
			double newData = (((int) new_Val.doubleValue()) / 100) * 100.0;
			sbReleaseDelay.setValue(newData);
			lblReleaseDelay.setText(String.valueOf((int) newData) + " mS");
			comCenter.setReleaseDelay((long) new_Val.doubleValue());
			comCenter.reOpenAllDevices(presentSpecifiedFrequency(), dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
		});
		dbPrimaryToneSelection.valueProperty().addListener((ov, old_val, new_Val) -> {
			int newData = presentSpecifiedFrequency();
			lblToneFrequency.setText(String.valueOf((int) newData) + " Hz");
			comCenter.reOpenAllDevices(newData, dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
		});
		dbToneEffectSelection.valueProperty().addListener((ov, old_val, new_Val) -> {
			comCenter.setToneEffect(new_Val);
			int newData = presentSpecifiedFrequency();
			comCenter.reOpenAllDevices(newData, dbAudioPort.getValue(), sbAtackDelay.getValue(),
					sbReleaseDelay.getValue(), sbAfVolume.getValue(), sbMonitorVolume.getValue());
		});

		sbAfVolume.setMax(100.0);
		sbAfVolume.setMin(0.0);
		sbAfVolume.setValue(18.0);

		sbMonitorVolume.setMax(100.0);
		sbMonitorVolume.setMin(0.0);
		sbMonitorVolume.setValue(10.0);

		sbAtackDelay.setMax(2000.0);
		sbAtackDelay.setMin(0.0);
		sbAtackDelay.setValue(700.0);

		sbReleaseDelay.setMax(5000.0);
		sbReleaseDelay.setMin(500.0);
		sbReleaseDelay.setValue(3000.0);

		sbToneFrequency.setMax(2000.0);
		sbToneFrequency.setMin(400.0);
		sbToneFrequency.setValue(700.0);

		dbAudioPort.getItems().addAll(comCenter.getAudioDeviceNameList());


		dbComPort4Rig.getItems().addAll(comCenter.getComPortList());
		dbComPort4KeyCDC.getItems().addAll(comCenter.getComPortList());
		dbPrimaryToneSelection.getItems().addAll(comCenter.getFrequencyNameList());
		dbToneEffectSelection.getItems().addAll(comCenter.getToneEffectList());
		dbTransmitterDestination.getItems().addAll(comCenter.getRigList());
	}

	@FXML
	void onBtnSendReceiveClicked(MouseEvent event) {
		if (comCenter.isPtt()) {
			comCenter.setPtt(false);
		} else {
			comCenter.setPtt(true);
		}
		dispSendReceive();
	}

	@FXML
	void onKeyPressed(KeyEvent event) {
		comCenter.addNewTimeStamp(System.currentTimeMillis(), EKeyStat.KEY_PRESSED);
	}

	@FXML
	void onKeyReleased(KeyEvent event) {
		comCenter.addNewTimeStamp(System.currentTimeMillis(), EKeyStat.KEY_RELEASED);
	}

	@FXML
	void onSettlementChanged(MouseEvent event) {
		System.out.print("*");
	}

	public int presentSpecifiedFrequency() {
		int res ;
		if ( dbPrimaryToneSelection.getValue() == null )  
			res = -1 ;
		else 
			res = comCenter.frequencyMap.get(dbPrimaryToneSelection.getValue());
		if (res == -1) {
			sbToneFrequency.disableProperty().set(false);			
			res = (int) sbToneFrequency.getValue();
		}
		else {
			sbToneFrequency.disableProperty().set(true);
		}
		return res ;
	}
	
	public String selectedAudioChannel() {
		return dbAudioPort.getValue();
	}

	public String selectedComPort4KeyCDC() {
		return dbComPort4KeyCDC.getValue();
	}

	public String selectedComPort4Rig() {
		return dbComPort4Rig.getValue();
	}
	
	public String selectedRig() {
		return dbTransmitterDestination.getValue();
	}

	public String selectedToneEffect() {
		return dbToneEffectSelection.getValue();
	}

}
