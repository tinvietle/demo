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
        ClientHandler clientHandler = new ClientHandler(s);
        try {
            nhan = new DataInputStream(s.getInputStream());
            while (true) {

                String tam = nhan.readUTF();
                if (tam.equals("ftp> ")) {
                    System.out.print("ftp> ");
                } else {
                    System.out.println(tam);
                }
                if(tam.equals("221 Service closing control connection")){
                    break;
                }
                if (tam.equals("put")){
                    clientHandler.receiveFile();
                }


            }
        } catch (IOException ex) {

        }

    }
}