/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.*;

/**
 *
 * @author Julien Feis
 */
public class HuffmanTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        HuffmanEncode huffy = new HuffmanEncode("foxtext.txt");
        System.out.println("Encoding File --");
        huffy.encodeByteStream();
        huffy.writeToFile("foxtext.huf");

        System.out.println("Decoding File --");
        HuffmanDecode huffD = new HuffmanDecode("foxtext.huf");
        huffD.readFromFile("foxtext_decoded.txt");
    }
}
