package com.example.danangnurfauzi.eticket;

import com.example.danangnurfauzi.eticket.R;
import com.example.danangnurfauzi.eticket.pockdata.PocketPos;

import com.example.danangnurfauzi.eticket.util.DateUtil;
import com.example.danangnurfauzi.eticket.util.FileOperation;
import com.example.danangnurfauzi.eticket.util.FontDefine;
import com.example.danangnurfauzi.eticket.pockdata.Printer;
import com.example.danangnurfauzi.eticket.util.StringUtil;
import com.example.danangnurfauzi.eticket.util.Util;
import com.example.danangnurfauzi.eticket.util.DataConstants;

import android.os.Build;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Toast;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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

/**
 * Created by danangnurfauzi on 7/11/17.
 */

public class Dashboard extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private Button mCetakBtn;
    private Button mConnectBtn;
    private Button mEnableBtn;
    private Spinner mDeviceSp;

    private ProgressDialog mProgressDlg;
    private ProgressDialog mConnectingDlg;

    private P25Connector mConnector;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_operator_loket);

        mCetakBtn   = (Button) findViewById(R.id.cetakTiket);
        mEnableBtn  = (Button) findViewById(R.id.btn_enable);
        mConnectBtn = (Button) findViewById(R.id.btn_connect);
        mDeviceSp   = (Spinner) findViewById(R.id.sp_device);

        mBluetoothAdapter	= BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null){
            showToast("HP ne ra nduwe bluetooth :') ");
        }else {
            if (!mBluetoothAdapter.isEnabled()) {
                //showToast("Bluetooth e mati :') ");
                showDisabled();
            } else {
                //showToast("Bluetooth e joss :') ");
                showEnabled();

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                if (pairedDevices != null) {
                    mDeviceList.addAll(pairedDevices);

                    updateDeviceList();
                }
            }

            ProgressDialog mProgressDlg 	= new ProgressDialog(this);

            mProgressDlg.setMessage("Scanning...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    mBluetoothAdapter.cancelDiscovery();
                }
            });

            mConnectingDlg 	= new ProgressDialog(this);

            mConnectingDlg.setMessage("Connecting...");
            mConnectingDlg.setCancelable(false);

            mConnectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    connect();
                }
            });

            mConnector 		= new P25Connector(new P25Connector.P25ConnectionListener() {

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

                startActivityForResult(intent, 1000);
            }
        });

        //print demo text
        mCetakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //printDemoContent();
                printStruk();
                //printStruk2();
                //print1DBarcode();
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

    public boolean onCreateOptionsMenu(Menu menu){

        getMenuInflater().inflate(R.menu.menu,menu);

        return super.onCreateOptionsMenu(menu);

    }

    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_scan) {
            mBluetoothAdapter.startDiscovery();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showUnsupported() {
        showToast("Bluetooth is unsupported by this device");

    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showDisabled() {
        showToast("Bluetooth disabled");

        mEnableBtn.setVisibility(View.VISIBLE);
        mConnectBtn.setVisibility(View.GONE);
        mDeviceSp.setVisibility(View.GONE);
    }

    private void showEnabled() {
        showToast("Bluetooth enabled");

        mEnableBtn.setVisibility(View.GONE);
        mConnectBtn.setVisibility(View.VISIBLE);
        mDeviceSp.setVisibility(View.VISIBLE);
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

    private void printDemoContent(){

        /*********** print head*******/
        String receiptHead = "************************"
                + "   TIKET MASUK WISATA  "+"\n"
                + "       KAWAH PUTIH  "+"\n"
                + "************************"
                + "\n";

        long milis		= System.currentTimeMillis();

        String date		= DateUtil.timeMilisToString(milis, "MMM dd, yyyy");
        String time		= DateUtil.timeMilisToString(milis, "hh:mm a");

        String hwDevice	= Build.MANUFACTURER;
        String hwModel	= Build.MODEL;
        String osVer	= Build.VERSION.RELEASE;
        String sdkVer	= String.valueOf(Build.VERSION.SDK_INT);

        StringBuffer receiptHeadBuffer = new StringBuffer(100);

        receiptHeadBuffer.append(receiptHead);
        receiptHeadBuffer.append(Util.nameLeftValueRightJustify(date, time, DataConstants.RECEIPT_WIDTH) + "\n");

        receiptHeadBuffer.append(Util.nameLeftValueRightJustify("Device:", hwDevice, DataConstants.RECEIPT_WIDTH) + "\n");

        receiptHeadBuffer.append(Util.nameLeftValueRightJustify("Model:",  hwModel, DataConstants.RECEIPT_WIDTH) + "\n");
        receiptHeadBuffer.append(Util.nameLeftValueRightJustify("OS ver:", osVer, DataConstants.RECEIPT_WIDTH) + "\n");
        receiptHeadBuffer.append(Util.nameLeftValueRightJustify("SDK:", sdkVer, DataConstants.RECEIPT_WIDTH));
        receiptHead = receiptHeadBuffer.toString();

        byte[] header = Printer.printfont(receiptHead + "\n", FontDefine.FONT_32PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);


        /*********** print English text*******/
        StringBuffer sb = new StringBuffer();
        for(int i=1; i<128; i++)
            sb.append((char)i);
        String content = sb.toString().trim();

        byte[] englishchartext24 			= Printer.printfont(content + "\n",FontDefine.FONT_24PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);
        byte[] englishchartext32			= Printer.printfont(content + "\n",FontDefine.FONT_32PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);
        byte[] englishchartext24underline	= Printer.printfont(content + "\n",FontDefine.FONT_24PX_UNDERLINE,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);

        //2D Bar Code
        byte[] barcode = StringUtil.hexStringToBytes("1d 6b 02 0d 36 39 30 31 32 33 34 35 36 37 38 39 32");


        /*********** print Tail*******/
        String receiptTail =  "Test Completed" + "\n"
                + "************************" + "\n";

        String receiptWeb =  "** union project ** " + "\n\n\n";

        byte[] foot = Printer.printfont(receiptTail,FontDefine.FONT_32PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);
        byte[] web	= Printer.printfont(receiptWeb,FontDefine.FONT_32PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);

        byte[] totladata =  new byte[header.length + englishchartext24.length + englishchartext32.length + englishchartext24underline.length +
                + barcode.length
                + foot.length + web.length
                ];
        int offset = 0;
        System.arraycopy(header, 0, totladata, offset, header.length);
        offset += header.length;

        System.arraycopy(englishchartext24, 0, totladata, offset, englishchartext24.length);
        offset+= englishchartext24.length;

        System.arraycopy(englishchartext32, 0, totladata, offset, englishchartext32.length);
        offset+=englishchartext32.length;

        System.arraycopy(englishchartext24underline, 0, totladata, offset, englishchartext24underline.length);
        offset+=englishchartext24underline.length;

        System.arraycopy(barcode, 0, totladata, offset, barcode.length);
        offset+=barcode.length;

        System.arraycopy(foot, 0, totladata, offset, foot.length);
        offset+=foot.length;

        System.arraycopy(web, 0, totladata, offset, web.length);
        offset+=web.length;

        byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totladata, 0, totladata.length);

        sendData(senddata);
    }

    private void printStruk() {

        String receiptHead = "\n************************"+"\n"
                           + "   TIKET MASUK WISATA  "+"\n"
                           + "       KAWAH PUTIH  "+"\n"
                           + "************************" + "\n";

        StringBuilder contentSb	= new StringBuilder();

        contentSb.append("--------------------------------" + "\n");
        contentSb.append("ORG DOM WD     6     300,000" + "\n");
        contentSb.append("MBL WD         1     150,000" + "\n");
        contentSb.append("--------------------------------" + "\n");
        contentSb.append("TOTAL                450,000" + "\n");
        contentSb.append("--------------------------------" + "\n");

        long milis		= System.currentTimeMillis();
        String date		= DateUtil.timeMilisToString(milis, "dd-MM-yy / HH:mm");

        StringBuilder contentSb2	= new StringBuilder();

        contentSb2.append("--------------------------------" + "\n");
        contentSb2.append("TANGGAL   : " + date + "\n");
        contentSb2.append("OPERATOR  : GANI GAIRAH A." + "\n");
        contentSb2.append("--------------------------------" + "\n");

        String message	= "Perhutani menyatakan struk ini \n  sebagai bukti pembayaran sah" + "\n";
        String message2	= "Hubungi Call Center: 1 500 235 \n       www.perhutani.co.id" + "\n";

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

        //byte[] dateByte		= Printer.printfont(date, FontDefine.FONT_24PX,FontDefine.Align_LEFT, (byte)0x1A,
        //        PocketPos.LANGUAGE_ENGLISH);

        byte[] barcode = StringUtil.hexStringToBytes("1d 6b 02 0d 31 33 30 37 31 37 32 30 30 30 30 31 32");

        byte[] btmSpaceByte	= Printer.printfont("\n\n", FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] totalByte	= new byte[logo.length + titleByte.length + content1Byte.length + messageByte.length +
                message2Byte.length + content2Byte.length + barcode.length + btmSpaceByte.length];


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

        System.arraycopy(btmSpaceByte, 0, totalByte, offset, btmSpaceByte.length);
        offset+=btmSpaceByte.length;

        //System.arraycopy(dateByte, 0, totalByte, offset, dateByte.length);

        byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totalByte, 0, totalByte.length);

        sendData(senddata);
    }

    private void printStruk2() {

        StringBuilder header = new StringBuilder();

        header.append("*****************************" + "\n");
        header.append("TIKET MASUK WISATA" + "\n");
        header.append("KAWAH PUTIH" + "\n");
        header.append("*****************************" + "\n");

        StringBuilder contentSb	= new StringBuilder();

        contentSb.append("-----------------------------" + "\n");
        contentSb.append("TANGGAL   : 12/07/2016 15.19" + "\n");
        contentSb.append("OPERATOR  : GANI GAIRAH A." + "\n");
        contentSb.append("-----------------------------" + "\n");
        contentSb.append("DOMESTIK WD    6     300,000" + "\n");
        contentSb.append("MOBIL WD       1     150,000" + "\n");
        contentSb.append("-----------------------------" + "\n");
        contentSb.append("TOTAL     :          450,000" + "\n");
        contentSb.append("-----------------------------" + "\n");

        String message	= "Perhutani menyatakan struk ini sebagai bukti pembayaran sah." + "\n";

        String message2	= "Informasi Hubungi Call Center: " + "\n"
                + "1 500 235 Atau Hub Perhutani Terdekat." + "\n";

        long milis		= System.currentTimeMillis();
        String date		= DateUtil.timeMilisToString(milis, "dd-MM-yy / HH:mm")  + "\n\n";

        byte[] titleByte	= Printer.printfont(header.toString(), FontDefine.FONT_24PX,FontDefine.Align_CENTER,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] content1Byte	= Printer.printfont(contentSb.toString(), FontDefine.FONT_24PX,FontDefine.Align_LEFT,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] messageByte	= Printer.printfont(message, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] message2Byte	= Printer.printfont(message2, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] dateByte		= Printer.printfont(date, FontDefine.FONT_24PX,FontDefine.Align_LEFT, (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] totalByte	= new byte[titleByte.length + content1Byte.length + messageByte.length +
                 message2Byte.length + dateByte.length];


        int offset = 0;
        System.arraycopy(titleByte, 0, totalByte, offset, titleByte.length);
        offset += titleByte.length;

        System.arraycopy(content1Byte, 0, totalByte, offset, content1Byte.length);
        offset += content1Byte.length;

        System.arraycopy(messageByte, 0, totalByte, offset, messageByte.length);
        offset += messageByte.length;

        System.arraycopy(message2Byte, 0, totalByte, offset, message2Byte.length);
        offset += message2Byte.length;

        System.arraycopy(dateByte, 0, totalByte, offset, dateByte.length);

        byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totalByte, 0, totalByte.length);

        //print1DBarcode();

        sendData(senddata);

    }

    private void print1DBarcode() {
        String content	= "6901234567892";

        //1D barcode format (hex): 1d 6b 02 0d + barcode data

        byte[] formats	= {(byte) 0x1d, (byte) 0x6b, (byte) 0x02, (byte) 0x0d};
        byte[] contents	= content.getBytes();

        byte[] bytes	= new byte[formats.length + contents.length];

        System.arraycopy(formats, 0, bytes, 0, formats.length);
        System.arraycopy(contents, 0, bytes, formats.length, contents.length);

        sendData(bytes);

        byte[] newline 	= Printer.printfont("\n\n",FontDefine.FONT_32PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);

        sendData(newline);
    }

    private void print2DBarcode() {
        String content 	= "Lorenz Blog - www.londatiga.net";

        //2D barcode format (hex) : 1d 6b 10 00 00 00 00 00 1f + barcode data

        byte[] formats	= {(byte) 0x1d, (byte) 0x6b, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x1f};
        byte[] contents	= content.getBytes();
        byte[] bytes	= new byte[formats.length + contents.length];

        System.arraycopy(formats, 0, bytes, 0, formats.length);
        System.arraycopy(contents, 0, bytes, formats.length, contents.length);

        sendData(bytes);

        byte[] newline 	= Printer.printfont("\n\n",FontDefine.FONT_32PX,FontDefine.Align_CENTER,(byte)0x1A,PocketPos.LANGUAGE_ENGLISH);

        sendData(newline);
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
