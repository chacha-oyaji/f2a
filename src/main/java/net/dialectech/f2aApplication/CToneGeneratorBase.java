package net.dialectech.f2aApplication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public abstract class CToneGeneratorBase {
	// グローバル変数0
	private final int SAMPLE_RATE = 44100; // サンプリングレート
	AudioFormat af;
	SourceDataLine sdl;
	byte[] byteBuffer;
	protected CComMemory comMem = CComMemory.getInstance();

	public CToneGeneratorBase() {
		super();
	}

	public void openToneGenerator(int frequency, double volume, Mixer mixer) {
		try {
			af = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);

			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, af);
			if (dataLineInfo != null) {
				sdl = (SourceDataLine) mixer.getLine(dataLineInfo);
			}

//			sdl = AudioSystem.getSourceDataLine(af);
			sdl.open(af);
			fillBufferBase(frequency, volume);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void closeToneGenerator() {
		if (sdl != null)
			sdl.close();
	}

	protected void fillBufferBase(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ
		int bufferSize = SAMPLE_RATE / frequency;
		byteBuffer = new byte[bufferSize * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int i = 0, index = 0; i < bufferSize; i++) {
			double angle = 2.0 * Math.PI * i / bufferSize;
			pointData = (short) (Math.sin(angle) * 32767 * volume / 100.0);
			byteBuffer[index++] = (byte) ((pointData >> 8) & 0xff);
			byteBuffer[index++] = (byte) (pointData & 0xff);
		}
	}

}
