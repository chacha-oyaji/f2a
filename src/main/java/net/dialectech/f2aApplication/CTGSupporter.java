package net.dialectech.f2aApplication;

/*
 * CLASS CTGSupporter
 * 
 * 	CToneGeneratorの支配下にあり、各種TONEを波形データとして生成するクラス。
 * 
 * 2026.06.18	06:27	コメント挿入
 * 
 */

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import javafx.concurrent.Task;
import lombok.Setter;

public class CTGSupporter extends Task<String> {

	private static final int BYTES_PER_WORD = 2;
	final static long SYSTEM_DELAY_MS = 0;
	final static int OUTER_BUFFER_SIZE = 128;
	final static int SOUND_BLOCK_VOLUME = 20;

	private AudioFormat af;
	private SourceDataLine sdl;
	protected CComCenter comCenter = CComCenter.getInstance();
	@Setter
	private Mixer mixer;
	@Setter
	private long atackDelayTime;

	byte[][] byteBufferToneOn; // 音声用バッファ(TONE ON の場合)
	byte[] byteBufferToneOff; // 音声用バッファ(TONE OFF直後のみ)

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
					int stepIndex = 0;
					for (;;) {
						boolean presentKeyStat = keyOn;
						if ((presentKeyStat && !formerStatus) || (!presentKeyStat && formerStatus)) {
							sdl.flush();
							sdl.stop();
							sdl.start();
							stepIndex = 0;
						}
						if (presentKeyStat) {
							formerStatus = presentKeyStat;
							if (sdl.available() > maxAvailableVol) {
								sdl.write(byteBufferToneOn[stepIndex], 0, byteBufferToneOn[stepIndex].length);
								if (stepIndex < OUTER_BUFFER_SIZE - 1) {
									stepIndex++;
								}
							}
						} else {
							formerStatus = presentKeyStat;
							// sdl.write(byteBufferToneOff, 0, byteBufferToneOff.length);
							stepIndex = 0;
						}
						if (Thread.currentThread().isInterrupted()) {
							break;
						}
						try {
							Thread.sleep(0, 1000);
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
				Thread.sleep(0, 10000);
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
		if (comCenter.getToneEffect() == null) {
			fillSoundBufferNormal(frequency, volume);
			return;
		}

		switch (comCenter.getToneEffect()) {
		case CComCenter.TONE_EFFECT_CHIRPY_UPPER_TO_LOWER:
			fillSoundBufferWithChappyUpper2Lower(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_CHIRPY_LOWER_TO_UPPER:
			fillSoundBufferWithChappyLower2Upper(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_GRADUALLY_ATACK:
			fillSoundBufferWithCurvedAtack(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_WHITE_NOISE:
			fillSoundBufferWithRandomNumber(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_SQUARE_WAVE:
			fillSoundBufferSquareWave(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_TRIANGLE_WAVE:
			fillSoundBufferTriangleWave(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_HARMONY:
			fillSoundBuffer2Harmonics(frequency, volume);
			break;
		case CComCenter.TONE_EFFECT_NORMAL:
		default:
			fillSoundBufferNormal(frequency, volume);
			break;
		}
	}

	private void fillSoundBufferWithRandomNumber(int frequency, double volume) {
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE - 1; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle; // = 2.0 * Math.PI * i / bufferSize ;
				angle = Math.random() * 65536.0;
				pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
	}

	/**
	 * fillSoundBufferWithChappyLower2Upperは、byteBufferToneOnにChirpy(低い方から高い方に遷移するパターン)の波形を作って設定する。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	private void fillSoundBufferWithChappyLower2Upper(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE - 1; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle; // = 2.0 * Math.PI * i / bufferSize ;
				angle = slipAngleUpper2Lower(outerIndex * SOUND_BLOCK_VOLUME, i, bufferSize);
				pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
		for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
			double angle = 2.0 * Math.PI * i / bufferSize;
			pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
			byteBufferToneOn[OUTER_BUFFER_SIZE - 1][index++] = (byte) ((pointData >> 8) & 0xff);
			byteBufferToneOn[OUTER_BUFFER_SIZE - 1][index++] = (byte) (pointData & 0xff);
		}
	}

	/**
	 * fillSoundBufferWithChappyUpper2Lowerは、byteBufferToneOnにChirpy(高い方から低い方に遷移するパターン)の波形を作って設定する。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	public void fillSoundBufferWithChappyUpper2Lower(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE - 1; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle; // = 2.0 * Math.PI * i / bufferSize ;
				angle = slipAngle(outerIndex * SOUND_BLOCK_VOLUME, i, bufferSize);
				pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
		for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
			double angle = 2.0 * Math.PI * i / bufferSize;
			pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
			byteBufferToneOn[OUTER_BUFFER_SIZE - 1][index++] = (byte) ((pointData >> 8) & 0xff);
			byteBufferToneOn[OUTER_BUFFER_SIZE - 1][index++] = (byte) (pointData & 0xff);
		}
	}

	/**
	 * fillSoundBufferWithCurvedAtackは、byteBufferToneOnに正弦波の波形を作って設定する。ただし、立ち上がり時に緩やかに強まっていくパターンとする。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	public void fillSoundBufferWithCurvedAtack(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(100周期分のみ生成する。)
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData = 0;
		// 波形を生成
		for (int outerIndex = 0; outerIndex < 2; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle = 2.0 * Math.PI * i / bufferSize;
				double amplify = (double) (i + bufferSize * SOUND_BLOCK_VOLUME * outerIndex)
						/ (((double) 2) * bufferSize * SOUND_BLOCK_VOLUME);
				pointData = (short) (Math.sin(angle) * 32767.0 * amplify * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
		for (int outerIndex = 2; outerIndex < OUTER_BUFFER_SIZE; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle = 2.0 * Math.PI * i / bufferSize;
				pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
	}

	/**
	 * fillSoundBufferSquareWaveは、byteBufferToneOnに方形波の波形を作って設定する。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	public void fillSoundBufferSquareWave(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(100周期分のみ生成する。)
		// bufferSizeは１波を構成するデータ数
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle = 2.0 * Math.PI * i / bufferSize;
				pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
				if (Math.sin(angle) > 0.0)
					pointData = (short) (32767.0 * volume / 100.0);
				else
					pointData = (short) (-32767.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
	}

	/**
	 * fillSoundBufferTriangleWaveは、byteBufferToneOnに三角波の波形を作って設定する。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	public void fillSoundBufferTriangleWave(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(100周期分のみ生成する。)
		// bufferSizeは１波を構成するデータ数
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		int span = bufferSize / 4;
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				int angle = i % bufferSize;
				if (angle < span) {
					pointData = (short) (32767.0 * volume * angle / span / 100.0);
				} else if (angle < span * 2) {
					pointData = (short) (32767.0 * volume * (2 * span - angle) / span / 100.0);
				} else if (angle < span * 3) {
					pointData = (short) (-32767.0 * volume * (angle - 2 * span) / span / 100.0);
				} else {
					pointData = (short) (-32767.0 * volume * (4 * span - angle) / span / 100.0);
				}

				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
	}

	/**
	 * fillSoundBufferNormalは、byteBufferToneOnに最も標準的な正弦波の波形を作って設定する。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	public void fillSoundBufferNormal(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(100周期分のみ生成する。)
		int bufferSize = CComCenter.SAMPLE_RATE / frequency;
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * SOUND_BLOCK_VOLUME * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize * SOUND_BLOCK_VOLUME; i++) {
				double angle = 2.0 * Math.PI * i / bufferSize;
				pointData = (short) (Math.sin(angle) * 32767.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
	}

	/**
	 * fillSoundBufferNormalは、byteBufferToneOnに最も標準的な正弦波の波形を作って設定する。
	 * 
	 * @param frequency そのときの周波数（Hz）
	 * @param volume    そのときの音量設定（%）
	 */
	public void fillSoundBuffer2Harmonics(int frequency, double volume) {
		// 波長に合わせたバッファサイズを設定して波形の切れ目を防ぐ(100周期分のみ生成する。)
		double halfPitchRatio = Math.pow(2.0, 1.0 / 12.0);
		int secondFrequency = (int) (frequency * halfPitchRatio* halfPitchRatio* halfPitchRatio* halfPitchRatio);
		int thirdFrequency = (int) (frequency / halfPitchRatio/  halfPitchRatio/ halfPitchRatio/ halfPitchRatio/ halfPitchRatio);
//		int thirdFrequency = (int) (secondFrequency * halfPitchRatio*  halfPitchRatio* halfPitchRatio);

		int bufferSize1 = CComCenter.SAMPLE_RATE / frequency;
		int bufferSize2 = CComCenter.SAMPLE_RATE / secondFrequency;
		int bufferSize3 = CComCenter.SAMPLE_RATE / thirdFrequency;
		int bufferSize = lcm(bufferSize1, bufferSize2);
		bufferSize = lcm(bufferSize, bufferSize3);
		if (bufferSize>6000)
			bufferSize = 5000;
		
		byteBufferToneOn = new byte[OUTER_BUFFER_SIZE][bufferSize * 2]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		byteBufferToneOff = new byte[OUTER_BUFFER_SIZE * BYTES_PER_WORD]; // 16bitのデータとするのでbuffersizeはその２倍にとっておく。
		short pointData;
		// 波形を生成
		int totalindex = 0 ;
		for (int outerIndex = 0; outerIndex < OUTER_BUFFER_SIZE; ++outerIndex) {
			for (int i = 0, index = 0; i < bufferSize ; i++) {
				double angle1 = 2.0 * Math.PI * totalindex / bufferSize1;
				double angle2 = 2.0 * Math.PI * totalindex / bufferSize2;
				double angle3 = 2.0 * Math.PI * totalindex / bufferSize3;
				totalindex++ ;
//				pointData = (short) ((Math.sin(angle1)+Math.sin(angle2)) * 32767.0 / 2.0 * volume / 100.0);
				pointData = (short) ((Math.sin(angle1)+Math.sin(angle2)+Math.sin(angle3)) * 32767.0 / 3.0 * volume / 100.0);
				byteBufferToneOn[outerIndex][index++] = (byte) ((pointData >> 8) & 0xff);
				byteBufferToneOn[outerIndex][index++] = (byte) (pointData & 0xff);
			}
		}
		System.out.println("Calculation Completed , buffer size = " + bufferSize);
	}

	// 最大公約数を求める関数
	int gcd(int x, int y) throws Error {
		int r;

		if (x == 0 || y == 0) // 引数チェック
		{
			return 0; // これは引数エラー
		}

		// ユーグリッドの互除法
		while ((r = x % y) != 0) // yで割り切れるまでループ
		{
			x = y;
			y = r;
		}
		return y;
	}

	// 最大公約数を求める関数
	int lcm(int x, int y) {
		if (x == 0 || y == 0) // 引数チェック
		{
			return 0; // これは引数エラー
		}

		return (x * y / gcd(x, y));
	}

	private double slipAngle(double outerOffset, double innerIndex, double bufferSize) {
		double baseAngular = outerOffset + innerIndex / bufferSize;
		double innerAngular = baseAngular + baseAngular * (0.7 / Math.exp(baseAngular / bufferSize / 2.0));
		double angle = 2.0 * Math.PI * innerAngular;
		return angle;
	}

	private double slipAngleUpper2Lower(double outerOffset, double innerIndex, double bufferSize) {
		double baseAngular = outerOffset + innerIndex / bufferSize;
		double innerAngular = baseAngular * (1.0 / (1.0 + 2.0 / Math.exp(baseAngular / bufferSize / 2.0)));
		double angle = 2.0 * Math.PI * innerAngular;
		return angle;
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

			af = new AudioFormat(CComCenter.SAMPLE_RATE, 16, 1, true, true);

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

}
