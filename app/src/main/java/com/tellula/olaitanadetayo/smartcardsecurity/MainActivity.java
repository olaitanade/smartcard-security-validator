package com.tellula.olaitanadetayo.smartcardsecurity;


import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.lang.String;

public class MainActivity extends AppCompatActivity {
    Dialog dialog;
    private NfcAdapter myNfcAdapter;
    PendingIntent pendingIntent;
    String[][] mTechLists;
    IntentFilter[] mFilters;
    private TextView myText;
    private TextView residentText;
    private TextView clusterText;
    private TextView serialText;
    private TextView mobileText;
    private TextView emailText;
    private TextView errorSerialText;
    char[] hexArray="0123456789ABCDEF".toCharArray();
    Locale locale=new Locale("en","US");
    byte[] langBytes=locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
    boolean encodeInUtf8=false;
    Charset utfEncoding=encodeInUtf8?Charset.forName("UTF-8"):Charset.forName("UTF-16");
    int utfBit=encodeInUtf8?0:(1<<7);
    char status=(char)(utfBit+langBytes.length);
    private boolean isLoading=false;
    RelativeLayout tapcardLayout;
    LinearLayout cardDetailsLayout;
    LinearLayout errorCardDetailsLayout;
    ImageView nfc;
    Animation animation1;
    Animation animation2;
    Button okSucessBtn;
    Button okErrorBtn;
    String myserial;
    DBHelper smartcardDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check if db exist
        smartcardDb=new DBHelper(this);

        nfc=(ImageView)findViewById(R.id.nfc_img);
        myText=(TextView)findViewById(R.id.app_info_tv);
        residentText=(TextView)findViewById(R.id.resident_tv);
        clusterText=(TextView)findViewById(R.id.cluster_tv);
        serialText=(TextView)findViewById(R.id.serial_tv);
        mobileText=(TextView)findViewById(R.id.mobile_tv);
        emailText=(TextView)findViewById(R.id.email_tv);
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


        // Check if this activity was created before
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (myNfcAdapter == null)
            Toast.makeText(this, "NFC is not available for the device!!!", Toast.LENGTH_LONG).show();
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{Ndef.class.getName()},
                new String[]{MifareClassic.class.getName()}};

       /** myText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //simulate a card scan
                simulateCard();
            }
        });**/
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

        //Setting up the local database if the app is running for the first time or instantiating

    }


    private  boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater(). inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        myNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{Ndef.class.getName()},
                new String[]{MifareClassic.class.getName()}};
        if (myNfcAdapter != null) {
            myNfcAdapter.enableForegroundDispatch(this, pendingIntent,
                    mFilters, mTechLists);
        }
    }
    // Called every time user clicks on an action
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //
        switch (item. getItemId()) { //
            case R.id.update_db:
                startActivity(new Intent(this, ConfigActivity. class));
                finish();
                return true;
            default:
                return false;
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

    public void simulateCard(){
        if(!isLoading){
            //Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //byte[] serial=intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            try{
                myserial="46C30707";
                readNdefMSimulation(myserial);

            }catch (Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void readNdefMSimulation(String serialno){
        //Parcelable[] rawMessages=intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            Toast.makeText(this, "Smartcard Empty",Toast.LENGTH_LONG).show();
            try{
                nfc.startAnimation(animation2);
                GetFromLocalDbTask get=new GetFromLocalDbTask();
                get.execute(serialno);

            }catch (Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }

    }
    @Override
    public void onNewIntent(Intent intent) {
        //Toast.makeText(this, "here-", Toast.LENGTH_LONG).show();
        super.onNewIntent(intent);
        if(!isLoading){
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] serial=intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            try{
                myserial=bytesToHex(serial);
                if(intent!=null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
                    //Toast.makeText(this, "Ndef discovered-"+new String(serial), Toast.LENGTH_LONG).show();
                }else if(intent!=null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())){
                    //Toast.makeText(this, "Other Tech discovered-" + myserial, Toast.LENGTH_LONG).show();
                    readNdefM(intent,myserial);
                }
            }catch (Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        }


    }

    private void performTagOperations(Intent intent){
        String action = intent.getAction();
        if(action.equals(NfcAdapter.ACTION_TAG_DISCOVERED) ||
                action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) ){

        }
    }

    public String reverseDecimal(String hex){
        //convert the hex to bin string
        BigInteger biH=new BigInteger(hex,16);
        String bin=biH.toString(2);
        //split the bin string into 8
        String[] binA=new String[8];
        String[] encBinA=new String[8];
        if(bin.length()==32){
            binA[0]=bin.substring(0, 4);
            binA[1]=bin.substring(4,8);
            binA[2]=bin.substring(8,12);
            binA[3]=bin.substring(12,16);
            binA[4]=bin.substring(16,20);
            binA[5]=bin.substring(20,24);
            binA[6]=bin.substring(24,28);
            binA[7]=bin.substring(28);
            //System.out.println(binA[7]);
        }else{
            String zeros="";
            for(int i=0;i<32-bin.length();i++){
                zeros+="0";
            }
            bin=zeros+bin;
            binA[0]=bin.substring(0, 4);
            binA[1]=bin.substring(4,8);
            binA[2]=bin.substring(8,12);
            binA[3]=bin.substring(12,16);
            binA[4]=bin.substring(16,20);
            binA[5]=bin.substring(20,24);
            binA[6]=bin.substring(24,28);
            binA[7]=bin.substring(28);
            //System.out.println(binA[7]);
        }

        //use the key 78563412 to re arrang and concatenate
        encBinA[0]=binA[6];
        encBinA[1]=binA[7];
        encBinA[2]=binA[4];
        encBinA[3]=binA[5];
        encBinA[4]=binA[2];
        encBinA[5]=binA[3];
        encBinA[6]=binA[0];
        encBinA[7]=binA[1];
        //System.out.println(String.join("", encBinA));
        String Bin=encBinA[0]+encBinA[1]+encBinA[2]+encBinA[3]+encBinA[4]+encBinA[5]+encBinA[6]+encBinA[7];
        //use BigInteger to convert to decimal
        BigInteger bi=new BigInteger(Bin,2);
        return bi.toString();
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

                nfc.startAnimation(animation2);
                GetFromLocalDbTask get=new GetFromLocalDbTask();
                get.execute(serialno);
                //GetTask get=new GetTask();
                //get.execute(new URL("http://smartcard1004security.herokuapp.com/api/smartcard/"+serialno+"/verify"));

                //Toast.makeText(this,"Resident Name: "+cardResidentName+" | Cluster: "+cardCluster+" | Card Serial: "+serialno ,Toast.LENGTH_SHORT).show();
            }
            catch(Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();

            }
        }else {
            Toast.makeText(this, "Smart Card Empty->Serial No:"+serialno,Toast.LENGTH_LONG).show();
            try{
                nfc.startAnimation(animation2);
                GetFromLocalDbTask get=new GetFromLocalDbTask();
                get.execute(serialno);

            }catch (Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private final class GetFromLocalDbTask extends
            AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... s) {
            //apply the function to convert the hex to decimal reversed
            //String reversedDec="1176212805";
            String reversedDec=reverseDecimal(s[0]);
            myserial=reverseDecimal(s[0]);
            try{
                Cursor rs = smartcardDb.getData(reversedDec);
                if(rs.getCount()==0){
                    return "error";
                }else{
                    String temp="";
                    rs.moveToFirst();

                    temp+=rs.getString(rs.getColumnIndex(DBHelper.SMARTCARDS_COLUMN_NAME))+",";
                    temp+=rs.getString(rs.getColumnIndex(DBHelper.SMARTCARDS_COLUMN_CLUSTER))+",";
                    temp+=rs.getString(rs.getColumnIndex(DBHelper.SMARTCARDS_COLUMN_SERIAL))+",";
                    temp+=rs.getString(rs.getColumnIndex(DBHelper.SMARTCARDS_COLUMN_MOBILE))+",";
                    temp+=rs.getString(rs.getColumnIndex(DBHelper.SMARTCARDS_COLUMN_EMAIL))+",";
                    temp+="--";

                    return temp;
                }
            }catch (Exception ex){
                //aHttpURLConnection.disconnect();
                return ex.getMessage()+" (error)";
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
                    String[] temp=s.split(",");
                    residentText.setText("Name: "+temp[0]);
                    clusterText.setText("Apartment: "+temp[1]);
                    serialText.setText("Serial: "+temp[2]);
                    mobileText.setText("Mobile: "+temp[3]);
                    emailText.setText("Expire: "+temp[4]);
                    tapcardLayout.setVisibility(View.GONE);
                    cardDetailsLayout.setVisibility(View.VISIBLE);

                }catch (Exception jsex){
                    Toast. makeText(MainActivity. this, jsex.getMessage(),
                            Toast. LENGTH_LONG). show();
                }
            }
        }
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
                    int streamData=in.read();
                    String result="";
                    while(streamData!=-1){
                        result+=(char)streamData;
                        streamData=in.read();
                    }
                    //check if db exist
                    if(doesDatabaseExist(MainActivity.this,"Security1004.db")){
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

                    return "{success} Database updated";
                }catch (JSONException jsonEx){
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
                Toast. makeText(MainActivity. this,s,
                        Toast. LENGTH_LONG). show();

            }else{
                Toast. makeText(MainActivity. this,s,
                        Toast. LENGTH_LONG). show();

            }

        }
    }

    private final class GetTask extends
            AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... url) {
            try{
                HttpURLConnection aHttpURLConnection =
                        (HttpURLConnection) url[0]. openConnection();
                try{

                    InputStream in =new BufferedInputStream(aHttpURLConnection. getInputStream());
                    int streamData=in.read();
                    String result="";
                    while(streamData!=-1){
                        result+=(char)streamData;
                        streamData=in.read();
                    }
                    return result;
                }finally{
                    aHttpURLConnection.disconnect();
                }
            }catch (IOException ex){
                //aHttpURLConnection.disconnect();
                return ex.getMessage();
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
                    Toast. makeText(MainActivity. this, "No Internet",
                            Toast. LENGTH_LONG). show();
                }
            }
        }
    }


}
