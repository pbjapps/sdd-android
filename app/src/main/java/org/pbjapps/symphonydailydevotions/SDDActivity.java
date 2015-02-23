package org.pbjapps.symphonydailydevotions;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class SDDActivity extends ActionBarActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener {

    ProgressDialog pDialog;
    ImageButton playButton, pauseButton, stopButton;
    SeekBar seekBar;
    TextView seekText;
    Button dateButton;

    String source = "http://pbjapps.github.io/sdd/generated_data/";
    String selectedYear = "";
    String selectedMonth = "";
    String selectedDate = "";
    String currentSource;

    Devotion currentDevotion;

    MediaPlayer devotionPlayer;
    Handler seekHandler = new Handler();
    int currentPositionSeconds, currentPositionMinutes,
            durationPositionSeconds, durationMinutes;
    String currentSeconds, durationSeconds;

    Calendar past,future;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sdd);
        initUI();
        initPlayer();
        retrieveDevotion(null);
    }

    Runnable run = new Runnable() {

        @Override
        public void run() {

            // check to make sure we have a real mp
            // and that it is still playing
            if (devotionPlayer != null && devotionPlayer.isPlaying()) {
                seekUpdation();
            }
        }
    };

    public void initUI() {
        playButton = (ImageButton) findViewById(R.id.play);
        pauseButton = (ImageButton) findViewById(R.id.pause);
        stopButton = (ImageButton) findViewById(R.id.stop);
        seekText = (TextView) findViewById(R.id.text_shown);
        seekBar = (SeekBar) findViewById(R.id.musicSeekBar);
        dateButton = (Button) findViewById(R.id.dateSelector);

        playButton.setEnabled(false);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        seekBar.setEnabled(false);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // nothing here
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // nothing here
            }

            @Override
            public void onProgressChanged(SeekBar _seekBar, int _progress,
                                          boolean _fromUser) {
                if (devotionPlayer != null && _fromUser) {
                    devotionPlayer.seekTo(_progress);
                    // sets the mp3's elapsed time in format 0:00

                    currentPositionSeconds = (devotionPlayer.getCurrentPosition() / 1000) % 60;
                    currentPositionMinutes = (devotionPlayer.getCurrentPosition() / (1000 * 60)) % 60;
                    if (currentPositionSeconds < 0) {
                        currentPositionSeconds = 0;
                    }
                    if (currentPositionMinutes < 0) {
                        currentPositionMinutes = 0;
                    }

                    if (currentPositionSeconds < 10) {
                        currentSeconds = "0" + currentPositionSeconds;
                    } else {
                        currentSeconds = "" + currentPositionSeconds;
                    }

                    // sets mp3's total time in format 0:00
                    durationPositionSeconds = (devotionPlayer.getDuration() / 1000) % 60;
                    durationMinutes = (devotionPlayer.getDuration() / (1000 * 60)) % 60;
                    if (durationPositionSeconds < 10) {
                        durationSeconds = "0" + durationPositionSeconds;
                    } else {
                        durationSeconds = "" + durationPositionSeconds;
                    }

                    // sets text above seekBar to reflect mp3's progress
                    seekText.setText(currentPositionMinutes + ":"
                            + currentSeconds + "/" + durationMinutes + ":"
                            + durationSeconds);
                }
            }
        });
    }

    public void initPlayer() {
        // volume adjusts with phone volume buttons
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (devotionPlayer != null) {
            Log.println(Log.INFO, "SSD-devotionPlayer", "Resetting devotionPlayer and disabling player buttons and seek bar.");
            devotionPlayer.stop();
            devotionPlayer.release();
            playButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            seekBar.setEnabled(false);
        }

        devotionPlayer = new MediaPlayer();
        devotionPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        devotionPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){

            @Override
            public void onPrepared(MediaPlayer mp) {
                // Dismiss the progress dialog
                if (pDialog.isShowing()) {
                    pDialog.dismiss();
                }
                Log.println(Log.INFO, "SSD-devotionPlayer", "devotionPlayer is prepared, enabling playButton.");
                playButton.setEnabled(true);
                seekBar.setEnabled(true);
                seekBar.setMax(devotionPlayer.getDuration());
                seekUpdation();
            }
        });
        Log.println(Log.INFO, "SSD-devotionPlayer", "Initialized new devotionPlayer.");
    }

    public void retrieveDevotion(Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }

        getCurrentValues(calendar);
        SimpleDateFormat date = new SimpleDateFormat("E MM/dd/yyyy", Locale.getDefault());
        dateButton.setText(date.format(calendar.getTime()));
        Log.println(Log.INFO, "SSD-source", "Retrieving media for " + date.format(calendar.getTime()));

        // expected source URL: "http://pbjapps.github.io/sdd/generated_data/2014/June/2014-06-10.json"
        currentSource = source + selectedYear + "/" + selectedMonth + "/" + selectedDate + ".json";
        Log.println(Log.INFO, "SSD-source", "Source URL: " + currentSource);
        initPlayer();
        new HttpAsyncTask().execute(currentSource);
    }

    public void seekUpdation() {

        // sets the mp3's elapsed time in format 0:00

        currentPositionSeconds = (devotionPlayer.getCurrentPosition() / 1000) % 60;
        currentPositionMinutes = (devotionPlayer.getCurrentPosition() / (1000 * 60)) % 60;
        if (currentPositionSeconds < 0) {
            currentPositionSeconds = 0;
        }
        if (currentPositionMinutes < 0) {
            currentPositionMinutes = 0;
        }

        if (currentPositionSeconds < 10) {
            currentSeconds = "0" + currentPositionSeconds;
        } else {
            currentSeconds = "" + currentPositionSeconds;
        }

        // sets mp3's total time in format 0:00
        durationPositionSeconds = (devotionPlayer.getDuration() / 1000) % 60;
        durationMinutes = (devotionPlayer.getDuration() / (1000 * 60)) % 60;
        if (durationPositionSeconds < 10) {
            durationSeconds = "0" + durationPositionSeconds;
        } else {
            durationSeconds = "" + durationPositionSeconds;
        }

        // sets the seekBar to correspond with mp3 time elapsed
        seekBar.setProgress(devotionPlayer.getCurrentPosition());
        seekHandler.postDelayed(run, 1000);

        // sets text above seekBar to reflect mp3's progress
        seekText.setText(currentPositionMinutes + ":" + currentSeconds + "/"
                + durationMinutes + ":" + durationSeconds);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sdd, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getCurrentValues(Calendar calendar) {
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -2);
        }
        SimpleDateFormat year = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date today = calendar.getTime();

        selectedMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        selectedYear = year.format(today);
        selectedDate = date.format(today);
        Log.println(Log.INFO, "SSD-parseDate", "Parsed selections for " + date.format(today));
        Log.println(Log.INFO, "SSD-parseDate", "selectedMonth: " + selectedMonth + ", selectedYear: " + selectedYear + ", selectedDate: " + selectedDate );
    }

    @Override
    public void onClick(View v) {

        if (v == playButton && playButton.isEnabled()) {
            if (!devotionPlayer.isPlaying()) {
                Log.println(Log.INFO, "SSD-playButton", "playButton clicked, starting devotionPlayer.");
                devotionPlayer.start();
                seekUpdation();
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            }
        }

        if (v == pauseButton && pauseButton.isEnabled()) {
            if (devotionPlayer.isPlaying()) {
                Log.println(Log.INFO, "SSD-pauseButton", "pauseButton clicked, pausing devotionPlayer.");
                devotionPlayer.pause();
                seekUpdation();
                pauseButton.setEnabled(false);
                playButton.setEnabled(true);
                stopButton.setEnabled(true);
            } else if (!devotionPlayer.isPlaying()) {
                Toast.makeText(SDDActivity.this,
                        "Please start playing before pausing!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SDDActivity.this, "Pause generic error!",
                        Toast.LENGTH_SHORT).show();
            }
        }

        if (v == stopButton && stopButton.isEnabled()) {
            if (devotionPlayer.isPlaying()) {
                Log.println(Log.INFO, "SSD-stopButton", "stopButton clicked, stopping devotionPlayer.");
                seekUpdation();
                devotionPlayer.pause();
                devotionPlayer.seekTo(0);
                seekUpdation();
                stopButton.setEnabled(false);
                pauseButton.setEnabled(false);
                playButton.setEnabled(true);
            } else if (!devotionPlayer.isPlaying()) {
                Toast.makeText(SDDActivity.this,
                        "Please start playing before stopping!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SDDActivity.this, "Stop generic error!",
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        SimpleDateFormat date = new SimpleDateFormat("E MM/dd/yyyy", Locale.getDefault());

        if (past == null) {
            past = Calendar.getInstance();
            past.set(2013,5,10);
        }
        future = Calendar.getInstance();
        if (future.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            future.add(Calendar.DAY_OF_MONTH, -1);
        } else if (future.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            future.add(Calendar.DAY_OF_MONTH, -2);
        }
        Log.println(Log.INFO, "SSD-date", "Bounding selected dates with [ " + date.format(past.getTime()) + ", " + date.format(future.getTime()) + " ].");

        // TODO: Handle corner-case for weekends: navigating to another date and back fails

        Calendar calendar = Calendar.getInstance();
        calendar.set(year,monthOfYear,dayOfMonth);

        if (calendar.before(past)) {
            Toast.makeText(getBaseContext(), "Sorry! Devotions before " + date.format(past.getTime()) + " are unsupported.", Toast.LENGTH_LONG).show();
            return;
        }
        if (calendar.after(future)) {
            Toast.makeText(getBaseContext(), "Sorry! Devotions beyond " + date.format(future.getTime()) + " are unsupported.", Toast.LENGTH_LONG).show();
            return;
        }

        retrieveDevotion(calendar);
    }

    // Borrowed from http://hmkcode.com/android-parsing-json-data/
    public static String GET(String url){
        InputStream inputStream;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {

        SimpleDateFormat date = new SimpleDateFormat("E MM/dd/yyyy", Locale.getDefault());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(SDDActivity.this);
            pDialog.setMessage("Fetching devotion info...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            // Dismiss the progress dialog
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }

            try {
                JSONObject devotionJSON = new JSONObject(result);
                currentDevotion = new Devotion(devotionJSON);
                Log.println(Log.INFO, "SSD-JSON", "Setting JSON texts for [date, id, title, passage]: [ "
                        + date.format(currentDevotion.getDate()) + ", "
                        + currentDevotion.getId() + ", "
                        + currentDevotion.getTitle() + ", "
                        + currentDevotion.getPassage() + " ].");
                ( (TextView) findViewById(R.id.date)).setText(date.format(currentDevotion.getDate()));
                ( (TextView) findViewById(R.id.id)).setText(currentDevotion.getId());
                ( (TextView) findViewById(R.id.title)).setText(currentDevotion.getTitle());
                ( (TextView) findViewById(R.id.passage)).setText(currentDevotion.getPassage());

                try {
                    devotionPlayer.setDataSource(currentDevotion.getSource().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                devotionPlayer.prepareAsync();

                pDialog = new ProgressDialog(SDDActivity.this);
                pDialog.setMessage("Fetching devotion media...");
                pDialog.setCancelable(false);
                pDialog.show();

                // TODO: Need a graceful timeout if the stream is unresolvable
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), (SDDActivity)getActivity(), year, month, day);
        }

    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
}
