package com.example.danangnurfauzi.eticket;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import android.util.Log;
import com.example.danangnurfauzi.eticket.pockdata.PocketPos;

import com.example.danangnurfauzi.eticket.util.DateUtil;
import com.example.danangnurfauzi.eticket.util.FontDefine;
import com.example.danangnurfauzi.eticket.pockdata.Printer;
import com.example.danangnurfauzi.eticket.util.StringUtil;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;

import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import android.text.TextWatcher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;

import android.app.ProgressDialog;

import com.example.danangnurfauzi.eticket.util.P25Connector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.math.BigInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by danangnurfauzi on 7/11/17.
 */

public class Dashboard extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private RadioGroup radioWisman, radioKendaraan;
    private EditText jumlahOrang, price;

    private Button mCetakBtn, mConnectBtn, mEnableBtn;
    private TextView roleLabel, kendaraanLabel, pengunjungLabel, priceLabel;
    private Spinner mDeviceSp;
    private SQLiteHandler db;
    private SessionManager session;

    private static final String URL_DATA = "http://118.97.50.196/union/api/etiketing/getHargaTiketLokasiWisata/59";
    private static final String URL_BARCODE_DATA = "http://118.97.50.196/union/api/etiketing/getBarcode/";
    private static final String URL_SENDTIKET = "http://118.97.50.196/union/api/etiketing/sendTiket/";
    private static final String TAG = "Dashboard";
    private static final String JSON_ARRAY = "result";
    private JSONArray result ;

    public static int wisnu, wisman, mobil, ontanganting;
    public static String nameSql, jenisUserSql, userAksesLokerSql,pAksesLokerSql, fullbarcode, barcode, final_mlw;
    public static int final_jumwis, final_jwis, final_kendaraan, final_harga;

    private ProgressDialog mProgressDlg;
    private ProgressDialog mConnectingDlg;

    private P25Connector mConnector;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_operator_loket);

        roleLabel           = (TextView) findViewById(R.id.role_label);
        kendaraanLabel      = (TextView) findViewById(R.id.kendaraan_label);
        pengunjungLabel     = (TextView) findViewById(R.id.pengunjung_label);
        priceLabel          = (TextView) findViewById(R.id.price_label);
        mCetakBtn           = (Button) findViewById(R.id.cetakTiket);
        mEnableBtn          = (Button) findViewById(R.id.btn_enable);
        mConnectBtn         = (Button) findViewById(R.id.btn_connect);
        mDeviceSp           = (Spinner) findViewById(R.id.sp_device);
        radioWisman         = (RadioGroup) findViewById(R.id.jenis_pengunjung);
        radioKendaraan      = (RadioGroup) findViewById(R.id.kendaraan);
        jumlahOrang         = (EditText) findViewById(R.id.jumlah_orang);
        price               = (EditText) findViewById(R.id.price);

        db      = new SQLiteHandler(getApplicationContext());
        session = new SessionManager(getApplicationContext());
        if (!session.isLoggedIn()) {
            logOutUser();
        }

        HashMap<String, String> user = db.getUserDetails();
        Dashboard.nameSql              = user.get("name");
        Dashboard.jenisUserSql         = user.get("jenisUser");
        Dashboard.userAksesLokerSql    = user.get("userAksesLoker");
        Dashboard.pAksesLokerSql       = user.get("pAksesLoker");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        getHargaTiket(new CallBack() {
            @Override
            public void onSuccess(JSONArray result) {
                try {
                    JSONObject first = result.getJSONObject(0);
                    Dashboard.wisnu = first.getInt("harga");

                    JSONObject second = result.getJSONObject(1);
                    Dashboard.wisman = second.getInt("harga");

                    JSONObject third = result.getJSONObject(2);
                    Dashboard.mobil = third.getInt("harga");

                    JSONObject fourth = result.getJSONObject(4);
                    Dashboard.ontanganting = fourth.getInt("harga");

                    this.setHargaGlobalValue(wisnu, wisman, mobil, ontanganting);
                    hitungHargaTiket();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            private void setHargaGlobalValue(int wisnu, int wisman, int mobil, int ontanganting) {
                Dashboard.wisnu = wisnu;
                Dashboard.wisman = wisman;
                Dashboard.mobil = mobil;
                Dashboard.ontanganting = ontanganting;
            }

            @Override
            public void onFail(String msg) {
                // Do Stuff
            }
        });

        getBarcode(new CallBack() {
            @Override
            public void onSuccess(JSONArray result) {
                try {
                    JSONObject JSONBarcode = result.getJSONObject(0);
                    Dashboard.fullbarcode = JSONBarcode.getString("fullbarcode");
                    Dashboard.barcode = JSONBarcode.getString("barcode");

                    this.setBarcodeGlobalValue(fullbarcode, barcode);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            private void setBarcodeGlobalValue(String fullbarcode, String barcode) {
                Dashboard.fullbarcode = fullbarcode;
                Dashboard.barcode     = barcode;
            }

            @Override
            public void onFail(String msg) {
                // Do Stuff
            }
        });

        //String strI = Integer.toString(Dashboard.wisnu);
        //showToast(strI);

        //System.out.println(wisnu);
        // This overrides the radiogroup onCheckListener
        radioWisman.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // This will get the radiobutton that has changed in its check state
                RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                // This puts the value (true/false) into the variable
                boolean isChecked = checkedRadioButton.isChecked();
                // If the radiobutton that has changed in check state is now checked...
                String nilai1;
                if (isChecked) {
                    // Changes the textview's text to "Checked: example radiobutton text"
                    //price.setText("Checked:" + checkedRadioButton.getText());
                    /*if(checkedRadioButton.getText().equals("Nusantara")){
                        nilai1 = "1";
                    }else{
                        nilai1 = "2";
                    }*/

                    //hitungHargaTiket();

                    price.setText(hitungHargaTiket());
                }

            }
        });

        radioKendaraan.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // This will get the radiobutton that has changed in its check state
                RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                // This puts the value (true/false) into the variable
                boolean isChecked = checkedRadioButton.isChecked();
                // If the radiobutton that has changed in check state is now checked...
                String nilai1;
                if (isChecked) {
                    // Changes the textview's text to "Checked: example radiobutton text"
                    //price.setText("Checked:" + checkedRadioButton.getText());
                    /*if(checkedRadioButton.getText().equals("Nusantara")){
                        nilai1 = "1";
                    }else{
                        nilai1 = "2";
                    }*/

                    //hitungHargaTiket();
                    price.setText(hitungHargaTiket());
                }

            }
        });

        jumlahOrang.addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                price.setText(hitungHargaTiket());
            }

            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });

        if (mBluetoothAdapter == null) {
            showUnsupported();
            showDisabled();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                showDisonnected();
                showDisabled();
            } else {
                showConnected();
                showEnabled();
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                if (pairedDevices != null) {
                    mDeviceList.addAll(pairedDevices);
                    updateDeviceList();
                }
            }

            mProgressDlg = new ProgressDialog(this);

            mProgressDlg.setMessage("Scanning...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    mBluetoothAdapter.cancelDiscovery();
                }
            });

            mConnectingDlg = new ProgressDialog(this);

            mConnectingDlg.setMessage("Connecting...");
            mConnectingDlg.setCancelable(false);

            mConnectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    connect();
                }
            });

            mConnector = new P25Connector(new P25Connector.P25ConnectionListener() {

                @Override
                public void onStartConnecting() {
                    mConnectingDlg.show();
                }

                @Override
                public void onConnectionSuccess() {
                    mConnectingDlg.dismiss();

                    showConnected();
                }

                @Override
                public void onConnectionFailed(String error) {
                    mConnectingDlg.dismiss();
                }

                @Override
                public void onConnectionCancelled() {
                    mConnectingDlg.dismiss();
                }

                @Override
                public void onDisconnected() {
                    showDisonnected();
                }
            });

        }

        //enable bluetooth
        mEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                //finish();
                //startActivity(getIntent());
                startActivityForResult(intent, 1000);
            }
        });

        //print demo text
        mCetakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printTiket();
                sendTiket();
            }
        });

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mReceiver, filter);
    }

    private void logOutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(Dashboard.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void getHargaTiket(final CallBack onCallBack){
        String cancel_req_tag = TAG;
        //showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_DATA, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                //hideDialog();

                try {
                    JSONObject j = null;
                    //Parsing the fetched Json String to JSON Object
                    j = new JSONObject(response);
                    result = j.getJSONArray(JSON_ARRAY);

                    onCallBack.onSuccess(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                    onCallBack.onFail(e.toString());
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                //hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("mobile", "true");
                return params;
            }
        };
        // Adding request to request queue
        strReq.setShouldCache(false);
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(strReq, cancel_req_tag);
    }

    public interface CallBack {
        void onSuccess(JSONArray result);

        void onFail(String msg);
    }

    private void getBarcode(final CallBack onCallBack){
        String cancel_req_tag = TAG;
        //showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_BARCODE_DATA, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                //hideDialog();

                try {
                    JSONObject j = null;
                    //Parsing the fetched Json String to JSON Object
                    j = new JSONObject(response);
                    result = j.getJSONArray(JSON_ARRAY);

                    onCallBack.onSuccess(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                    onCallBack.onFail(e.toString());
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                //hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("uname", nameSql);
                params.put("userAksesLoker", userAksesLokerSql);
                return params;
            }
        };
        // Adding request to request queue
        strReq.setShouldCache(false);
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(strReq, cancel_req_tag);
    }

    private String hitungHargaTiket(){

        int wisnu = Dashboard.wisnu;
        int wisman = Dashboard.wisman;
        int hargaWisatawanNusantara;
        int hargaWisatawanMancanegara;

        int kendaraanYa = Dashboard.mobil;
        int kendaraanTidak = Dashboard.ontanganting;
        int hargaKendaraanYa;
        int hargaKendaraanTidak;

        int orang;

        int total;

        EditText munyuk = (EditText) findViewById(R.id.jumlah_orang);

        //wisatawan
        RadioButton wis = (RadioButton) findViewById(R.id.Pengunjung1);
        if(wis.isChecked())
        {
            hargaWisatawanNusantara = wisnu;
        }
        else
        {
            hargaWisatawanNusantara = 0;
        }

        RadioButton jem = (RadioButton) findViewById(R.id.Pengunjung2);
        if(jem.isChecked())
        {
            hargaWisatawanMancanegara = wisman;
        }
        else
        {
            hargaWisatawanMancanegara = 0;
        }

        //kendaraan
        RadioButton ken = (RadioButton) findViewById(R.id.Kendaraan1);
        if(ken.isChecked())
        {
            hargaKendaraanYa = kendaraanYa;
        }
        else
        {
            hargaKendaraanYa = 0;
        }

        RadioButton tut = (RadioButton) findViewById(R.id.Kendaraan2);
        if(tut.isChecked())
        {
            hargaKendaraanTidak = kendaraanTidak;
        }
        else
        {
            hargaKendaraanTidak = 0;
        }

        if(munyuk.getText().toString() != "" && munyuk.getText().length() > 0) {
            orang = Integer.parseInt(munyuk.getText().toString());
        } else {
            orang = 0;
        }

        total = (hargaWisatawanMancanegara * orang) + (hargaWisatawanNusantara * orang) + hargaKendaraanYa + hargaKendaraanTidak;

        //Toast.makeText(Dashboard.this,String.valueOf(total),Toast.LENGTH_LONG).show();

        return Integer.toString(total);
    }

    private void printTiket() {

        //calculate ticket price
        int wisnu = Dashboard.wisnu;
        int wisman = Dashboard.wisman;
        int hargaWisatawanNusantara;
        int hargaWisatawanMancanegara;
        int hargaWisatawan;
        int hargaSatuanWisatawan;

        int kendaraanYa = Dashboard.mobil;
        int kendaraanTidak = Dashboard.ontanganting;
        int hargaKendaraanYa;
        int hargaKendaraanTidak;
        int hargaKendaraan;

        int orang;

        int total;

        String wawan;
        String wiwin;

        EditText munyuk = (EditText) findViewById(R.id.jumlah_orang);

        //wisatawan
        RadioButton wis = (RadioButton) findViewById(R.id.Pengunjung1);
        if(wis.isChecked())
        {
            hargaWisatawanNusantara = wisnu;
            wawan = "Wisnu";
        }
        else
        {
            hargaWisatawanNusantara = 0;
            wawan = " ";
        }

        RadioButton jem = (RadioButton) findViewById(R.id.Pengunjung2);
        if(jem.isChecked())
        {
            hargaWisatawanMancanegara = wisman;
            wiwin = "Wisman";
        }
        else
        {
            hargaWisatawanMancanegara = 0;
            wiwin = " ";
        }

        //kendaraan
        RadioButton ken = (RadioButton) findViewById(R.id.Kendaraan1);
        if(ken.isChecked())
        {
            hargaKendaraanYa = kendaraanYa;
        }
        else
        {
            hargaKendaraanYa = 0;
        }

        RadioButton tut = (RadioButton) findViewById(R.id.Kendaraan2);
        if(tut.isChecked())
        {
            hargaKendaraanTidak = kendaraanTidak;
        }
        else
        {
            hargaKendaraanTidak = 0;
        }

        if(munyuk.getText().toString() != "" && munyuk.getText().length() > 0) {
            orang = Integer.parseInt(munyuk.getText().toString());
        } else {
            orang = 0;
        }

        hargaSatuanWisatawan    = hargaWisatawanMancanegara+hargaWisatawanNusantara;
        hargaWisatawan          = (hargaWisatawanMancanegara * orang) + (hargaWisatawanNusantara * orang);
        hargaKendaraan          = hargaKendaraanYa + hargaKendaraanTidak;

        total = (hargaWisatawanMancanegara * orang) + (hargaWisatawanNusantara * orang) + hargaKendaraanYa + hargaKendaraanTidak;
        Dashboard.final_jumwis      = orang;
        Dashboard.final_jwis        = ((wawan == "Wisnu") ? 1 : 2);
        Dashboard.final_kendaraan   = ((hargaKendaraanTidak == 0) ? 1 : 2);
        Dashboard.final_harga       = total;
        Dashboard.final_mlw         = ((Dashboard.fullbarcode.charAt(1) == '0') ? Dashboard.fullbarcode.substring(2, 4) : Dashboard.fullbarcode.substring(1, 4));

        String receiptHead = "\n************************"+"\n"
                + "   TIKET MASUK WISATA  "+"\n"
                + "       KAWAH PUTIH  "+"\n"
                + "************************" + "\n";

        StringBuilder contentSb	= new StringBuilder();

        contentSb.append("--------------------------------" + "\n");
        contentSb.append("ORG  " + wawan + wiwin + "@"+ hargaSatuanWisatawan +' '+ orang +" Rp." + hargaWisatawan + "\n");
        contentSb.append("MOBIL               Rp."+ hargaKendaraan + "\n");
        contentSb.append("--------------------------------" + "\n");
        contentSb.append("TOTAL               Rp." + total + "\n");
        contentSb.append("--------------------------------" + "\n");

        long milis		= System.currentTimeMillis();
        String date		= DateUtil.timeMilisToString(milis, "dd-MM-yy / HH:mm");

        StringBuilder contentSb2	= new StringBuilder();

        contentSb2.append("--------------------------------" + "\n");
        contentSb2.append("TANGGAL   : " + date + "\n");
        contentSb2.append("OPERATOR  : " + Dashboard.nameSql + "\n");
        contentSb2.append("--------------------------------" + "\n");

        String message	= "Perhutani menyatakan struk ini\n  sebagai bukti pembayaran sah" + "\n";
        String message2	= "Hubungi Call Center: 1 500 235 \n       www.perhutani.co.id" + "\n";
        String message3 = "\n\n\n";

        byte[] logo = StringUtil.hexStringToBytes("1b58 311f 6300 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 01fc 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0001 fff8 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 01ff fc00 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 fffc 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 00ff fc00 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 fffc 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 01ff fc00 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0003 fffc 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 07ff fc00 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "000f fffc 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "1fff fc00 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 003f" +
                "fffc 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 3fff" +
                "fc00 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 007f fffc" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 ffff fc00" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 01ff ffbc 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0001 fffe 1c00 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 03ff fc0c 07fc 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0007 fff8 007f ffc0 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 07ff f800 ffff f000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 000f fff0 03ff fffc 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0fff e007 ffff fe00 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 001f ffc0 0fff ffff 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 1fff 801f ffff ff80 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "003f ff00 3fff ffff c000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "3fff 007f ffff ffe0 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 007f" +
                "fe00 7fff ffff f000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 7ffc" +
                "007f ff07 fff8 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 007f fc00" +
                "fffc 01ff fc00 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 fff8 00ff" +
                "f800 7ffc 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 00ff f800 fff0" +
                "003f fe00 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 fff0 0000 0000" +
                "1ffe 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 01ff f000 0000 000f" +
                "ff00 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0001 fff0 0000 0000 07ff" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 01ff e000 0060 0007 ff00" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0001 ffe0 0078 f000 03ff 8000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 03ff e000 7cf8 0003 ff80 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0003 ffe0 007c f800 03ff 8000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 03ff e00e 78f1 c001 ffc0 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0003 ffc0 0f00 03c0 01ff c000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "03ff c01f 0383 e001 ffc0 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0003" +
                "ffc0 1f07 83c0 01ff c000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 03ff" +
                "c00e 07c1 8001 ffc0 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0003 ffc0" +
                "0007 c000 01ff c000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 03ff c070" +
                "c38c 3801 ffc0 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0003 ffc0 79e0" +
                "1e7c 01ff c000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 03ff c0f9 f79e" +
                "7c01 ffc0 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0003 ffc0 f9ff de78" +
                "01ff c000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 03ff e071 efde 3801" +
                "ffc0 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0003 ffe0 000f c000 01ff" +
                "c000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 01ff e000 0780 0001 ffc0" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0001 ffe0 1c70 70f0 01ff c000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 01ff f03c f878 f801 ffc0 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0001 fff0 3ef8 f8f8 03ff 8000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 00ff f83c f8f8 f003 ff80 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 fff8 1870 7060 07ff 8000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "00ff fc01 8000 0007 ff00 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "7ffc 03c0 1e00 0fff 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 007f" +
                "fe03 e31f 000f ff00 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 3fff" +
                "03c7 9f00 1ffe 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 003f ff03" +
                "cf9e 003f fe00 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 1fff 8007" +
                "8000 7ffc 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 001f ffc0 0780" +
                "007f fc00 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0fff f000 0001" +
                "fff8 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 000f fff8 0000 03ff" +
                "f000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 07ff fe00 000f ffe0" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0003 ffff 8000 3fff e000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 01ff fff0 01ff ffc0 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 ffff ffff ffff 8000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 007f ffff ffff fe00 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 3fff ffff fffc 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 001f ffff ffff f800 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 07ff ffff fff0 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0001 ffff ffff c000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "00ff ffff ff00 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "1fff fffc 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0003" +
                "ffff e000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 000f" +
                "fc00 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "0001 fc3f f3fc 181d c0cf fe10 101c c000" +
                "0000 0000 0000 0000 0000 0000 0000 0000" +
                "03ff 3ff3 fe18 1dc0 cffe 3838 1cc0 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0003" +
                "ff3f f3ff 181d c0cf fe78 3c1c c000 0000" +
                "0000 0000 0000 0000 0000 0000 0000 0303" +
                "b003 0398 1dc0 c0e0 7c3e 1cc0 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0003 03b0" +
                "0303 981d c0c0 e07c 3e1c c000 0000 0000" +
                "0000 0000 0000 0000 0000 0000 0303 b003" +
                "0398 1dc0 c0e0 ec3f 1cc0 0000 0000 0000" +
                "0000 0000 0000 0000 0000 0003 ff3f e3ff" +
                "1ffd c0c0 e0ce 3b9c c000 0000 0000 0000" +
                "0000 0000 0000 0000 0000 03ff 3fe3 fe1f" +
                "fdc0 c0e1 c639 dcc0 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0003 fc30 03fe 181d" +
                "c0c0 e1ff 38dc c000 0000 0000 0000 0000" +
                "0000 0000 0000 0000 0300 3003 0e18 1dc0" +
                "c0e1 ff38 fcc0 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0003 0030 0307 181c c1c0" +
                "e383 387c c000 0000 0000 0000 0000 0000" +
                "0000 0000 0000 0300 3ff3 0718 1ce3 80e3" +
                "03b8 3cc0 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0003 003f f303 981c 7f80 e701" +
                "b81c c000 0000 0000 0000 0000 0000 0000" +
                "0000 0000 0300 3ff3 0198 1c3e 00e6 01f8" +
                "18c0 ");

        byte[] titleByte	= Printer.printfont(receiptHead, FontDefine.FONT_32PX,FontDefine.Align_CENTER,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] content1Byte	= Printer.printfont(contentSb.toString(), FontDefine.FONT_24PX,FontDefine.Align_LEFT,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] content2Byte	= Printer.printfont(contentSb2.toString(), FontDefine.FONT_24PX,FontDefine.Align_LEFT,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] messageByte	= Printer.printfont(message, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] message2Byte	= Printer.printfont(message2, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] message3Byte	= Printer.printfont(message3, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        String contong = Dashboard.fullbarcode;

        String fak = toHex(contong);

        //byte[] barcode = StringUtil.hexStringToBytes("1d 6b 02 0d 36 39 30 31 32 33 34 35 36 37 38 39 32");

        byte[] barcode = StringUtil.hexStringToBytes("1d 6b 02 0d"+fak);

        byte[] totalByte	= new byte[logo.length + titleByte.length + content1Byte.length + messageByte.length +
                message2Byte.length + content2Byte.length + barcode.length + message3Byte.length];


        int offset = 0;
        System.arraycopy(logo, 0, totalByte, offset, logo.length);
        offset+=logo.length;

        System.arraycopy(titleByte, 0, totalByte, offset, titleByte.length);
        offset += titleByte.length;

        System.arraycopy(content1Byte, 0, totalByte, offset, content1Byte.length);
        offset += content1Byte.length;

        System.arraycopy(content2Byte, 0, totalByte, offset, content2Byte.length);
        offset += content2Byte.length;

        System.arraycopy(messageByte, 0, totalByte, offset, messageByte.length);
        offset += messageByte.length;

        System.arraycopy(message2Byte, 0, totalByte, offset, message2Byte.length);
        offset += message2Byte.length;

        System.arraycopy(barcode, 0, totalByte, offset, barcode.length);
        offset+=barcode.length;

        System.arraycopy(message3Byte, 0, totalByte, offset, message3Byte.length);
        offset += message3Byte.length;

        //System.arraycopy(dateByte, 0, totalByte, offset, dateByte.length);

        byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totalByte, 0, totalByte.length);

        sendData(senddata);
    }

    private void sendTiket() {

        // Tag used to cancel the request
        String cancel_req_tag = TAG;

        //progressDialog.setMessage("Adding Storage Point ...");
        //showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_SENDTIKET, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                //hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Tiket Berhasil Dikirim ke Server", Toast.LENGTH_SHORT).show();

                        // Launch login activity
                        // Intent intent = new Intent( AddStoragePoint.this,!!!.class);
                        //onBackPressed();
                        //startActivity(intent);
                        //finish();
                    } else {

                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                //hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tp_jumwis", Integer.toString(Dashboard.final_jumwis));
                params.put("tp_jwis", Integer.toString(Dashboard.final_jwis));
                params.put("tp_kendaraan", Integer.toString(Dashboard.final_kendaraan));
                params.put("tp_harga", Integer.toString(Dashboard.final_harga));
                params.put("tp_sign", "0");
                params.put("tp_barcode", Dashboard.barcode);
                params.put("tp_barcode_lengkap", Dashboard.fullbarcode);
                params.put("tp_id_mlw", Dashboard.final_mlw);
                //params.put("mobile", "true");
                return params;
            }
        };
        // Adding request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(strReq, cancel_req_tag);
    }

    public String toHex(String arg) {
        return String.format("%26x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }

    public boolean onCreateOptionsMenu(Menu menu){

        getMenuInflater().inflate(R.menu.menu,menu);
        MenuItem actionScan = menu.findItem(R.id.action_scan);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // show the button when some condition is true
        if (mBluetoothAdapter == null) {
            actionScan.setVisible(false);
        } else {
            if(!mBluetoothAdapter.isEnabled()) {
                actionScan.setVisible(false);
            } else {
                actionScan.setVisible(true);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_scan) {
            mBluetoothAdapter.startDiscovery();
            finish();
            startActivity(getIntent());
        } else if (item.getItemId() == R.id.log_out){
            logOutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }

        if (mConnector != null) {
            try {
                mConnector.disconnect();
            } catch (P25ConnectionException e) {
                e.printStackTrace();
            }
        }

        super.onPause();
    }

    private void showUnsupported() {
        showToast("Bluetooth is unsupported by this device");
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showDisabled() {
        showToast("Bluetooth disabled");

        roleLabel.setVisibility(View.GONE);
        kendaraanLabel.setVisibility(View.GONE);
        pengunjungLabel.setVisibility(View.GONE);
        priceLabel.setVisibility(View.GONE);
        radioWisman.setVisibility(View.GONE);
        radioKendaraan.setVisibility(View.GONE);
        jumlahOrang.setVisibility(View.GONE);
        price.setVisibility(View.GONE);
        mCetakBtn.setVisibility(View.GONE);
        mConnectBtn.setVisibility(View.GONE);
        mDeviceSp.setVisibility(View.GONE);
        mEnableBtn.setVisibility(View.VISIBLE);
    }

    private void showEnabled() {
        showToast("Bluetooth enabled");

        roleLabel.setVisibility(View.VISIBLE);
        kendaraanLabel.setVisibility(View.VISIBLE);
        pengunjungLabel.setVisibility(View.VISIBLE);
        priceLabel.setVisibility(View.VISIBLE);
        radioWisman.setVisibility(View.VISIBLE);
        radioKendaraan.setVisibility(View.VISIBLE);
        jumlahOrang.setVisibility(View.VISIBLE);
        price.setVisibility(View.VISIBLE);
        mCetakBtn.setVisibility(View.VISIBLE);
        mConnectBtn.setVisibility(View.VISIBLE);
        mDeviceSp.setVisibility(View.VISIBLE);
        mEnableBtn.setVisibility(View.GONE);
    }

    private void updateDeviceList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item, getArray(mDeviceList));

        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        mDeviceSp.setAdapter(adapter);
        mDeviceSp.setSelection(0);
    }

    private String[] getArray(ArrayList<BluetoothDevice> data) {
        String[] list = new String[0];

        if (data == null) return list;

        int size	= data.size();
        list		= new String[size];

        for (int i = 0; i < size; i++) {
            list[i] = data.get(i).getName();
        }

        return list;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private void connect() {
        if (mDeviceList == null || mDeviceList.size() == 0) {
            return;
        }

        BluetoothDevice device = mDeviceList.get(mDeviceSp.getSelectedItemPosition());

        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            try {
                createBond(device);
            } catch (Exception e) {
                showToast("Failed to pair device");

                return;
            }
        }

        try {
            if (!mConnector.isConnected()) {
                mConnector.connect(device);
            } else {
                mConnector.disconnect();

                showDisonnected();
            }
        } catch (P25ConnectionException e) {
            e.printStackTrace();
        }
    }

    private void createBond(BluetoothDevice device) throws Exception {

        try {
            Class<?> cl 	= Class.forName("android.bluetooth.BluetoothDevice");
            Class<?>[] par 	= {};

            Method method 	= cl.getMethod("createBond", par);

            method.invoke(device);

        } catch (Exception e) {
            e.printStackTrace();

            throw e;
        }
    }

    private void sendData(byte[] bytes) {
        try {
            mConnector.sendData(bytes);
        } catch (P25ConnectionException e) {
            e.printStackTrace();
        }
    }

    private void showConnected() {
        showToast("Connected");

        mConnectBtn.setText("Disconnect");

        mDeviceSp.setEnabled(false);
    }

    private void showDisonnected() {
        showToast("Disconnected");

        mConnectBtn.setText("Connect");

        mDeviceSp.setEnabled(true);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state 	= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    showEnabled();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    showDisabled();
                }
            }
        }
    };
}
