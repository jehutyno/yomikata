package com.jehutyno.yomikata.util;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by valentin on 01/12/2016.
 */

public class CopyUtils {

    static public void restoreEncryptedBdd(File file, String toPath) throws Exception {
        byte[] filesBytes = fileToByteArray(file);
        byte[] decodedData = decodeFile(generateKey("jaimemangerdesgrossespatat&s!!!!!!!!!trololo"), filesBytes);
        OutputStream myOutput = new FileOutputStream(toPath);
        // transfer bytes from the inputfile to the outputfile
        myOutput.write(decodedData);
        // Close the streams
        myOutput.flush();
        myOutput.close();

    }

    private static byte[] fileToByteArray(File file) throws Exception {
        FileInputStream fin = null;
        byte fileContent[] = new byte[(int) file.length()];
        try {
            // create FileInputStream object
            fin = new FileInputStream(file);
            // Reads up to certain bytes of data from this input stream into an array of bytes.
            fin.read(fileContent);
            //create string from byte array
            String s = new String(fileContent);
//            System.out.println("File content: " + s);
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
            throw e;
        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
            throw ioe;
        } finally {
            // close the streams using close method
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
        return fileContent;
    }

    @SuppressLint("SdCardPath")
    static public int count(String filename) throws IOException {
        String outFileName = "/data/data/com.yomikata/" + filename;
        //Open the empty db as the output stream
        InputStream is = new FileInputStream(outFileName);
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n')
                        ++count;
                }
            }
            return count;
        } finally {
            is.close();
        }
    }

    public static SecretKeySpec generateKey(String password) throws Exception {
        byte[] keyStart = password.getBytes("UTF-8");
        return new SecretKeySpec(InsecureSHA1PRNGKeyDerivator.deriveInsecureKey(keyStart, 16), "AES");
    }

    public static byte[] encodeFile(SecretKeySpec skeySpec, byte[] fileData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

        byte[] encrypted = cipher.doFinal(fileData);

        return encrypted;
    }

    public static byte[] decodeFile(SecretKeySpec skeySpec, byte[] fileData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);

        byte[] decrypted = cipher.doFinal(fileData);

        return decrypted;
    }

}
