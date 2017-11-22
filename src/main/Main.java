package main;

import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * Einstiegspunkt der Applikation Hier kann der Benutzer angeben, ob er einen
 * Server oder einen Client starten möchte
 *
 * @author Dominik Sust
 * @creation 22.11.2017 07:42:40
 */
public class Main
{

    public static void main( String[] args ) throws IOException
    {
        String[] optionen = new String[]
        {
            "Server", "Client", "Beenden"
        };
        int choice = JOptionPane.showOptionDialog(
                null,
                "Was möchten Sie starten?",
                "Bitte wählen",
                0,
                JOptionPane.PLAIN_MESSAGE,
                null,
                optionen,
                optionen[1] );
        switch ( choice )
        {
            case 0:
                new ChatServer();
                break;
            case 1:
                new ChatClient();
                break;
        }
    }
}
