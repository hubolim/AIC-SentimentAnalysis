package at.tuwien.aic;

import java.net.UnknownHostException;
import java.util.concurrent.*;

public class SubscribeExecutor {

    public static SubscribeExecutor getInstance() throws UnknownHostException {
        return new SubscribeExecutor();
    }

    private ExecutorService executorService;

    private SubscribeExecutor() throws UnknownHostException {
        executorService = Executors.newCachedThreadPool();
    }

    public void submit(final SubscribeService r, final Integer time) {
        executorService.submit(r);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("subscription started");
                    executorService.awaitTermination(time, TimeUnit.SECONDS);
                    r.stop();
                    System.out.println("subscription stopped");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
