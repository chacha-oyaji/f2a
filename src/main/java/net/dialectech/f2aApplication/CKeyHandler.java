package net.dialectech.f2aApplication;

import com.fazecast.jSerialComm.SerialPort;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class CKeyHandler extends Service<String> {

	byte[] buffer = new byte[16];

	CComCenter comCenter = CComCenter.getInstance();
	SerialPort sp = null ;
	
	public CKeyHandler() {
		super();
	}
	
	@Override
	protected Task<String> createTask() {
		return new Task<String>() {

			@Override
			protected String call() throws Exception {
				for (;;) {
					if (isCancelled()) {
						break;
					}
					try {
						Thread.sleep(0,10000);
					} catch (InterruptedException e) {
						break;
					}

					if (sp == null)
						continue ;
					int vol = sp.readBytes(buffer, 1);
					if (vol!=0) {
						switch (buffer[0]) {
						case 'T' :
							// starT
							comCenter.addNewTimeStamp(System.currentTimeMillis(), EKeyStat.KEY_PRESSED);
							break ;
						case 'P' :
							// stoP
							comCenter.addNewTimeStamp(System.currentTimeMillis(), EKeyStat.KEY_RELEASED);
							break ;
						}
					}
				}
				return "CKeyHandler executed";
			}
		};
	}

	public void reOpen(String keyPort) {
		if (sp!=null) 
			sp.closePort();
		if (keyPort==null)
			return ;
		String[] keyPortCore = keyPort.split(":");
		sp = SerialPort.getCommPort(keyPortCore[0]);
		sp.setBaudRate(19200);
		sp.setNumDataBits(8);
		
		sp.openPort();
	}

}
