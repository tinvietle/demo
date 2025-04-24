package com.example;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientSending implements Runnable{
    private Socket s;

    ClientSending(Socket Cl) {
        s = Cl;
    }

    public void run() {

        Scanner bp = new Scanner(System.in);
        DataOutputStream gui = null;
        ClientHandler clientHandler = new ClientHandler(s);
        try {
            gui = new DataOutputStream(s.getOutputStream());
            while (true) {
                String tam2 = bp.nextLine();
                gui.writeUTF(tam2);
                if (tam2.equals("put")){
                    clientHandler.openFileChoser();
                }

            }
        } catch (IOException ex) {

        }

    }
}