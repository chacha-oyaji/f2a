package net.dialectech.f2aApplication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import javafx.concurrent.Task;
import lombok.Setter;

public class CTGSupporter extends Task<String> {

	final long SYSTEM_DELAY_MS = 0;
	private AudioFormat af;
	private SourceDataLine sdl;
	protected CComCenter comCenter = CComCenter.getInstance();
	@Setter
	private Mixer mixer;
	@Setter
	private long atackDelayTime;

	byte[] byteBufferToneOn; // 音声用バッファ(TONE ON の場合)
	byte[] byteBufferToneOff; // 音声用バッファ(TONE OFF の場合)

	private int pointer2ReadTiming;
	boolean keyOn = false;

	Thread coreToneGenerator;

	public CTGSupporter(Mixer mixer) {
		super();
		this.mixer = mixer;
		pointer2ReadTiming = 0;
	}

	private Thread createNewSingleToneThread() {
		// TODO 自動生成されたメソッド・スタブ
		Thread toneGenerationCore = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean formerStatus = false;
				int maxAvailableVol;
				if (sdl != null) {
					sdl.start();
					maxAvailableVol = sdl.available() * 1 / 3;

					for (;;) {
						boolean presentKeyStat = keyOn;
						if ((presentKeyStat && !formerStatus) || (!presentKeyStat && formerStatus)) {
							sdl.flush();
							sdl.stop();
							sdl.start();
						}
						if (presentKeyStat) {
							formerStatus = presentKeyStat;
							if (sdl.available() > maxAvailableVol)
								sdl.write(byteBufferToneOn, 0, byteBufferToneOn.length);
						} else {
							formerStatus = presentKeyStat;
							// sdl.write(byteBufferToneOff, 0, byteBufferToneOff.length);
						}
						if (Thread.currentThread().isInterrupted()) {
							break;
						}
						try {
							Thread.sleep(0,1000);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}
		});
		toneGenerationCore.setName("ToneGenerationCore");
		return toneGenerationCore;
	}

	@Override
	protected String call() throws Exception {
		Thread.currentThread().setName("Tone Generator Handler");
		long pressedEventTime = 0;
		coreToneGenerator = createNewSingleToneThread();
		startPlayTone();

		for (;;) {
			if (isCancelled()) {
				if (coreToneGenerator != null)
					coreToneGenerator.interrupt();
				break;
			}
			try {
				Thread.sleep(0,10000);
			} catch (InterruptedException e) {
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
					incrementPointer2ReadTiming();
					keyOn = true;
				}
				pressedEventTime = eventTime;
				break;
			case KEY_RELEASED:
				if (((pressedEventTime - SYSTEM_DELAY_MS > eventTime)
						|| ((eventTime + atackDelayTime - SYSTEM_DELAY_MS) < presentTime)) && keyOn) {
					// Key off直後を検出
					incrementPointer2ReadTiming();
					keyOn = false;
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
		if (coreToneGenerator != null && coreToneGenerator.isAlive())
			coreToneGenerator.interrupt();
	}

	public void fillSoundBuffer(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(100周期分のみ生成する。)
		int bufferSize = comCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[bufferSize * 200]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[bufferSize * 200]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int i = 0, index = 0; i < bufferSize * 100; i++) {
			double angle = 2.0 * Math.PI * i / bufferSize;
			pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
			byteBufferToneOn[index++] = (byte) ((pointData >> 8) & 0xff);
			byteBufferToneOn[index++] = (byte) (pointData & 0xff);
		}
		for (int i = 0, index = 0; i < bufferSize * 100; i++) {
			byteBufferToneOff[index++] = (byte) 0;
			byteBufferToneOff[index++] = (byte) 0;
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
			startPlayTone();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startPlayTone() {
		if (sdl == null) {
			return;
		}
		if (coreToneGenerator != null) {
			coreToneGenerator.interrupt();
			while (coreToneGenerator.isAlive())
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			coreToneGenerator = createNewSingleToneThread();
		}
		coreToneGenerator.start();
	}

	public void startPlayTone(int frequency, double volume) {
		fillSoundBuffer(frequency, volume);
		startPlayTone();
	}

}