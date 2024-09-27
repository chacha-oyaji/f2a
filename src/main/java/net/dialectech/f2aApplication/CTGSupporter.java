package net.dialectech.f2aApplication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import javafx.concurrent.Task;
import lombok.Setter;

public class CTGSupporter extends Task<String> {

	final long SYSTEM_DELAY_MS = 0 ;
	private AudioFormat af;
	private SourceDataLine sdl;
	protected CComCenter comCenter = CComCenter.getInstance();
	@Setter
	private Mixer mixer;
	@Setter
	private long atackDelayTime;

	byte[] byteBuffer; // 音声用バッファ

	private int pointer2ReadTiming;
	boolean keyOn = false;

	Thread coreToneGenerator = createNewSingleToneThread();

	public CTGSupporter(Mixer mixer) {
		super();
		this.mixer = mixer;
		pointer2ReadTiming = 0;
	}

	private Thread createNewSingleToneThread() {
		// TODO 自動生成されたメソッド・スタブ
		return new Thread(new Runnable() {
			@Override
			public void run() {
				if (sdl != null) {
					sdl.start();
					while (keyOn) {
						sdl.write(byteBuffer, 0, byteBuffer.length);
						if (Thread.currentThread().isInterrupted())
							break;
					}
					sdl.flush();
					sdl.stop();
				}
			}
		});
	}

	@Override
	protected String call() throws Exception {
		long pressedEventTime = 0;
		for (;;) {
			if (isCancelled()) {
				break;
			}
			if ((pointer2ReadTiming == comCenter.PointerOfTimeStamp)
					&& ((comCenter.keyStat[comCenter.PointerOfTimeStamp] == EKeyStat.KeyNull)
							|| (comCenter.keyStat[comCenter.PointerOfTimeStamp] == null))) {
				// 過去分は既読で、未だ新規分が更新されていない場合には、何もしないでループする。
				continue;
			}
			long eventTime = comCenter.timeStamp[pointer2ReadTiming];
			EKeyStat keyStat = comCenter.keyStat[pointer2ReadTiming];

			long presentTime = System.currentTimeMillis();
			switch (keyStat) {
			case KEY_PRESSED:
				if ((eventTime + atackDelayTime) < presentTime) {
					// Key on直後を検出
					keyOn = true;
					startPlayTone();
					incrementPointer2ReadTiming();
				}
				pressedEventTime = eventTime;
				break;
			case KEY_RELEASED:
				if (((pressedEventTime - SYSTEM_DELAY_MS > eventTime) || ((eventTime + atackDelayTime - SYSTEM_DELAY_MS) < presentTime))
						&& keyOn) {
					// Key off直後を検出
					incrementPointer2ReadTiming();
					keyOn = false;
					coreToneGenerator = createNewSingleToneThread();
				}
				break;
			default:
				break;
			}
		}
		return "executed";
	}

	public void closeToneGenerator() {
		if (sdl != null) {
			sdl.close();
			sdl = null;
		}
	}

	public void fillSoundBuffer(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(一周期分のみ生成する。)
		int bufferSize = comCenter.SAMPLE_RATE / frequency;
		byteBuffer = new byte[bufferSize * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int i = 0, index = 0; i < bufferSize; i++) {
			double angle = 2.0 * Math.PI * i / bufferSize;
			pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
			byteBuffer[index++] = (byte) ((pointData >> 8) & 0xff);
			byteBuffer[index++] = (byte) (pointData & 0xff);
		}
	}

	private void incrementPointer2ReadTiming() {
		pointer2ReadTiming++;
		if (pointer2ReadTiming >= comCenter.TIME_STAMP_VOL)
			pointer2ReadTiming = 0;
	}

	public void reOpenToneGenerator(int frequency, double volume) {
		reOpenToneGenerator(frequency, volume, mixer);
	}

	public void reOpenToneGenerator(int frequency, double volume, Mixer targetMixer) {
		reOpenToneGenerator(frequency, volume, targetMixer, 0.0);
	}

	public void reOpenToneGenerator(int frequency, double volume, Mixer targetMixer, double atackDelay) {
		try {
			if (sdl != null) {
				closeToneGenerator();
				// System.out.println("closeToneGenerator executed, before reOpenToneGenerator
				// activated");
			}

			mixer = targetMixer;
			atackDelayTime = (long) atackDelay;

			af = new AudioFormat(comCenter.SAMPLE_RATE, 16, 1, true, true);

			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, af);
			if (dataLineInfo != null) {
				sdl = (SourceDataLine) mixer.getLine(dataLineInfo);
			}

			sdl.open(af);
			fillSoundBuffer(frequency, volume);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startPlayTone() {
		if (sdl == null) {
			return;
		}
		coreToneGenerator.start();
	}

	public void startPlayTone(int frequency, double volume) {
		fillSoundBuffer(frequency, volume);
		startPlayTone();
	}

}
