package me.escoffier.loom.loomunit.snippets;

import java.util.concurrent.CountDownLatch;

public class CodeUnderTest {

    void pin() {
        pin(1);
    }

    void pin(int count) {
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            CodeUnderTest pinning = new CodeUnderTest();
            Thread.ofVirtual().start(() -> pinning.callSynchronizedMethod(latch));
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    public void callSynchronizedMethod(CountDownLatch latch) {
        synchronized (this) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        latch.countDown();
    }

}
