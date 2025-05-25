package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

public class ClientReceiving implements Runnable {
    Socket s;

    ClientReceiving(Socket cl) {
        s = cl;
    }

    public void run() {
        BufferedReader nhan;
        ClientHandler clientHandler = new ClientHandler(s);
        try {
            nhan = new BufferedReader(new InputStreamReader(s.getInputStream()));
            while (true) {
                String tam = nhan.readLine();
                if (tam == null) break;
                
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