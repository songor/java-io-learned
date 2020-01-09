package com.imooc;

import java.io.IOException;

public class Client_B {

    public static void main(String[] args) throws IOException {
        new NioClient().start("Client_B");
    }

}
