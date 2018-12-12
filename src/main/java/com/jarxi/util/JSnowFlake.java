package com.jarxi.util;

import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * 改良的twitter的snowflake算法
 * ref: https://blog.csdn.net/u013970991/article/details/69388427
 * 参考了MongoDB的ObjectId.java
 * /---32 bit 秒单位时间戳---22 bit序列号---10 bit机器标识---/
 * 32位时间戳据说能用136年
 * 每秒的开始，时间戳不会归零，就不怕时钟回拨了，毕竟时钟回拨的时间窗口内序列号不会重复(序列号占用位数越大越安全)
 * 机器标识可以通过构造函数传入，没有传入就根据mac号生成
 * 
 * @author cjp
 */
public class JSnowFlake {

    /**
     * 起始的时间戳 2018/1/1
     */
    private final static long START_STMP = 1514736000L;

    /**
     * 每一部分占用的位数
     * 时间戳占32 bit，秒为单位
     */
    private final static long SEQUENCE_BIT = 22; //序列号占用的位数
    private final static long MACHINE_BIT = 10;   //机器标识占用的位数

    /**
     * 每一部分的最大值
     */
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long SEQUENCE_LEFT = MACHINE_BIT;
    private final static long TIMESTMP_LEFT = SEQUENCE_BIT + MACHINE_BIT;

    private long machineId;     //机器标识
    private long sequence = new SecureRandom().nextLong(); //序列号从一个随机数开始

    public JSnowFlake() {
        this(createMachineIdentifier());
    }

    public JSnowFlake(long machineId) {
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     *
     * @return a type long ID
     */
    public synchronized long nextId() {
        long currStmp = getNewstmp();

        // 序列号自增
        sequence++;
        sequence &= MAX_SEQUENCE;

        return (currStmp - START_STMP) << TIMESTMP_LEFT //时间戳部分
                | sequence << SEQUENCE_LEFT             //序列号部分
                | machineId;                            //机器标识部分
    }

    private long getNewstmp() {
        // 当前时间转为秒单位
        return System.currentTimeMillis() / 1000;
    }

    private static int createMachineIdentifier() {
        // build a 2-byte machine piece based on NICs info
        int machinePiece;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                sb.append(ni.toString());
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    ByteBuffer bb = ByteBuffer.wrap(mac);
                    try {
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                    } catch (BufferUnderflowException shortHardwareAddressException) { //NOPMD
                        // mac with less than 6 bytes. continue
                    }
                }
            }
            machinePiece = sb.toString().hashCode();
        } catch (Throwable t) {
            // exception sometimes happens with IBM JVM, use SecureRandom instead
            machinePiece = (new SecureRandom().nextInt());
            // Failed to get machine identifier from network interface, using SecureRandom instead
        }
        // 截取机器标识的后n bit
        machinePiece = machinePiece & (int)MAX_MACHINE_NUM;
        return machinePiece;
    }

    private static void useTimeTest() {
        JSnowFlake jsnowFlake = new JSnowFlake();
        int times = 4000000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            jsnowFlake.nextId();
        }
        long end = System.currentTimeMillis();
        System.out.println("times " + times + " milliseconds " + (end - start));
    }

    public static void baseTest() {
        JSnowFlake jsnowFlake = new JSnowFlake();
        System.out.println("Curr Time 0x" + Long.toHexString((jsnowFlake.getNewstmp() - START_STMP)));
        System.out.println("Sequence 0x" + Long.toHexString(jsnowFlake.sequence & MAX_SEQUENCE));
        System.out.println("Machine Id 0x" + Integer.toHexString(createMachineIdentifier()));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                long newid = jsnowFlake.nextId();
                System.out.println("0x" + Long.toHexString(newid) + " " + newid);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        baseTest();
        useTimeTest();
    }
}
