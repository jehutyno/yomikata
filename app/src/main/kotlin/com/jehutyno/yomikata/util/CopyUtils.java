package com.jehutyno.yomikata.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;

import com.jehutyno.yomikata.R;
import com.jehutyno.yomikata.repository.local.SQLiteHelper;

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


    static public void reinitDataBase(Context context) throws IOException {
        // Open your local db as the input stream
        InputStream myInput = context.getAssets().open(SQLiteHelper.Companion.getDATABASE_NAME());
        // Path to the just created empty db
        String outFileName = context.getString(R.string.db_path) + SQLiteHelper.Companion.getDATABASE_NAME();
        // Open the empty db as the output stream
        FileOutputStream myOutput = new FileOutputStream(outFileName);
        // transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        // Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    static public void copyEncryptedBddToSd(Activity activity) {
        // Check if we have write permission
        String db_name = SQLiteHelper.Companion.getDATABASE_NAME();
        String db_path = activity.getString(R.string.db_path);
        File input = new File(db_path + db_name);
        try {
            SecretKeySpec yourKey = generateKey("jaimemangerdesgrossespatat&s!!!!!!!!!trololo");
            byte[] filesBytes = encodeFile(yourKey, fileToByteArray(input));
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/Yomikata/");
            dir.mkdirs();
            File bitch = new File(dir + "/" + "backup.yomikataz");
            bitch.createNewFile();
            OutputStream myOutput;
            myOutput = new FileOutputStream(bitch);
            myOutput.write(filesBytes);
            myOutput.flush();
            myOutput.close();
            new AlertDialog.Builder(activity).setTitle(R.string.backup_success)
                .setMessage(activity.getString(R.string.backup_saved_here, bitch.getAbsolutePath()))
                .setPositiveButton(R.string.ok, null)
                .show();
        } catch (Exception e) {
            new AlertDialog.Builder(activity).setTitle(R.string.backup_error)
                .setMessage(R.string.backup_error_message)
                .setPositiveButton(R.string.ok, null)
                .show();
            e.printStackTrace();
        }
    }


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
