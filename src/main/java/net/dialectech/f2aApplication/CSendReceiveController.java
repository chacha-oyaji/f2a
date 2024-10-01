package net.dialectech.f2aApplication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

import com.fazecast.jSerialComm.SerialPort;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;

public class CSendReceiveController extends Service<String> {

	CUIController controller;

	AudioFormat af;
	SourceDataLine sdl;
	byte[] byteBuffer;
	CComCenter comCenter = CComCenter.getInstance();
	private int pointer2ReadTiming;
	private boolean sendOn = false;
	private boolean formerPtt = false;

	@Setter
	private long releaseDelayTime;

	public CSendReceiveController(CUIController controller) {
		super();
		this.controller = controller;
	}

	@Override
	protected Task<String> createTask() {
		// TODO 自動生成されたメソッド・スタブ
		return new Task<String>() {

			@Override
			protected String call() throws Exception {
				long finalReleaseTime = 0;
				for (;;) {
					if (isCancelled()) {
						break;
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
						break;
					}

					if (comCenter.isBreakInMode()) {
						// Break-Inモードの場合の処理
						long presentTime = System.currentTimeMillis();

						if ((finalReleaseTime < presentTime) && sendOn) {
							// Key off直後を検出
							sendOn = false;
							sendCI_V_Command(0x1c, 0x00, 0x00, "RECEIVE"); // 送信終了
							updateMessage("message for Receive");
							comCenter.setPtt(false);
							Platform.runLater(() -> {
								controller.dispSendReceive();
							});
						}
						
						if (((pointer2ReadTiming == comCenter.PointerOfTimeStamp)
								&& comCenter.keyStat[comCenter.PointerOfTimeStamp] == EKeyStat.KeyNull)) {
							// 過去分は既読で、未だ新規分が更新されていない場合には、何もしないでループする。
							continue;
						}
						long eventTime = comCenter.timeStamp[pointer2ReadTiming];
						EKeyStat keyStat = comCenter.keyStat[pointer2ReadTiming];

						switch (keyStat) {
						case KEY_PRESSED:
							if ((eventTime < presentTime) && !sendOn) {
								// Key on直後を検出
								sendOn = true;
								sendCI_V_Command(0x1c, 0x00, 0x01, "SEND"); // 送信開始
								updateMessage("message for Send");
								comCenter.setPtt(true);
								Platform.runLater(() -> {
									controller.dispSendReceive();
								});
							}
							if (sendOn)
								incrementPointer2ReadTiming();
							finalReleaseTime = eventTime + releaseDelayTime;
							break;
						case KEY_RELEASED:
							finalReleaseTime = eventTime + releaseDelayTime;
							incrementPointer2ReadTiming();
						default:
							finalReleaseTime = eventTime + releaseDelayTime;
							break;
						}
					} else {
						// PTTモードの場合の処理
						if (!comCenter.isBreakInMode()) {
							if (comCenter.isPtt() && !formerPtt) {
								sendCI_V_Command(0x1c, 0x00, 0x01, "SEND"); // 送信開始
								updateMessage("message for Send");
								Platform.runLater(() -> {
									controller.dispSendReceive();
								});
								formerPtt = true;
							}
							if (!comCenter.isPtt() && formerPtt) {
								sendCI_V_Command(0x1c, 0x00, 0x00, "RECEIVE"); // 送信終了
								updateMessage("message for Receive");
								Platform.runLater(() -> {
									controller.dispSendReceive();
								});
								formerPtt = false;
							}

							continue;
						}
					}
				}
				return "executed";
			}
		};
	}

	private void incrementPointer2ReadTiming() {
		pointer2ReadTiming++;
		if (pointer2ReadTiming >= comCenter.TIME_STAMP_VOL)
			pointer2ReadTiming = 0;
	}

	/**
	 * operand==-1ならば、operandの送信なし。
	 * 
	 * @param com
	 * @param comSub
	 * @param operand
	 * @param message
	 */
	void sendCI_V_Command(int com, int comSub, int operand, String message) {
		CComCenter commMem = CComCenter.getInstance();
		String comPort = controller.selectedComPort4Rig();
		if (comPort==null)
			return ;
		String[] keyPortCore = comPort.split(":");

		if (keyPortCore[0] == null) {
			System.out.println("PTT Com Port is not selected.");
			return;
		}
		String portId = keyPortCore[0].trim();
		
		System.out.println("COM: " + portId + ": " + message);
		if (controller.selectedRig() == null) {
			System.out.println("Rig is not selected.");
			return;

		}
		SerialPort sp = SerialPort.getCommPort(portId);
		sp.setBaudRate(19200);
		sp.setNumDataBits(8);

		sp.openPort();
		sendByte(sp, 0xfe);
		sendByte(sp, 0xfe);
		sendByte(sp, commMem.getRigsAddress().get(controller.selectedRig()));
		sendByte(sp, 0xe0);
		sendByte(sp, com);
		sendByte(sp, comSub);
		if (operand != -1)
			sendByte(sp, operand);
		sendByte(sp, 0xfd);
		// byte[] returnData = readDataTypeA(sp);
		sp.closePort();
	}

	private byte[] readDataTypeA(SerialPort sp) {
		byte[] data = new byte[2];
		byte[] result = new byte[128];
		int index = 0;
		byte byteData;
		do {
			sp.readBytes(data, 1);
			byteData = data[0];
		} while (byteData != (byte) 0xfe);
		do {
			sp.readBytes(data, 1);
			byteData = data[0];
			result[index++] = byteData;
			System.out.print(String.format(" %02x", byteData));
		} while (byteData != (byte) 0xfd);
		System.out.println("INDEX : " + index);
		return data;
	}

	private void sendByte(SerialPort sp, int data) {
		byte[] oneByte = new byte[1];

		oneByte[0] = (byte) (data & 0xff);
		sp.writeBytes(oneByte, 1);
		System.out.print(String.format("%02X ", data));
	}

}
