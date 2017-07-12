package com.example.danangnurfauzi.eticket;

import com.example.danangnurfauzi.eticket.R;
import com.example.danangnurfauzi.eticket.pockdata.PocketPos;

import com.example.danangnurfauzi.eticket.util.DateUtil;
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
                + "   TIKET MASUK TEST BROH WISATA  "+"\n"
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

        String receiptHead = "************************"
                + "   TIKET MASUK WISATA  "+"\n"
                + "       KAWAH PUTIH  "+"\n"
                + "************************"
                + "\n";

        String titleStr	= "STRUK PEMBAYARAN TAGIHAN LISTRIK" + "\n\n";

        StringBuilder contentSb	= new StringBuilder();

        contentSb.append("WISAT     : 435353535435353" + "\n");
        contentSb.append("NAMA      : LORENSIUS WLT" + "\n");
        contentSb.append("TRF/DAYA  : 50/12244 VA" + "\n");
        contentSb.append("BL/TH     : 02/14" + "\n");
        contentSb.append("ST/MTR    : 0293232" + "\n");
        contentSb.append("RP TAG    : Rp. 100.000" + "\n");
        contentSb.append("JPA REF   :" + "\n");

        StringBuilder content2Sb = new StringBuilder();

        content2Sb.append("ADM BANK  : Rp. 1.600" + "\n");
        content2Sb.append("RP BAYAR  : Rp. 101.600,00" + "\n");

        String jpaRef	= "XXXX-XXXX-XXXX-XXXX" + "\n";
        String message	= "PLN menyatakan struk ini sebagai bukti pembayaran yang sah." + "\n";
        String message2	= "Rincian tagihan dapat diakses di www.pln.co.id Informasi Hubungi Call Center: "
                + "123 Atau Hub PLN Terdekat: 444" + "\n";

        long milis		= System.currentTimeMillis();
        String date		= DateUtil.timeMilisToString(milis, "dd-MM-yy / HH:mm")  + "\n\n";

        byte[] titleByte	= Printer.printfont(receiptHead, FontDefine.FONT_24PX,FontDefine.Align_CENTER,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] content1Byte	= Printer.printfont(contentSb.toString(), FontDefine.FONT_24PX,FontDefine.Align_LEFT,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] refByte		= Printer.printfont(jpaRef, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] messageByte	= Printer.printfont(message, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] content2Byte	= Printer.printfont(content2Sb.toString(), FontDefine.FONT_24PX,FontDefine.Align_LEFT,
                (byte)0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] message2Byte	= Printer.printfont(message2, FontDefine.FONT_24PX,FontDefine.Align_CENTER,  (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] dateByte		= Printer.printfont(date, FontDefine.FONT_24PX,FontDefine.Align_LEFT, (byte)0x1A,
                PocketPos.LANGUAGE_ENGLISH);

        byte[] totalByte	= new byte[titleByte.length + content1Byte.length + refByte.length + messageByte.length +
                content2Byte.length + message2Byte.length + dateByte.length];


        int offset = 0;
        System.arraycopy(titleByte, 0, totalByte, offset, titleByte.length);
        offset += titleByte.length;

        System.arraycopy(content1Byte, 0, totalByte, offset, content1Byte.length);
        offset += content1Byte.length;

        System.arraycopy(refByte, 0, totalByte, offset, refByte.length);
        offset += refByte.length;

        System.arraycopy(messageByte, 0, totalByte, offset, messageByte.length);
        offset += messageByte.length;

        System.arraycopy(content2Byte, 0, totalByte, offset, content2Byte.length);
        offset += content2Byte.length;

        System.arraycopy(message2Byte, 0, totalByte, offset, message2Byte.length);
        offset += message2Byte.length;

        System.arraycopy(dateByte, 0, totalByte, offset, dateByte.length);

        byte[] senddata = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totalByte, 0, totalByte.length);

        sendData(senddata);
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
