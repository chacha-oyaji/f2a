package net.dialectech.f2aApplication;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class CToneGenerator extends Service<String> {
	// グローバル変数0

	CTGSupporter sup;
	private Mixer mixer;

	public CToneGenerator() {
		super();
		mixer = AudioSystem.getMixer(null); // オブジェクト生成時には、デフォルトのAudioSystemMixerを設定
		sup = new CTGSupporter(mixer);
	}

	@Override
	protected Task<String> createTask() {
		return sup;
	}

	// モニター用オーディオデバイス
	public void reOpenToneGenerator(int frequency, double volume, Mixer mixer) {
		sup.reOpenToneGenerator(frequency, volume, mixer);
	}

	// リグ接続用オーディオデバイス
	public void reOpenToneGenerator(int frequency, double micVolume, Mixer targetMixer, double atackDelay) {
		sup.reOpenToneGenerator(frequency, micVolume, targetMixer, atackDelay);
	}

	public void closeToneGenerator() {
		sup.closeToneGenerator();		
	}

}
