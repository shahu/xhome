/*******************************************************************************
*
*
*    xhome-camera
*
*    StringUtils
*    TODO File description or class description.
*
*    @author: Evilsylvana
*    @since:  2013-9-7
*    @version: 1.0
*
******************************************************************************/
package com.xhome.camera.utils;

import com.xhome.camera.model.Constants;

/**
 * StringUtils of xhome-camera.
 * @author Evilsylvana
 *
 */

public class StringUtils {
    public static String getCameraName(String item) {
        return item.split(Constants.SEPARATE)[0];
    }

    public static String getCameraUrl(String item) {
        return item.split(Constants.SEPARATE)[1];
    }
}
