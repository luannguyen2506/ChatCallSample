package com.stringee.kit.ui.commons.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;

import com.stringee.kit.ui.model.StringeeFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.PatternSyntaxException;

public class FileUtils {

    private static final String MAIN_FOLDER_META_DATA = "main_folder_name";
    public static final String STRINGEE_IMAGES_FOLDER = "/image";
    public static final String STRINGEE_VIDEOS_FOLDER = "/video";
    public static final String STRINGEE_CONTACT_FOLDER = "/contact";
    public static final String STRINGEE_OTHER_FILES_FOLDER = "/other";
    public static final String STRINGEE_THUMBNAIL_SUFIX = "/.Thumbnail";
    public static final String IMAGE_DIR = "image";
    private static final String TAG = "FileUtils";
    public static final String BEGIN_VCARD = "BEGIN:VCARD";
    public static final String END_VCARD = "END:VCARD";

    public static File getFilePath(String fileName, Context context, String contentType) {
        return getFilePath(fileName, context, contentType, false);
    }

    public static File getFilePath(String fileName, Context context, String contentType, boolean isThumbnail) {
        File filePath;
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_OTHER_FILES_FOLDER;

            if (contentType.startsWith("image")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_IMAGES_FOLDER;
            } else if (contentType.startsWith("video")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_VIDEOS_FOLDER;
            } else if (contentType.equalsIgnoreCase("text/x-vCard")) {
                folder = "/" + Utils.getMetaDataValue(context, MAIN_FOLDER_META_DATA) + STRINGEE_CONTACT_FOLDER;
            }
            if (isThumbnail) {
                folder = folder + STRINGEE_THUMBNAIL_SUFIX;
            }
            dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + folder);
            Log.e("Stringee", "Dir path: " + dir.getPath());
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } else {
            ContextWrapper cw = new ContextWrapper(context);
            // path to /data/data/yourapp/app_data/imageDir
            dir = cw.getDir(IMAGE_DIR, Context.MODE_PRIVATE);
        }
        // Create image name
        //String extention = "." + contentType.substring(contentType.indexOf("/") + 1);
        filePath = new File(dir, fileName);
        return filePath;
    }

    public Bitmap loadMessageImage(Context context, String url) {
        try {
            Bitmap attachedImage = null;

            if (attachedImage == null) {
                InputStream in = new java.net.URL(url).openStream();
                if (in != null) {
                    attachedImage = BitmapFactory.decodeStream(in);
                }
            }
            return attachedImage;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            Log.e(TAG, "File not found on server: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Exception fetching file from server: " + ex.getMessage());
        }

        return null;
    }

    /**
     * @param contactData
     * @return
     */
    public static String vCard(Uri contactData, Context context) throws Exception {
        Cursor cursor = context.getContentResolver().query(contactData, null, null, null, null);
        cursor.moveToFirst();
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);

//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "CONTACT_" + timeStamp + "_" + ".vcf";
//
//        File outputFile = FileUtils.getFilePath(imageFileName, context.getApplicationContext(), "text/x-vcard");
        BufferedReader br = null;
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            if (br != null) {
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        byte[] buf = sb.toString().trim().getBytes();
//        if (!validateData(sb.toString())) {
//            throw new Exception("Contact exported is not in proper format.");
//        }
//        FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsoluteFile());
//        fileOutputStream.write(buf);
//        fileOutputStream.close();
        return sb.toString().trim();
    }

//    private static boolean validateData(String data) {
//        return (data != null && data.replaceAll("[\n\r]", "").trim().startsWith(BEGIN_VCARD) && data.replaceAll("[\n\r]", "").trim().endsWith(END_VCARD));
//    }

    public static String getStorage(Context context) {
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else
            return null;
    }

    public static ArrayList<StringeeFile> getFiles(String folder, String fileNamePattern, int sort)
            throws PatternSyntaxException {
        ArrayList<StringeeFile> dics = new ArrayList<StringeeFile>();
        ArrayList<StringeeFile> files = new ArrayList<StringeeFile>();
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory())
            return null;
        String[] subfiles = file.list();
        if (subfiles.length == 0)
            return null;
        for (int i = 0; i < subfiles.length; i++) {
            if (fileNamePattern == null || subfiles[i].matches(fileNamePattern)) {
                StringeeFile StringeeFile = new StringeeFile();
                String filePath = folder + "/" + subfiles[i];
                StringeeFile.setName(subfiles[i]);
                StringeeFile.setPath(filePath);
                // check type file
                StringeeFile.setType(checkTypeFile(filePath));
                if (StringeeFile.getType() == StringeeFile.TYPE_OTHER_FILE || StringeeFile.getType() == StringeeFile.TYPE_DOCUMENT
                        || StringeeFile.getType() == StringeeFile.TYPE_IMAGE || StringeeFile.getType() == StringeeFile.TYPE_MEDIA
                        || StringeeFile.getType() == StringeeFile.TYPE_VIDEO || StringeeFile.getType() == StringeeFile.TYPE_ZIP) {
                    File f = new File(filePath);
                    int length = (int) f.length() / 1024;
                    StringeeFile.setSize(length);
                    if (!StringeeFile.getName().substring(0, 1).equals("."))
                        files.add(StringeeFile);
                } else {
                    if (!StringeeFile.getName().substring(0, 1).equals("."))
                        dics.add(StringeeFile);
                }
            }
        }
        if (files.size() == 0 && dics.size() == 0)
            return null;
        if (sort == 1) {
            Collections.sort(dics, new Comparator<StringeeFile>() {

                @Override
                public int compare(StringeeFile lhs, StringeeFile rhs) {
                    String name1 = lhs.getName();
                    if (name1 != null && name1.trim().length() > 0) {
                        name1 = name1.trim();
                    } else {
                        name1 = "";
                    }
                    name1 = name1.toLowerCase();

                    String name2 = rhs.getName();
                    if (name2 != null && name2.trim().length() > 0) {
                        name2 = name2.trim();
                    } else {
                        name2 = "";
                    }
                    name2 = name2.toLowerCase();
                    return name1.compareTo(name2);
                }
            });
            Collections.sort(files, new Comparator<StringeeFile>() {

                @Override
                public int compare(StringeeFile lhs, StringeeFile rhs) {
                    String name1 = lhs.getName();
                    if (name1 != null && name1.trim().length() > 0) {
                        name1 = name1.trim();
                    } else {
                        name1 = "";
                    }
                    name1 = name1.toLowerCase();

                    String name2 = rhs.getName();
                    if (name2 != null && name2.trim().length() > 0) {
                        name2 = name2.trim();
                    } else {
                        name2 = "";
                    }
                    name2 = name2.toLowerCase();
                    return name1.compareTo(name2);
                }
            });
        }
        dics.addAll(files);
        return dics;
    }

    public static int checkTypeFile(String path) {
        if (isDirectory(path)) {
            return StringeeFile.TYPE_DIRECTORY;
        } else if (isImage(path)) {
            return StringeeFile.TYPE_IMAGE;
        }
        return StringeeFile.TYPE_OTHER_FILE;
    }

    public static boolean isDirectory(String path) {
        if (new File(path).isDirectory()) {
            return true;
        } else if (new File(path).isFile()) {
            return false;
        }
        return false;
    }

    public static boolean isImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options.outWidth != -1 && options.outHeight != -1;
    }
}
