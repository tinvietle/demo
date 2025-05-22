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
        try {
            gui = new DataOutputStream(s.getOutputStream());
            while (true) {
                String tam2 = bp.nextLine();
                gui.writeUTF(tam2);

                if (tam2.equals("quit")){
                    break;
                }
            }
        } catch (IOException ex) {

        }

        bp.close();
        try {
            gui.close();
        } catch (IOException ex) {
            System.out.println("Error closing DataOutputStream: " + ex.getMessage());
        }

    }
}