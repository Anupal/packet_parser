package com.example.anupal.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.BitSet;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothAdapter mBluetoothAdapter;

    TextView rtctv, meterid, rssi, kwh, kw;

    private ScanCallback mLeScanCallback =
            new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, final ScanResult result) {

                    String year, month, day, hour, minute, second;


                    String str = result.getDevice().getName();
                    if (str != null) {

                        if (str.equals("NB5505581")) {
                            int rssival = result.getRssi();
                            Log.i("BLE", "Device Name: "+ str);
                            byte[] bytes = result.getScanRecord().getBytes();
                            StringBuilder sb = new StringBuilder();
                            for (byte b : bytes) {
                                sb.append(String.format("%02X ", b));
                            }
                            String input = removeSpace(sb.toString());
                            String rtc = input.substring(42, 52);
                            String reading = input.substring(52,58);
                            String reading2 = input.substring(58,62);


                            rtc = "0x" + rtc;
                            BitSet bitSet = hexStringToBitSet(rtc);

                            year = Long.toString(Bitset2Int(bitSet.get(0,12)));
                            month = Long.toString(Bitset2Int(bitSet.get(12,16)));
                            day = Long.toString(Bitset2Int(bitSet.get(16,21)));
                            second = Long.toString(Bitset2Int(bitSet.get(21,27)));
                            minute = Long.toString(Bitset2Int(bitSet.get(27,33)));
                            hour = Long.toString(Bitset2Int(bitSet.get(33,38)));

                            double val = ((double)hex2Decimal(reading)/10);
                            double val2 = ((double)hex2Decimal(reading2)/1000);


                            Log.i("BLE", "Reading: " + val  + " RTC: " + hour + ':' + minute + ':' + second + " -- " + day + '/' + month + '/' + year);
                            meterid.setText(str);
                            rssi.setText(Integer.toString(rssival));
                            rtctv.setText( hour + ':' + minute + ':' + second + " -- " + day + '/' + month + '/' + year);
                            kwh.setText(Double.toString(val));
                            kw.setText(Double.toString(val2));

                        }
                    }
                }
                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.i("BLE", "error");
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rtctv = (TextView) findViewById(R.id.rtcvalue);
        meterid = (TextView) findViewById(R.id.meterid);
        rssi = (TextView) findViewById(R.id.rssivalue);
        kwh = (TextView) findViewById(R.id.kwhvalue);
        kw = (TextView) findViewById(R.id.kwvalue);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(mLeScanCallback);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("BLE", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public static int hex2Decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }

    public static String removeSpace(String s) {
        String withoutspaces = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ')
                withoutspaces += s.charAt(i);

        }
        return withoutspaces;

    }

    public static BitSet hexStringToBitSet(String hexString) {
        return BitSet.valueOf(new long[]{Long.valueOf(hexString.substring(2), 16)});
    }

    public static long Bitset2Int(BitSet bits) {
        long value = 0L;
        for (int i = 0; i < bits.length(); ++i) {
            value += bits.get(i) ? (1L << i) : 0L;
        }
        return value;
    }

}
