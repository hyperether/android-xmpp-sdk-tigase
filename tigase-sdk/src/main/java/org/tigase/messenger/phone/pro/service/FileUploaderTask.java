package org.tigase.messenger.phone.pro.service;

public abstract class FileUploaderTask {

    public static String guessMimeType(String filename) {
        int idx = filename.lastIndexOf(".");
        if (idx == -1) {
            return "application/octet-stream";
        }
        final String suffix = filename.substring(idx + 1).toLowerCase();
        switch (suffix) {
            case "gif":
                return "image/gif";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "avi":
                return "video/avi";
            case "mkv":
                return "video/x-matroska";
            case "mpg":
            case "mp4":
                return "video/mpeg";
            case "mp3":
                return "audio/mpeg3";
            case "ogg":
                return "audio/ogg";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }
}
