package boxsniffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPool {

	private final BlockingQueue<Runnable> workerQueue;
	private Thread[] workerThreads;

	public ThreadPool(int numThreads) {
		workerQueue = new LinkedBlockingQueue<>();
		workerThreads = new Thread[numThreads];

		int i = 0;
		for (Thread t : workerThreads) {
			i++;
			t = new Worker("Pool Thread "+i);
			t.start();
		}
	}
	
	public void addTask(Runnable r){
		try {
			workerQueue.put(r);
		} catch (InterruptedException e) {
		}
	}
        
        public void stopThreads(){
            this.workerQueue.clear();
            this.workerThreads = null;
        }

	private class Worker extends Thread {
		
		public Worker(String name){
			super(name);
		}
		
                @Override
		public void run() {
			while (true) {
				try {
					Runnable r = workerQueue.take();
					r.run();
				} catch (InterruptedException | RuntimeException e) {
				}
			}
		}
	}

}