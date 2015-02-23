package org.pbjapps.symphonydailydevotions;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jfernandes on 2/22/15.
 */
public class Devotion {

//            "date": "2015-02-20",
//            "id": 817,
//            "time": "7:45:00",
//            "source": "http://rs0796.freeconferencecall.com/fcc/cgi-bin/play.mp3?id=6054754850:611287-817",
//            "title": "817 Title",
//            "passage": "817 Passage",
//            "duration": "817 Duration"
    Date date;
    String id;
    String time;
    URL source;
    String title;
    String passage;
    String duration;

    public Devotion(JSONObject devotionJSON) {

        try {
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                this.date = dateParser.parse(devotionJSON.getString("date"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            this.id = devotionJSON.getString("id");
            this.time = devotionJSON.getString("time");
            try {
                this.source = new URL(devotionJSON.getString("source"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            this.title = devotionJSON.getString("title");
            this.passage = devotionJSON.getString("passage");
            this.duration = devotionJSON.getString("duration");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Date getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getTime() {
        return time;
    }

    public URL getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getPassage() {
        return passage;
    }

    public String getDuration() {
        return duration;
    }
}
