package me.escoffier.loom.loomunit.snippets;

import java.util.concurrent.CountDownLatch;

public class CodeUnderTest {

    void pin() {
        CountDownLatch latch = new CountDownLatch(1);
        CodeUnderTest pinning = new CodeUnderTest();
        Thread.ofVirtual().start(() -> pinning.callSynchronizedMethod(latch));
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
