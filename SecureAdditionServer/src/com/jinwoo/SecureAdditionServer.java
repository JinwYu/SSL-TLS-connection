package com.jinwoo;
import java.io.*;
import javax.net.ssl.*;
import java.security.*;

/**
 * Created by Jinwoo on 2016-12-20.
 *
 * The majority of this code is standard code from
 * the book, page 774, on the course website.
 *
 * Run the SecureAdditionServer then run the SecureAdditionClient.
 */
public class SecureAdditionServer {
    private int port;
    // This is not a reserved port number
    static final int DEFAULT_PORT = 8189;

    // These strings are the names of the keystore and truststore files, as weIl
    // as the password for accessing these two files (both files have the same password in this example)
    static final String KEYSTORE = "certificates/LIUkeystore.ks";
    static final String TRUSTSTORE = "certificates/LIUtruststore.ks";
    static final String STOREPASSWD = "123456";
    static final String ALIASPASSWD = "123456";
    static final String DOWNLOAD = "DOWNLOAD";
    static final String UPLOAD = "UPLOAD";
    static final String DELETE = "DELETE";
    static final String EXIT = "EXIT";
    static final String ERROR = "ERROR";
    static final String DATA_TEXT_SENT = "DATA_TEXT_SENT";
    static final String DATA_NAME_SENT = "DATA_NAME_SENT";
    static final String FINISHED_REQUEST = "FINISHED_REQUEST";

    BufferedReader socketInput;
    PrintWriter socketOutput;

    /**
     * Constructor
     *
     * @param port The port where the server
     *             will listen for requests
     */
    SecureAdditionServer(int port ) {
        this.port = port;
    }

    /**
     * The method that does the work for the class
     */
    public void run() {
        try {
            // Set up security. The following statements create an empty keystore and an empty truststore
            // objects and then load them with the contents of the program's keystore and truststore files.
            KeyStore ks = KeyStore.getInstance("JCEKS");
            ks.load(new FileInputStream(KEYSTORE), STOREPASSWD.toCharArray() );
            KeyStore ts = KeyStore.getInstance("JCEKS");
            ts.load(new FileInputStream(TRUSTSTORE), STOREPASSWD.toCharArray() );

            // The SSL connection will require access to encryption keys and certificates. For that reason,
            // factory objects to create both KeyManager and TrustManager objects are created and then
            // initialized with the KeyStore and TrustStoreobjects:
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, ALIASPASSWD.toCharArray() );
            TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
            tmf.init(ts);

            // Create an SSLContext object. The input parameter, TLS, indicates that we want to use the
            // Transport Layer Security standard. Once the SSLContext object is created,
            // it is initialized with all the KeyManager and TrustManager objects that the factory
            // objects support. The initialization method will also accept a third parameter, a random
            // number used in the process of generating the secret key that the SSL handshake will use.
            // In this case the null reference is input, so the default random number seed will be used:
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null );

            // Before creating an SSL server socket you must first use the SSLContext object to create
            // the factory object that will create the SSL server socket, because there is no public
            // constructor for the SSLServerSocket class that you can call:
            SSLServerSocketFactory sslServerFactory = sslContext.getServerSocketFactory();

            // Once a factory object is available,the SSLServerSocket object can be created to listen
            // on the specified port. To provide for maximum flexibility when doing protocol
            // negotiation with clients, all supported cipher suites are enabled and the accept method
            // is invoked to begin listening for connections:
            SSLServerSocket sss = (SSLServerSocket) sslServerFactory.createServerSocket(port);
            sss.setEnabledCipherSuites(sss.getSupportedCipherSuites() );
            sss.setNeedClientAuth(true);
            System.out.println("The server is online and waiting for incoming connections.");
            SSLSocket incoming = (SSLSocket) sss.accept();


            // Reads the receiving stream from the SSLSocket above.
            socketInput = new BufferedReader(new InputStreamReader(incoming.getInputStream() ) );
            // Prints the output to the SSLSocket (the clients).
            socketOutput = new PrintWriter(incoming.getOutputStream(), true );

            String temp;
            boolean running = true;
            // Handles all of the requests sent from the clients.
            while(running){
                temp = socketInput.readLine();

                if(temp.equals(DOWNLOAD)){
                    sendFileToClient();
                }
                else if(temp.equals(UPLOAD)){
                    uploadFileFromClient();
                }
                else if(temp.equals(DELETE)) {
                    deleteFileFromServer();
                }
                else if(temp.equals(EXIT)) {
                    System.out.println("Exiting the program.");
                    running = false;
                }else{
                    System.out.println("Unexpected input.");
                }
            }
            // Close the socket.
            incoming.close();
        }
        catch(Exception x) {
            // The catch block at the end of the run method has been altered to catch the more
            // generic Exception class instead of the IOException class. This was required because
            // the statements that work with keys and certificates and the statements to set up an SSL
            // connection have the potential to throw a wide range of exception classes.
            System.out.println("Server error.");
            System.out.println(x);
            x.printStackTrace();
        }
    }

    /** Sends a text file from the server to the client.
     *  Called when the client requests to download from
     *  the server.
     */
    private void sendFileToClient() {

        String temp;
        String fileName = "";
        try {
            // Get the file name.
            do {
                temp = socketInput.readLine();
                fileName = temp;
            }while(!(temp = socketInput.readLine()).equals(DATA_NAME_SENT));

            String fileText;

            // Try to read the file.
            try{
                BufferedReader reader = new BufferedReader(new FileReader("files/" + fileName));

                // Read through the file and send  the text to the client.
                while ((fileText = reader.readLine()) != null) {
                    socketOutput.println(fileText);
                }

                // Notify the client that the server has sent all of the text.
                socketOutput.println(DATA_TEXT_SENT);

            }catch (IOException x) {
                System.out.println("The file " + fileName + " can not be found.");

                // Notify the client that the file could not be found.
                socketOutput.println(ERROR);
                socketOutput.println("The file " + fileName + " can not be found.");
                //System.out.println(x);
                //x.printStackTrace();
            }

        } catch (IOException x) {
            System.out.println("There was an error trying to send the file " + fileName + " to the client.");
            socketOutput.println(ERROR);
            socketOutput.println("There was an error trying to send the file " + fileName + " to the client.");
            socketOutput.flush();
            //x.printStackTrace();
        }
        // Notify the client that the download request is over.
        socketOutput.println(FINISHED_REQUEST);
    }

    /** Upload a text file from the client to the server.
     *  Called when the clients requests to upload a file
     *  to the server.
     */
    private void uploadFileFromClient() {
        try {
            socketOutput.flush();
            String fileText;
            String fileName = "";
            String temp;

            // Get the file name.
            temp = socketInput.readLine();
            while(!(temp.equals(DATA_NAME_SENT))){
                fileName = temp;
                temp = socketInput.readLine();
            }

            // Receiving the text from the text file from the client.
            StringBuilder stringBuilder = new StringBuilder();
            fileText = socketInput.readLine();

            while(!(fileText.equals(DATA_TEXT_SENT))){
                stringBuilder.append(fileText);
                fileText = socketInput.readLine();
            }

            // Create the new file to be stored on the server.
            String textFromClientFile = stringBuilder.toString();

            try{
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("files/" + fileName));
                bufferedWriter.write(textFromClientFile, 0, textFromClientFile.length());
                bufferedWriter.close();

                // Notify the client that the file was uploaded successfully.
                socketOutput.println(FINISHED_REQUEST);
                socketOutput.println("The file " + fileName + " was uploaded successfully.");

            }catch (IOException x) {
                System.out.println("There was an error trying write the new file called " + fileName + " to the server.");

                // Notify the client that an error occurred.
                socketOutput.println(ERROR);
                socketOutput.println("There was an error trying upload the new file called " + fileName + " to the server.");
                //System.out.println(x);
                //x.printStackTrace();
            }
        }catch (IOException x) {
            System.out.println("There was an error trying to upload the file to the server.");

            // Notify the client that an error occurred.
            socketOutput.println(ERROR);
            socketOutput.println("There was an error trying to upload the file to the server.");
            //System.out.println(x);
            //x.printStackTrace();
        }
    }

    /** Delete a text file from the server.
     *  Called when the client requests to
     *  delete a file from the server.
     */
    private void deleteFileFromServer() {
        try {
            String temp;
            String fileName = "";

            // Receive the file name to the file we want to delete.
            while(!(temp = socketInput.readLine()).equals(DATA_NAME_SENT)) {
                fileName = temp;
            }

            // Check if the file name is empty.
            // If not, then search for the file.
            if(!fileName.equals("")) {
                try {
                    // Find the file and delete it.
                    File file = new File("files/" + fileName);

                    // Check if the file can be deleted.
                    if(file.delete()){
                        // Notify the client that the file was deleted.
                        socketOutput.println(FINISHED_REQUEST);
                        socketOutput.println("The file " + fileName + " was deleted from the server.");
                        System.out.println("The file " + fileName + " was deleted from the server.");
                    }else{
                        // Notify the client that the file was not deleted.
                        socketOutput.println(ERROR);
                        socketOutput.println("An error occurred when trying to delete the file " + fileName + " from the server.");
                        System.out.println("An error occurred when trying to delete the file " + fileName + " from the server.");
                    }

                }catch (Exception e) {
                    // Notify the client that the file was not deleted.
                    socketOutput.println(ERROR);
                    socketOutput.println("An error occurred when trying to delete the file " + fileName + " from the server.");

                    //e.printStackTrace();
                }
            }

        }catch (IOException e) {
            socketOutput.println("An error occurred when trying to delete the file from the server.");
            e.printStackTrace();
        }
    }


    /** The test method for the class
     * @param args[0] Optional port number in place of
     *        the default
     */
    public static void main( String[] args ) {
        int port = DEFAULT_PORT;
        if (args.length > 0 ) {
            port = Integer.parseInt( args[0] );
        }
        // Start the server.
        SecureAdditionServer addServe = new SecureAdditionServer(port);
        addServe.run();
    }
}

