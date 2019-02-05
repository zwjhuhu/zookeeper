package net.zwj.zkcide;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ElectionTest {
	
	private static final Random random = new Random(System.currentTimeMillis());
	
	private static final int zxidBase = 100;
	
	private static final int portBase = 10000;
	
	
	public static void main(String[] args) {
		
		int size = 5;
		int[] ports = new int[size];
		ExecutorService threadPool = Executors.newFixedThreadPool(size);
		for(int i=0;i<size;i++) {
			ports[i] = portBase+i;
		}
		for(int i=0;i<size;i++) {
			threadPool.submit(new Quram(i, ports));	
		}
		
		threadPool.shutdown();
		
	}
	
	static class Quram implements Runnable{
		private int index;
		private int[] ports;
		

		Quram(int index,int[] ports){
			this.index = index;
			this.ports = ports;
		}

		@Override
		public void run() {
			new QuramPeer(random.nextInt(10)+zxidBase, index+1, ports[index], random.nextInt(5),ports).start();
		}
		
	}

}
