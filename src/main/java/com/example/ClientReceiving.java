package com.example;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientReceiving implements Runnable {
    Socket s;

    ClientReceiving(Socket cl) {
        s = cl;
    }

    public void run() {
        DataInputStream nhan;
        try {
            nhan = new DataInputStream(s.getInputStream());
            while (true) {

                String tam = nhan.readUTF();
                System.out.println(tam);

            }
        } catch (IOException ex) {

        }

    }
}
