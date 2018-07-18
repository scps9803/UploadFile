package com.example.uploadfile;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private TextView messageText;
    private Button uploadButton, btnselectpic;
    private ImageView imageview;
    private int serverResponseCode = 0;
    private ProgressDialog dialog = null;

    private String upLoadServerUri = null;
    private String imagepath=null;

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uploadButton = (Button)findViewById(R.id.uploadButton);
        messageText  = (TextView)findViewById(R.id.messageText);
        btnselectpic = (Button)findViewById(R.id.button_selectpic);
        imageview = (ImageView)findViewById(R.id.imageView_pic);

        btnselectpic.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        upLoadServerUri = "http://192.168.42.113/php/UploadToServer.php";
//        upLoadServerUri = "http://140.117.71.66:8000/app/upload";
    }


    @Override
    public void onClick(View arg0) {
        if(arg0==btnselectpic)
        {
            Intent intent = new Intent();
            intent.setType("image/*");
//            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(intent, "Complete action using"), 1);
        }
        else if (arg0==uploadButton) {

            dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
            messageText.setText("uploading started.....");
            new Thread(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= 23) {
                        int hasPermissions = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (hasPermissions != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                            return;
                        }
                    }
                    uploadFile(imagepath);

                }
            }).start();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1 && resultCode == RESULT_OK) {
            //Bitmap photo = (Bitmap) data.getData().getPath();

            Uri selectedImageUri = data.getData();
            imagepath = getPath(selectedImageUri);
            Bitmap bitmap=BitmapFactory.decodeFile(imagepath);
            imageview.setImageBitmap(bitmap);
            messageText.setText("Uploading file path:" +imagepath);

        }
    }
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public int uploadFile(String sourceFileUri) {


        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"+imagepath);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"+ imagepath);
                }
            });

            return 0;

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("image_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    runOnUiThread(new Runnable() {
                        public void run() {
                            String msg = "File Upload Completed.\n\n See uploaded file your server. \n\n";
                            messageText.setText(msg);
                            Toast.makeText(MainActivity.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    messageText.setText("Error");
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file Exception", "Exception : "  + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }


}
