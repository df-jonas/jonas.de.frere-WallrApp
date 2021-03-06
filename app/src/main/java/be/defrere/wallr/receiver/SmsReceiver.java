package be.defrere.wallr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.List;

import be.defrere.wallr.R;
import be.defrere.wallr.WallrNewTextNotification;
import be.defrere.wallr.database.AppDatabase;
import be.defrere.wallr.entity.Event;
import be.defrere.wallr.util.http.HttpInterface;
import be.defrere.wallr.util.http.HttpRequest;
import be.defrere.wallr.util.http.HttpResponse;
import be.defrere.wallr.util.http.HttpTask;
import be.defrere.wallr.util.http.HttpVerb;
import be.defrere.wallr.entity.Text;

public class SmsReceiver extends BroadcastReceiver implements HttpInterface {

    private AppDatabase db;
    private Text t;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;
        db = AppDatabase.getAppDatabase(context);

        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {

            Bundle b = intent.getExtras();
            StringBuilder message = new StringBuilder();
            String source = "";

            if (b != null) {
                try {
                    Object[] pdus = (Object[]) b.get("pdus");
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage m = SmsMessage.createFromPdu((byte[]) pdu);
                            source = m.getOriginatingAddress();
                            message.append(m.getMessageBody());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Exception caught! " + e.getMessage());
                }
            }

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + context.getSharedPreferences(context.getString(R.string.preferences), Context.MODE_PRIVATE).getString("api_token", ""));

            List<Event> events = db.eventDao().all();
            for (Event e : events) {
                String k = e.getKeyword();

                String msgKey = "";
                if (k.length() <= message.toString().trim().length()) {
                    msgKey = message.toString().trim().substring(0, k.length()).toUpperCase();
                }
                String oriKey = k.trim().toUpperCase();

                if (message.length() > k.length() && msgKey.equals(oriKey)) {

                    System.out.println(source + ": " + message);

                    HashMap<String, String> params = new HashMap<>();
                    params.put("source", source);
                    params.put("message", message.toString().trim().substring(k.length() + 1));

                    t = new Text();
                    t.setEventId(e.getId());
                    t.setSource(source);
                    t.setContent(message.toString());

                    HttpRequest request = new HttpRequest("texts/" + e.getId(), HttpVerb.POST, params, headers);
                    new HttpTask(this).execute(request);
                }
            }
        }
    }

    @Override
    public void httpCallback(HttpResponse httpResponse) {
        if (httpResponse.getResponseCode() == 200) {
            System.out.println("Task executed successfully.");
            WallrNewTextNotification.notify(context);
            t.setSynced(true);
        } else {
            System.err.println("The task for source " + t.getSource() + " failed.");
            t.setSynced(false);
        }
        db.textDao().insert(t);
    }
}
