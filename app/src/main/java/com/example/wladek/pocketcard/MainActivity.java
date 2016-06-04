package com.example.wladek.pocketcard;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;

    ToggleButton tglReadWrite;
    EditText txtTagContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        tglReadWrite = (ToggleButton)findViewById(R.id.tglReadWrite);

        tglReadWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!tglReadWrite.isChecked()){
                    tglReadWrite.setText(tglReadWrite.getTextOff());
                    tglReadWrite.setChecked(false);
                }else{
                    tglReadWrite.setText(tglReadWrite.getTextOn());
                    tglReadWrite.setChecked(true);
                }

            }
        });

        txtTagContent = (EditText)findViewById(R.id.txtTagContent);

        if (nfcAdapter != null){
            if (nfcAdapter != null && nfcAdapter.isEnabled()){
//            Toast.makeText(this, "NFC AVAILABLE !", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "NFC NOT AVAILABLE :(" , Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(this, "NFC NOT SUPPORTED BY THIS DEVICE " , Toast.LENGTH_LONG).show();
            finish();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {

//        Toast.makeText(this,"NFC INTENT RECEIVED ! ", Toast.LENGTH_LONG).show();
        super.onNewIntent(intent);

        if(intent.hasExtra(NfcAdapter.EXTRA_TAG)){

            Toast.makeText(this," CARD DETECTED ", Toast.LENGTH_SHORT).show();

            if (tglReadWrite.isChecked()){

                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                if (parcelables != null && parcelables.length > 0){

                    readTextFromMessage((NdefMessage)parcelables[0]);

                }else {
                    Toast.makeText(this , " No NDEF messages fount ! " , Toast.LENGTH_SHORT).show();
                }

            }else {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                NdefMessage ndefMessage = createNdefMessage(txtTagContent.getText() + "");

                writeNdefMessage(tag, ndefMessage);

                tglReadWrite.setChecked(true);
            }

        }

    }

    private void readTextFromMessage(NdefMessage ndefMessage) {

        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if (ndefRecords != null && ndefRecords.length > 0){

            NdefRecord ndefRecord = ndefRecords[0];

            String tagContent = getTextFromNdefRecord(ndefRecord);

            txtTagContent.setText(tagContent);

        }else {

            Toast.makeText(this , " No Records Found ! ", Toast.LENGTH_SHORT).show();

        }

    }

    /**
     *
     * @param ndefRecord
     * @return string of ndef record payload
     *
     * To Do : Convert byte[] to string.
     *
     */
    private String getTextFromNdefRecord(NdefRecord ndefRecord) {

        String cardContent = null;

        byte[] contents = ndefRecord.getPayload();

        try {
            cardContent = new String(contents , "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Log.e(" NDEF RECORD " , " RECORD ++++ " + cardContent.substring(3));

        cardContent = cardContent.substring(3);

        return cardContent;

    }


    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void enableForegroundDispatchSystem(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilter = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    public void disableForegroundDispatchSystem(){
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag , NdefMessage ndefMessage){

        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null){
                Toast.makeText(this," Tag in not NDF formatable ! ",Toast.LENGTH_LONG).show();
                return;
            }

            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

        }catch (Exception e){
            Log.e("formatTag" , e.getMessage());
        }
    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage){
        try {

            if(tag == null){
                Toast.makeText(this, " Tag object cannot be null ! ", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if(ndef == null){
                //Format tag with the ndef format and writes the message
                formatTag(tag, ndefMessage);
            }else {
                ndef.connect();

                if (!ndef.isWritable()){
                    Toast.makeText(this, "Tag is not writable !" , Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, " Tag written " , Toast.LENGTH_SHORT).show();

            }

        }catch (Exception e){
            Log.e("writeNdefMessage" , e.getMessage());
        }
    }


    private NdefRecord createTextRecord(String content){

        try {

            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;

            final ByteArrayOutputStream payLoad = new ByteArrayOutputStream(1 + languageSize + textLength);

            payLoad.write((byte)(languageSize & 0x1F));
            payLoad.write(language , 0 , languageSize);
            payLoad.write(text , 0 , textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN , NdefRecord.RTD_TEXT , new byte[0] , payLoad.toByteArray());

        }catch (UnsupportedEncodingException e){
            Log.e("createTextRecord" , e.getMessage());
        }
        return null;
    }

    private NdefMessage createNdefMessage(String content){

        NdefRecord ndefRecord = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ ndefRecord});

        return ndefMessage;
    }

    public void tglReadWriteOnClick(View view){
        tglReadWrite.setText("");
    }
}
