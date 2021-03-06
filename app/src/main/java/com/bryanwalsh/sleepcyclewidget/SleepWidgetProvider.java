package com.bryanwalsh.sleepcyclewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class SleepWidgetProvider extends AppWidgetProvider {
    private static final String onClick1 = "GET_TIME";
    private static final String onClick2 = "ADD_ALARM";
    private static final String prefUpdate = "android.appwidget.action.APPWIDGET_UPDATE";
    public Context timeContext;
    public Intent timeIntent;

    Locale aLocale = Locale.getDefault();
    Calendar calendar = Calendar.getInstance(aLocale);

    //java.util.Calendar
    int mHour = calendar.get(Calendar.HOUR);
    //int mHour24 = calendar.get(Calendar.HOUR_OF_DAY);
    int mMin = calendar.get(Calendar.MINUTE);

    //Times to be converted into readable w/ ConvertTime() method
    String min;
    String hour;
    String time24;
    String time12 = "Error Retrieving Locale";
    String nextTime1, nextTime2, nextTime3, nextTime4, nextTime5;

    //User Preference Modifiers
    int time_offset = 15;
    int cycle_num;
    boolean curr_flag;
    boolean toast_flag;

    //Alarm Broadcast Values
    final static int ABR = 1;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
//        int theme_id = 0;
//        try {
//            theme_id = Integer.parseInt(getDefaults("theme1", context));
//        } catch (NumberFormatException e) {
//            theme_id = 0;
//        }
//        int theme = R.layout.sleep_widget_light_rounded;
//        switch(theme_id) {
//            case 0:
//                theme = R.layout.sleep_widget_light_rounded;
//                break;
//            case 1:
//                theme = R.layout.sleep_widget_dark_rounded;
//                break;
//            case 2:
//                theme = R.layout.sleep_widget_pearl;
//                break;
//            case 3:
//                theme = R.layout.sleep_widget_champagne;
//                break;
//            case 4:
//                theme = R.layout.sleep_widget_light;
//                break;
//            case 5:
//                theme = R.layout.sleep_widget_dark;
//                break;
//            default:
//                theme = R.layout.sleep_widget_light_rounded;
//        }
        int theme = getTheme(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), theme);
        int[] idArray = new int[]{appWidgetId};

        Intent intent = new Intent(context, SleepWidgetProvider.class);
        intent.setAction(onClick1);
        intent.putExtra("appWidgetId", appWidgetId);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);
        intent.putExtra(AppWidgetManager.ACTION_APPWIDGET_UPDATE, idArray);

        views.setOnClickPendingIntent(R.id.sleep, PendingIntent.getBroadcast(context,0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        views.setOnClickPendingIntent(R.id.nT1, PendingIntent.getBroadcast(context, 0, intent.setAction(onClick2), PendingIntent.FLAG_UPDATE_CURRENT));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int theme = getTheme(context);
        super.onReceive(context, intent);
        buttonPending(context, theme);

        timeIntent = intent;
        timeContext = context; //links context and intent updates to UpdateTime()

        //User Preferences
        try {
            time_offset = Integer.parseInt(getDefaults("tts", context));
        }
        catch (NumberFormatException e) {
            time_offset = 15;
        }
        toast_flag = getDefaultBool("toast_flag", context);
        curr_flag = getDefaultBool("curr_flag", context);
        try {
            cycle_num = Integer.parseInt(getDefaults("cycle_amt", context)) + 3; //Partly hardcoded, fine now but fix later //Use an overloaded get() method;
        }
        catch (NumberFormatException e) {
            cycle_num = 5;
        }

        if (onClick1.equals(intent.getAction())) {
                if (time_offset >= 90) { //double check if user accidentally set offset too high
                Toast.makeText(context, "Are you sure it takes you " + time_offset + " minutes to sleep?" , Toast.LENGTH_LONG).show();
                    ConvertTime(); //Moved into if statement to prevent double-run on init for efficiency
                    UpdateTime(theme); //call helper
                }
                else {
                    ConvertTime();
                    UpdateTime(theme);
                    if (!toast_flag) {
                        Toast.makeText(context, "Time updated, sleep well!", Toast.LENGTH_SHORT).show();
                    }
                    //ToDo: Add option for 12h or 24h time style (after v1.0 release)
                }
        }
        //Disabled alarm picker until ready to implement
//        else if (onClick2.equals(intent.getAction())) {
//            openTimePickerDialog(context);
//        }

        else if (prefUpdate.equals(intent.getAction())) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName AppWidget = new ComponentName(context.getPackageName(), SleepWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(AppWidget);
                //Update misc. preferences
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), theme);
                remoteViews.setViewVisibility(R.id.default_ll, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.nextTimes_ll, View.GONE);
                remoteViews.setViewVisibility(R.id.currTime, View.GONE);
                //Restart widget instance to apply theme
                appWidgetManager.updateAppWidget(appWidgetIds, remoteViews); //Usage:
                    for (int appWidgetId : appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId);
                    }
        }
    }

    public void UpdateTime(int theme) { //Helper for grabbing time -only- when clicked
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(timeContext);
            ComponentName AppWidget = new ComponentName(timeContext.getPackageName(), SleepWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(AppWidget);

            RemoteViews remoteViews = new RemoteViews(AppWidget.getPackageName(), theme);
            remoteViews.setTextViewText(R.id.currTime, time12);

            remoteViews.setViewVisibility(R.id.default_ll, View.GONE);

            if (!curr_flag) {
                remoteViews.setViewVisibility(R.id.currTime, View.VISIBLE);
            }
            remoteViews.setViewVisibility(R.id.nextTimes_ll, View.VISIBLE);

            remoteViews.setTextViewText(R.id.nT1, nextTime1);
            remoteViews.setTextViewText(R.id.nT2, nextTime2);
            remoteViews.setTextViewText(R.id.nT3, nextTime3);
            remoteViews.setTextViewText(R.id.nT4, nextTime4);
            remoteViews.setTextViewText(R.id.nT5, nextTime5);

            switch(cycle_num) {
                case 5:
                    remoteViews.setViewVisibility(R.id.nT1, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.nT2, View.VISIBLE);
                    break;
                case 4:
                    remoteViews.setViewVisibility(R.id.nT1, View.GONE);
                    remoteViews.setViewVisibility(R.id.nT2, View.VISIBLE);
                    break;
                case 3:
                    remoteViews.setViewVisibility(R.id.nT1, View.GONE);
                    remoteViews.setViewVisibility(R.id.nT2, View.GONE);
                    break;
                default:
                    remoteViews.setViewVisibility(R.id.nT1, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.nT2, View.VISIBLE);
            }

            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    public void ConvertTime() {

        min = AppendMin(mMin);
        hour = AppendHour(mHour);

        //time24 = mHour24 + ":" + min;
        time12 = hour + ":" + min + GetMeridiem();

        NextTimes();
    }

    public String AppendMin(int integer) {
        if (integer < 10) {
            return "0" + Integer.toString(integer);
        }
                else {
            return Integer.toString(integer);
        }
    }

    public String AppendHour(int integer) {
        if (integer == 0)
            return "12";
        else {
            return Integer.toString(integer);
        }
    }

    public String GetMeridiem() { //am/pm for 12h format
        if (calendar.get(Calendar.AM_PM) != 0) {
            return " PM";
        }
        else {
            return " AM";
        }
    }

    public void NextTimes(){

        calendar.add(Calendar.MINUTE, 180 + time_offset);
        nextTime1 = ShortenString(AppendHour(calendar.get(Calendar.HOUR)) + ":" +
                AppendMin(calendar.get(Calendar.MINUTE)) + GetMeridiem());                               //TODO: Find a way to incorporate ConvertTimes() for min < 10

        calendar.add(Calendar.MINUTE, 90);
        nextTime2 = ShortenString(AppendHour(calendar.get(Calendar.HOUR)) + ":" +
                AppendMin(calendar.get(Calendar.MINUTE)) + GetMeridiem());

        calendar.add(Calendar.MINUTE, 90);
        nextTime3 = ShortenString(AppendHour(calendar.get(Calendar.HOUR)) + ":" +
                AppendMin(calendar.get(Calendar.MINUTE)) + GetMeridiem());

        calendar.add(Calendar.MINUTE, 90);
        nextTime4 = ShortenString(AppendHour(calendar.get(Calendar.HOUR)) + ":" +
                AppendMin(calendar.get(Calendar.MINUTE)) + GetMeridiem());

        calendar.add(Calendar.MINUTE, 90);
        nextTime5 = ShortenString(AppendHour(calendar.get(Calendar.HOUR)) + ":" +
                AppendMin(calendar.get(Calendar.MINUTE)) + GetMeridiem());
        Log.e("NextTimes:", nextTime1 + ", " + nextTime2 + ", " + nextTime3 + ", " + nextTime4 + ", " + nextTime5);
    }

    public String ShortenString(String nextTime) {
        String temp = "";
        String[] strArray = nextTime.split("\\s+");
        StringBuffer result = new StringBuffer();

        for (String singleWord : strArray) {
            if ((temp + singleWord).length() > 5) {
                result.append(temp + "\n");
                temp = singleWord;
            } else {
                temp = temp + singleWord;
            }
        }
        if (temp.length() > 0) {
            result.append(temp);
        }
        return result.toString();
    }

    private void openTimePickerDialog(Context context) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                onTimeSetListener, 12, 30, false);
        timePickerDialog.setTitle("Set Alarm Time");

        timePickerDialog.show();
    }
    TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            Calendar calNow = Calendar.getInstance();
            Calendar calSet = (Calendar) calNow.clone();

            calSet.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calSet.set(Calendar.MINUTE, minute);
            calSet.set(Calendar.SECOND, 0);
            calSet.set(Calendar.MILLISECOND, 0);

            if (calSet.compareTo(calNow) <= 0) {
                // Today Set time passed, count to tomorrow
                calSet.add(Calendar.DATE, 1);
            }

            setAlarm(calSet);
        }
    };

    private void setAlarm(Calendar targetCal) {

        Intent intent = new Intent(timeContext, addAlarmActivity.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                timeContext, ABR, intent, 0);
        AlarmManager alarmManager = (AlarmManager) timeContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, targetCal.getTimeInMillis(),
                pendingIntent);
    }

    //Internal helper functions
    public static String getDefaults(String key, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, null);
    }

    public static Boolean getDefaultBool(String key, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, false);
    }

    public static PendingIntent buttonPendingIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction("GET_TIME");
        Log.e("buttonPendingIntent", "Run2");
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void buttonPending(Context context, int theme_id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), theme_id);
        views.setOnClickPendingIntent(R.id.sleep, SleepWidgetProvider.buttonPendingIntent(context));
        Log.e("buttonPendingCall", "Run1");
    }

    public static int getTheme(Context context) {
        int theme_id = 0;
        try {
            theme_id = Integer.parseInt(getDefaults("theme1", context));
        } catch (NumberFormatException e) {
            theme_id = 0;
        }
        int theme = R.layout.sleep_widget_light_rounded;
        switch(theme_id) {
            case 0:
                theme = R.layout.sleep_widget_light_rounded;
                break;
            case 1:
                theme = R.layout.sleep_widget_dark_rounded;
                break;
            case 2:
                theme = R.layout.sleep_widget_pearl;
                break;
            case 3:
                theme = R.layout.sleep_widget_champagne;
                break;
            case 4:
                theme = R.layout.sleep_widget_light;
                break;
            case 5:
                theme = R.layout.sleep_widget_dark;
                break;
            default:
                theme = R.layout.sleep_widget_light_rounded;
        }
        return theme;
    }
}