package com.woosim.sample;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.woosim.printer.WoosimCmd;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class MultiLanguage extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_language);
    }

    public void printGreek(View v) throws UnsupportedEncodingException {
        String str1 = "Windows 1253: Greek\n";
        String str2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&*()<>ΑΒΓΔΕΖΗΘΛΠΣΦΨΩ\n";

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(WoosimCmd.initPrinter());
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_WIN1253, WoosimCmd.FONT_LARGE));
        buffer.put(str1.getBytes());
        buffer.put(str2.getBytes("windows-1253"));
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        buffer.put(WoosimCmd.printLineFeed(2));
        printBuffer(buffer);
    }

    public void printLatin9(View v) {
        String str1 = "ISO-8859-15: Latin9\n";
        String str2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&*()<>€¢£¥ŠŒ¿ÀÁÂÃÄÅÐÑÝÞßæøý\n";

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(WoosimCmd.initPrinter());
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_ISO8859_15, WoosimCmd.FONT_LARGE));
        buffer.put(str1.getBytes());
        try {
            buffer.put(str2.getBytes("ISO-8859-15"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        buffer.put(WoosimCmd.printLineFeed(2));
        printBuffer(buffer);
    }

    public void printRussian(View v) {
        String str1 = "Windows 1251: Russian(Cyrillic)\n";
        String str2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&*()<>ЂЉГДЕЖЗИЙКЛПЦШЩЫЮЯ\n";

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(WoosimCmd.initPrinter());
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_WIN1251, WoosimCmd.FONT_LARGE));
        buffer.put(str1.getBytes());
        try {
            buffer.put(str2.getBytes("windows-1251"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        buffer.put(WoosimCmd.printLineFeed(2));
        printBuffer(buffer);
    }

    public void printHebrew(View v) throws UnsupportedEncodingException {
        String str1 = "Windows 1255: Hebrew\n";
        String str2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&*()<>€£§©®†אבגהזטמףצקשת\n";

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(WoosimCmd.initPrinter());
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_WIN1255, WoosimCmd.FONT_LARGE));
        buffer.put(str1.getBytes());
        buffer.put(str2.getBytes("windows-1255"));
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        buffer.put(WoosimCmd.printLineFeed(2));
        printBuffer(buffer);
    }

    public void printHangul(View v) throws UnsupportedEncodingException {
        String str1 = "EUC-KR: Korean\n";
        String str2 = "가나다라마바사아자차카타파하\n";

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(WoosimCmd.initPrinter());
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_DBCS, WoosimCmd.FONT_LARGE));
        buffer.put(str1.getBytes());
        buffer.put(str2.getBytes("EUC-KR"));
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        buffer.put(WoosimCmd.printLineFeed(2));
        printBuffer(buffer);
    }

    public void printChinese(View v) throws UnsupportedEncodingException {
        String str1 = "GB18030: Chinese\n";
        String str2 = "您好，见到您很高兴。\n";

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put(WoosimCmd.initPrinter());
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_DBCS, WoosimCmd.FONT_LARGE));
        buffer.put(str1.getBytes());
        buffer.put(str2.getBytes("GB18030"));
        buffer.put(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        buffer.put(WoosimCmd.printLineFeed(2));
        printBuffer(buffer);
    }

    private void printBuffer(ByteBuffer buffer) {
        byte[] byteArray = new byte[buffer.position()];
        buffer.position(0);
        buffer.get(byteArray);
        MainActivity.mPrintService.write(byteArray);
    }
}
