package com.codencode.opencvtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity{

    Button mButton , mShowResButton;
    ImageView mImage;
    ImageView mGrayImage;
    String uri;
    ArrayList<Bitmap> mItemList;
    OcrManager manager;
    TextView mResTextView;
    StringBuffer mQueryBuffer , mEquation;
    char[] mCharList;
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();
        manager = new OcrManager();
        manager.initApi();
        mItemList = new ArrayList<>();
        mButton = findViewById(R.id.main_img_select_button);
        mShowResButton = findViewById(R.id.main_show_res_button);
        mResTextView = findViewById(R.id.main_result_text_view);

        mCharList = new char[100];
        mQueryBuffer = new StringBuffer();
        mEquation = new StringBuffer();

        mImage = findViewById(R.id.main_image);
        mGrayImage = findViewById(R.id.main_gray_img);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEquation.delete(0 , mEquation.length());
                mQueryBuffer.delete(0 , mQueryBuffer.length());
                mResTextView.setText("Res = ");
                getUri();
            }
        });

        mShowResButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mQueryBuffer != null)
                {
                    String query = mQueryBuffer.toString();
                    if(query.equals(""))
                        Toast.makeText(MainActivity.this, "Select some image", Toast.LENGTH_SHORT).show();
                    else
                    {
                        Intent i = new Intent(MainActivity.this , ResultActivity.class);
                        i.putExtra("URL" , query);
                        mEquation.delete(0 , mEquation.length());
                        mQueryBuffer.delete(0 , mQueryBuffer.length());
                        mResTextView.setText("Res = ");
                        startActivity(i);
                    }
                }
            }
        });
    }

    public void getUri()
    {
        mEquation.delete(0 , mEquation.length());
        mQueryBuffer.delete(0 , mQueryBuffer.length());
        mResTextView.setText("Res = ");
        CropImage.activity().start(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    mImage.setImageBitmap(bitmap);
                    process(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uri = resultUri.toString();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public void process(Bitmap bitmap)
    {
        Bitmap tmp = Bitmap.createBitmap(bitmap.getWidth() , bitmap.getHeight() , Bitmap.Config.RGB_565);
        Bitmap grayImg = Bitmap.createBitmap(bitmap.getWidth() , bitmap.getHeight() , Bitmap.Config.RGB_565);

        long minArea = bitmap.getWidth() * bitmap.getHeight();
        minArea /= 900;
        minArea = Math.max(50 , minArea);
        Mat rgba = new Mat();
        Mat gray = new Mat();
        Mat heirarchy = new Mat();
        Utils.bitmapToMat(bitmap , rgba);

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<MatOfPoint> _final = new ArrayList<MatOfPoint>();
        ArrayList<Rect> _finalPoints = new ArrayList<>();
        ArrayList<Rect> originalPoints = new ArrayList<>();
        Imgproc.cvtColor(rgba , gray , Imgproc.COLOR_RGBA2GRAY);


        Imgproc.GaussianBlur(gray , gray , new Size(3 , 3) , 0);
        Imgproc.adaptiveThreshold(gray , gray , 255 , Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C , Imgproc.THRESH_BINARY , 75 , 10);
        Core.bitwise_not(gray , gray);
        Utils.matToBitmap(gray , grayImg);
        mGrayImage.setImageBitmap(grayImg);

        Imgproc.findContours(gray, contours , heirarchy , Imgproc.RETR_TREE , Imgproc.CHAIN_APPROX_SIMPLE);
        Collections.sort(contours ,new myComparator());
        //Merging contours which are inside of each others
        for(int i=0;i<contours.size();i++)
        {
            Rect r = Imgproc.boundingRect(contours.get(i));
            if(r.area() > minArea)
            {
                boolean isInside = false;
                for(int j=0;j<contours.size();j++)
                {
                    if(i == j) continue;
                    Rect r1 = Imgproc.boundingRect(contours.get(j));
                    if(r1.area() < r.area()) continue;
                    if(r.x>r1.x && r.x+r.width<r1.x+r1.width &&
                            r.y>r1.y && r.y+r.height< r1.y+r1.height){
                        isInside = true;
                        break;
                    }
                }

                if(isInside)
                    continue;
                _final.add(contours.get(i));
                Rect r1 = new Rect();
                r1.x = r.x;
                r1.y = r.y;
                r1.width = r.width;
                r1.height = r.height;
                originalPoints.add(r1);
                if(_finalPoints.size() >= 1)
                {
                    Rect R = _finalPoints.get(_finalPoints.size()-1);
                    if(r.x > R.x+R.width+2)
                        r.x -= 2;
                    r.y = Math.max(0 , r.y-1);

                    if(r.width + 2 <= tmp.getWidth())
                        r.width += 2;
                    if(r.height + 2<= tmp.getHeight())
                        r.height += 2;
                }
                _finalPoints.add(r);
            }

        }
        //end of block


        //finding equal sign
        for(int i=0;i<_finalPoints.size();i++)
        {
            mCharList[i] = '&';
            if((i+1) == _finalPoints.size())
                break;

            Rect r1 = _finalPoints.get(i);
            Rect r2 = _finalPoints.get(i+1);

            if(Math.abs(r1.x - r2.x) < 20 && (r1.height * 2 <= r1.width) && (r2.height*2 <= r2.width))
            {
                int minX = Math.min(r1.x , r2.x);
                int minY = Math.min(r1.y , r2.y);
                int maxX = Math.max(r1.x+r1.width , r2.x + r2.width);
                int maxY = Math.max(r1.y+r1.height , r2.y+r2.height);
                originalPoints.get(i).x = _finalPoints.get(i).x = minX;
                originalPoints.get(i).y = _finalPoints.get(i).y = minY;
                originalPoints.get(i).width = _finalPoints.get(i).width = maxX - minX;
                originalPoints.get(i).height = _finalPoints.get(i).height = maxY - minY;
                _finalPoints.remove(i+1);
                originalPoints.remove(i+1);
                mCharList[i] = '=';
                break;
            }
        }
        //end of block

        for(int i=0;i<_finalPoints.size();i++)
        {
            Rect rect = _finalPoints.get(i);
            Imgproc.rectangle(rgba , rect , new Scalar(0 , 0 , 255) ,1);
        }
        Utils.matToBitmap(rgba , tmp);
        mImage.setImageBitmap(tmp);
//        Toast.makeText(this, "The number of found contours are " + _finalPoints.size(), Toast.LENGTH_SHORT).show();

        //finding each character
        for(int i=0;i<_finalPoints.size();i++) {
            if (mCharList[i] == '=') {
//                Toast.makeText(this, "String " + (i + 1) + " = " + mCharList[i], Toast.LENGTH_LONG).show();
                addChar('=');
                mEquation.append('=');
                continue;
            }
            Rect r = _finalPoints.get(i);
            if(r.width >= 2 * r.height)
            {
//                Toast.makeText(this, "String "+(i+1)+" = " + '-', Toast.LENGTH_LONG).show();
                mCharList[i] = '-';
                addChar('-');
                mEquation.append('-');
                continue;
            }

            r.y = 0;
            r.height = tmp.getHeight();

            Mat mat = new Mat(gray, r);
            Bitmap bm = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bm);
            mItemList.add(bm);
            String st = manager.startRecognition(bm);
            if (!st.isEmpty())
            {
                mCharList[i] = getChar(st.charAt(0));
                if(i > 0 && (Character.isDigit(mCharList[i])) && (mCharList[i-1] != '-') && (mCharList[i-1] != '+') && (mCharList[i-1] != '*') && (mCharList[i-1] != '=') && (mCharList[i-1] != ')') && (mCharList[i-1] != '('))
                {
                    Rect r1 = originalPoints.get(i);
                    Rect r2 = originalPoints.get(i-1);

                    Log.i(TAG, "r1.y = " + r1.y + " r1.height = " + r1.height);
                    Log.i(TAG, "r2.y = " + r2.y + " r2.height = " + r2.height);

                    if(r1.y + r1.height <= r2.y + r2.height/2)
                    {
                        mEquation.append('^');
                        addChar('^');
                    }
                }
                mEquation.append(mCharList[i]);
                addChar(mCharList[i]);
            }
        }
        //end of block

        mResTextView.setText(mEquation.toString());
    }

    public char getChar(char c)
    {
        return c;
    }

    public void addChar(char c)
    {
        if(c == '+')
            mQueryBuffer.append("%2B");
        else
        if(c == '^')
            mQueryBuffer.append("%5E");
        else
        if(c == '=')
            mQueryBuffer.append("%3D");
        else
        if(c == '(')
            mQueryBuffer.append("%28");
        else
        if(c == ')')
            mQueryBuffer.append("%29");
        else
            mQueryBuffer.append(c);
    }


    public class myComparator implements Comparator<MatOfPoint>
    {

        @Override
        public int compare(MatOfPoint o1, MatOfPoint o2) {
            int x1 = Imgproc.boundingRect(o1).x;
            int x2 = Imgproc.boundingRect(o2).x;

            if(x1 < x2) return -1;
            else
            if(x1 == x2) return 0;
            else
                return 1;
        }
    }
}
