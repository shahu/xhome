/******************************************************************************
*
* Copyright 2012, Baina Technologies Co. Ltd.
*
* File name : NetworkUtil.java
* Create time : 2012-2-16
* Author : xhu
* Description : TODO
* History:
*     1. Date: 2012-2-16
*         Author: xhu
*         Modifycation: Create class.
*****************************************************************************/
package com.xhome.camera.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author xhu
 *
 */
public class NetworkUtil {


    private static Method sEnableDataConnectivityMethod;

    private static Method sDisableDataConnectivityMethod;

    private static Method sSetMobileEnableMethod;

    /**
     * 判断网络（包括WIFI和移动网络）是否可用
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivity != null) {
            final NetworkInfo[] info = connectivity.getAllNetworkInfo();

            if(info != null) {
                for(int i = 0; i < info.length; i = i + 1) {
                    if(info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
        * 判断网络是否连接上（包括WIFI和移动网络）
        * @param context
        * @return whether the Mobile Data is connected
        */
    public static boolean isNetworkConnected(Context context) {
        Boolean isEnabled = false;

        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            isEnabled = telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED ;

        } catch(Exception e) {
            e.printStackTrace();

        } catch(Error e) {
            e.printStackTrace();
        }

        return isEnabled;
    }

    /**
        * 判断移动网络是否连接上
        * @param context
        * @return
        */

    public static synchronized boolean isMobileNetworkConnected(final Context context) {
        boolean isMobileNetworkConnected = false;
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo mobileNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if(null != mobileNetworkInfo) {
            isMobileNetworkConnected = mobileNetworkInfo.getState() == NetworkInfo.State.CONNECTED;
        }

        return isMobileNetworkConnected;
    }

    /**
     * 打开或者关闭移动网络（GPRS/2G/3G/4G）,这里以Android 2.3为分割点，使用不同方法来进行调用
     * @param context
     * @param enable
     * @return
     */
    public static boolean setMobileNetworkEnable(Context context, boolean enable) {
        boolean success = false;

        if(isGingerbreadOrlater()) {
            success = setMobileEnableOnConnectivityManager(context, enable);

        } else {
            success = setMobileEnableOnTelephony(context, enable);
        }

        return success;
    }

    /**
     * Open or close Mobile Data through invoking ITelephony interface with reflection
     * @param context
     * @param enable true for Open Mobile Data,or false.
     * @return whether invoke successfully.
     */
    private static boolean setMobileEnableOnTelephony(Context context, boolean enable) {
        boolean success = false;

        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // Will be used to invoke hidden methods with reflection
            // Get the current object implementing ITelephony interface
            Class telManager = telephonyManager.getClass();
            Method getITelephony = telManager.getDeclaredMethod("getITelephony");
            getITelephony.setAccessible(true);
            Object telephonyObject = getITelephony.invoke(telephonyManager);

            // Call the enableDataConnectivity/disableDataConnectivity method
            // of Telephony object
            if(enable) {
                if(null == sEnableDataConnectivityMethod) {
                    Class telephonyClass = telephonyObject.getClass();

                    sEnableDataConnectivityMethod = telephonyClass.getMethod("enableDataConnectivity");
                    sEnableDataConnectivityMethod.setAccessible(true);
                }

                success = (Boolean)sEnableDataConnectivityMethod.invoke(telephonyObject);

            } else {
                if(null == sDisableDataConnectivityMethod) {
                    Class telephonyClass = telephonyObject.getClass();

                    sDisableDataConnectivityMethod = telephonyClass.getMethod("disableDataConnectivity");
                    sDisableDataConnectivityMethod.setAccessible(true);
                }

                success = (Boolean)sDisableDataConnectivityMethod.invoke(telephonyObject);
            }

        } catch(Exception e) {
            success = false;
            e.printStackTrace();
        }

        return success;
    }

    /**
     * Open or close Mobile Data through invoking ConnectivityManager interface with reflection
     * @param context
     * @param enable true for Open Mobile Data,or false.
     * @return whether invoke successfully.
     */
    private static boolean setMobileEnableOnConnectivityManager(Context context, boolean enable) {
        boolean setSuccess = false;

        try {
            ConnectivityManager localConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if(null == sSetMobileEnableMethod) {
                Class localClass1 = localConnectivityManager.getClass();
                Class[] arrayOfClass = new Class[1];
                Class localClass2 = Boolean.TYPE;
                arrayOfClass[0] = localClass2;
                sSetMobileEnableMethod = localClass1.getMethod("setMobileDataEnabled", arrayOfClass);
                sSetMobileEnableMethod.setAccessible(true);
            }

            Object[] arrayOfObject = new Object[1];
            Boolean localBoolean = Boolean.valueOf(enable);
            arrayOfObject[0] = localBoolean;
            sSetMobileEnableMethod.invoke(localConnectivityManager, arrayOfObject);
            setSuccess = true;

        } catch(Exception e) {
            e.printStackTrace();
        }

        return setSuccess;
    }

    /**
    * 判断WIFI网络是否连接上
    * @param context
    * @return
    */

    public static synchronized boolean isWifiConnected(final Context context) {
        boolean isWifiConnected = false;
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if(null != networkInfo) {
            isWifiConnected = networkInfo.getState() == NetworkInfo.State.CONNECTED ;
        }

        return isWifiConnected;
    }

    /**
        * 只判断WIFI是否已打开，不能反应出WIFI的连接状态（是否连接上）
        * @param context
        * @return
        */
    public static boolean isWifiEnable(final Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        boolean isWifiConnect = false;

        if(null != wifiManager) {
            isWifiConnect = wifiManager.isWifiEnabled();
        }

        return isWifiConnect;
    }


    /**
     * 打开或者关闭WIFI开关。调用后，请用广播事件监听WIFI的连接状态
     * @param context
     * @param enable
     */
    public static void setWifiEnable(Context context, boolean enable) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(enable);
    }


    /**
     * Android2.3 or later
     * @return
     */
    private static boolean isGingerbreadOrlater() {
        boolean IsGingerbread = false;
        final Integer sdkInt = Integer.valueOf(Build.VERSION.SDK_INT);

        if(sdkInt >= 9) {
            IsGingerbread = true;
        }

        return IsGingerbread;
    }

    public static String getLocalIpAddress() {
        String ip = "";

        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();

            while(en.hasMoreElements()) {
                final NetworkInterface intf = en.nextElement();
                final Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();

                while(enumIpAddr.hasMoreElements()) {
                    final InetAddress inetAddress = enumIpAddr.nextElement();

                    if(!inetAddress.isLoopbackAddress()) {
                        ip = inetAddress.getHostAddress().toString();
                    }
                }
            }

        } catch(final SocketException ex) {
            ex.printStackTrace();
        }

        return ip;
    }

}
