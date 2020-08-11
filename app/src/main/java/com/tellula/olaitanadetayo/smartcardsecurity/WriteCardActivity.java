package com.tellula.olaitanadetayo.smartcardsecurity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.opengl.Visibility;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

public class WriteCardActivity extends AppCompatActivity {
    private NfcAdapter myNfcAdapter;
    PendingIntent pendingIntent;
    String[][] mTechLists;
    IntentFilter[] mFilters;
    private EditText residentText;
    private EditText clusterText;
    private Button writeBtn;
    private LinearLayout writeFormLayout;
    private TextView tapCardTv;
    char[] hexArray="0123456789ABCDEF".toCharArray();
    Locale locale=new Locale("en","US");
    byte[] langBytes=locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
    boolean encodeInUtf8=false;
    Charset utfEncoding=encodeInUtf8?Charset.forName("UTF-8"):Charset.forName("UTF-16");
    int utfBit=encodeInUtf8?0:(1<<7);
    char status=(char)(utfBit+langBytes.length);
    private boolean scannerFlag=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_card);
        residentText=(EditText)findViewById(R.id.resident_name_txt);
        clusterText=(EditText)findViewById(R.id.cluster_no_txt);
        writeBtn=(Button)findViewById(R.id.write_btn);
        writeFormLayout=(LinearLayout)findViewById(R.id.write_form_layout);
        tapCardTv=(TextView)findViewById(R.id.write_response_tv);
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (myNfcAdapter == null)
            Toast.makeText(this, "NFC is not available for the device!!!", Toast.LENGTH_LONG).show();
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{Ndef.class.getName()},
                new String[]{MifareClassic.class.getName()}};
        writeBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //check the edittext for both and validate
                        if (residentText.getText().toString().isEmpty() || clusterText.getText().toString().isEmpty()) {
                            Toast.makeText(v.getContext(), "Cannot have Resident Name or Cluster Empty", Toast.LENGTH_LONG).show();
                        } else {
                            writeFormLayout.setVisibility(View.GONE);
                            tapCardTv.setVisibility(View.VISIBLE);
                            scannerFlag = true;
                            //then run the intent
                        }
                    }
                }
        );
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
            myNfcAdapter.enableForegroundDispatch(this, pendingIntent,mFilters, mTechLists);
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
        if(scannerFlag){
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] serial=intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            try{
                String myserial=bytesToHex(serial);
                if(intent!=null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
                    //Toast.makeText(this, "Ndef discovered-"+new String(serial), Toast.LENGTH_LONG).show();
                }else if(intent!=null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())){
                    //Toast.makeText(this, "Other Tech discovered-" + myserial, Toast.LENGTH_LONG).show();
                    writeNdefM(intent,residentText.getText().toString()+","+clusterText.getText().toString(), tag);
                    writeFormLayout.setVisibility(View.VISIBLE);
                    tapCardTv.setVisibility(View.GONE);
                }
            }catch (Exception myex){
                Toast.makeText(this, myex.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
            scannerFlag=false;
        }else {
            Toast.makeText(this, "Nothing to write",Toast.LENGTH_SHORT).show();
        }


    }

    public void writeNdefM(Intent intent,String datatosend,Tag tag){
        try{

            byte[] textBytes=datatosend.getBytes(utfEncoding);
            byte[] dataa=new byte[1+langBytes.length+textBytes.length];
            dataa[0]=(byte)status;
            System.arraycopy(langBytes,0,dataa,1,langBytes.length);
            System.arraycopy(textBytes,0,dataa,1+langBytes.length,textBytes.length);
            NdefRecord textRecord=new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,new byte[0],dataa);
            NdefMessage newMessage=new NdefMessage(new NdefRecord[]{textRecord});
            Ndef ndef=Ndef.get(tag);
            if(ndef!=null){
                ndef.connect();
                if(!ndef.isWritable()){
                    Toast.makeText(this, "Tag is read-only.", Toast.LENGTH_SHORT).show();
                }
                if(ndef.getMaxSize()<newMessage.toByteArray().length){//change 64 to ndefmessage bytearray length
                    Toast.makeText(this,"The data cannot be written since the tag capacity is "+ndef.getMaxSize()+" bytes and the data to be transferrred is "+ newMessage.toByteArray().length+" bytes",Toast.LENGTH_SHORT);
                }
                ndef.writeNdefMessage(newMessage);
                ndef.close();
                Toast.makeText(this, "Data sent to the Tag.", Toast.LENGTH_SHORT).show();
            }else{
                NdefFormatable ndefFormat=NdefFormatable.get(tag);
                if(ndefFormat!=null){
                    try{
                        ndefFormat.connect();
                        ndefFormat.format(newMessage);
                        ndefFormat.close();
                        Toast.makeText(this, "Data sent to the Tag.", Toast.LENGTH_SHORT).show();
                    }catch (IOException myex){
                        Toast.makeText(this, "Unable to format Tag.", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(this, "Not supported Tag.", Toast.LENGTH_SHORT).show();
                }
            }
        }catch (IOException ex){
            Toast.makeText(this, ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }catch (FormatException fEx){
            Toast.makeText(this, fEx.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
