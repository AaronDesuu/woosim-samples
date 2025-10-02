package com.woosim.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.woosim.printer.WoosimBarcode;
import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Example extends AppCompatActivity {
    private static final String TAG = "Example";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example);
    }

    public void printLines(View v) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        byteStream.write(WoosimCmd.initPrinter());
        byteStream.write(WoosimCmd.setTextStyle(true, false, false, 1, 1));
        byteStream.write("Draw Lines\n".getBytes());
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(WoosimCmd.setPageMode());
        int height = 500;
        byteStream.write(WoosimCmd.PM_setArea(0, 0, 384, height));
        byteStream.write(WoosimImage.drawLine(0, 10, 384, 10, 2));
        byteStream.write(WoosimImage.drawLine(0, 50, 384, 50, 4));
        byteStream.write(WoosimImage.drawLine(0, 90, 384, 90, 8));
        byteStream.write(WoosimImage.drawLine(0, 120, 0, height, 2));
        byteStream.write(WoosimImage.drawLine(40, 120, 40, height, 4));
        byteStream.write(WoosimImage.drawLine(80, 120, 80, height, 8));
        byteStream.write(WoosimImage.drawLine(120, 120, 384, height, 2));
        byteStream.write(WoosimImage.drawLine(384, 120, 120, height, 4));
        byteStream.write(WoosimCmd.PM_printStdMode());
        byteStream.write(WoosimCmd.printLineFeed(4));
        MainActivity.mPrintService.write(byteStream.toByteArray());
    }

    public void printBoxes(View v) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        byteStream.write(WoosimCmd.initPrinter());
        byteStream.write(WoosimCmd.setTextStyle(true, false, false, 1, 1));
        byteStream.write("Draw Boxes\n".getBytes());
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(WoosimCmd.setPageMode());
        int height = 500;
        byteStream.write(WoosimCmd.PM_setArea(0, 0, 384, height));
        byteStream.write(WoosimImage.drawBox(0, 0, 384, height, 4));
        byteStream.write(WoosimImage.drawBox(20, 20, 384-40, 150, 2));
        byteStream.write(WoosimImage.drawBox(20, 200, 384/2-20, height-250, 6));
        byteStream.write(WoosimImage.drawBox(384/2+40, 300, 384/2-60, height-320, 1));
        byteStream.write(WoosimCmd.PM_printStdMode());
        byteStream.write(WoosimCmd.printLineFeed(4));
        MainActivity.mPrintService.write(byteStream.toByteArray());
    }

    public void printEllipse(View v) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        byteStream.write(WoosimCmd.initPrinter());
        byteStream.write(WoosimCmd.setTextStyle(true, false, false, 1, 1));
        byteStream.write("Draw Ellipse\n".getBytes());
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(WoosimCmd.setPageMode());
        int height = 300;
        byteStream.write(WoosimCmd.PM_setArea(0, 0, 384, height));
        byteStream.write(WoosimImage.drawEllipse(384/2, height/2, 384/2-100, height/2-20, 4));
        byteStream.write(WoosimCmd.PM_printData());
        byteStream.write(WoosimCmd.PM_deleteData());
        height = 200;
        byteStream.write(WoosimCmd.PM_setArea(0, 0, 384, height));
        byteStream.write(WoosimImage.drawEllipse(384/2, height/2, 384/2-50, height/2-20, 2));
        byteStream.write(WoosimCmd.PM_printData());
        byteStream.write(WoosimCmd.PM_deleteData());
        for (int i= 0 ; i < 4 ; i++) {
            byteStream.write(WoosimImage.drawEllipse(384/2-60+(i*30), height/2, 80, 80, 2));
        }
        byteStream.write(WoosimCmd.PM_printStdMode());
        byteStream.write(WoosimCmd.printLineFeed(4));
        MainActivity.mPrintService.write(byteStream.toByteArray());
    }

    public void printDirection(View v) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        byteStream.write(WoosimCmd.initPrinter());
        byteStream.write(WoosimCmd.setTextStyle(true, false, false, 1, 1));
        byteStream.write("Set Writing Direction\n".getBytes());
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(WoosimCmd.setPageMode());
        byteStream.write(WoosimCmd.PM_setArea(0, 0, 384, 300));
        byteStream.write(WoosimImage.drawBox(0, 0, 384, 300, 3));
        byteStream.write(WoosimCmd.PM_setPosition(5, 5));
        byteStream.write("LEFT to RIGHT".getBytes());
        byteStream.write(WoosimCmd.PM_setDirection(1));
        byteStream.write(WoosimCmd.PM_setPosition(5, 5));
        byteStream.write("BOTTOM to UP".getBytes());
        byteStream.write(WoosimCmd.PM_setDirection(2));
        byteStream.write(WoosimCmd.PM_setPosition(5, 5));
        byteStream.write("RIGHT to LEFT".getBytes());
        byteStream.write(WoosimCmd.PM_setDirection(3));
        byteStream.write(WoosimCmd.PM_setPosition(5, 5));
        byteStream.write("TOP to BOTTOM".getBytes());
        byteStream.write(WoosimCmd.PM_printStdMode());
        byteStream.write(WoosimCmd.printLineFeed(4));
        MainActivity.mPrintService.write(byteStream.toByteArray());
    }

    public void printImageText(View v) {
        MainActivity.mPrintService.write(WoosimCmd.initPrinter());
        MainActivity.mPrintService.write(WoosimCmd.setPageMode());

        sendImg(0, 0, R.drawable.logo, false);
        sendImg(280, 10, R.drawable.android, true);

        MainActivity.mPrintService.write(WoosimCmd.PM_setArea(0, 0, 384, 150));
        MainActivity.mPrintService.write(WoosimCmd.PM_setPosition(70, 75));
        MainActivity.mPrintService.write(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        MainActivity.mPrintService.write("Hello, Woosim!".getBytes());
        MainActivity.mPrintService.write(WoosimCmd.PM_printStdMode());
    }

    public void printLabel(View v) throws IOException {
        MainActivity.mPrintService.write(WoosimCmd.initPrinter());
        sendImg(0, 0, R.drawable.logo, false);
        MainActivity.mPrintService.write(WoosimCmd.printData());

        String str1 = "SHIP TO:\n";
        String str2 = "        #60, Sandan-ro 388beon-gil\n" +
                      "        Galsan-myeon, Hongseong-gun,\n" +
                      "        Chungcheongnam-do, Rep. of Korea\n";
        String str3 = "http://www.woosim.com/";
        String str4 = "ITEM    : Printer";
        String str5 = "Quantity: 10";
        String str6 = "TRACKING NUMBER:";
        String str7 = "134 35490 7564";

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
        byteStream.write(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        byteStream.write(WoosimCmd.setTextStyle(true, false, false, 1, 1));
        byteStream.write(str1.getBytes());
        byteStream.write(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_MEDIUM));
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(str2.getBytes());
        byteStream.write(WoosimCmd.setPageMode());
        byteStream.write(WoosimCmd.PM_setArea(0, 0, 384, 300));
        byteStream.write(WoosimImage.drawBox(2, 1, 370, 0, 4));
        byteStream.write(WoosimCmd.PM_setPosition(0, 7));
        byteStream.write(WoosimBarcode.create2DBarcodeQRCode(0, (byte)0x4D, 3, str3.getBytes()));
        byteStream.write(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_LARGE));
        byteStream.write(WoosimCmd.setTextStyle(true, false, false, 1, 1));
        byteStream.write(WoosimCmd.PM_setPosition(100, 20));
        byteStream.write(str4.getBytes());
        byteStream.write(WoosimCmd.PM_setPosition(100, 55));
        byteStream.write(str5.getBytes());
        byteStream.write(WoosimImage.drawBox(2, 90, 370, 0, 4));
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(WoosimCmd.PM_setPosition(0, 100));
        byteStream.write(str6.getBytes());
        byteStream.write(WoosimCmd.setCodeTable(WoosimCmd.MCU_RX, WoosimCmd.CT_CP437, WoosimCmd.FONT_MEDIUM));
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(WoosimCmd.PM_setPosition(130, 130));
        byteStream.write(str7.getBytes());
        byteStream.write(WoosimCmd.PM_setPosition(20, 160));
        byteStream.write(WoosimBarcode.createBarcode(WoosimBarcode.CODE128, 2, 100, false, str7.getBytes()));
        byteStream.write(WoosimCmd.PM_printStdMode());
        byteStream.write(WoosimCmd.feedToMark());
        MainActivity.mPrintService.write(byteStream.toByteArray());
    }

    private void sendImg(int x, int y, int id, boolean dithering) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), id, options);
        if (bmp == null)
            return;
        byte[] data = dithering ? WoosimImage.drawColorBitmap(x, y, bmp) : WoosimImage.drawBitmap(x, y, bmp);
        bmp.recycle();
        MainActivity.mPrintService.write(data);
    }

    public void printPDF(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            mDocumentLauncher.launch(intent);
        } else {
            int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                getPFD();
            } else {
                mPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    ActivityResultLauncher<Intent> mDocumentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent intent = result.getData();
            Uri uri = intent != null ? intent.getData() : null;
            Log.d(TAG, "Document URI: " + uri);
            if (uri != null) {
                try {
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                    printPFD(pfd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    ActivityResultLauncher<String> mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            getPFD();
        }
    });

    private void getPFD() {
        // Check for external storage mount
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED) && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Log.e(TAG, "External Storage State: "+ state);
            Toast.makeText(this, R.string.warn_unmount, Toast.LENGTH_SHORT).show();
            return;
        }
        // Assume that sample.pdf file exists in the Documents directory
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/sample.pdf");
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            printPFD(pfd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printPFD(ParcelFileDescriptor pfd) {
        new Thread(() -> {
            MainActivity.mPrintService.write(WoosimCmd.initPrinter());
            try {
                PdfRenderer renderer = new PdfRenderer(pfd);
                for (int i=0 ; i < renderer.getPageCount() ; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    // Original page is resized to fit 2 inch roll paper width (384 dot).
                    // It can be changed to 576 and 832 for 3 and 4 inch roll paper respectively.
                    Bitmap bmp = Bitmap.createBitmap(384, page.getHeight() * 384 / page.getWidth(), Bitmap.Config.ARGB_8888);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                    MainActivity.mPrintService.write(WoosimImage.printStdModeBitmap(bmp));
                    bmp.recycle();
                    page.close();
                }
                renderer.close();
                pfd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            MainActivity.mPrintService.write(WoosimCmd.printLineFeed(2));
        }).start();
    }
}
