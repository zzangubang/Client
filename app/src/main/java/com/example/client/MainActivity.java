package com.example.client;

import androidx.appcompat.app.AppCompatActivity;

import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    TextView connectText;
    TextView sORgText, dataText;
    EditText ipEdit, portEdit, dataEdit;
    Button sendDataBTN, connectBTN;
    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectText = (TextView) findViewById(R.id.connectText);
        ipEdit = (EditText) findViewById(R.id.ipEdit);
        portEdit = (EditText) findViewById(R.id.portEdit);
        dataEdit = (EditText) findViewById(R.id.dataEdit);
        sendDataBTN = (Button) findViewById(R.id.sendDataBTN);
        connectBTN = (Button) findViewById(R.id.connectBTN);
        sORgText = (TextView) findViewById(R.id.sORgText);
        dataText = (TextView) findViewById(R.id.dataText);

        int SDK_INT = Build.VERSION.SDK_INT;
        if(SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        connectBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ipEdit.getText().toString().equals("") || portEdit.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),"ip와 port를 입력해주세요",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Connect 시도", Toast.LENGTH_SHORT).show();
                    String ip = ipEdit.getText().toString();
                    int port = Integer.valueOf(portEdit.getText().toString());
                    ConnectThread thread = new ConnectThread(ip, port);
                    thread.start();
                }
            }
        });
        sendDataBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(dataEdit.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),"데이터를 입력해주세요",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "\"" + dataEdit.getText().toString() + "\" 데이터 전송", Toast.LENGTH_SHORT).show();
                    String data = dataEdit.getText().toString();
                    SendThread thread = new SendThread(data);
                    thread.start();
                }
            }
        });
    }

    // 앱 종료시, 소켓 종료.
    @Override
    protected void onStop() {
        super.onStop();
        try {
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    // 뒤로가기 버튼 클릭시, 앱 종료(= 소켓 종료)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 아두이노 서버와 연결.
    class ConnectThread extends Thread {
        String dataG;
        String ip;
        int port;
        public ConnectThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public void run() {
            try {
                socket = new Socket(ip, port);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InetAddress addr = socket.getInetAddress();
                        String tmp = addr.getHostAddress();
                        connectText.setText(tmp + " 연결 완료");
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    }
                });

            }
            // 오류 발생시.
            catch (UnknownHostException uhe) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error: 호스트의 IP 주소를 식별할 수 없음", Toast.LENGTH_SHORT).show();
                        connectText.setText("Error: 호스트의 IP 주소를 식별할 수 없음");
                    }
                });
            } catch (IOException e) { // 소켓 생성 과정에서 I/O 에러 발생(주로 네트워크 응답 없음).
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error: 네트워크 응답 없음", Toast.LENGTH_SHORT).show();
                        connectText.setText("Error: 네트워크 응답 없음");
                    }
                });
            } catch (SecurityException se) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error: 보안 위반에 대해 보안 관리자에 의해 발생", Toast.LENGTH_SHORT).show();
                        connectText.setText("Error: 보안 위반에 대해 보안 관리자에 의해 발생.");
                    }
                });
            } catch (IllegalArgumentException le) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error: 0~65535 범위 밖의 포트 번호 사용, null 프록시 사용", Toast.LENGTH_SHORT).show();
                        connectText.setText("Error: 0~65535 범위 밖의 포트 번호 사용, null 프록시 사용");
                    }
                });
            }


        }
    }

    // 서버 연결 후, 데이터 전송.
    class SendThread extends Thread {
        String data;
        String dataG;

        public SendThread(String data) {
            this.data = data;
        }


        public String byteArrayToHex(byte[] a) {
            StringBuilder sb = new StringBuilder();
            for(final byte b: a)
                sb.append(String.format("%02x", b&0xff));
            return sb.toString();
        }

        public void run() {
            try { // 데이터 송신.
                String outdata = data;
                byte[] dataS= outdata.getBytes();
                OutputStream output = socket.getOutputStream();
                output.write(dataS);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sORgText.setText("SEND");
                        dataText.setText(data);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            try { // 데이터 수신.
                while (true) {
                    byte[] buffer = new byte[1024];
                    InputStream input = socket.getInputStream();
                    int bytes = input.read(buffer);

                    String dataG = new String(buffer, 0, bytes);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sORgText.setText("GET");
                            dataText.setText(dataG);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
}