import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * A class that reads in a file and creates a Huffman Tree used to encode the file.
 *
 * @author Jesse Bernoudy
 * @version 12/08/2015
 */
public class HuffmanEncode {

    private File inputFile;
    private String fileName;
    private long fileLength;
    private int numberOfSymbols;
    private List<Integer> fileBytes;
    private HashMap<Integer, String> huffmanCodes;

    /**
     * Constructs a new HuffmanEncode class
     * @param input File to encode
     */
    public HuffmanEncode(File input) {
        inputFile = input;
    }

    /**
     * Overloaded constructor takes a String fileName which it will read and encode
     * @param fileName path to the file to read and encode
     */
    public HuffmanEncode (String fileName){
        this.fileName = fileName;
    }

    /**
     * Reads the File Object passed into the constructor and counts the occurrence of every character
     *     (including special characters like newline and blanks).
     * Fills the Priority Queue with the nodes that you create out of the characters and occurrences.
     * Builds the Huffman Tree
     * Traverses the Huffman Tree
     */
    public void encode() {
        // Read in the file
        List<Character> chars = extractChars(inputFile);
        // Count occurrences
        HashMap<Character, Integer> occurrences = parseChars(chars);
        // Fill priority queue
        PriorityQueue<CharNode> charNodes = buildQueue(occurrences);
        // Build Huffman Tree
        buildTree(charNodes);
        // Traverse Huffman Tree
        traverseTree(charNodes.peek(), "", HuffmanEncode::printEncodedChar);
    }

    /**
     * Reads the File Object passed into the constructor and counts the occurrence of every symbol
     */
    public void encodeByteStream(){
        // Read in the file
        extractBytes(fileName);
        fileLength = fileBytes.size();
        // Count occurrences
        HashMap<Integer, Integer> occurrences = parseBytes(fileBytes);
        numberOfSymbols = occurrences.size();
        // Fill priority queue
        PriorityQueue<ByteNode> byteNodes = buildByteQueue(occurrences);
        // Build Huffman Tree
        buildByteTree(byteNodes);
        // Traverse Huffman Tree and store the generated codes
        huffmanCodes = new HashMap<>();
        traverseByteTree(byteNodes.peek(), "");
    }

    /**
     * Writes out an encoded version of the file
     * @param fileName Path to the encoded file to write
     */
    public void writeToFile(String fileName){
        // Create a new file object and open it for writing
        File file = new File(fileName);
        FileChannel channel = null;
        try {
            channel = new FileOutputStream(file).getChannel();
            writeFileHeader(channel);
            writeSymbolCodes(channel);
            writeContentsToFile(channel);
            channel.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * Read the file into a List of Bytes(actually int due to limitations of byte)
    */
    private void extractBytes(String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            fileBytes = new ArrayList<>();
            int nextByte = fileInputStream.read();
            while (nextByte != -1) {
                fileBytes.add(nextByte);
                nextByte = fileInputStream.read();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Read the file into a List of Characters
     */
    private List<Character> extractChars(File inputFile) {
        // Try to read the contents of the file into a char array
        try {
            FileReader fileReader = new FileReader(inputFile);
            char[] chars = new char[(int)inputFile.length()];
            // This method gets all of the characters including '\r'.
            fileReader.read(chars);
            fileReader.close();
            List<Character> charList = new ArrayList<Character>();
            // Put them into a List
            for(char c : chars) {
                charList.add(c);
            }
            return charList;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Counts number of occurrences of each byte in the file and stores them
     * in a HashMap for easy retrieval
     */
    private HashMap<Integer, Integer> parseBytes(List<Integer> bytes) {
        HashMap<Integer, Integer> byteCount = new HashMap<>();
        for (int i : bytes) {
            // Add each char the first time you come across it.
            if (!byteCount.containsKey(i)) {
                int occurrences = Collections.frequency(bytes, i);
                byteCount.put(i, occurrences);
            }
        }
        return byteCount;
    }

    /*
     * Counts number of occurrences of each character in the file and stores them
     * in a HashMap for easy retrieval
     */
    private HashMap<Character, Integer> parseChars(List<Character> chars) {
        HashMap<Character, Integer> charCount = new HashMap<>();
        for (char c : chars) {
            // Add each char the first time you come across it.
            if (!charCount.containsKey(c)) {
                int occurrences = Collections.frequency(chars, c);
                charCount.put(c, occurrences);
            }
        }
        return charCount;
    }

    /**
     * Build CharNode for each entry in the HashMap and put in a PriorityQueue
     */
    private PriorityQueue<CharNode> buildQueue(HashMap<Character, Integer> parsedChars) {
        PriorityQueue<CharNode> pq = new PriorityQueue<>();
        for (Character key : parsedChars.keySet()) {
            CharNode newNode = new CharNode(key, parsedChars.get(key), null, null);
            pq.offer(newNode);
        }
        return pq;
    }

    /**
     * Build ByteNode for each entry in the HashMap and put in a PriorityQueue
     */
    private PriorityQueue<ByteNode> buildByteQueue(HashMap<Integer, Integer> parsedBytes) {
        PriorityQueue<ByteNode> pq = new PriorityQueue<>();
        for (Integer key : parsedBytes.keySet()) {
            ByteNode newNode = new ByteNode(key, parsedBytes.get(key), null, null);
            pq.offer(newNode);
        }
        return pq;
    }

    /*
     * Build tree using Huffman Encoding algorithm
     */
    private void buildTree(PriorityQueue<CharNode> charNodes) {
        // Continue until there is only one item in the PQ, that item is the
        // root of the tree.
        while (charNodes.size() > 1) {
            // Take the two items with the lowest occurrences
            CharNode left = charNodes.poll();
            CharNode right = charNodes.poll();
            // Combine to a new node with no symbol and the combined weight of the children
            CharNode parent = new CharNode(null, left.weight + right.weight, left, right);
            // Put the new node in the tree
            charNodes.offer(parent);
        }
    }

    /*
     * Build byte tree using Huffman Encoding algorithm
     */
    private void buildByteTree(PriorityQueue<ByteNode> charNodes) {
        // Continue until there is only one item in the PQ, that item is the
        // root of the tree.
        while (charNodes.size() > 1) {
            // Take the two items with the lowest occurrences
            ByteNode left = charNodes.poll();
            ByteNode right = charNodes.poll();
            // Combine to a new node with no symbol and the combined weight of the children
            ByteNode parent = new ByteNode(null, left.weight + right.weight, left, right);
            // Put the new node in the tree
            charNodes.offer(parent);
        }
    }

    /*
     * Traverse the tree and do some action at leaf nodes.  Encode the nodes as you go
     */
    private void traverseTree(CharNode root, String code, BiConsumer<CharNode, String> action) {
        if(root != null) {
            // A node with a symbol is a leaf
            if(root.symbol != null) {
                action.accept(root, code);
            } else {
                // Go down the left and right trees adding to the code
                // Left adds a 0
                traverseTree(root.leftChild, code + "0", action);
                // Right adds a 1
                traverseTree(root.rightChild, code + "1", action);
            }
        }
    }

    /*
     * Traverse the byte tree and do some action at leaf nodes.  Encode the nodes as you go
     */
    private void traverseByteTree(ByteNode root, String code) {
        if(root != null) {
            // A node with a symbol is a leaf
            if(root.symbol != null) {
                // Store the code for use in writting the encoded file
                huffmanCodes.put(root.symbol, code);
            } else {
                // Go down the left and right trees adding to the code
                // Left adds a 0
                traverseByteTree(root.leftChild, code + "0");
                // Right adds a 1
                traverseByteTree(root.rightChild, code + "1");
            }
        }
    }

    /*
     * Write out the length of the original file, and the number of sybols that are encoded
     */
    private void writeFileHeader(FileChannel channel){
        try {
            byte[] fileLengthBytes = longToBytes(fileLength);
            channel.write(ByteBuffer.wrap(fileLengthBytes));

            byte[] numberOfSymbolsBytes = intToBytes(numberOfSymbols);
            channel.write(ByteBuffer.wrap(numberOfSymbolsBytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Write out the symbols
     */
    private void writeSymbolCodes(FileChannel channel) {
        try {
            for (Integer key : huffmanCodes.keySet()) {
                // Write symbol
                byte symbol = key.byteValue();
                channel.write(ByteBuffer.wrap(new byte[]{symbol}));
                // Write out the code length
                String code = huffmanCodes.get(key);
                byte symbolLength = (byte)code.length();
                // Write the code, need to align them with byte boundaries.
                channel.write(ByteBuffer.wrap(new byte[]{symbolLength}));
                byte[] codeBytes = binaryStringToBytes(code);
                channel.write(ByteBuffer.wrap(codeBytes));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Write out the bytes from the original file using the huffman code symbols.
     */
    private void writeContentsToFile(FileChannel channel) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            // Pack in all the codes with no gaps
            for (int b : fileBytes) {
                String code = huffmanCodes.get(b);
                stringBuilder.append(code);
            }
            byte[] encodedBytes = binaryStringToBytes(stringBuilder.toString());
            channel.write(ByteBuffer.wrap(encodedBytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Print the Huffman code for this node.
     */
    private static void printEncodedChar(CharNode node, String code){
        // 'Character'<blank>Code<blank>Occurrence.
        System.out.println("\'" + node.symbol + "\'" + " " + code + " " + node.weight);
    }

    /*
     * Convert a string representation of a binary value into as many bytes as needed
     */
    private byte[] binaryStringToBytes(String value){
        // Calculate number of bytes needed
        byte[] bytes = new byte[(int) Math.ceil(value.length()/8.0f)];
        // Split string into char array
        char[] codeChars = value.toCharArray();
        int bitIndex = 0;
        int byteIndex = 0;
        int bitOffset = 7;
        for(char c:codeChars){
            // flip bits from left to right if char is '1'
            int n = bitOffset - bitIndex;
            if(c == '1') {
                bytes[byteIndex] = (byte) (bytes[byteIndex] ^ (1 << n));
            }
            bitIndex++;
            if(bitIndex > 7){
                // Move to the next bit
                byteIndex++;
                bitIndex = 0;
            }
        }
        // Extra space will be filled with 0's
        return bytes;
    }

    /*
     * Converts int value into a MSB ordered byte array
     */
    private byte[] intToBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        return buffer.array();
    }

    /*
     * Converts long value into a MSB ordered byte array
     */
    private byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    /*
     * Internal class to use for creating the huffman tree
     */
    private class CharNode implements Comparable {
        private Character symbol; // char to be coded, null if combined node
        private CharNode leftChild;
        private CharNode rightChild;
        private Integer weight; // occurrence # of this char in the text

        CharNode(Character symbol, Integer weight, CharNode left, CharNode right) {
            this.symbol = symbol;
            this.weight = weight;
            this.leftChild = left;
            this.rightChild = right;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof CharNode) {
                CharNode other = (CharNode) o;
                // Order by weight
                return weight - other.weight;
            }
            throw new ClassCastException("Can't compare to a non CharNode");
        }
    }

    /*
     * Internal class to use for creating the huffman tree
     */
    private class ByteNode implements Comparable {
        private Integer symbol; // char to be coded, null if combined node
        private ByteNode leftChild;
        private ByteNode rightChild;
        private Integer weight; // occurrence # of this 'byte' in the text

        ByteNode(Integer symbol, Integer weight, ByteNode left, ByteNode right) {
            this.symbol = symbol;
            this.weight = weight;
            this.leftChild = left;
            this.rightChild = right;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof ByteNode) {
                ByteNode other = (ByteNode) o;
                // Order by weight
                return weight - other.weight;
            }
            throw new ClassCastException("Can't compare to a non ByteNode");
        }
    }
}
