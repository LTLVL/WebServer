package org.zju.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class MySocket {
    private ArrayList<Runnable> subThreads = new ArrayList<>();
    private ArrayList<Socket> sockets = new ArrayList<>();

    public MySocket() throws IOException {
        System.out.println("服务端在监听");
        ServerSocket ss = new ServerSocket(4535);
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String s = scanner.next();
            if ("quit".equals(s)) {
                System.exit(0);
            }
        }
        ).start();
        int index = 0;
        while (true) {
            {
                Socket socket = ss.accept();
                new SubThread(socket).start();
            }
        }
    }
}




