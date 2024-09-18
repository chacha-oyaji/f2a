package net.dialectech.f2aApplication;

public class CToneGenerator extends CToneGeneratorBase {

	public void startPlayTone(int frequency) {
		sdl.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (comMem.isKeydown()) {
					sdl.write(byteBuffer, 0, byteBuffer.length);
				}
				sdl.flush();
				sdl.stop();
			}			
		}).start();
	}

}
