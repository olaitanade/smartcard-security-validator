package com.tellula.olaitanadetayo.smartcardsecurity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConfigActivity extends Activity {

    ImageView nfcImage;
    DBHelper smartcardDb;
    Animation animation2;
    private NfcAdapter myNfcAdapter;
    PendingIntent pendingIntent;
    String[][] mTechLists;
    IntentFilter[] mFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);


        smartcardDb=new DBHelper(this);
        try{
            GetDbTask get=new GetDbTask();
            get.execute(new URL("http://your_url/api/smartcards/all"));
        }catch(Exception myexception){
            Toast.makeText(this, myexception.getMessage(), Toast.LENGTH_LONG).show();
        }

        nfcImage=(ImageView)findViewById(R.id.nfc_img);
        animation2 =
                AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spin_nfc_image);
        nfcImage.startAnimation(animation2);

    }

    private  boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    private final class GetDbTask extends
            AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... url) {
            try{
                HttpURLConnection aHttpURLConnection =
                        (HttpURLConnection) url[0]. openConnection();
                try{
                    InputStream in =new BufferedInputStream(aHttpURLConnection. getInputStream());
                    Reader rd=new InputStreamReader(in);
                    BufferedReader input=new BufferedReader(rd);

                    String line, result;
                    result="";
                    while ((line=input.readLine()) != null) {
                        result += line;
                    }

                    //check if db exist
                    if(doesDatabaseExist(ConfigActivity.this,"Security1004.db")){
                        //do upgrade
                        smartcardDb.onUpgrade(smartcardDb.getWritableDatabase(),1,2);
                        //Toast.makeText(this, "(doing upgrade)", Toast.LENGTH_LONG).show();
                    }
                    JSONObject reader = new JSONObject(result);
                    JSONArray jsonData  = reader.getJSONArray("data");
                    for(int i=0;i<jsonData.length();i++){
                        JSONObject temp=(JSONObject)jsonData.get(i);
                        smartcardDb.insertCard(temp.getString("name"),temp.getString("cluster"),temp.getString("email"),temp.getString("mobile_no"),temp.getString("holder_category"),temp.getString("serial"));
                    }

                    return "{success} Database updated, Size = "+jsonData.length();
                }catch (Exception jsonEx){
                    return "No Internet, Database could not be updated";
                }
                finally{
                    aHttpURLConnection.disconnect();
                }
            }catch (IOException ex){
                //aHttpURLConnection.disconnect();
                return "No Internet, Database could not be updated";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s.contains("success")){
                //
                Toast. makeText(ConfigActivity. this,s,
                        Toast. LENGTH_LONG). show();
                Intent i=new Intent(ConfigActivity.this,MainActivity.class);
                finish();
                startActivity(i);
            }else{
                Toast. makeText(ConfigActivity. this,s,
                        Toast. LENGTH_LONG). show();
                Intent i=new Intent(ConfigActivity.this,MainActivity.class);
                finish();
                startActivity(i);
            }

        }
    }
}
