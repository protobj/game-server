package io.protobj.microserver.net;//package com.guangyu.cd003.projects.message.core.net;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.StringTokenizer;
//
//import org.apache.commons.io.FileSystemUtils;
//
//public class OSUtils{
//
//    /**
//     * 功能：可用磁盘
//     */
//    public static int disk() {
//        try {
//            long total = FileSystemUtils.freeSpaceKb("/home");
//            double disk = (double) total / 1024 / 1024;
//            return (int) disk;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return 0;
//    }
//
//    /**
//     * 功能：获取Linux系统cpu使用率
//     */
//    public static int cpuUsage() {
//        try {
//            Map<?, ?> map1 = OSUtils.cpuinfo();
//            Thread.sleep(CPUTIME);
//            Map<?, ?> map2 = OSUtils.cpuinfo();
//
//            long user1 = Long.parseLong(map1.get("user").toString());
//            long nice1 = Long.parseLong(map1.get("nice").toString());
//            long system1 = Long.parseLong(map1.get("system").toString());
//            long idle1 = Long.parseLong(map1.get("idle").toString());
//
//            long user2 = Long.parseLong(map2.get("user").toString());
//            long nice2 = Long.parseLong(map2.get("nice").toString());
//            long system2 = Long.parseLong(map2.get("system").toString());
//            long idle2 = Long.parseLong(map2.get("idle").toString());
//
//            long total1 = user1 + system1 + nice1;
//            long total2 = user2 + system2 + nice2;
//            float total = total2 - total1;
//
//            long totalIdle1 = user1 + nice1 + system1 + idle1;
//            long totalIdle2 = user2 + nice2 + system2 + idle2;
//            float totalidle = totalIdle2 - totalIdle1;
//
//            float cpusage = (total / totalidle) * 100;
//            return (int) cpusage;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return 0;
//    }
//
//    /**
//     * 功能：CPU使用信息
//     */
//    public static Map<?, ?> cpuinfo() {
//        InputStreamReader inputs = null;
//        BufferedReader buffer = null;
//        Map<String, Object> map = new HashMap<String, Object>();
//        try {
//            inputs = new InputStreamReader(new FileInputStream("/proc/stat"));
//            buffer = new BufferedReader(inputs);
//            String line = "";
//            while (true) {
//                line = buffer.readLine();
//                if(line == null) {
//                    break;
//                }
//                if(line.startsWith("cpu")) {
//                    StringTokenizer tokenizer = new StringTokenizer(line);
//                    List<String> temp = new ArrayList<String>();
//                    while (tokenizer.hasMoreElements()) {
//                        String value = tokenizer.nextToken();
//                        temp.add(value);
//                    }
//                    map.put("user", temp.get(1));
//                    map.put("nice", temp.get(2));
//                    map.put("system", temp.get(3));
//                    map.put("idle", temp.get(4));
//                    map.put("iowait", temp.get(5));
//                    map.put("irq", temp.get(6));
//                    map.put("softirq", temp.get(7));
//                    map.put("stealstolen", temp.get(8));
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                buffer.close();
//                inputs.close();
//            } catch (Exception e2) {
//                e2.printStackTrace();
//            }
//        }
//        return map;
//    }
//
//    /**
//     * 功能：内存使用率
//     */
//    public static int memoryUsage() {
//        Map<String, Object> map = new HashMap<String, Object>();
//        InputStreamReader inputs = null;
//        BufferedReader buffer = null;
//        try {
//            inputs = new InputStreamReader(new FileInputStream("/proc/meminfo"));
//            buffer = new BufferedReader(inputs);
//            String line = "";
//            while (true) {
//                line = buffer.readLine();
//                if(line == null)
//                    break;
//                int beginIndex = 0;
//                int endIndex = line.indexOf(":");
//                if(endIndex != -1) {
//                    String key = line.substring(beginIndex, endIndex);
//                    beginIndex = endIndex + 1;
//                    endIndex = line.length();
//                    String memory = line.substring(beginIndex, endIndex);
//                    String value = memory.replace("kB", "").trim();
//                    map.put(key, value);
//                }
//            }
//
//            long memTotal = Long.parseLong(map.get("MemTotal").toString());
//            long memFree = Long.parseLong(map.get("MemFree").toString());
//            long memused = memTotal - memFree;
//            long buffers = Long.parseLong(map.get("Buffers").toString());
//            long cached = Long.parseLong(map.get("Cached").toString());
//
//            double usage = (double) (memused - buffers - cached) / memTotal * 100;
//            return (int) usage;
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                buffer.close();
//                inputs.close();
//            } catch (Exception e2) {
//                e2.printStackTrace();
//            }
//        }
//        return 0;
//    }
//
//
//    public static double getCpuRatioForWindows() {
//        try {
//            String procCmd = System.getenv("windir")
//
//                    + "//system32//wbem//wmic.exe process get Caption,CommandLine,"
//
//                    + "KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
//            // 取进程信息
//            long[] c0 = readCPU(Runtime.getRuntime().exec(procCmd));
//
//            Thread.sleep(CPUTIME);
//
//            long[] c1 = readCPU(Runtime.getRuntime().exec(procCmd));
//
//            if(c0 != null && c1 != null) {
//                long idletime = c1[0] - c0[0];
//
//                long busytime = c1[1] - c0[1];
//
//                return (double) (PERCENT * (busytime) / (busytime + idletime));
//
//            } else {
//                return 0.0;
//
//            }
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//
//            return 0.0;
//
//        }
//
//    }
//
//    private static final int CPUTIME = 50;
//    private static final int PERCENT = 100;
//    private static final int FAULTLENGTH = 10;
//
//    //读取cpu相关信息
//    public static long[] readCPU(final Process proc) {
//        long[] retn = new long[2];
//        try {
//            proc.getOutputStream().close();
//            InputStreamReader ir = new InputStreamReader(proc.getInputStream());
//            LineNumberReader input = new LineNumberReader(ir);
//            String line = input.readLine();
//            if(line == null || line.length() < FAULTLENGTH) {
//                return null;
//            }
//            int capidx = line.indexOf("Caption");
//            int cmdidx = line.indexOf("CommandLine");
//            int rocidx = line.indexOf("ReadOperationCount");
//            int umtidx = line.indexOf("UserModeTime");
//            int kmtidx = line.indexOf("KernelModeTime");
//            int wocidx = line.indexOf("WriteOperationCount");
//            long idletime = 0;
//            long kneltime = 0;
//            long usertime = 0;
//            while ((line = input.readLine()) != null) {
//                if(line.length() < wocidx) {
//                    continue;
//                }
//                // 字段出现顺序：Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperation
//                String caption = substring(line, capidx, cmdidx - 1).trim();
//                String cmd = substring(line, cmdidx, kmtidx - 1).trim();
//                if(cmd.indexOf("wmic.exe") >= 0) {
//                    continue;
//                }
//                String s1 = substring(line, kmtidx, rocidx - 1).trim();
//                String s2 = substring(line, umtidx, wocidx - 1).trim();
//                if(caption.equals("System Idle Process") || caption.equals("System")) {
//                    if(s1.length() > 0)
//                        idletime += Long.valueOf(s1).longValue();
//                    if(s2.length() > 0)
//                        idletime += Long.valueOf(s2).longValue();
//                    continue;
//                }
//                if(s1.length() > 0)
//                    kneltime += Long.valueOf(s1).longValue();
//                if(s2.length() > 0)
//                    usertime += Long.valueOf(s2).longValue();
//            }
//            retn[0] = idletime;
//            retn[1] = kneltime + usertime;
//            return retn;
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            try {
//                proc.getInputStream().close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//    }
//
//    private static String substring(String src, int start_idx, int end_idx) {
//        byte[] b = src.getBytes();
//        String tgt = "";
//        for (int i = start_idx; i <= end_idx; i++) {
//            tgt += (char) b[i];
//        }
//        return tgt;
//    }
//}