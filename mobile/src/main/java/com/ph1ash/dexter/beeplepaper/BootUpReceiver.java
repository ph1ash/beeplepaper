package com.ph1ash.dexter.beeplepaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by dexter on 1/31/16.
 */
public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent i = new Intent(context, BeeplePaperService.class);
        context.startService(i);
    }
}
