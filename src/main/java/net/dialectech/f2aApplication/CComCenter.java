package net.dialectech.f2aApplication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;

/**
 * CComMemoryは、UIスレッドとWorkerスレッドとの通信用オブジェクト。Singletonとして利用することにした。
 */
public class CComCenter {

	final static int INDEX_FOR_MONITOR = 0;
	final static int INDEX_FOR_MIC_OUTPUT = 1;

	private static CComCenter instance = new CComCenter();

	@Getter
	@Setter
	private boolean ptt;

	@Getter
	@Setter
	private boolean breakInMode;

	// Monitor用のキー状態把握変数
	@Setter
	private boolean keyDownDetected;
	@Setter
	private boolean keyUpDetected;
	@Setter
	@Getter
	private boolean keydown;
	@Setter
	@Getter
	private boolean keyUp;

	// 送信音のキー状態把握変数
	@Setter
	private boolean lateKeyDownDetected;
	@Setter
	private boolean lateKeyUpDetected;
	@Setter
	@Getter
	private boolean lateKeydown;
	@Setter
	@Getter
	private boolean lateKeyUp;

	// CW Event Timing Records
	public final int TIME_STAMP_VOL = 128;
	public long timeStamp[] = new long[TIME_STAMP_VOL];
	public EKeyStat keyStat[] = new EKeyStat[TIME_STAMP_VOL];
	public int PointerOfTimeStamp;

	@Getter
	@Setter
	LinkedHashMap<String, Mixer> mixerMap;
	@Getter
	LinkedHashMap<String, Integer> rigsAddress;
	@Getter
	LinkedList<String> rigList;
	@Getter
	LinkedHashMap<String, Integer> frequencyMap ;
	
	@Getter
	Info[] mixerInfos;

	@Getter
	@Setter
	private Mixer selectedMixer;
	@Getter
	@Setter
	private double monitorVolume;
	@Getter
	@Setter
	private double micVolume;

	@Getter
	@Setter
	private long releaseDelay;
	@Getter
	@Setter
	private long atackDelay;

	@Getter
	@Setter
	private CSendReceiveController sendReceiveController;

	@Getter
	@Setter
	private CKeyHandler keyHandler;

	@Getter
	@Setter
	private ArrayList<CToneGenerator> toneGeneratorArray = new ArrayList<CToneGenerator>();

	@Getter
	@Setter
	long timer;

	public final int SAMPLE_RATE = 44100; // サンプリングレート

	private CComCenter() {
		rigsAddress = new LinkedHashMap<String, Integer>();
		rigsAddress.put("IC-9700（Hz）", 0xA2);
		rigsAddress.put("ID-52（Hz）", 0xA6);

		frequencyMap = new LinkedHashMap<String, Integer>();
		frequencyMap.put("安全・安定感UP（174 Hz）",174 );
		frequencyMap.put("催淫・陶酔効果（256 Hz）",256 );
		frequencyMap.put("細胞組織再生促進・記憶力UP（285 Hz）",285 );
		frequencyMap.put("恐怖からの解放（396 Hz）",396 );
		frequencyMap.put("変化・転換促進、マイナス思考からの解放（417 Hz）",417 );
		frequencyMap.put("DNA修復・精神啓発（528 Hz）",528 );
		frequencyMap.put("人間関係の向上（639 Hz）",639 );
		frequencyMap.put("直感力・想像力UP（741 Hz）",741 );
		frequencyMap.put("松果体の活性・霊性UP（852 Hz）",852 );
		frequencyMap.put("宇宙と結合（963 Hz）",963 );
		frequencyMap.put("10Hz毎",-1 );
		
		rigList = new LinkedList<String>();
		for (Entry<String, Integer> rig : rigsAddress.entrySet()) {
			rigList.add(rig.getKey());
		}

		// keyHistory reset
		PointerOfTimeStamp = 0;
		timeStamp[PointerOfTimeStamp] = 0;
		keyStat[PointerOfTimeStamp] = EKeyStat.KeyNull;

		mixerMap = new LinkedHashMap<String, Mixer>();
		System.out.println("=== Output Devices ===");

		mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info mixerInfo : mixerInfos) {
			if (mixerInfo.getName().contains("Port"))
				continue;
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			Line.Info[] targetLineInfos = mixer.getSourceLineInfo(); // 出力ライン
			if (targetLineInfos.length > 0) {
				mixerMap.put(mixerInfo.getName(), mixer);
			}
		}
	}

	public LinkedList<String> getFrequencyNameList() {
		LinkedList<String> res = new LinkedList<String>();
		for (Entry<String, Integer> freq : frequencyMap.entrySet()) {
			res.add(freq.getKey());
		}
		return res;
	}

	public LinkedList<String> getAudioDeviceNameList() {
		LinkedList<String> res = new LinkedList<String>();
		for (Entry<String, Mixer> mixer : mixerMap.entrySet()) {
			res.add(mixer.getKey());
		}
		return res;
	}

	public static CComCenter getInstance() {
		return instance;
	}

	public boolean isKeyDownDetected() {
		if (keyDownDetected == false) {
			return false;
		}
		keyDownDetected = false;
		return true;
	}

	public boolean isKeyUpDetected() {
		if (keyUpDetected == false) {
			return false;
		}
		keyUpDetected = false;
		return true;
	}

	public boolean isLateKeyDownDetected() {
		if (lateKeyDownDetected == false) {
			return false;
		}
		lateKeyDownDetected = false;
		return true;
	}

	public boolean isLateKeyUpDetected() {
		if (lateKeyUpDetected == false) {
			return false;
		}
		lateKeyUpDetected = false;
		return true;
	}

	public Mixer getPresentMonitorMixer(int index) {
		return AudioSystem.getMixer(null);
	}

	public Mixer getPresentMicMixer() {
		if (selectedMixer == null)
			selectedMixer = AudioSystem.getMixer(null);
		return selectedMixer;
	}

	public synchronized void addNewTimeStamp(long timeStamp2Store, EKeyStat status) {
		// 重複検出して、前回のものと同じKeyStatを登録しようとしているのであれば、エラーなので、登録しない。
		int checkPoint = PointerOfTimeStamp;
		if (checkPoint == 0 ) {
			if (keyStat[0] == EKeyStat.KeyNull)
				checkPoint = TIME_STAMP_VOL - 1;
		} else if (checkPoint != 0) {
			checkPoint--;
		}

		if (status == keyStat[checkPoint])
			return;

		// System.out.println(">> " + PointerOfTimeStamp);
		// 登録作業
		timeStamp[PointerOfTimeStamp] = timeStamp2Store;
		keyStat[PointerOfTimeStamp++] = status;
		if (PointerOfTimeStamp >= TIME_STAMP_VOL)
			PointerOfTimeStamp = 0;
		keyStat[PointerOfTimeStamp] = EKeyStat.KeyNull;
		// System.out.println("ADD NEW TIME-STAMP : " + timeStamp2Store + " as " +
		// status.toString());
	}

	public void addToneGenerator(CToneGenerator tg) {
		toneGeneratorArray.add(tg);
	}

	public CToneGenerator getToneGeneratorOf(int index) {
		if (index >= toneGeneratorArray.size()) {
			return null;
		}
		return toneGeneratorArray.get(index);
	}

	public void closeAllAudioDevice() {
		if (toneGeneratorArray == null)
			return;
		for (CToneGenerator tg : toneGeneratorArray) {
			tg.closeToneGenerator();
		}
	}

	public void reOpenKeyHandler(String keyPort) {
		if (keyHandler == null) {
			Platform.runLater(() -> {

			});
		} else {
			keyHandler.reOpen(keyPort);

		}
	}

	public void reOpenAllDevices(double frequency, String afPortName, double atackDelay, double releaseDelay,
			double micVolume, double monitorVolume) {
		if (toneGeneratorArray == null)
			return;
		for (int index = 0; index < toneGeneratorArray.size(); index++) {
			CToneGenerator tg = toneGeneratorArray.get(index);
			Mixer targetMixer = null;
			switch (index) {
			case INDEX_FOR_MONITOR:
				// index=0のものは、モニター用
				targetMixer = AudioSystem.getMixer(null); // モニターにはデフォルトを設定。
				tg.closeToneGenerator();
				tg.reOpenToneGenerator((int) frequency, monitorVolume, targetMixer);
				break;
			case INDEX_FOR_MIC_OUTPUT:
				// index=1のものは、マイク投入用とする
				if (afPortName == null)
					targetMixer = AudioSystem.getMixer(null); // 未設定の場合にはデフォルトを設定。
				else
					targetMixer = mixerMap.get(afPortName);

				if (targetMixer == null) {
					targetMixer = AudioSystem.getMixer(null); // 異常があればデフォルトを設定。
				}
				tg.closeToneGenerator();
				tg.reOpenToneGenerator((int) frequency, micVolume, targetMixer, atackDelay);
				break;
			default:
				break;

			}
		}
		if (sendReceiveController != null)
			sendReceiveController.setReleaseDelayTime((long) releaseDelay);

	}

}
