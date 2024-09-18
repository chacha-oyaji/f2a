package net.dialectech.f2aApplication;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

import lombok.Getter;
import lombok.Setter;

/**
 * CComMemoryは、UIスレッドとWorkerスレッドとの通信用オブジェクト。Singletonとして利用することにした。
 */
public class CComMemory {

	private static CComMemory instance = new CComMemory();

	@Getter
	@Setter
	private boolean ptt;

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

	@Getter
	@Setter
	LinkedHashMap<String, Mixer> mixerMap;
	@Getter
	LinkedHashMap<String, Integer> rigsAddress;
	@Getter
	LinkedList<String> rigList;

	@Getter
	Info[] mixerInfos;
	
	@Getter
	@Setter
	private Mixer selectedMixer ;
	@Getter
	@Setter
	private double monitorVolume ;
	@Getter
	@Setter
	private double micVolume ;
	
	@Getter
	@Setter
	private long releaseDelay;
	
	@Getter
	@Setter
	private CCwManager cwManager ;
	
	@Getter
	@Setter
	long timer;

	private CComMemory() {
		rigsAddress = new LinkedHashMap<String, Integer>();
		rigsAddress.put("IC-9700", 0xA2);
		rigsAddress.put("ID-52", 0xA6);

		rigList = new LinkedList<String>();
		for (Entry<String, Integer> rig : rigsAddress.entrySet()) {
			rigList.add(rig.getKey());
		}

		mixerMap = new LinkedHashMap<String, Mixer>();
		System.out.println("=== Output Devices ===");

		mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info mixerInfo : mixerInfos) {
			if (mixerInfo.getName().contains("Port"))
				continue;
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			Line.Info[] targetLineInfos = mixer.getSourceLineInfo(); // 出力ライン
			if (targetLineInfos.length>0) {
				System.out.println("Mixer Name: " + mixerInfo.getName());
				System.out.println("Description: " + mixerInfo.getDescription());
				System.out.println("Vendor: " + mixerInfo.getVendor());
				System.out.println("Version: " + mixerInfo.getVersion());
				System.out.println("-----------------------------------");
				mixerMap.put(mixerInfo.getName(),mixer);
			}
		}
	}

	public LinkedList<String> getAudioDeviceNameList() {
		LinkedList<String> res = new LinkedList<String>();
		for (Entry<String, Mixer> mixer : mixerMap.entrySet()) {
			res.add(mixer.getKey());
		}
		return res;
	}

	public static CComMemory getInstance() {
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

	public Mixer getPresentMonitorMixer() {
		return AudioSystem.getMixer(null);
	}

	public Mixer getPresentMicMixer() {
		if (selectedMixer==null)
			selectedMixer = AudioSystem.getMixer(null);
		return selectedMixer;
	}

}
