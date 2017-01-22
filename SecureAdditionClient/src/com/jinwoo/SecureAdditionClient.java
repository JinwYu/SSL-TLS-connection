package com.jinwoo;
// A client-side class that uses a secure TCP/IP socket

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Scanner;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by Jinwoo on 2016-12-20.
 *
 * A client-side class that uses a secure Tcp/IP socket
 *
 * The majority of this code is standard code from
 * the book, pages 777-779, on the course website.
 *
 * Run the SecureAdditionServer then run the SecureAdditionClient.
 */
public class SecureAdditionClient {
    private InetAddress host;
    private int port;
    // This is not a reserved port number
    static final int DEFAULT_PORT = 8189;

    static final String KEYSTORE = "certificates/PIERkeystore.ks";
    static final String TRUSTSTORE = "certificates/PIERtruststore.ks";
    static final String STOREPASSWD = "123456";
    static final String ALIASPASSWD = "123456";
    static final String EXIT = "EXIT";
    static final String DOWNLOAD = "DOWNLOAD";
    static final String UPLOAD = "UPLOAD";
    static final String DELETE = "DELETE";
    static final String ERROR = "ERROR";
    static final String DATA_TEXT_SENT = "DATA_TEXT_SENT";
    static final String DATA_NAME_SENT = "DATA_NAME_SENT";
    static final String FINISHED_REQUEST = "FINISHED_REQUEST";

    BufferedReader socketInput;
    PrintWriter socketOutput;

    /** Constructor
     * @param host Internet address of the host
     *        where the server is located
     * @param port Port number on the host where
     *        the server is listening
     */
    public SecureAdditionClient(InetAddress host, int port ) {
        this.host = host;
        this.port = port;
    }

    /** The method used to start a client object
     */
    public void run() {
        try {
            // Set up security. The following statements create an empty keystore and an empty truststore
            // objects and then load them with the contents of the program's keystore and truststore files.
            KeyStore ks = KeyStore.getInstance( "JCEKS" );
            ks.load( new FileInputStream( KEYSTORE ), STOREPASSWD.toCharArray() );
            KeyStore ts = KeyStore.getInstance( "JCEKS" );
            ts.load( new FileInputStream( TRUSTSTORE ), STOREPASSWD.toCharArray() );

            // The SSL connection will require access to encryption keys and certificates. For that reason,
            // factory objects to create both KeyManager and TrustManager objects are created and then
            // initialized with the KeyStore and TrustStore objects:
            KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
            kmf.init(ks, ALIASPASSWD.toCharArray() );
            TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
            tmf.init(ts);
            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );

            // The client needs an SSLSocketFactory object.
            SSLSocketFactory sslFact = sslContext.getSocketFactory();

            // With the factory object available, the required SSLSocket object is created to connect
            // with the specified host using the port identified. As with the SSLServerSocketobject,
            // all the supported cipher suites are enabled for the dient to maximize its flexibility when
            // negotiating with the server to agree upon the cipher suite that will be used for the secure
            // connection:
            SSLSocket client =  (SSLSocket)sslFact.createSocket(host, port);
            client.setEnabledCipherSuites( client.getSupportedCipherSuites() );

            System.out.println("--------------------------------------------");
            System.out.println("The SSL/TLS handshake was completed.");
            System.out.println("The client is now connected to the server.");
            System.out.println("--------------------------------------------");

            // These are used to receive and send information between the client and the server.
            socketInput = new BufferedReader(new InputStreamReader(client.getInputStream() ) );
            socketOutput = new PrintWriter(client.getOutputStream(), true );

            // Used to read the user input.
            Scanner scanner = new Scanner(System.in);

            String command;
            boolean running = true;

            // Handles incoming commands from the server.
            while(running){
                // Display the menu.
                displayMenu();

                // Read the input from the user.
                command = scanner.nextLine();

                if(command.equals("1")){
                    downloadFileFromServer();
                }
                else if(command.equals("2")){

                    uploadFileToServer();
                }
                else if(command.equals("3")) {
                    deleteFileFromServer();
                }
                else if(command.equals("4")){
                    deleteFileFromClient();
                }
                else if(command.equals("5")) {
                    socketOutput.println(EXIT);
                    System.out.println("Exiting the program.");
                    System.exit(0);
                }else{
                    System.exit(0);
                    running = false;
                }
            }
        }catch(Exception x) {
            System.out.println("Client error.");
            System.out.println(x);
            x.printStackTrace();
        }
    }

    /** Display the options.
     */
    private void displayMenu(){
        System.out.println("\n" + "-----------------");
        System.out.println("Enter the nr to:");
        System.out.println("1: Download from the server");
        System.out.println("2: Upload to the server");
        System.out.println("3: Delete from the server");
        System.out.println("4: Delete from the client");
        System.out.println("5: Exit");
        System.out.println("-----------------");
    }

    /** Download a text file from the server.
     */
    private void downloadFileFromServer() {

        // Get the file name for the requested file.
        System.out.println("Enter the file name that you wish to download:");
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.nextLine();

        // Send the request to the server.
        socketOutput.println(DOWNLOAD);
        // Send the filename.
        socketOutput.println(fileName);
        // End the request.
        socketOutput.println(DATA_NAME_SENT);


        // Downloading the file by copying the text from the text file
        // and then writing that text to a new text file.
        try{

            String temp = socketInput.readLine();

            // Check if the file exists.
            if(!temp.equals(ERROR)){

                StringBuilder stringBuilder = new StringBuilder();

                // Reading the text from the file, which is on the server.
                while(!temp.equals(DATA_TEXT_SENT)) {
                    // Build the string with the text from the file.
                    stringBuilder.append(temp);
                    // Iterate.
                    temp = socketInput.readLine();
                }

                String fileTextContent = stringBuilder.toString();

                // Creating a new file with the text taken from above.
                try{
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("files/" + fileName));
                    // Write the whole string to the newly created file.
                    bufferedWriter.write(fileTextContent, 0, fileTextContent.length());
                    bufferedWriter.close();

                    System.out.println("The file " + fileName + " has been downloaded.");

                }catch (IOException x) {
                    System.out.println("An error occurred in the client when trying to write the downloaded file.");
                    //System.out.println(x);
                    //x.printStackTrace();
                }

            }else{
                // Print the error message from the server.
                System.out.println(socketInput.readLine());
            }

            // Print the error message from the server if an error has occurred.
            if(socketInput.readLine().equals(ERROR)){
                System.out.println(socketInput.readLine());
            }

        }catch (IOException x) {
            System.out.println("An error occurred in the client when trying to download the file " + fileName + ".");
            //System.out.println(x);
            //x.printStackTrace();
        }

    }

    /** Upload a text file to the server.
     */
    private void uploadFileToServer() {
        System.out.println("Enter the file name that you wish to upload to the server:");
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.nextLine();

        // Read the text from the file and send to the server.
        try {
            // Try to read the file, it will throw an exception if the file does not exist.
            BufferedReader bufferedReader = new BufferedReader(new FileReader("files/"  + fileName));

            // If the file has been found then send the request
            // to the server that the client wants to upload a file.
            socketOutput.println(UPLOAD);
            socketOutput.println(fileName);
            // Notify the server that the file name has been sent.
            socketOutput.println(DATA_NAME_SENT);

            String fileText;
            // Read the text. "readLine" returns null when reached the end of the line.
            while ((fileText = bufferedReader.readLine()) != null) {
                // Send the text to the server.
                socketOutput.println(fileText);
            }
            // Notify the server that all of the text from the file has been sent.
            socketOutput.println(DATA_TEXT_SENT);

            // Print the message from the server if the file was successfully uploaded or not.
            if(socketInput.readLine().equals(FINISHED_REQUEST) || socketInput.readLine().equals(ERROR)){
                System.out.println(socketInput.readLine());
            }

        } catch (IOException x) {
            System.out.println("The file can " + fileName + " not be found.");
            socketOutput.flush();
            //System.out.println(x);
            //x.printStackTrace();
        }
    }

    /** Delete a text file from the server.
     */
    private void deleteFileFromServer() {
        try {
            System.out.println("Enter the file name that you wish to delete from the server:");
            Scanner scanner = new Scanner(System.in);
            String fileName = scanner.nextLine();

            // Send the request to the server that the
            // client wants to delete a file from the server.
            socketOutput.println(DELETE);
            socketOutput.println(fileName);
            socketOutput.println(DATA_NAME_SENT);

            // Print the message from the server if the file was successfully removed or not.
            String temp = socketInput.readLine();
            boolean running = true;

            while(running){
                // Check if the client has received any commands from the server.
                if(temp.equals(FINISHED_REQUEST) || temp.equals(ERROR)){
                    // Print the message from the server.
                    System.out.println(socketInput.readLine());
                    running = false;
                }else{
                    // Iterate.
                    temp = socketInput.readLine();
                }
            }

        }catch (IOException x) {
            System.out.println("An error occurred when trying to delete the file from the server.");
            //System.out.println(x);
            //x.printStackTrace();
        }
    }

    /** Delete a text file from the client.
     */
    private void deleteFileFromClient(){
        System.out.println("Enter the file name that you wish to delete from the client:");
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.nextLine();

        if(!fileName.equals("")){
            try {
                // Find the file and delete it.
                File file = new File("files/" + fileName);

                // Check if the file can be deleted.
                if(file.delete()){
                    System.out.println("The file " + fileName + " was deleted from the client.");
                }else{
                    System.out.println("An error occurred when trying to delete the file " + fileName + " from the client.");
                }

            }catch (Exception e) {
                System.out.println("An error occurred when trying to delete the file " + fileName + " from the client.");
                //e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        try {
            InetAddress host = InetAddress.getLocalHost();
            int port = DEFAULT_PORT;
            if ( args.length > 0 ) {
                port = Integer.parseInt( args[0] );
            }
            if ( args.length > 1 ) {
                host = InetAddress.getByName( args[1] );
            }
            SecureAdditionClient addClient = new SecureAdditionClient( host, port );
            addClient.run();
        }
        catch ( UnknownHostException uhx ) {
            System.out.println( uhx );
            uhx.printStackTrace();
        }

    }

}
