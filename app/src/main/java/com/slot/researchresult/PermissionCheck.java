package com.slot.researchresult;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by limecm on 16. 6. 12..
 */
public class PermissionCheck {

    public static final int RESULT_GRANTED      = 0;
    public static final int RESULT_NOT_GRANTED  = 1;
    public static final int RESULT_DENIED       = 2;
    public static final int RESULT_ERROR        = 3;

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 77;

    public static boolean checkWriteSettingPermission(Activity context) {

        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        if(!Settings.System.canWrite(context)) {
            return false;
        }
        return true;
    }

    public static void requestWriteSettionPermission(Activity context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);

        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }


    public static  boolean checkAndRequestPermissions(Activity context, String[] arrPermission) {

        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        List<String> listPermissionsNeeded = new ArrayList<>();

        for(int i = 0; i < arrPermission.length; i++) {
            int permission = ContextCompat.checkSelfPermission(context, arrPermission[i]);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(arrPermission[i]);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(context, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public static int onRequestPermissionsResult(Activity context, int requestCode,
                                                 String permissions[], int[] grantResults) {
        //Log.d(TAG, "Permission callback called-------");
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                    {
                        // Check for both permissions
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED ) {
                            Log.d("PermissionCheck", "++ result permission=" + permissions[i] + ", result=" + grantResults[i]);
                            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permissions[i])) {
                                return RESULT_NOT_GRANTED;
                            }
                            else {
                                return RESULT_DENIED;
                            }
                        }
                    }
                    return RESULT_GRANTED;
                }
            }
        }

        return RESULT_ERROR;
    }

}
