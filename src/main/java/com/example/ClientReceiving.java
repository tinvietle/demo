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
                switch (tam) {
                    case "ftp> ":
                        System.out.print("ftp> ");
                        break;
                    case "221 Service closing control connection":
                        System.out.println(tam);
                        break;
                    case "put":
                        clientHandler.receiveFile();
                        break;
                    case "put allowed":
                        clientHandler.openFileChoser();
                        break;
                    default:
                        System.out.println(tam);
                        break;
                }
                if (tam.equals("221 Service closing control connection")) {
                    System.out.println("221 Server closed connection");
                    break;
                }


            }
        } catch (IOException ex) {

        }

    }
}