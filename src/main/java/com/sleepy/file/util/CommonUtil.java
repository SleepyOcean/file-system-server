package com.sleepy.file.util;

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 工具类
 *
 * @author gehoubao
 * @create 2019-12-23 17:38
 **/
public class CommonUtil {
    public static String CATEGORY_AUDIO = "audio";
    public static String CATEGORY_VIDEO = "video";
    public static String CATEGORY_IMAGE = "image";
    public static String CATEGORY_OTHER = "other";
    public static List<String> AUDIO_SUFFIX_SET;
    public static List<String> VIDEO_SUFFIX_SET;
    public static List<String> IMAGE_SUFFIX_SET;

    static {
        AUDIO_SUFFIX_SET = Arrays.asList(".mp3", ".wav", ".flac", ".aac", ".wma");
        VIDEO_SUFFIX_SET = Arrays.asList(".rmvb", ".flv", ".mpg", ".mp4", ".mkv");
        IMAGE_SUFFIX_SET = Arrays.asList(".gif", ".jpg", ".jpeg", ".png");
    }

    public static String getCategoryBySuffix(String suffix) {
        if (AUDIO_SUFFIX_SET.contains(suffix)) {
            return CATEGORY_AUDIO;
        }
        if (VIDEO_SUFFIX_SET.contains(suffix)) {
            return CATEGORY_VIDEO;
        }
        if (IMAGE_SUFFIX_SET.contains(suffix)) {
            return CATEGORY_IMAGE;
        }
        return CATEGORY_OTHER;
    }
    public static void main(String[] args) {
        Properties props = System.getProperties();
        System.out.println("Java的运行环境版本：" + props.getProperty("java.version"));
        System.out.println("Java的运行环境供应商：" + props.getProperty("java.vendor"));
        System.out.println("Java供应商的URL：" + props.getProperty("java.vendor.url"));
        System.out.println("Java的安装路径：" + props.getProperty("java.home"));
        System.out.println("Java的虚拟机规范版本：" + props.getProperty("java.vm.specification.version"));
        System.out.println("Java的虚拟机规范供应商：" + props.getProperty("java.vm.specification.vendor"));
        System.out.println("Java的虚拟机规范名称：" + props.getProperty("java.vm.specification.name"));
        System.out.println("Java的虚拟机实现版本：" + props.getProperty("java.vm.version"));
        System.out.println("Java的虚拟机实现供应商：" + props.getProperty("java.vm.vendor"));
        System.out.println("Java的虚拟机实现名称：" + props.getProperty("java.vm.name"));
        System.out.println("Java运行时环境规范版本：" + props.getProperty("java.specification.version"));
        System.out.println("Java运行时环境规范供应商：" + props.getProperty("java.specification.vender"));
        System.out.println("Java运行时环境规范名称：" + props.getProperty("java.specification.name"));
        System.out.println("Java的类格式版本号：" + props.getProperty("java.class.version"));
        System.out.println("默认的临时文件路径：" + props.getProperty("java.io.tmpdir"));
        System.out.println("一个或多个扩展目录的路径：" + props.getProperty("java.ext.dirs"));
        System.out.println("操作系统的名称：" + props.getProperty("os.name"));
        System.out.println("操作系统的构架：" + props.getProperty("os.arch"));
        System.out.println("操作系统的版本：" + props.getProperty("os.version"));
        System.out.println("文件分隔符：" + props.getProperty("file.separator"));
        //在 unix 系统中是＂／＂
        System.out.println("路径分隔符：" + props.getProperty("path.separator"));
        //在 unix 系统中是＂:＂
        System.out.println("行分隔符：" + props.getProperty("line.separator"));
        //在 unix 系统中是＂/n＂
        System.out.println("用户的账户名称：" + props.getProperty("user.name"));
        System.out.println("用户的主目录：" + props.getProperty("user.home"));
        System.out.println("用户的当前工作目录：" + props.getProperty("user.dir"));
    }

    public static String getRootDir() {
        Properties props = System.getProperties();
        return "E:\\tmp\\";
    }


    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean stringIsNotEmpty(String s) {
        if (null == s || "".equals(s)) {
            return false;
        }
        return true;
    }
}