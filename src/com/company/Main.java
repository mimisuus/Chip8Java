package com.company;

import java.io.IOException;

public class Main {
    public static void main (String[] args)throws IOException {
        try {
            Chip8 chip = new Chip8();
        } catch (Exception e){
            System.out.print(e);
        }
    }


}
