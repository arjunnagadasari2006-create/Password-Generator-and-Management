package com.password;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class PasswordServer {

    private static SessionFactory sessionFactory;

    public static void main(String[] args) throws Exception {

        // ==============================
        // HIBERNATE CONFIGURATION
        // ==============================

        Configuration cfg = new Configuration();

        cfg.configure();

        cfg.addAnnotatedClass(Strongpassword.class);

        sessionFactory = cfg.buildSessionFactory();


        // ==============================
        // CREATE SERVER
        // ==============================

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(8080),
                        0
                );


        // ==============================
        // HOME PAGE
        // ==============================

        server.createContext(
                "/",
                PasswordServer::serveIndex
        );


        // ==============================
        // HEALTH CHECK
        // ==============================

        server.createContext(
                "/health",
                PasswordServer::health
        );


        // ==============================
        // SAVE USER
        // ==============================

        server.createContext(
                "/save",
                PasswordServer::saveUser
        );


        // ==============================
        // GET USERS
        // ==============================

        server.createContext(
                "/users",
                PasswordServer::getUsers
        );


        // ==============================
        // START SERVER
        // ==============================

        server.setExecutor(null);

        server.start();


        System.out.println(
                "===================================="
        );

        System.out.println(
                "Password Server Started"
        );

        System.out.println(
                "Open: http://localhost:8080"
        );

        System.out.println(
                "===================================="
        );
    }


    // ==================================================
    // SERVE INDEX.HTML
    // ==================================================

    private static void serveIndex(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    "Only GET method allowed",
                    405
            );

            return;
        }


        InputStream input =
                PasswordServer.class
                        .getClassLoader()
                        .getResourceAsStream(
                                "static/index.html"
                        );


        if (input == null) {

            sendResponse(
                    exchange,
                    "index.html not found",404);
            return;
        }
        byte[] html =input.readAllBytes();
        exchange.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200,html.length);
        try (OutputStream output =exchange.getResponseBody()) {

            output.write(html);
        }

        input.close();
    }
    // ==================================================
    // HEALTH CHECK
    // ==================================================
    private static void health( HttpExchange exchange) throws IOException {
        String response = "Java server is running";
        exchange.getResponseHeaders().set("Content-Type","text/plain; charset=UTF-8");
        byte[] bytes =response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200,bytes.length  );

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }


    // ==================================================
    // SAVE USER
    // ==================================================

    private static void saveUser(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(  exchange, "Only POST method allowed",405);
            return;
        }
        // READ REQUEST
        String body =new String( exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
        System.out.println("Received: " + body);
        // PARSE DATA
        Map<String, String> data = parseData(body);
        String username =data.get("username");
        String password =data.get("password");
        if (username == null ||
            password == null ||
            username.isBlank() ||
            password.isBlank()) {
            sendResponse(exchange,"Username and password required",400);
            return;
        }
        Session session = null;
        Transaction transaction = null;
        try {
            session =sessionFactory.openSession();
            transaction =session.beginTransaction();
            // FIND NEXT ID
            Number maxId =(Number) session.createQuery("select max(s.id) " +"from Strongpassword s").uniqueResult();
            int id;

            if (maxId == null) {
                id = 1;
            } else {
                id =maxId.intValue() + 1;
            }
            // CREATE USER
            Strongpassword user =new Strongpassword();

            user.setId(id);
            user.setUser(username);
            user.setPassword(password);

            // SAVE

            session.persist(user);
            transaction.commit();

            System.out.println("User saved successfully: "+ username);

            // RETURN ID TO HTML

            String response =
                    "{"
                    + "\"id\":" + id + ","
                    + "\"username\":\""
                    + escapeJson(username)
                    + "\""
                    + "}";

            exchange.getResponseHeaders().set( "Content-Type", "application/json; charset=UTF-8");
            byte[] bytes =response.getBytes( StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length );

            try (OutputStream output =exchange.getResponseBody()) {

                output.write(bytes);
            }

        } catch (Exception e) {

            if (transaction != null) {

                transaction.rollback();
            }
            e.printStackTrace();
           sendResponse(  exchange, "Database error: "+ e.getMessage(),500);

        } finally {

            if (session != null) {

                session.close();
            }
        }
    }

    // ==================================================
    // GET ALL USERS FROM MYSQL
    // ==================================================

    private static void getUsers(HttpExchange exchange)throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {

           sendResponse(  exchange, "Only GET method allowed",405);
            return;
        }

        Session session = null;
        try {
            session =sessionFactory.openSession();
            List<Strongpassword> users = session.createQuery("from Strongpassword " + "order by id",Strongpassword.class).getResultList();
            StringBuilder json =new StringBuilder();
            json.append("[");

            for (int i = 0;i < users.size();i++) {
                Strongpassword user =users.get(i);
                json.append("{");
                json.append( "\"id\":"+ user.getId());
                json.append(",");
                json.append("\"username\":\""  + escapeJson(user.getUser())+ "\"");

                json.append(",");

                json.append("\"password\":\""+ escapeJson(user.getPassword())+ "\"");

                json.append("}");

                if (i < users.size() - 1) {

                    json.append(",");
                }
            }

            json.append("]");

            exchange.getResponseHeaders().set( "Content-Type","application/json; charset=UTF-8");

            byte[] bytes =json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);
            try (OutputStream output =exchange.getResponseBody()) {
                output.write(bytes);
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(exchange,"Database error: "+ e.getMessage(),500 );

        } finally {

            if (session != null) {
                session.close();
            }
        }
    }

    // ==================================================
    // PARSE FORM DATA
    // ==================================================

    private static Map<String, String> parseData(String body) {

        Map<String, String> map =new HashMap<>();

        String[] pairs = body.split("&");

        for (String pair : pairs) {

            String[] keyValue =pair.split("=", 2);

            if (keyValue.length == 2) {

                String key =URLDecoder.decode(keyValue[0],StandardCharsets.UTF_8);

                String value =URLDecoder.decode(keyValue[1],StandardCharsets.UTF_8);

                map.put(key,value);
            }
        }
        return map;
    }

    // ==================================================
    // JSON ESCAPE
    // ==================================================

    private static String escapeJson(String text) {

        if (text == null) {
            return "";
        }
        return text.replace( "\\","\\\\").replace("\"","\\\"");
    }

    // ==================================================
    // SEND RESPONSE
    // ==================================================

    private static void sendResponse(HttpExchange exchange,String response,int statusCode)throws IOException {

        exchange.getResponseHeaders().set("Content-Type","text/plain; charset=UTF-8");

        byte[] bytes =response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode,bytes.length);

        try (OutputStream output =exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}