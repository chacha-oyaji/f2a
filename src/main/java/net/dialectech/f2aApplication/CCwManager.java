package net.dialectech.f2aApplication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

import com.fazecast.jSerialComm.SerialPort;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class CCwManager extends Service<String> {

	CUIController controller;

	AudioFormat af;
	SourceDataLine sdl;
	byte[] byteBuffer;

	public CCwManager(CUIController controller) {
		super();
		this.controller = controller;
	}

	@Override
	protected Task<String> createTask() {
		// TODO 自動生成されたメソッド・スタブ
		return new Task<String>() {

			@Override
			protected String call() throws Exception {

				CToneGenerator toneGenerator = new CToneGenerator();
				CToneGeneratorLate toneGeneratorLate = new CToneGeneratorLate();
				CComMemory comMem = CComMemory.getInstance();
				toneGenerator.openToneGenerator((int) (controller.sbToneFrequency.getValue()),
						comMem.getMonitorVolume(), comMem.getPresentMonitorMixer());
				toneGeneratorLate.openToneGenerator((int) (controller.sbToneFrequency.getValue()),
						comMem.getMicVolume(), comMem.getPresentMicMixer());

				boolean pttStatus = false;
				long startTime = System.currentTimeMillis();
				while (true) {
					// ここで永久ループする。
					if (controller.getCbBreakIn().isSelected() && comMem.isPtt()) {
						long timeOfPresentSending = System.currentTimeMillis() - startTime;
						if (timeOfPresentSending > comMem.getReleaseDelay()) {
							comMem.setPtt(false);
							Platform.runLater(()->{
								controller.dispSendReceive() ;
							});
						}
					}
					if (controller.getCbBreakIn().isSelected() && comMem.isKeydown()) {
						comMem.setPtt(true);
						Platform.runLater(()->{
							controller.dispSendReceive() ;
						});
						startTime = System.currentTimeMillis();
					}
					
					if (comMem.isKeyDownDetected()) {
						toneGenerator.startPlayTone((int) controller.sbToneFrequency.getValue());
					}

					if (comMem.isLateKeyDownDetected()) {
						toneGeneratorLate.startPlayTone((int) controller.sbToneFrequency.getValue());
					}
					if (comMem.isPtt()) {
						if (pttStatus == false) {
							// 送受信にスイッチが押され、送信に入ったときの立ち上がり時にのみ実行
							sendCI_V_Command(0x1c, 0x00, 0x01, "SEND"); // 送信開始
							updateMessage("message for Send");
						}
						pttStatus = true;
					} else {
						if (pttStatus == true) {
							// 送受信にスイッチが押され、受信に入ったときの立ち上がり時にのみ実行
							sendCI_V_Command(0x1c, 0x00, 0x00, "RECEIVE"); // 送信終了
							updateMessage("message for Receive");
						}
						pttStatus = false;
					}
					if (isCancelled()) {
						// toneGenerator.close();
						System.out.println("CANCELLED in Service");
						toneGenerator.closeToneGenerator();
						toneGeneratorLate.closeToneGenerator();
						break;
					}
				}
				return "test";
			}
		};
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
		CComMemory commMem = CComMemory.getInstance();
		String comPort = controller.selectedComPort();
		System.out.println("COM: " + comPort + " : " + message);
		if (comPort == null) {
			System.out.println("Com Port is not selected.");
			return;
		}
		SerialPort sp = SerialPort.getCommPort(comPort);
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
