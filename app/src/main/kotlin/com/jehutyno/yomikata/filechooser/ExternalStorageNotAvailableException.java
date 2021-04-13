package com.jehutyno.yomikata.filechooser;

public class ExternalStorageNotAvailableException extends Exception {
    public ExternalStorageNotAvailableException() {
        super("There is no external storage available on this device.");
    }
}