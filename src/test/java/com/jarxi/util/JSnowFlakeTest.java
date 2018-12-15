package com.jarxi.util;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;


public class JSnowFlakeTest {
    private long times = JSnowFlake.MAX_SEQUENCE + 1000;

    @Test
    public void contextLoads() {
    }

    // 连续生产超过MAX_SEQUENCE个id，不会重复
    @Test
    public void testRepetitionLess() {
        JSnowFlake jsnowFlake = new JSnowFlake();
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < times; i++) {
            set.add(jsnowFlake.nextId());
        }
        assertThat((long)set.size(), equalTo(times));
    }

    // 多个线程并发，多个ID生成器，workid不同，生成的id不会重复
    @Test
    public void testConcurrent() {
        int threads = 3;
        final CountDownLatch latch = new CountDownLatch(threads);
        final Set<Long> set = new ConcurrentSkipListSet<>();
        for (int i = 0; i < threads; i++) {
            Runnable runnable = new Runnable() {
                int workid = 0;
                public void run() {
                    JSnowFlake jsnowFlake = new JSnowFlake(workid);
                    for (long i = 0; i < times; i++) {
                        set.add(jsnowFlake.nextId());
                    }
                    latch.countDown(); // 执行完毕，计数器减1
                }
                public Runnable accept(int workid) { // 接收从外部传递的参数
                    this.workid = workid;
                    return this;
                }
            }.accept(i);
            new Thread(runnable).start();
        }
        try {
            latch.await(); // 主线程等待
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat((long)set.size(), equalTo(threads * times));
    }

    // 多个线程并发，只有一个ID生成器，生成的id不会重复
    @Test
    public void testSameWorkidConcurrent() {
        int threads = 3;
        final CountDownLatch latch = new CountDownLatch(threads);
        final Set<Long> set = new ConcurrentSkipListSet<>();
        final JSnowFlake jsnowFlake = new JSnowFlake();
        for (int i = 0; i < threads; i++) {
            Runnable runnable = new Runnable() {
                public void run() {
                for (long i = 0; i < times; i++) {
                    set.add(jsnowFlake.nextId());
                }
                latch.countDown(); // 执行完毕，计数器减1
                }
            };
            new Thread(runnable).start();
        }
        try {
            latch.await(); // 主线程等待
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat((long)set.size(), equalTo(threads * times));
    }

    // 测试时钟调整的影响，java中设置时钟不能影响currentTimeMillis()
    // 依赖手动用shell命令'date -s hh:mm:ss'调整时钟测试
    @Ignore
    @Test
    public void testClockRepetition() {
        long itimes = 10;
        long jtimes = times;
        long newMillis = System.currentTimeMillis();
        long oldMillis = newMillis;
        JSnowFlake jsnowFlake = new JSnowFlake();
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < itimes; i++) {
            for (long j = 0; j < jtimes; j++) {
                set.add(jsnowFlake.nextId());
            }
            newMillis = System.currentTimeMillis();
            if (oldMillis > newMillis) {
                System.out.println("Clock moved backwards");
            }
            oldMillis = newMillis;
            // 调整时钟
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat((long)set.size(), equalTo(itimes * jtimes));
    }
}
