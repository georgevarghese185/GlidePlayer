package com.teefourteen.glideplayer.activities;
import android.content.Context;

import com.teefourteen.glideplayer.R;

import org.acra.*;
import org.acra.annotation.*;

/**
 * Created by Karan D'souza on 11-04-2017.
 */
@ReportsCrashes(mailTo = "georgevarghese185@yahoo.com",
        mode = ReportingInteractionMode.DIALOG,
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT },
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast,
        resDialogTheme = R.style.AppTheme_Dialog,
        reportSenderFactoryClasses = {Our_Sender_Factory.class}
                )
public class MyApplication extends android.app.Application {
    @Override
    protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        ACRA.init(this);


    }

}
