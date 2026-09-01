package com.quimia.quimia;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class Conexoes{
    private static final Dotenv envReader = Dotenv.load();
    private final static String url = envReader.get("url");
    private final static String username = envReader.get("username");
    private final static String password = envReader.get("password");
    private final static BlockingQueue<Connection>  poolDeConexoes = new ArrayBlockingQueue<>(20);

    public static void fillPool() throws SQLException{
        Connection conn = DriverManager.getConnection(url,username,password);
        while(poolDeConexoes.offer(conn)){
            conn = DriverManager.getConnection(url,username,password);
        }
        conn.close();
    }

    public static Connection getConexao() throws SQLException{
        Connection conn = poolDeConexoes.poll();
        if(conn.isValid(2)){
            return conn;
        } else {
            return DriverManager.getConnection(url,username,password);
        }
    }

    public static void devolverConexao(Connection conexao) {
        poolDeConexoes.add(conexao);
    }

    private Conexoes(){
        throw new IllegalStateException("Isso é uma classe utilitária, não deve ser instanciada");
    }
}