package main;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import main.ChatServer.MessageType;

/**
 *
 * @author Dominik Sust
 * @creation 22.11.2017 09:09:03
 */
public class ChatClient
{

    private BufferedReader in;
    private PrintWriter out;

    private final JTextField tf_username = new JTextField( 20 );
    private final JTextField tf_message = new JTextField();
    private final JButton button_send = new JButton( "Senden" );
    private final JTextArea ta_messages = new JTextArea( 20, 50 );
    private JFrame frame;

    public ChatClient()
    {
        //Frame aufbauen
        buildFrame();

        connectToServer();

        //ActionListener setzen
        button_send.addActionListener( (e) -> sendMessage() );
        tf_message.addActionListener( (e) -> sendMessage() );
    }

    /**
     * Baut das Chatfenster auf
     */
    private void buildFrame()
    {
        frame = new JFrame( "Chat" );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setLayout( new GridBagLayout() );

        //Werden aktiviert, sobald die Verbindung steht
        tf_message.setEnabled( false );
        button_send.setEnabled( false );

        JPanel wrapper_username = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        wrapper_username.add( new JLabel( "Username:" ) );
        wrapper_username.add( tf_username );
        tf_username.setEditable( false );

        //Username Wrapper
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets( 5, 5, 5, 5 );
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        frame.add( wrapper_username, gbc );

        //TextArea
        ta_messages.setEditable( false );
        JScrollPane scrollpane = new JScrollPane( ta_messages );
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        frame.add( scrollpane, gbc );

        //Message Textfield
        gbc.gridy = 2;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        frame.add( tf_message, gbc );

        //Button
        gbc.weightx = 0.0;
        gbc.gridx = 1;
        frame.add( button_send, gbc );

        frame.pack();
        frame.setLocationRelativeTo( null );

        frame.setVisible( true );
    }

    private void sendMessage()
    {
        String message = tf_message.getText();
        if ( message != null && !message.isEmpty() )
        {
            out.println( message );
        }
        tf_message.setText( "" );
        tf_message.requestFocusInWindow();
    }

    private void connectToServer()
    {
        SwingWorker sw = new SwingWorker()
        {
            private String username;

            @Override
            protected Object doInBackground() throws Exception
            {
                String serverAdresse = JOptionPane.showInputDialog( frame, "Bitte Serveradresse eingeben:" );
                Socket socket = new Socket();
                socket.connect( new InetSocketAddress( serverAdresse, ChatServer.PORT ), 10000 );

                //Stream vom Server
                in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

                //Stream zum Server
                out = new PrintWriter( socket.getOutputStream(), true );

                while ( true )
                {
                    //Message vom Server abrufen
                    String line = in.readLine();
                    //Server möchte den Usernamen wissen
                    if ( line.startsWith( MessageType.ENTERUSERNAME.toString() ) )
                    {
                        username = JOptionPane.showInputDialog( frame, "Bitte Username eingeben:" );
                        out.println( username );
                    }
                    //Username wurde akzeptiert
                    else if ( line.startsWith( MessageType.USERNAMEACCEPTED.toString() ) )
                    {
                        tf_username.setText( username );
                        tf_message.setEnabled( true );
                        button_send.setEnabled( true );
                        tf_message.requestFocusInWindow();
                    }
                    //Neue Message
                    else if ( line.startsWith( MessageType.MESSAGE.toString() ) )
                    {
                        ta_messages.append( line.substring( MessageType.MESSAGE.toString().length() ) + "\n" );
                    }
                    else if ( line.startsWith( MessageType.SERVERSHUTDOWN.toString() ) )
                    {
                        ta_messages.append( "Der Server wurde beendet." + "\n" );
                        tf_message.setEnabled( false );
                        button_send.setEnabled( false );
                    }
                }
            }
        };
        sw.execute();
    }

}
