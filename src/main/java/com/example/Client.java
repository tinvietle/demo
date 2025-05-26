package com.example;

import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try{
            // Socket s = new Socket("192.168.1.13",1234);

            
            Scanner scanner = new Scanner(System.in);
            String sc = scanner.nextLine();
            System.out.print("Enter host address: ");
            String host = scanner.nextLine();
            System.out.print("Enter port number: ");
            int port = Integer.parseInt(scanner.nextLine());
            System.out.println("Connecting to " + host + " on port " + port);
            Socket s = new Socket(host, port);

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
