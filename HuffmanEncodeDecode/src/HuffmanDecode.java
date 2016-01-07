import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Decodes a file created with HuffmanEncode.java class.
 *
 * @author Jesse Bernoudy
 * @version 12/08/2015
 */
public class HuffmanDecode {

    private String fileName;
    private long fileLength;
    private int numberOfSymbols;
    private HashMap<String, Integer> encodedBytes;
    private List<Integer> fileBody;

    /**
     * Class constructor. Reads the File Header Info from the file (encodedFileName)
     * and stores that information appropriately.
     *
     * @param encodedFileName the file to decode
     */
    public HuffmanDecode(String encodedFileName) {
        fileName = encodedFileName;
        List<Integer> fileBytes = extractBytes(fileName);
        int currentIndex = readFileHeader(fileBytes, 0);
        currentIndex = readSymbolCodes(fileBytes, currentIndex);
        fileBody = new ArrayList<>(fileBytes.subList(currentIndex, fileBytes.size()));
    }

    /**
     * Reads the encoded file bits and writes out a decoded file
     *
     * @param decodedFileName The name of the file to write the decoded bits too.
     */
    public void readFromFile(String decodedFileName) {

        // Decode the file body
        List<Integer> decodedFileBytes = readFileBody(fileBody);

        try {
            // Create a file to write to.
            File decodedFile = new File(decodedFileName);
            FileChannel channel;
            byte[] decodedFileByte = new byte[decodedFileBytes.size()];
            for (int i = 0; i < decodedFileByte.length; i++) {
                decodedFileByte[i] = decodedFileBytes.get(i).byteValue();
            }

            channel = new FileOutputStream(decodedFile).getChannel();
            // Write it out using ByteBuffer
            channel.write(ByteBuffer.wrap(decodedFileByte));
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Reads a file and return a List<Integers> it contains (treating them as bytes)
     */
    private List<Integer> extractBytes(String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            ArrayList<Integer> bytes = new ArrayList<>();
            int nextByte = fileInputStream.read();
            while (nextByte != -1) {
                bytes.add(nextByte);
                nextByte = fileInputStream.read();
            }
            return bytes;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Reads the data from the file header portion of the encoded file
     */
    private int readFileHeader(List<Integer> fileBytes, int currentIndex) {
        // Grab the bytes that represent the long fileLength value
        byte[] fileLengthBytes = new byte[Long.BYTES];
        for (int i = 0; i < fileLengthBytes.length; i++) {
            fileLengthBytes[i] = fileBytes.get(currentIndex++).byteValue();
        }
        // Grab the bytes that represent the int numberOfSybmols value
        byte[] numberOfSymbolsBytes = new byte[Integer.BYTES];
        for (int i = 0; i < numberOfSymbolsBytes.length; i++) {
            numberOfSymbolsBytes[i] = fileBytes.get(currentIndex++).byteValue();
        }

        // Convert fileLengthBytes array to long
        fileLength = bytesToLong(fileLengthBytes);
        // Convert numberOfSymbolBytes array to int
        numberOfSymbols = bytesToInt(numberOfSymbolsBytes);

        // Increment the index to be at the start of the symbols
        return currentIndex;
    }

    /*
     * Reads in the huffman codes and stores them in a HashMap
     */
    private int readSymbolCodes(List<Integer> fileBytes, int currentIndex) {
        encodedBytes = new HashMap<>();
        for (int i = 0; i < numberOfSymbols; i++) {
            // Get the symbol, stored as just one byte
            byte symbolValue = fileBytes.get(currentIndex++).byteValue();
            // Get the lengh of the code, just one byte
            byte codeLength = fileBytes.get(currentIndex++).byteValue();
            // Get enough bytes to store the code
            byte[] codeBytes = new byte[(int) Math.ceil(codeLength / 8.0f)];
            for (int j = 0; j < codeBytes.length; j++) {
                codeBytes[j] = fileBytes.get(currentIndex++).byteValue();
            }
            // Convert into a string
            String code = readBytesToCode(codeBytes, codeLength);
            // Store in HashMap
            encodedBytes.put(code, new Integer(symbolValue));
        }
        return currentIndex;
    }

    /*
     * Converts an array of bytes into the HuffmanCode stored within
     */
    private String readBytesToCode(byte[] bytes, int codeLength) {
        StringBuilder stringBuilder = new StringBuilder();
        // Start by creating a binary string representation of all the bytes
        for (int i = 0; i < bytes.length; i++) {
            stringBuilder.append(bytesToBinaryString(bytes[i]));
        }
        // Only grab the number of 'bits' the codeLength dictates.
        return stringBuilder.substring(0, codeLength);
    }

    /*
     * Reads the encoded bytes and converts back to the original file body contents
     */
    private List<Integer> readFileBody(List<Integer> fileBytes) {
        // Turn the body into a binary string, i.e. 11000110011100011000111...
        List<Integer> returnList = new ArrayList<>();
        StringBuilder fileBody = new StringBuilder();
        for (int i = 0; i < fileBytes.size(); i++) {
            fileBody.append(bytesToBinaryString(fileBytes.get(i).byteValue()));
        }

        // Break into single chars
        char[] chars = fileBody.toString().toCharArray();
        StringBuilder symbolCode = new StringBuilder();
        // Build up a code and check at each step to see if it matches a stored code
        for (char c : chars) {
            symbolCode.append(c);
            if (encodedBytes.containsKey(symbolCode.toString())) {
                // Found a match, get the symbol and put it in the list
                returnList.add(encodedBytes.get(symbolCode.toString()));
                // Clear the code and start fresh
                symbolCode = new StringBuilder();
                // If we found all the characters the original file had, we are done.
                if (returnList.size() == fileLength) {
                    break;
                }
            }
        }
        // Return the list which now holds the original file contents.
        return returnList;
    }

    /*
     * Convert a MSB ordered byte array to an int
     */
    private int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    /*
     * Convert a MSB ordered byte array to long
     */
    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    /*
     * Convert a byte array into a binary string representation
     */
    private String bytesToBinaryString(byte b) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 7; i >= 0; --i) {
            stringBuilder.append(b >>> i & 1);
        }
        return stringBuilder.toString();
    }
}
