package org.zju.service;

import jdk.jfr.DataAmount;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SubThread extends Thread {
    private Socket socket;
    private ArrayList<String> list = new ArrayList<>();
    private String[] firstLine;
    private String contentType;
    private Integer contentLength = 0;
    private String loginFail = """
            <html>
            <head>
                <title>first page</title>
            </head>
            <body>
                <h1>登陆失败！</h1>
            </body>
            </html>
            """;
    private String loginSuccess = """
            <html>
            <head>
                <title>first page</title>
            </head>
            <body>
                <h1>登陆成功！</h1>
            </body>
            </html>
            """;

    public SubThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            list = new ArrayList<>();
            String line;
            int bodyLength = 0;
            while ((line = br.readLine()) != null && !line.equals("")) {
                list.add(line);
                if (line.startsWith("Content-Length: ")) {
                    bodyLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                }
            }
            char[] buffer = new char[bodyLength];
            br.read(buffer, 0, bodyLength);
            list.add(new String(buffer));
            firstLine = list.get(0).split(" ");
            String method = firstLine[0];
            String path = firstLine[1];
            System.out.println(method);
            System.out.println(path);
            switch (method) {
                case "GET" -> {
                    doGet(path);
                }
                case "POST" -> {
                    doPost(path);
                }
                default -> {
                    throw new RuntimeException("未知请求");
                }
            }

            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doPost(String path) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String[] split = path.split("/");
        String dopost = split[split.length - 1];
        contentType = "text/html";
        if (!"dopost".equals(dopost)) {
            //文件不存在
            StringBuilder httpResponse = HTTPResponse("404");
            httpResponse.append("\n");
            bw.write(httpResponse.toString());
            bw.flush();
            bw.close();
            return;
        }
        // 将输入流传给输出流
        contentLength = loginSuccess.length();
        StringBuilder httpResponse = HTTPResponse("200").append("\n");
        //读取2个回车换行后面的体部内容（长度根据头部的Content-Length字段的指示）
        // 并提取出登录名（login）和密码（pass）的值
        String login = null;
        String pass = null;
        for (String s : list) {
            if (s.contains("login")) {
                String[] strings = s.split("=");
                login = strings[1].split("&")[0];
                pass = strings[2];
                break;
            }
        }
        if ("3170104535".equals(login) && "4535".equals(pass)) {
            httpResponse.append(loginSuccess);
        } else {
            httpResponse.append(loginFail);
        }
        bw.write(httpResponse.toString());
        bw.flush();
        bw.close();
        socket.close();
    }

    private void doGet(String path) throws IOException {
        String[] split = path.split("\\.");
        String type = split[split.length - 1];
        switch (type) {
            case "txt" -> contentType = "text/plain";
            case "jpg" -> contentType = "image/jpeg";
            case "html" -> contentType = "text/html";
        }
        path = "D:\\project\\WebServerStatic" + path.replaceAll("/", "\\\\");
        System.out.println(path);
        FileInputStream fis = null;
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            //文件不存在
            StringBuilder httpResponse = HTTPResponse("404");
            httpResponse.append("\n");
            bw.write(httpResponse.toString());
            bw.flush();
            bw.close();
            return;
        }
        byte[] bytes = fis.readAllBytes();
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append((char) b);
            contentLength += 1;
        }
        // 将输入流传给输出流
        StringBuilder httpResponse = HTTPResponse("200");

        if (contentType.contains("text")) {

            httpResponse.append("\n").append(builder);
            // 关闭输入流和输出流
            bw.write(httpResponse.toString());
            bw.flush();
            fis.close();
            bw.close();
        } else {
            httpResponse.append("\n");
            bos.write(httpResponse.toString().getBytes());
            bos.write(bytes);
            fis.close();
            bos.flush();
            bos.close();
        }


        socket.close();
    }

    private StringBuilder HTTPResponse(String statusCode) {
        StringBuilder buffer = new StringBuilder();
        if ("404".equals(statusCode)) {
            buffer.append(firstLine[2]).append(" 404").append(" NOT FOUND\n");
        }
        if ("200".equals(statusCode)) {
            buffer.append(firstLine[2]).append(" 200").append(" OK\n");
        }
        buffer.append("Content-Type: ").append(contentType).append("; charset=UTF-8\n");
        buffer.append("Content-Length: ").append(contentLength).append("\n");
        return buffer;
    }
}
