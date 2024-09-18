package net.dialectech.f2aApplication;

public class CToneGeneratorLate extends CToneGeneratorBase {

	public void startPlayTone(int frequency) {
		sdl.start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (comMem.isLateKeydown()) {
					sdl.write(byteBuffer, 0, byteBuffer.length);
				}
				sdl.flush();
				sdl.stop();
			}			
		}).start();
	}

}
