package com.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    /**
     * Says hello to the world.
     * @param args The arguments of the program.
     */
    private static final int NTHREADS = 100; //maximum threads for this program
	private static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS); 
	
    public static void main(String[] args) {
        try {
            ServerSocket socket = new ServerSocket(1234); 
            while (true) { 
                final Socket connection = socket.accept(); 
                ServerHandler each_client = new ServerHandler(connection, "tin");
                exec.execute(each_client); 
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }          
    }
}
