package main;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

/**
 * Der Chatserver kümmert sich um die Anmeldung und Verteilung aller gesendeten
 * Messages im Chat. Er selbst hält die Messages nicht bereit
 *
 * @author Dominik Sust
 * @creation 22.11.2017 08:14:05
 */
public class ChatServer
{

    //Die Userliste des Frames
    private JList<Object> userliste;

    //Alle Messages, die gesendet werden, beginnen mit einer dieser 
    //Optionen. So können Server alsauch Client erkennen, um welche 
    //Art Message es sich handelt
    enum MessageType
    {
        ENTERUSERNAME, USERNAMEACCEPTED, MESSAGE, SERVERSHUTDOWN
    }

    //Port des Servers
    public static final int PORT = 9001;

    //Hashset mit allen Benutzernamen
    private final static HashSet<String> usernames = new HashSet<>();

    //Hashset mit allen Printwritern der User um die Nachrichten zu verteilen
    private static HashSet<PrintWriter> writers = new HashSet<>();

    /**
     * Konstruktor der Klasse. Hier wird das Server-Überwachungsframe gebaut.
     * Danach wird ein Server-Socket erstellt und auf einen bestimmten Port
     * gehört. Immer wenn sich ein Client anmeldet, wird ein neuer Handler
     * erstellt, der sich dann um die Kommunikation mit diesem Client kümmert
     *
     * @throws IOException
     */
    public ChatServer() throws IOException
    {
        //Baue das Server-Fenster und zeige es an
        buildFrame();

        //Der Shutdown-Hook wird ausgeführt, sobald die Applikation beendet wird.
        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
        {
            public void run()
            {
                sendMessage( MessageType.SERVERSHUTDOWN, "Server", "Server wurde beendet" );
            }
        } ) );

        //Erstellt einen Serversocket, der auf eingehende Verbindungen auf
        //diesen Port hört
        ServerSocket listener = new ServerSocket( PORT );
        try
        {
            while ( true )
            {
                //Immer wenn ein Client eine Verbindung aufbauen möchte,
                //wird ein neues Handler-Objekt erzeugt, das in einem separaten
                //Thread läuft
                new Handler( listener.accept() ).start();
            }
        }
        finally
        {
            //Schliessen des ServerSockets bei einem Fehler
            listener.close();
        }
    }

    /**
     * Baut das Fenster mit der Liste aller angemeldeten Benutzer auf.
     */
    private void buildFrame()
    {
        JFrame frame = new JFrame( "Chatserver" );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setLayout( new BorderLayout() );

        frame.add( new JLabel( "Angemeldete User" ), BorderLayout.NORTH );

        userliste = new JList<>();
        userliste.setModel( new DefaultListModel<>() );
        userliste.setVisibleRowCount( 10 );

        JScrollPane scrollpane = new JScrollPane( userliste );
        frame.add( scrollpane, BorderLayout.CENTER );

        frame.pack();

        frame.setVisible( true );
    }

    /**
     * Sendet eine Message an alle angemeldeten Clients
     *
     * @param type der MessageType
     * @param user Der Benutzername von dem die Message ist
     * @param message Die Message
     */
    private void sendMessage( MessageType type, String user, String message )
    {
        writers.forEach( (writer) -> writer.println( type.toString() + " " + user + ": " + message ) );
    }

    /**
     * Handler Klasse. Wird aus dem Loop des Server erstellt und gestartet und
     * kümmert sich um die Kommunikation mit einem Client
     */
    private class Handler extends Thread
    {

        //Der Username
        private String name;
        //Der übergebene Socket
        private Socket socket;
        //Kommunikation vom Client zum Server
        private BufferedReader in;
        //Kommunikation vom Server zum Client
        private PrintWriter out;

        private Handler( Socket socket )
        {
            this.socket = socket;
        }

        @Override
        public void run()
        {
            try
            {
                //Messagestream vom Client zum Server
                in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

                //Stream vom Server zum Client
                out = new PrintWriter( socket.getOutputStream(), true );

                //Phase 1: Abfrage nach einem Usernamen
                //Wird wiederholt, bis ein güliger Username eingegeben wurde
                while ( true )
                {
                    out.println( MessageType.ENTERUSERNAME.toString() );
                    name = in.readLine();
                    if ( name == null )
                    {
                        return;
                    }
                    //Da theoretisch mehrere Anmeldungen gleichzeitig ausgeführt
                    //werden könnten, muss das Hashset während der Überprüfung
                    //mittels synchronized gesperrt werden
                    //Andere Threads müssen dann warten, bis das Hashset wieder
                    //freigegeben ist
                    synchronized (usernames)
                    {
                        //Ist der Username noch nicht in der Liste
                        if ( !usernames.contains( name ) )
                        {
                            //Username aufnehmen
                            usernames.add( name );
                            //Username im Serverfenster anzeigen
                            ((DefaultListModel) userliste.getModel()).addElement( name );
                            break;
                        }
                    }
                }

                //Phase 2: Benutzer hat einen gültigen Benutzernamen eingegeben
                //Aufnahme des Streams für die Messages
                out.println( MessageType.USERNAMEACCEPTED.toString() );
                //Aufnahme des Streams in das Set, damit auch dieser nun 
                //alle Messages des Chats erhält
                writers.add( out );
                //Übrige Benutzer informieren
                sendMessage( MessageType.MESSAGE, "Server", "'" + name + "' hat den Chat betreten." );

                //Phase 3: Messages des Users werden akzeptiert und an alle
                //anderen Benutzer weitergeleitet
                while ( true )
                {
                    String input = in.readLine();
                    if ( input == null )
                    {
                        return;
                    }
                    sendMessage( MessageType.MESSAGE, name, input );
                }

            }
            catch ( Exception e )
            {
                System.out.println( name + ": " + e );
            }
            finally
            {
                // Phase 4: Der Client ist weg. Name und Streams entfernen
                if ( name != null )
                {
                    //Übrige Benutzer informieren
                    System.out.println( "'" + name + "' hat den Chat verlassen." );
                    sendMessage( MessageType.MESSAGE, "Server", "'" + name + "' hat den Chat verlassen." );
                    ((DefaultListModel) userliste.getModel()).removeElement( name );
                    usernames.remove( name );
                }
                if ( out != null )
                {
                    writers.remove( out );
                }
                try
                {
                    socket.close();
                }
                catch ( IOException e )
                {
                }
            }
        }
    }
}
