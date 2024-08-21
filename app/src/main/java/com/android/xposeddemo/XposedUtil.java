package com.android.xposeddemo;

import dalvik.system.BaseDexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class XposedUtil implements IXposedHookLoadPackage {
    public static String TAG = "Xposed";
    public static ClassLoader classLoader;
    public static String packageName;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        classLoader = loadPackageParam.classLoader;//类加载器
        packageName = loadPackageParam.packageName;//应用名
        try {
            //在这里对应用名进行筛选
            switch (packageName) {
                //贪吃蛇
                case "io.supercent.linkedcubic":
                    doSupercent();
                    break;
                //谷歌商店
                case "com.android.vending":
                    doVending();
                    break;
                default:
                    break;
            }
            //打印包名，如果没看到包名说明Xposed代码没生效，配置有问题，可能是面具没装，Lposed没生效等
            Log.e(TAG, packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //hook 类里面的普通方法
    public static void StringBuilder_toString(String re) {
        /**
         java.lang.StringBuilder是类名，"toString"是方法名，如果还有参数的话还需要再方法名后加参数
         比如 StringBuilder.toString(String,int)方法要怎么写
         XposedHelpers.findAndHookMethod("java.lang.StringBuilder", classLoader, "toString",String.class,int.calss, new XC_MethodHook() {
         参数可以说String.class这种格式，也可以是"java.long.String"这种类名路径，都是可以的
         但是对于app内部的自己的类，还是传类名路径好一点
         */

        XposedHelpers.findAndHookMethod("java.lang.StringBuilder", classLoader, "toString", new XC_MethodHook() {
            //这个是方法内部调用之前执行
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }
            //这个是方法内部调用之后执行
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //获取方法的返回值
                String result = (String) param.getResult();
                //针对某些特定的参数进行打印
                if (result.contains(re) && result.length() < 80) {
                    Log.e(TAG, "appsflyer_kef:" + result);
                }
            }
        });
    }

    //hook 类的构造方法
    public static void HookConstructer(){
        try {
            Class<?> aClass = classLoader.loadClass("java.lang.StringBuilder");
            XposedBridge.hookAllConstructors(aClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    //hook 类的类名，所有方法，所有参数
    public static void Hook3(){
        try {
            Class<?> aClass = classLoader.loadClass("java.lang.StringBuilder");
            XposedBridge.hookAllConstructors(aClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object thisObject = param.thisObject;
                    System.out.println("类名："+thisObject.getClass().getName());
                    //这个类的所有方法
                    Method[] declaredMethods = thisObject.getClass().getDeclaredMethods();
                    //遍历所有方法
                    for(Method method:declaredMethods){
                        System.out.println("方法名："+method.getName()+"--"+method.toString());
                    }
                    //这个类的所有参数
                    Field[] declaredFields = thisObject.getClass().getDeclaredFields();
                    for(Field field:declaredFields){
                        System.out.println("参数名："+field.getClass().getName()+"--内容："+field);
                    }
                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }


    //hookMap的put调用方法，主要看某个值是什么地方加上的
    public static void Map_Put(String key) {
        XposedHelpers.findAndHookMethod(HashMap.class, "put", Object.class, Object.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] instanceof String) {
                    String p1 = (String) param.args[0];
                    if (p1.equals(key)) {
                        Log.e(TAG, "key:" + param.args[0] + "--value:" + param.args[1]);
                    }
                }
            }
        });
    }

    //App内有些类直接hook会失败，因为它是通过动态加载得到的，他们存放在dex文件里通过BaseDexClassLoader加载
    public static void BaseDexClassLoader(ClassLoader classLoader) {
        Class<?> clazz = XposedHelpers.findClass("dalvik.system.BaseDexClassLoader", classLoader);
        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //BaseDexClassLoader是一个类加载器
                BaseDexClassLoader baseDexClassLoader = (BaseDexClassLoader) param.thisObject;
                try {
                    Class<?> aClass = baseDexClassLoader.loadClass("com.appsflyer.internal.AFb1zSDK");
                    Log.e(TAG, "加载成功");
                } catch (Exception e) {
                }
            }
        });
    }

    //Hook app发送的网络请求
    public static void Find_Url(ClassLoader classLoader, String urlname) {
        Class<?> clazz = XposedHelpers.findClass("java.net.URL", classLoader);
        XposedHelpers.findAndHookConstructor(clazz, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String url = (String) param.args[0];
                if (url.contains(urlname)) {
                    Log.e(TAG, param.args[0] + "");
                    Log.d(TAG, "URL请求：", new Throwable("URL请求:"));
                }
            }
        });
    }


    //将动态加载的dex文件缓存到指定目录下
    public static void LoadDex_DexPathList(ClassLoader classLoader, String appname) {
        XposedHelpers.findAndHookMethod("dalvik.system.DexPathList", classLoader, "initByteBufferDexPath", ByteBuffer[].class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.e(TAG, "DexPathList.initByteBufferDexPath()");
                try {
                    ByteBuffer[] arg = (ByteBuffer[]) param.args[0];
                    //定义Dex目录路径
                    String dexDirPath = "/data/data/"+packageName+"/Dex";
                    File dexDir = new File(dexDirPath);
                    //如果4Dex目录不存在，创建目录
                    if (!dexDir.exists()) {
                        dexDir.mkdirs();
                    }
                    //定义out.dex文件路径
                    String outputPath = dexDirPath + "/out" + System.currentTimeMillis() + "af.dex";
                    File outputFile = new File(outputPath);
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    //将ByteBuffer[]转换为File并保存为out.dex
                    try (FileOutputStream fos = new FileOutputStream(outputFile);
                         FileChannel fc = fos.getChannel()) {
                        for (ByteBuffer buffer : arg)
                            fc.write(buffer);
                    } catch (Exception e) {
                        //处理异常
                        Log.e(TAG,TAG,e);
                    }
                    Log.e(TAG,"Dex缓存成功，文件位于手机的位置：" + outputPath);
                } catch (Exception e) {
                    Log.e(TAG,TAG,e);
                }
            }
        });
    }

    //缓存动态加载的dex文件到app目录下
    public static void LoadDex_BaseDexClassLoader(ClassLoader classLoader, String appname) {
        Class<?> clazz = XposedHelpers.findClass("dalvik.system.BaseDexClassLoader", classLoader);
        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Log.e(TAG, "uid=" + Process.myUid());
                try {
                    ByteBuffer[] arg = (ByteBuffer[]) param.args[0];
                    //定义4Dex目录路径
                    String dexDirPath = "/data/data/" + appname + "/Dex";
                    File dexDir = new File(dexDirPath);
                    //如果4Dex目录不存在，创建目录
                    if (!dexDir.exists()) {
                        dexDir.mkdirs();
                    }
                    //定义out.dex文件路径
                    String outputPath = dexDirPath + "/out" + System.currentTimeMillis() + "af.dex";
                    File outputFile = new File(outputPath);
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    //将ByteBuffer[]转换为File并保存为out.dex
                    try (FileOutputStream fos = new FileOutputStream(outputFile);
                         FileChannel fc = fos.getChannel()) {
                        for (ByteBuffer buffer : arg)
                            fc.write(buffer);
                    } catch (Exception e) {
                        //处理异常
                    }
                    Log.e(TAG,"Dex缓存成功，文件位于手机的位置：" + outputPath);
                } catch (Exception e) {

                    Log.e(TAG,TAG,e);
                }
            }
        });

    }

    //查看Activtiy的切换
    public static void intent_SetAction() {
        XposedBridge.hookAllConstructors(Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (int i = 0; i < param.args.length; i++) {
                    Log.e(TAG, "Intent参数" + i + ":参数类型" + param.args[i].getClass().getName() + "--参数内容：" + param.args[i]);
                }
            }
        });
    }


    private void doVending() {

    }

    private void doSupercent() {

    }
}