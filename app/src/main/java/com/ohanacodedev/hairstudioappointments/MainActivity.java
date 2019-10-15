package com.ohanacodedev.ocsimpleappointments;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;



public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";

    private static final String LOCALE_EN = "en";
    private static final String LOCALE_SR = "sr";

    private static final String KEY_LOCALE = "locale";
    private static final String KEY_THEME = "theme";
    private static final String KEY_DATA = "app_data";

    private static final int ACTION_TYPE_DEFAULT = 0;
    private static final int ACTION_TYPE_UP = 1;
    private static final int ACTION_TYPE_RIGHT = 2;
    private static final int ACTION_TYPE_DOWN = 3;
    private static final int ACTION_TYPE_LEFT = 4;
    private static final int SLIDE_RANGE = 100;

    private float mTouchStartPointX;
    private float mTouchStartPointY;
    private int mActionType = ACTION_TYPE_DEFAULT;
    private TextView current_day;
    private Calendar cal;

    private ArrayList<String> clients = new ArrayList<>();
    private ArrayAdapter<String> clientAdapter;

    private static String appLocale;
    private final int displayInterval = 30;
    private ImageButton weekPrev;
    private ImageButton weekNext;
    private ImageButton dayPrev;
    private ImageButton dayNext;
    private ListView termini;

    private FloatingActionButton addAppointmentBtn;

    private int themeNumber = 0;
    public static String addAppointmentTime = "";
    public static String addAppointmentText = "";
    public static final String DIVIDER = "~~~";
    public static final String DIV_DATE = "%DATE%";
    public static final String DIV_TIME = "%TIME%";
    public static final String DIV_MAIN = "%MAIN%";
    public static final String DIV_ITEM = "%ITEM%";

    public static Map<String, Map<String, String>> clientList = new HashMap<>();


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* Get credentials from saved settings */
        getSavedSettings();

        setLocale();

        /* Get current date */
        cal = Calendar.getInstance();
        current_day = findViewById(R.id.text_date);
        current_day.setText(formatTitleDate());

        /* Display list of appointments */
        termini = findViewById(R.id.list_termini);
        clientAdapter = new patientsListAdapter(this, clients);

        termini.setAdapter(clientAdapter);

        termini.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                setNextDay();
                updateUi();
            }

            @Override
            public void onSwipeRight() {
                setPreviousDay();
                updateUi();
            }
        });

        termini.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                changeTableEntry(position);
            }
        });

        /* Set button for previous week */
        weekPrev = findViewById(R.id.imgBtn_w_prev);
        weekPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreviousWeek();
                updateUi();
            }
        });

        /* Set button for next week */
        weekNext = findViewById(R.id.imgBtn_w_next);
        weekNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNextWeek();
                updateUi();
            }
        });

        /* Set button for next day */
        dayNext = findViewById(R.id.imgBtn_d_next);
        dayNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNextDay();
                updateUi();
            }
        });

        /* Set button for previous week */
        dayPrev = findViewById(R.id.imgBtn_d_prev);
        dayPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreviousDay();
                updateUi();
            }
        });

        /* Add appointment button */
        addAppointmentBtn = findViewById(R.id.addAppointment);
        addAppointmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAppointment();
            }
        });

        changeTheme(false);
        updateUi();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.set_locale:
                changeLocale();
                return true;
            case R.id.change_theme:
                changeTheme(true);
                return true;
            case R.id.about:
                aboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /* This implements appointment list slide detection. */
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartPointX = event.getRawX();
                mTouchStartPointY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchStartPointX - x > SLIDE_RANGE) {
                    mActionType = ACTION_TYPE_LEFT;
                } else if (x - mTouchStartPointX > SLIDE_RANGE) {
                    mActionType = ACTION_TYPE_RIGHT;
                } else if (mTouchStartPointY - y > SLIDE_RANGE) {
                    mActionType = ACTION_TYPE_UP;
                } else if (y - mTouchStartPointY > SLIDE_RANGE) {
                    mActionType = ACTION_TYPE_DOWN;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mActionType == ACTION_TYPE_RIGHT) {
                    setPreviousWeek();
                } else if (mActionType == ACTION_TYPE_LEFT) {
                    setNextWeek();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void changeTheme(boolean toggle){

        if(toggle){
            themeNumber++;

            if(themeNumber > 2){
                themeNumber = 0;
            }

            saveSettings();
        }

        if(themeNumber == 0) {
            GlobalVars.colorBg1 = getString(R.string.colorBlack);
            GlobalVars.colorBg2 = getString(R.string.colorGrayDark);
            GlobalVars.colorBg3 = getString(R.string.colorGrayLight);
            GlobalVars.colorText1 = getString(R.string.colorOrange);
            GlobalVars.colorText2 = getString(R.string.colorBlack);
            GlobalVars.colorText3 = getString(R.string.colorWhite);
            GlobalVars.colorText4 = getString(R.string.colorWhite);

        }else if(themeNumber == 1){
            GlobalVars.colorBg1 = getString(R.string.colorPinkDark);
            GlobalVars.colorBg2 = getString(R.string.colorPinkLight );
            GlobalVars.colorBg3 = getString(R.string.colorPinkDark);
            GlobalVars.colorText1 = getString(R.string.colorWhite);
            GlobalVars.colorText2 = getString(R.string.colorBlack);
            GlobalVars.colorText3 = getString(R.string.colorBlack);
            GlobalVars.colorText4 = getString(R.string.colorWhite);
        }else{
            GlobalVars.colorBg1 = getString(R.string.colorPurpleDark);
            GlobalVars.colorBg2 = getString(R.string.colorPurpleLight );
            GlobalVars.colorBg3 = getString(R.string.colorPurpleDark);
            GlobalVars.colorText1 = getString(R.string.colorWhite);
            GlobalVars.colorText2 = getString(R.string.colorBlack);
            GlobalVars.colorText3 = getString(R.string.colorBlack);
            GlobalVars.colorText4 = getString(R.string.colorWhite);
        }


        current_day.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        current_day.setTextColor(Color.parseColor(GlobalVars.colorText1));
        weekPrev.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        weekNext.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        dayPrev.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        dayNext.setBackgroundColor(Color.parseColor(GlobalVars.colorBg1));
        termini.setBackgroundColor(Color.parseColor(GlobalVars.colorBg2));
        findViewById(R.id.mainLayout).setBackgroundColor(Color.parseColor(GlobalVars.colorBg2));
        addAppointmentBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(GlobalVars.colorBg1)));

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(GlobalVars.colorBg1)));

        clientAdapter.notifyDataSetChanged();
    }

    private void aboutDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final TextView message = new TextView(this);
        final SpannableString s = new SpannableString(getString(R.string.about_msg));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setText(s);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        layout.addView(message);

        builder.setView(layout);

        builder.setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }


    public class TimePickerCustom extends TimePickerDialog{

        public TimePickerCustom(Context arg0, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
            super(arg0, 2, callBack, hourOfDay, minute, is24HourView);
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            // TODO Auto-generated method stub
            //super.onTimeChanged(arg0, arg1, arg2);
            if (mIgnoreEvent)
                return;
            if (minute%displayInterval!=0){
                int minuteFloor=minute-(minute%displayInterval);
                minute=minuteFloor + (minute==minuteFloor+1 ? displayInterval : 0);
                if (minute==60)
                    minute=0;
                mIgnoreEvent=true;
                view.setCurrentMinute(minute);
                mIgnoreEvent=false;
            }
        }

        // NOTE: Change this if you want to change displayed interval
        private boolean mIgnoreEvent=false;

    }

    /* Show a dialog to add an appointment. */
    private void addAppointment(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.new_appointment));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText in_time = new EditText(this);
        in_time.setHint(getString(R.string.time));
        /* Set last chosen time to enable editing existing appointments */
        if(addAppointmentTime.length() < 5){
            addAppointmentTime = "00:00";
        }

        in_time.setText(addAppointmentTime);
        in_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] selected_time = addAppointmentTime.split(":");
                int hourOfDay = Integer.valueOf(selected_time[0]);
                int minute = Integer.valueOf(selected_time[1]);

                TimePickerCustom timePickerDialog = new TimePickerCustom(MainActivity.this ,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            in_time.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
                        }
                    }, hourOfDay, minute, true);

                timePickerDialog.show();
            }
        });

        layout.addView(in_time);

        final EditText in_message = new EditText(this);
        in_message.setHint(getString(R.string.appointment_text));
        in_message.setText(addAppointmentText);
        layout.addView(in_message);

        builder.setView(layout);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                addAppointmentText = in_message.getText().toString();
                addAppointmentTime = in_time.getText().toString();

                if(addAppointmentTime.length() > 2){
                    // Save this appointment
                    addNewAppointment(formatQueryDate(), addAppointmentTime, addAppointmentText);
                }

                dialog.dismiss();
            }
        });
    }

    /* Creates a string representing a user readable date */
    private String formatTitleDate(){
        return String.format( new Locale(appLocale), "%1$tA, %1$td.%1$tb.%1$tY.", cal);
    }

    /* Creates a string to be used as date in the server url query */
    public String formatQueryDate(){
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        return df.format(cal.getTime());
    }

    /* Sets the date to previous week and triggers the new appointment update. */
    protected void setPreviousWeek() {
        cal.add(Calendar.DAY_OF_MONTH, -7);

        if(cal.compareTo(Calendar.getInstance()) < 0) {
            cal = Calendar.getInstance();
        }

        current_day.setText(formatTitleDate());
    }

    /* Sets the date to next week and triggers the new appointment update. */
    protected void setNextWeek() {
        cal.add(Calendar.DAY_OF_MONTH, 7);
        current_day.setText(formatTitleDate());
    }

    /* Sets the date to previous day and triggers the new appointment update. */
    protected void setPreviousDay() {
        cal.add(Calendar.DAY_OF_MONTH, -1);

        if(cal.compareTo(Calendar.getInstance()) < 0) {
            cal = Calendar.getInstance();
        }

        current_day.setText(formatTitleDate());
    }

    /* Sets the date to next day and triggers the new appointment update. */
    protected void setNextDay() {
        cal.add(Calendar.DAY_OF_MONTH, 1);
        current_day.setText(formatTitleDate());
    }

    /* Selects the clicked appointment and opens the appointment update dialog. */
    private void changeTableEntry(int id){
        try {
            String[] entry = clients.get(id).split(": ");
            addAppointmentTime = entry[0];
            addAppointmentText = entry[1].trim();
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
            addAppointmentTime =  clients.get(id);
            addAppointmentText = "";
        }

        addAppointment();
    }


    public void updateUi(){
        try {
            /* The following is to populate vacant appointments for better preview. */
            Map<String, String> displayList = clientList.get(formatQueryDate());

            clients.clear();

            boolean firstAppointmentFound = false;

            int lastT = 0;
            int resPerHour = 60 / displayInterval;
            for (int i = 0; i < (24 * resPerHour); i++) {

                int h = i / resPerHour;
                int m = (i % resPerHour) * displayInterval;

                String curTime = String.format("%02d:%02d", h, m);

                if (displayList.containsKey(curTime)) {
                    if (firstAppointmentFound) {
                        /* populate free appointments with blank data for better preview */
                        for (int j = lastT; j < i; j++) {
                            h = j / resPerHour;
                            m = (j % resPerHour) * displayInterval;
                            String newTime = String.format("%02d:%02d", h, m);
                            clients.add(newTime);
                        }
                    }

                    firstAppointmentFound = true;
                    lastT = i + 1;

                    String recordedAp = curTime + ": " + displayList.get(curTime);

                    if (recordedAp.contains(DIVIDER)) {
                        clients.addAll(Arrays.asList(recordedAp.split(DIVIDER)));
                    } else {
                        clients.add(recordedAp);
                    }
                }
            }
        }catch(Exception e){
            Log.e(TAG, e.getMessage());

        }

        /* needed to refresh display */
        clientAdapter.notifyDataSetChanged();
    }

    private void setLocale(){
        Locale locale = new Locale(appLocale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        setTitle(getString(R.string.app_name));

    }

    private void changeLocale(){
        if(appLocale.equals(LOCALE_EN)){
            appLocale = LOCALE_SR;
        }else{
            appLocale = LOCALE_EN;
        }

        setLocale();
        saveSettings();
        /* Restart the application to apply the new locale. */
        restart();
    }

    /* Restarts the application. */
    public void restart(){
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        finish();
        startActivity(i);

    }

    private void addNewAppointment(String date, String timeString, String clientName){

        Map<String, String> todaysAppointments = clientList.get(date);

        if (todaysAppointments == null) {
            todaysAppointments = new HashMap<>();
        }

        if(clientName.length() > 2) {
            todaysAppointments.put(timeString, clientName);
        }else if(todaysAppointments.containsKey(timeString)){
            todaysAppointments.remove(timeString);
        }
        clientList.put(date, todaysAppointments);


        updateUi();
        saveSettings();
    }

    private String serializeAppointments(){
        String result = "";

        for (String date : clientList.keySet()) {
            result += date + DIV_DATE;

            Map<String, String> todaysAppointments = clientList.get(date);

            for (String timeString : todaysAppointments.keySet()) {
                String clientData = todaysAppointments.get(timeString);
                result += timeString + DIV_TIME + clientData + DIV_ITEM;
            }

            result += DIV_MAIN;
        }

        return result;
    }

    private void deSerializeAppointments(String data){
        clientList.clear();

        String[] dateData = data.split(DIV_MAIN);
        try {
            for (int i = 0; i < dateData.length; i++) {
                String currentDateData = dateData[i];
                String[] currentDateDataArray = currentDateData.split(DIV_DATE);

                String currentDate = currentDateDataArray[0];

                // Check if this date has passed
                DateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);

                try {
                    Date date = format.parse(currentDateData);

                    Calendar yesterdayCalendar = Calendar.getInstance();
                    yesterdayCalendar.add(Calendar.DAY_OF_MONTH,-1);

                    if(date.before(yesterdayCalendar.getTime()) ){
                        continue;
                    }
                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                }



                String[] todayAppointments = currentDateDataArray[1].split(DIV_ITEM);

                Map<String, String> appointmentData = new HashMap<>();

                for (int t = 0; t < todayAppointments.length; t++) {
                    String[] currentTimeData = todayAppointments[t].split(DIV_TIME);
                    String timeString = currentTimeData[0];
                    String nameAndService = currentTimeData[1];

                    appointmentData.put(timeString, nameAndService);
                }

                clientList.put(currentDate, appointmentData);
            }
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    /* Reads saved settings from shared preferences. */
    private void getSavedSettings(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        appLocale = sharedPref.getString(KEY_LOCALE, LOCALE_EN);
        themeNumber = sharedPref.getInt(KEY_THEME, 0);
        deSerializeAppointments(sharedPref.getString(KEY_DATA, ""));
    }

    /* Saves the settings to the shared preferences. */
    private void saveSettings(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_LOCALE, appLocale);
        editor.putInt(KEY_THEME, themeNumber);

        editor.putString(KEY_DATA, serializeAppointments());

        editor.apply();
    }

}
