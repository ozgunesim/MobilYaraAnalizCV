package com.example.fatihkorum.proje_f_k;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


import java.security.Policy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.opencv.imgproc.Imgproc.arrowedLine;
import static org.opencv.imgproc.Imgproc.putText;


class FilterInfo{
    public int minR = 0;
    public int maxR = 255;
    public int minG = 0;
    public int maxG = 255;
    public int minB = 0;
    public int maxB = 255;
    public Mat mat;
}


class CustomCamera extends JavaCameraView{

    public CustomCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedFlashModes();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getFlashMode() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getFlashMode();
    }

    public void setEffect(String effect) {
        if(mCamera != null) {
            mCamera.getParameters();
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(effect);
            mCamera.setParameters(params);
        }
    }

    public void cameraRelease() {
        if(mCamera != null){
            mCamera.release();
        }
    }
}


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private JavaCameraView cameraManager;

    TextView txtTimer, txtDateTime;
    Date startDate;
    Handler diffHandler = new Handler();

    private Mat current_frame;
    private Button btnCapture;
    private Button btnReset;
    private TextView txtReport;

    bgWorker wound_detector;
    ProgressDialog pDialog;
    ImageView imgResult;
    FilterInfo fi;
    double distance = 0.0;
    private static final double multiplier = 0.0693;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            cameraManager.enableView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        }

        cameraManager =  (JavaCameraView) findViewById(R.id.camera);
        //cameraManager.setMaxFrameSize(320,240);
        cameraManager.setVisibility(View.VISIBLE);
        cameraManager.setCvCameraViewListener(this);
        cameraManager.setCameraIndex(0);

        cameraManager.disableFpsMeter();

        btnCapture=(Button)findViewById(R.id.btnCapture);



        txtTimer = (TextView)findViewById(R.id.txtTimer);
        txtDateTime = (TextView)findViewById(R.id.txtDateTime);
        imgResult = (ImageView)findViewById(R.id.imgResult);
        btnReset = (Button)findViewById(R.id.btnReset);
        txtReport = (TextView)findViewById(R.id.txtReport);

        startDate = Calendar.getInstance().getTime();

        diffHandler = new Handler();
        diffHandler.postDelayed(updateTimerThread, 0);

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = getIntent();
                finish();
                startActivity(intent);*/
                cameraManager.enableView();
                cameraManager.setVisibility(View.VISIBLE);
                btnReset.setVisibility(View.GONE);
                btnCapture.setVisibility(View.VISIBLE);
                imgResult.setVisibility(View.GONE);
                txtReport.setVisibility(View.GONE);
                txtReport.setText("");
                txtTimer.setVisibility(View.VISIBLE);
                startDate = Calendar.getInstance().getTime();
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraManager.disableView();
                btnCapture.setVisibility(View.GONE);


                imgResult = (ImageView)findViewById(R.id.imgResult);
                imgResult.setImageBitmap(Mat2Bmp(current_frame));
                imgResult.setVisibility(View.VISIBLE);

                cameraManager.setVisibility(View.GONE);

                //DetectWound();

                Drawable imgDrawable = imgResult.getDrawable();

                Bitmap bitmap = ((BitmapDrawable) imgDrawable).getBitmap();


                int touchedRGB = bitmap.getPixel(bitmap.getWidth()/2, bitmap.getHeight()/2);

                //then do what you want with the pixel data, e.g
                int redValue = Color.red(touchedRGB);
                int greenValue = Color.green(touchedRGB);
                int blueValue = Color.blue(touchedRGB);
                Log.d("Bilgi","Secilen Renk: " + redValue + "," + greenValue + "," + blueValue);
                int sensivity = 50;

                fi = new  FilterInfo();
                fi.mat = current_frame;


                fi.maxR = redValue + sensivity*2;
                fi.minR = redValue - sensivity*2;
                fi.maxG = greenValue + sensivity;
                fi.minG = greenValue - sensivity;
                fi.maxB = blueValue + sensivity;
                fi.minB = blueValue - sensivity;

                if(fi.maxR > 255) fi.maxR = 255;
                if(fi.maxG > 255) fi.maxG = 255;
                if(fi.maxB > 255) fi.maxB = 255;
                if(fi.minR < 0) fi.minR = 0;
                if(fi.minG < 0) fi.minG = 0;
                if(fi.minB < 0) fi.minB = 0;

                DetectWound(fi);
            }
        });


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private Runnable updateTimerThread = new Runnable()
    {
        public void run()
        {
            Date currentDate = Calendar.getInstance().getTime();
            long diff = currentDate.getTime() - startDate.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            //long diffDays = diff / (24 * 60 * 60 * 1000);

            String dateDiff = String.valueOf(diffHours) + ":" + String.valueOf(diffMinutes) + ":" + String.valueOf(diffSeconds);

            txtTimer.setText(dateDiff);
            txtDateTime.setText(DateFormat.getDateTimeInstance().format(new Date()));
            diffHandler.postDelayed(this, 1000);
        }
    };


    private Bitmap Mat2Bmp(Mat m){
        Bitmap bmp = Bitmap.createBitmap(m.cols(), m.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        return bmp;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        current_frame=new Mat();

        Camera.Parameters params = cameraManager.mCamera.getParameters();
        params.setFlashMode(params.FLASH_MODE_TORCH);
        cameraManager.mCamera.setParameters(params);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat mRgba = inputFrame.rgba();

        mRgba.copyTo(current_frame);
        Imgproc.line(mRgba, new Point(mRgba.cols()/2-20, mRgba.rows()/2),new Point(mRgba.cols()/2+20, mRgba.rows()/2), new Scalar(0,0,255),5);
        Imgproc.line(mRgba, new Point(mRgba.cols()/2, mRgba.rows()/2-20),new Point(mRgba.cols()/2, mRgba.rows()/2+20), new Scalar(0,0,255),5);

        return mRgba; //inputFrame.rgba();


    }


    private void DetectWound(FilterInfo fi){
        wound_detector = new bgWorker();
        wound_detector.execute(fi);
    }

    private double CalculateDistance(Point p1, Point p2){
        return Math.sqrt((Math.pow((p2.x - p1.x),2) + (Math.pow((p2.y - p1.y),2))));
    }


    public class bgWorker extends AsyncTask<FilterInfo, Integer, Mat> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = ProgressDialog.show(MainActivity.this, "İşleniyor", "Lütfen Bekleyin...", true);
        }

        @Override
        protected Mat doInBackground(FilterInfo... params) {
            FilterInfo fi  = params[0];
            Mat mRgba = params[0].mat;

            float[] hsv1 = new float[3];
            float[] hsv2 = new float[3];
            Color.RGBToHSV(fi.minR, fi.minG, fi.minB, hsv1);
            Color.RGBToHSV(fi.maxR, fi.maxG, fi.maxB, hsv2);
            //Log.d("Bilgi",String.valueOf(hsv));

            if(hsv1[0] > hsv2[0]){
                float f = hsv1[0];
                hsv1[0] = hsv2[0];
                hsv2[0] = f;
            }

            if(hsv1[1] > hsv2[1]){
                float f = hsv1[1];
                hsv1[1] = hsv2[1];
                hsv2[1] = f;
            }

            if(hsv1[2] > hsv2[2]){
                float f = hsv1[2];
                hsv1[2] = hsv2[2];
                hsv2[2] = f;
            }

            Mat hsvImg = new Mat();
            Mat temp = new Mat();
            //Imgproc.blur(mRgba, mRgba, new Size(11, 11));
            Imgproc.cvtColor(mRgba, hsvImg, Imgproc.COLOR_RGB2HSV);

            int h1 = (int)(hsv1[0]/2);
            int s1 = (int)(hsv1[1]*255);
            int v1 = (int)(hsv1[2]*255);

            int h2 = (int)(hsv2[0]/2);
            int s2 = (int)(hsv2[1]*255);
            int v2 = (int)(hsv2[2]*255);



            Core.inRange(hsvImg,
                    new Scalar(h1,s1,v1),
                    new Scalar(h2,s2,v2),
                    temp
                );


            //parazit temizleme
            Imgproc.blur(temp, temp, new Size(11, 11));
            //Imgproc.cvtColor(mRgba, temp, Imgproc.COLOR_RGB2GRAY, 4);
            //Imgproc.Canny(temp, temp, 10, 200);

            Imgproc.erode(temp, temp, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(11, 11)));
            Imgproc.dilate(temp, temp, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(11, 11)));


            //Imgproc.threshold(temp,temp,1,255,Imgproc.THRESH_BINARY);

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierachy = new Mat();
            Imgproc.findContours(temp, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            /*Mat mOutput = new Mat();
            Imgproc.cvtColor(temp, mOutput, Imgproc.COLOR_GRAY2BGR);*/
            //Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGB);

            Mat temp2 = new Mat(mRgba.rows(), mRgba.cols(),CvType.CV_8UC1, new Scalar(0,0,0) );

            Imgproc.drawContours(temp2, contours, -1, new Scalar(255,0,0), 50);
            for(int i=0; i<contours.size(); i++){
                Imgproc.fillConvexPoly(temp2, contours.get(i), new Scalar(255,0,0));
            }

            Imgproc.blur(temp2, temp2, new Size(51, 51));

            contours = new ArrayList<MatOfPoint>();
            hierachy = new Mat();
            Imgproc.findContours(temp2, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            double contourArea = 0;
            int biggestIndex = -1;
            for(int i=0; i<contours.size(); i++){
                Double a = Imgproc.contourArea(contours.get((i)));
                if(a > contourArea){
                    biggestIndex = i;
                    contourArea = a;
                }
            }

            Mat zeroMat = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC1, new Scalar(0,0,0));
            Imgproc.fillConvexPoly(zeroMat, contours.get(biggestIndex), new Scalar(0,0,255));
            //Log.d("Bilgi", zeroMat.dump());

            MatOfPoint mop = new MatOfPoint(contours.get(biggestIndex));
            Point[] points = mop.toArray();
            Point distance_p1 = null;
            Point distance_p2 = null;

            for(int i=0; i<points.length; i++){
                Point p1 = points[i];
                for(int j=0; j<points.length; j++){
                    Point p2 = points[j];
                    double dist = CalculateDistance(p2,p1);
                    if(dist > distance){
                        distance = dist;
                        distance_p1 = p1;
                        distance_p2 = p2;
                    }
                }
            }

            Mat final_mat = new Mat();
            mRgba.copyTo(final_mat);
            //Imgproc.cvtColor(mRgba, final_mat, Imgproc.COLOR_BGR2RGB);
            try{
                arrowedLine(final_mat, distance_p1, distance_p2, new Scalar(255,0,0), 20, 8, 0,0.1);
                arrowedLine(final_mat, distance_p2, distance_p1, new Scalar(255,0,0), 20, 8, 0,0.1);
            }catch (Exception e){
                return null;
            }

            //Imgproc.erode(temp2, temp2, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(51, 51)));
            //Imgproc.dilate(temp2, temp2, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(51, 51)));


            //Imgproc.fillPoly(mRgba, contours, new Scalar(0,255,0),1,);



            return final_mat;
        }

        @Override
        protected void onPostExecute(Mat result) {
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }

            if(result == null){
                Toast.makeText(MainActivity.this, "Ölçüm Yapılamadı", Toast.LENGTH_LONG).show();
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return;
            }

            imgResult.setVisibility(View.VISIBLE);
            imgResult.setImageBitmap(Mat2Bmp(result));
            cameraManager.setVisibility(View.GONE);

            btnCapture.setVisibility(View.GONE);
            btnReset.setVisibility(View.VISIBLE);

            double mm = distance * multiplier;
            String report = "";
            report += Html.fromHtml("<h3>Analiz Raporu<h3><hr><strong>Yara Uzunluğu: </strong>" + String.valueOf(Math.round(mm)) + " mm<br>");
            report += Html.fromHtml("<strong>Geçen Süre: </strong>" + txtTimer.getText());
            txtTimer.setVisibility(View.GONE);
            txtReport.setText(report);
            txtReport.setVisibility(View.VISIBLE);
            //Toast.makeText(MainActivity.this, "Sonuç: " + String.valueOf(mm), Toast.LENGTH_LONG).show();

            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }



    }




}
