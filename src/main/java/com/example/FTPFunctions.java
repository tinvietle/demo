package com.example;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class FTPFunctions {
    Socket socket;
    DataOutputStream outputStream;
    private String accessKey = "AKIA4Y4VQJAB65FA6NCF";
    private String secretKey = "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";
    private Regions region = Regions.AP_SOUTHEAST_2;
    private AmazonS3 s3Client;
    private String bucketName = "streamlitss"; // replace with your bucket name

    public FTPFunctions (Socket socket) {
        this.socket = socket;
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
        } catch (Exception e) {
            System.out.println("Error initializing output stream: " + e.getMessage());
        }           
    }

    public void sendFile(){
        System.out.println("Sending file...");
        try {
            outputStream.writeUTF("File sent successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void receiveFile(){
        System.out.println("Receiving file...");
        try {
            outputStream.writeUTF("File received successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void listFiles(){
        System.out.println("Listing files...");
        try {
            ListObjectsV2Result result = s3Client.listObjectsV2(bucketName);
            List<S3ObjectSummary> objects = result.getObjectSummaries();
            for (S3ObjectSummary os : objects) {
                System.out.println(" - " + os.getKey() + "  " + "(size = " + os.getSize() + ")");
                outputStream.writeUTF(" - " + os.getKey());
            }
            outputStream.writeUTF("Files listed successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void deleteFile(){
        System.out.println("Deleting file...");
        try {
            outputStream.writeUTF("File deleted successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void renameFile(){
        System.out.println("Renaming file...");
        try {
            outputStream.writeUTF("File renamed successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void createDirectory(){
        System.out.println("Creating directory...");
        try {
            outputStream.writeUTF("Directory created successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void deleteDirectory(){
        System.out.println("Deleting directory...");
        try {
            outputStream.writeUTF("Directory deleted successfully");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
