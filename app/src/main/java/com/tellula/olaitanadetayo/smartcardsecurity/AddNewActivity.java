package com.tellula.olaitanadetayo.smartcardsecurity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Locale;

public class AddNewActivity extends AppCompatActivity {
    private NfcAdapter myNfcAdapter;
    PendingIntent pendingIntent;
    String[][] mTechLists;
    IntentFilter[] mFilters;
    private TextView myText;
    private TextView residentText;
    private TextView clusterText;
    private TextView serialText;
    private TextView errorSerialText;
    char[] hexArray="0123456789ABCDEF".toCharArray();
    Locale locale=new Locale("en","US");
    byte[] langBytes=locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
    boolean encodeInUtf8=false;
    Charset utfEncoding=encodeInUtf8?Charset.forName("UTF-8"):Charset.forName("UTF-16");
    int utfBit=encodeInUtf8?0:(1<<7);
    char status=(char)(utfBit+langBytes.length);
    ImageView nfc;
    Animation animation1;
    Animation animation2;
    Button okSucessBtn;
    Button okErrorBtn;
    String myserial;
    RelativeLayout tapcardLayout;
    LinearLayout cardDetailsLayout;
    LinearLayout errorCardDetailsLayout;
    DBHelper smartcardDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new);

        smartcardDb=new DBHelper(this);
        //initializing the view components
        nfc=(ImageView)findViewById(R.id.nfc_img);
        residentText=(TextView)findViewById(R.id.resident_tv);
        clusterText=(TextView)findViewById(R.id.cluster_tv);
        serialText=(TextView)findViewById(R.id.serial_tv);
        errorSerialText=(TextView)findViewById(R.id.error_serial_tv);
        okSucessBtn=(Button)findViewById(R.id.ok_success_btn);
        okErrorBtn=(Button)findViewById(R.id.ok_error_btn);

        tapcardLayout=(RelativeLayout)findViewById(R.id.tapcard_layout);
        cardDetailsLayout=(LinearLayout)findViewById(R.id.card_details_layout);
        errorCardDetailsLayout=(LinearLayout)findViewById(R.id.card_error_details_layout);
        animation1 =
                AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move_nfc_image);
        animation2 =
                AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spin_nfc_image);
        nfc.startAnimation(animation1);

        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (myNfcAdapter == null)
            Toast.makeText(this, "NFC is not available for the device!!!", Toast.LENGTH_LONG).show();
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{Ndef.class.getName()},
                new String[]{MifareClassic.class.getName()}};
        okSucessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tapcardLayout.setVisibility(View.VISIBLE);
                cardDetailsLayout.setVisibility(View.GONE);
                nfc.startAnimation(animation1);
            }
        });

        okErrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tapcardLayout.setVisibility(View.VISIBLE);
                errorCardDetailsLayout.setVisibility(View.GONE);
                nfc.startAnimation(animation1);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        myNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myNfcAdapter != null) {
            myNfcAdapter.enableForegroundDispatch(this, pendingIntent,
                    mFilters, mTechLists);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String bytesToHex(byte[] bytes){
        char[] hexChars=new char[bytes.length*2];
        for(int j=0;j<bytes.length;j++){
            int v=bytes[j]&0xFF;
            hexChars[j*2]=hexArray[v>>>4];
            hexChars[j*2+1]=hexArray[v&0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] serial=intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        try{
            myserial=bytesToHex(serial);
            if(intent!=null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
               // Toast.makeText(this, "Ndef discovered-" + new String(serial), Toast.LENGTH_LONG).show();
            }else if(intent!=null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())){
                readNdefM(intent,myserial);
            }
        }catch (Exception myex){
            Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void readNdefM(Intent intent,String serialno){
        Parcelable[] rawMessages=intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if(rawMessages!=null){
            try{
                NdefMessage[] messages=new NdefMessage[rawMessages.length];
                //work on extracting the Resident Name,Cluster
                for(int i=0; i<rawMessages.length;i++){
                    messages[i]=(NdefMessage)rawMessages[i];
                }
                String recordTemp=new String(messages[0].getRecords()[0].getPayload());
                recordTemp=recordTemp.substring(5,recordTemp.length());
                String[] recordTempArray=recordTemp.split(",");
                String cardResidentName=recordTempArray[0];
                String cardCluster=recordTempArray[1];

                if(isNetworkAvailable()){
                    nfc.startAnimation(animation2);
                    PostTask post=new PostTask();
                    post.execute(cardResidentName, cardCluster, serialno);
                }else{
                    Toast.makeText(this, "No Internet",Toast.LENGTH_SHORT).show();
                }

                //Toast.makeText(this,"Resident Name: "+cardResidentName+" | Cluster: "+cardCluster+" | Card Serial: "+serialno ,Toast.LENGTH_SHORT).show();
            }
            catch(Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(this, "Smartcard Empty",Toast.LENGTH_LONG).show();
        }
    }

    private final class PostTask extends
            AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try{
                String data = "name=" + URLEncoder. encode(params[0], "UTF-8")+
                        "&cluster=" + URLEncoder. encode(params[1] , "UTF-8" )+
                        "&serial=" + URLEncoder. encode(params[2] , "UTF-8" );
                URL url = new URL("http://your_url/api/smartcard/add" );
                HttpURLConnection urlConnection = (HttpURLConnection) url. openConnection();
                try{
                    urlConnection. setDoOutput(true);
                    urlConnection. setRequestMethod("POST");
                    urlConnection. setFixedLengthStreamingMode(data.getBytes().length);
                    urlConnection. setRequestProperty("Content-Type" ,
                            "application/x-www-form-urlencoded" );
                    OutputStream out =
                            new BufferedOutputStream(urlConnection. getOutputStream());
                    out.write(data.getBytes());
                    out. flush();
                    out. close();
                    InputStream in =
                            new BufferedInputStream(urlConnection. getInputStream());
                    int streamData=in.read();
                    String result="";
                    while(streamData!=-1){
                        result+=(char)streamData;
                        streamData=in.read();
                    }
                    if(!result.contains("error")){
                        //smartcardDb.insertCard(params[0],params[1],params[2]);
                    }
                    return result;
                }finally{
                    urlConnection.disconnect();
                }
            }catch (IOException ex){
                //aHttpURLConnection.disconnect();
                return "error--"+ex.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s.contains("error")){
                errorSerialText.setText("Serial: "+myserial);
                tapcardLayout.setVisibility(View.GONE);
                errorCardDetailsLayout.setVisibility(View.VISIBLE);
            }else{
                try{
                    JSONObject reader = new JSONObject(s);
                    JSONObject jsonData  = reader.getJSONObject("data");
                    residentText.setText("Name: "+jsonData.getString("name"));
                    clusterText.setText("Cluster: "+jsonData.getString("cluster"));
                    serialText.setText("Serial: "+jsonData.getString("serial"));
                    tapcardLayout.setVisibility(View.GONE);
                    cardDetailsLayout.setVisibility(View.VISIBLE);

                }catch (Exception jsex){
                    Toast. makeText(AddNewActivity. this, "No Internet",
                            Toast. LENGTH_LONG). show();
                }
            }
        }
    }
}
