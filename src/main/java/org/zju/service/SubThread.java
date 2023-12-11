package org.zju.service;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SubThread extends Thread {
    private Socket socket;
    private ArrayList<String> list;
    private String[] firstLine;
    private String contentType;
    private Integer contentLength = 0;

    public SubThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            list = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
            firstLine = list.get(0).split(" ");
            String method = firstLine[0];
            String path = firstLine[1];
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

    private void doGet(String path) throws IOException {
        String[] split = path.split("\\.");
        String type = split[split.length - 1];
        switch (type) {
            case "txt" -> contentType = "text/plain";
            case "jpg" -> contentType = "image/jpeg";
            case "html" -> contentType = "text/html";
            default -> throw new RuntimeException("请求路径错误");
        }
        path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("")).getPath() + path;
        FileInputStream fis = null;
        OutputStream out = socket.getOutputStream();
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            //文件不存在
            StringBuilder httpResponse = HTTPResponse("404");
            httpResponse.append("\n");
            out.write(httpResponse.toString().getBytes());
            out.flush();
            out.close();
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

        if(contentType.contains("text")){
            httpResponse.append("\n\n").append(builder);
        }else {
            httpResponse.append("\n\n").append(Arrays.toString(bytes));
        }

        // 关闭输入流和输出流
        fis.close();
        out.close();
    }

    private void doPost(String path) throws IOException {
        String[] split = path.split("/");
        String dopost = split[split.length - 1];
        contentType = "text/html";
        OutputStream out = socket.getOutputStream();
        if(!"dopost".equals(dopost)){
            //文件不存在
            StringBuilder httpResponse = HTTPResponse("404");
            httpResponse.append("\n");
            out.write(httpResponse.toString().getBytes());
            out.flush();
            out.close();
            return;
        }
        // 将输入流传给输出流
        StringBuilder httpResponse = HTTPResponse("200").append("\n");
        //读取2个回车换行后面的体部内容（长度根据头部的Content-Length字段的指示）
        // 并提取出登录名（login）和密码（pass）的值
        String login = null;
        String pass = null;
        for (String s : list) {
            if(s.contains("login")){
                String[] strings = s.split("=");
                login = strings[1].split("&")[0];
                pass = strings[2];
                break;
            }
        }
        if("3170104535".equals(login) && "4535".equals(pass)){
            httpResponse.append("<html><body>登录成功</body></html>");
        }else {
            httpResponse.append("<html><body>登录失败</body></html>");
        }
        out.write(httpResponse.toString().getBytes());
        out.flush();
        socket.close();
        out.close();
    }

    private StringBuilder HTTPResponse(String statusCode) {
        StringBuilder buffer = new StringBuilder();
        if ("404".equals(statusCode)) {
            buffer.append(firstLine[2]).append(" 404").append(" NOT FOUND\n");
        }
        if ("200".equals(statusCode)) {
            buffer.append(firstLine[2]).append(" 200").append(" OK\n");
        }
        buffer.append("Content-Type: ").append(contentType).append("\n");
        buffer.append("Content-Length: ").append(contentLength).append("\n");
        return buffer;
    }
}
