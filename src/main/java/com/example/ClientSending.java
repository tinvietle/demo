package com.example;

import java.io.PrintWriter;
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
        PrintWriter gui = null;
        try {
            gui = new PrintWriter(s.getOutputStream(), true);
            while (true) {
                String tam2 = bp.nextLine();
                gui.println(tam2);

                if (tam2.equals("quit")){
                    break;
                }
            }
        } catch (IOException ex) {

        }

        bp.close();
        if (gui != null) {
            gui.close();
        }
    }
}