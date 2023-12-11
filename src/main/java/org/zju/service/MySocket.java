package org.zju.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class MySocket {
    private ArrayList<SubThread> subThreads = new ArrayList<>();
    private ArrayList<Socket> sockets = new ArrayList<>();

    public MySocket() throws IOException {
        System.out.println("服务端在监听");
        ServerSocket ss = new ServerSocket(4535);
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String s = scanner.next();
            if("quit".equals(s)){
                for (SubThread subThread : subThreads) {
                    subThread.interrupt();
                }
                for (Socket socket : sockets) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.exit(0);
            }
        }
        ).start();
        int index = 0;
        while (true) {
            {
                Socket socket = ss.accept();
                SubThread subThread = new SubThread(socket);
                subThread.setName("client" + index++);
                subThread.start();
                sockets.add(socket);
                subThreads.add(subThread);
            }
        }
    }
}
