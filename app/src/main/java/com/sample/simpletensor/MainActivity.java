package com.sample.simpletensor;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    //미리보기 클래스
    public PreviewView previewView;

    //카메라 제공 클래스 + 이미지 처리 클래스
    private ProcessCameraProvider processCameraProvider;
    //이미지 처리 중인지 아닌지 확인하는 boolean
    public boolean isFrameProcess = false;


    //모델 클래스
    public TfLiteRunner detector;

    //비트맵 가로,세로 크기
    private static final int INPUT_SIZE = 320;


    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);

        //권한 확인하기
        permissionCheck();

        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            processCameraProvider = cameraProviderFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        //추론에 쓰일 클래스
        detector = new TfLiteRunner(this,this);

        //카메라 켜기
        startCamera();
    }

    public void startCamera() {
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        //카메라 렌즈 중 자기가 고를 렌즈 선택
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        //표준 비율로 화면 미리보기
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        //이미지 분석
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                //분석할 사진의 크기, 프리뷰의 크기와 같게 해야한다. 이후에 모델에 맞게 수정하고 넣어야함.
                .setTargetResolution(new Size(previewView.getWidth(), previewView.getHeight()))
                //분석 중이면 그 다음 화면이 대기중인게 아니라 계속 받아오는 화면으로 새로 고침함. 분석이 끝나면 그 최신 화면을 다시 분석
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();


        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                //만약 이미지 처리 과정을 하고 있다면 이미지 처리를 하지 않음.
                if (isFrameProcess) {
                    imageProxy.close();
                    return;
                }
                //이미지 처리 메소드
                imageToBitmap(imageProxy);

                // after done, release the ImageProxy object
                imageProxy.close();
            }
        });

        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void imageToBitmap(ImageProxy imageProxy) {
        //이미지 받아오기
        Image image = imageProxy.getImage();

        //이미지 처리과정 On
        isFrameProcess = true;

        //do something
        if (image != null) {
            //화면에 비춰지는 2차원 평면 공간
            Image.Plane[] planes = image.getPlanes();

            //yuv 형태의 색상 표현 RGB 색상 표현 방식은 색 전부를 표현하지만, 용량이 너무 크기에 yuv 형태로 표현한다.
            //y = 빛의 밝기를 나타내는 휘도, u,v = 색상신호,  u가 클수록 보라색,파란색 계열 ,v가 클수록 빨간색, 초록색 계열이다.
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            //buffer 에 담긴 데이터를 limit 까지 읽어들일 수 있는 데이터의 갯수를 리턴한다.
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            //버퍼 최대크기를 모아놓은 바이트 배열
            byte[] yuvBytes = new byte[ySize + uSize + vSize];
            //buffer 의 offset 위치부터 length 길이 만큼 읽음.
            //각각 y, u, v 읽어오기
            yBuffer.get(yuvBytes, 0, ySize);
            vBuffer.get(yuvBytes, ySize, vSize);
            uBuffer.get(yuvBytes, ySize + vSize, uSize);

            //이제 바이트 배열을 가지고와서 Yuv 이미지를 만듬.
            YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            //jpeg 파일로 압축
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

            //비트맵으로 변환
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            //모델파일에 넣으려면 비트맵 크기를 줄여야한다. 문제는 가로 비율로 하면 잘린 화면으로 모델에 집어넣음. 어떻게 해야할까나
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

            //추론 클래스에 넣기
            detector.setInput(bitmap);

            //이미지 처리과정 Off
            isFrameProcess = false;
        }
    }


    @Override
    protected void onDestroy() {
        processCameraProvider.unbindAll();
        super.onDestroy();
    }


    //권한 체크
    private void permissionCheck() {

        //권한 요청 클래스
        PermissionSupport permissionSupport = new PermissionSupport(this, this);

        permissionSupport.onCheckPermission();
    }

    // permissionCheck 대한 결과 값 받고나서
    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        if (requestCode == PermissionSupport.MULTIPLE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한 확인", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("권한: ", "권한 취소");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}