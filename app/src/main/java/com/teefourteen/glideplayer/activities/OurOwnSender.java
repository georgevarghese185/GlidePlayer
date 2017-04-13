package com.teefourteen.glideplayer.activities;

import android.content.Context;

import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

/**
 * Created by Karan D'souza on 11-04-2017.
 */

public class OurOwnSender implements ReportSender {
    @Override
    public void send(Context context, CrashReportData report) throws ReportSenderException {

    }
}
