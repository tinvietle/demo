package com.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try{
            Socket s = new Socket("localhost",1234);
            DataInputStream Network_in = new DataInputStream(s.getInputStream());
            DataOutputStream Network_out = new DataOutputStream(s.getOutputStream());
            Scanner keyboard = new Scanner(System.in);
            
            ClientSending sending = new ClientSending(s);
            ClientReceiving receiving = new ClientReceiving(s);

            Thread t1 = new Thread(sending);
            Thread t2 = new Thread(receiving);
            t1.start();
            t2.start();
            
        }
        catch(Exception e){
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
