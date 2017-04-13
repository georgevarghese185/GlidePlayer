package com.teefourteen.glideplayer.activities;

import android.content.Context;

import org.acra.ACRA;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

/**
 * Created by Karan D'souza on 11-04-2017.
 */

public class Our_Sender_Factory implements ReportSenderFactory {
    @Override
    public ReportSender create(Context context, ACRAConfiguration config){

        return new OurOwnSender();
    }
}
