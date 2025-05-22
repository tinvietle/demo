package com.example;

import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try{
            // Socket s = new Socket("192.168.1.13",1234);
            Socket s = new Socket("localhost",1234);
            
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
