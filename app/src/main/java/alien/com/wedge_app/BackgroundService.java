/**
 * LICENSE
 * This Source Code and its associated files are
 * Copyright 2017 Alien Technology LLC. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package alien.com.wedge_app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.alien.common.KeyCode;
import com.alien.rfid.RFID;
import com.alien.rfid.RFIDCallback;
import com.alien.rfid.RFIDReader;
import com.alien.rfid.RFIDResult;
import com.alien.rfid.ReaderException;
import com.alien.rfid.Tag;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dmeng on 2/10/2017.
 */
public class BackgroundService extends AccessibilityService {
    private RFIDReader reader;
    private RFIDResult result;
    private int keyResult;
    private static final String TAG_EPC = "colEPC";
    private static final String TAG_COUNT = "colCount";
    private ArrayList<HashMap<String, String>> tagList;
    private AccessibilityNodeInfo source1 = null;
    private int btnKey = 0;
    private String res = null;

    @Override
    public boolean onKeyEvent(KeyEvent event) {

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        // create an array of EPC-hashed items to store tags
        if (tagList == null)
            tagList = new ArrayList<HashMap<String, String>>();

        if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyCode.ALR_H450.SCAN) {
                Log.e("-SCAN KEY UP Event-", "----------");
                // Display the tag data

                try {
                    // initialize RFID interface and obtain a global RFID Reader instance
                    reader = RFID.open();
                    if (reader.isRunning()) {
                        stopScan();
                        btnKey = 0;
                        if (reader != null) reader.close();
                    } else {
                        startScan();
                        btnKey = 1;
                    }

                } catch (ReaderException e) {
                    Toast.makeText(this, "RFID init failed: " + e, Toast.LENGTH_LONG).show();
                }


/*
                String r;
                if (result.isSuccess()) {
                    Tag tag = (Tag) result.getData();
                    r = "EPC: " + tag.getEPC() + "\n" + "PC: " + tag.getPC() + "\n" + "RSSI: " + tag.getRSSI();
                } else {
                    r = "No tag found.";
                }

                Toast.makeText(getApplicationContext(), r, Toast.LENGTH_LONG).show();*/
                return true;
            } else if (keyCode == KeyCode.ALR_H450.SIDE_LEFT) {
                Log.e("-LEFT KEY UP Event-", "----------");
                return true;
            } else {
                return super.onKeyEvent(event);
            }
        } /*else if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyCode.ALR_H450.SCAN) {
                Log.e("-SCAN KEY DOWN Event-", "----------");
                // read a tag
                try {
                    reader = RFID.open();
                    result = reader.read();
                } catch (ReaderException e) {
                    e.printStackTrace();
                }
                return true;
            } else if (keyCode == KeyCode.ALR_H450.SIDE_LEFT) {
                Log.e("-LEFT KEY DOWN Event-", "----------");
                return true;
            } else {
                return super.onKeyEvent(event);
            }
        }*/ else {
            return super.onKeyEvent(event);
        }
    }

    @Override
    public void onServiceConnected() {
        Log.e("-onServiceConnected-", "----------");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS |
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.e("-onAccessibilityEvent-", "----------");
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            source1 = source;
        }

        if (source1 != null && btnKey == 1) {
           // btnKey = 0;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", res);
            clipboard.setPrimaryClip(clip);
            source1.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
    }

    @Override
    public void onInterrupt() {
    }

    private void startScan() {
        if (reader == null || reader.isRunning()) return;

        try {
            // create a callback to receive tag data
            reader.inventory(new RFIDCallback() {
                @Override
                public void onTagRead(Tag tag) {
                    addTag(tag);
                }
            });
        } catch (ReaderException e) {
            Toast.makeText(this, "ERROR: " + e, Toast.LENGTH_LONG).show();
        }
    }

    private void stopScan() {
        if (reader == null || !reader.isRunning()) return;

        // stop continuous inventory
        try {
            reader.stop();
        } catch (ReaderException e) {
            Toast.makeText(this, "ERROR: " + e, Toast.LENGTH_LONG).show();
        }
    }

    public void onRead(View view) {
        if (reader == null) return;

        try {
            // Read a single tag and add it to the list
            RFIDResult result = reader.read();
            if (!result.isSuccess()) {
                Toast.makeText(this, "No tags found", Toast.LENGTH_LONG).show();
                return;
            }
            addTag((Tag) result.getData());
        } catch (ReaderException e) {
            Toast.makeText(this, "ERROR: " + e, Toast.LENGTH_LONG).show();
        }
    }

    public void onScan(View view) {
        if (reader.isRunning())
            stopScan();
        else
            startScan();
    }

    public void addTag(final Tag tag) {

        if (tag.getEPC().isEmpty()) {
            return;
        }

        // if this tag is already in the list, increment the count
        for (HashMap<String, String> item : tagList) {
            if (item.get(TAG_EPC).equals(tag.getEPC())) {
                int c = Integer.parseInt(item.get(TAG_COUNT)) + 1;
                item.put(TAG_COUNT, String.valueOf(c));
                return;
            }
        }
        // this is a new tag, add it to the list
        res = tag.getEPC();
        btnKey = 1;
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put(TAG_EPC, tag.getEPC());
        hm.put(TAG_COUNT, "1");
        tagList.add(hm);
    }

}
