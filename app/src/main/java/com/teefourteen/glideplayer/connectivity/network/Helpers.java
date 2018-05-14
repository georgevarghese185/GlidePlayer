package com.teefourteen.glideplayer.connectivity.network;

import android.support.annotation.Nullable;

import java.io.Closeable;

public class Helpers {
    public static void safeClose(@Nullable Closeable obj) {
        if(obj != null) {
            try {
                obj.close();
            } catch (Exception e) {
                //Ignore
            }
        }
    }
}
