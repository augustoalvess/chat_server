/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modulo.sistema.dao;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import modulo.sistema.negocio.ChatMessage;
import modulo.sistema.negocio.ChatMessage.Action;

/**
 *
 * @author augusto
 * ServidorService
 */
public class ChatMessageServidorDAO {
    
    private ServerSocket serverSocket;
    private Socket socket;
    private Map<String, ObjectOutputStream> mapOnlines = new HashMap<String, ObjectOutputStream>();
    
    public ChatMessageServidorDAO() {
        try {
            serverSocket = new ServerSocket(5555);
            System.out.println("Servidor on!");
            
            while (true) {
                socket = serverSocket.accept();
                new Thread(new ListenerSocket(socket)).start();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ChatMessageServidorDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private class ListenerSocket implements Runnable {
        
        private ObjectOutputStream output;
        private ObjectInputStream input;
        
        public ListenerSocket(Socket socket) throws IOException {
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            ChatMessage message = null;
            
            
            try {
                while ((message = (ChatMessage) input.readObject()) != null) {
                    Action action = message.getAction();
                    
                    if (action.equals(Action.CONNECT)) {
                        boolean isConnect = connect(message, output);
                        if ( isConnect ) {
                            mapOnlines.put(message.getName(), output);
                            sendOnlines();
                        }
                    } else if (action.equals(Action.DISCONNECT)) {
                        disconnect(message, output);
                        sendOnlines();
                        return;
                    } else if (action.equals(Action.SEND_ONE)) {
                        sendOne(message);
                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(message);
                    } else if (action.equals(Action.USERS_ONLINE)) {
                        
                    }
                }
            } catch (IOException ex) {
                disconnect(message, output);
                sendOnlines();
                System.out.println(message.getName() + "Deixou o chat!");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ChatMessageServidorDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private boolean connect(ChatMessage message, ObjectOutputStream output) throws IOException {
        
        if ( mapOnlines.size() == 0 ) {
            message.setText("YES");
            send(message, output);
            
            return true;
        }
        
        for ( Map.Entry<String, ObjectOutputStream> kv: mapOnlines.entrySet() ) {
            if ( kv.getKey().equals(message.getName()) ) {
                message.setText("NO");
                send(message, output);                
                return false;
            } else {
                message.setText("YES");
                send(message, output);
                return true;
            }
        }
        
        return false;
    }
    
    private void disconnect(ChatMessage message, ObjectOutputStream output) {
        mapOnlines.remove(message.getName());
        message.setText("está desconectado!");
        message.setAction(Action.SEND_ONE);
        sendAll(message);
    }
    
    private void send(ChatMessage message, ObjectOutputStream output) throws IOException {
        output.writeObject(message);
    }
    
    private void sendOne(ChatMessage message) throws IOException {
        for ( Map.Entry<String, ObjectOutputStream> kv: mapOnlines.entrySet() ) {
            if ( kv.getKey().equals(message.getNameReserved()) ) {
                kv.getValue().writeObject(message);
                break;
            }
        }
    }
    
    private void sendAll(ChatMessage message) {
        for ( Map.Entry<String, ObjectOutputStream> kv: mapOnlines.entrySet() ) {
            if ( !kv.getKey().equals(message.getName()) ) {
                message.setAction(Action.SEND_ONE);
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ChatMessageServidorDAO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void sendOnlines() {
        Set<String> setNames = new HashSet<String>();
        for ( Map.Entry<String, ObjectOutputStream> kv: mapOnlines.entrySet() ) {
            setNames.add(kv.getKey());
        }
        
        ChatMessage message = new ChatMessage();
        message.setAction(Action.USERS_ONLINE);
        message.setSetOnlines(setNames);
        
        for ( Map.Entry<String, ObjectOutputStream> kv: mapOnlines.entrySet() ) {
            message.setName(kv.getKey());
            
            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(ChatMessageServidorDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
}
