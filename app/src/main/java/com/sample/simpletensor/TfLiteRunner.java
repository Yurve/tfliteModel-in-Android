package com.sample.simpletensor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;


public class TfLiteRunner {
    Context context;
    Activity activity;
    ObjectDetector detector;
    Bitmap picture;
    JSONObject jsonObject;

    //생성자
    public TfLiteRunner(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public void setInput(Bitmap bitmap) {
        //비트맵을 받아 텐서플로우의 이미지 객체로 변환 현재는 RGBA 채널중 RGB 만 사용중 A는 무시됨.
        TensorImage image = TensorImage.fromBitmap(bitmap);

        //화면에 넣을 비트맵
        picture = bitmap;

        //추론 프로그램 초기화 메소드
        initDetector();

        //추론 실행
        if (detector != null) {
            //결과
            List<Detection> results = detector.detect(image);
            //감지 결과 Log 에 띄우기
            debugPrint(results);
        }

    }

    //추론할 프로그램 초기화
    public void initDetector() {

        //추론할 프로그램 초기화 1.최대결과(모델에서 감지해야 하는 최대 객체 수) 2. 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도) 지금은 70%
        //3.라벨 허용 목록/거부 목록 (사전 정의된 목록에서 객체 허용/거부)
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(0.7f)
                .build();
        try {
            detector = ObjectDetector.createFromFileAndOptions(context, "model.tflite", options);
        } catch (IOException e) {
            e.printStackTrace();
        }

        jsonObject = new JSONObject();
    }

    //감지 결과 Log 에 띄우기
    public void debugPrint(List<Detection> results) {
        for (Detection detection : results) {
            //바운딩 박스를 그릴 좌표 값 float 로 받고 Log 에 띄울려고 String 으로 변환 
            RectF rectF = detection.getBoundingBox();
            int top = (int) rectF.top;
            int bottom = (int) rectF.bottom;
            int left = (int) rectF.left;
            int right = (int) rectF.right;
            String Top = Integer.toString(top);
            String Bottom = Integer.toString(bottom);
            String Left = Integer.toString(left);
            String Right = Integer.toString(right);
            Log.d("detect", Left + "," + Top + " & " + Right + "," + Bottom);


            //검출된 물체의 이름과 신뢰도 표현 String 과 Double 형으로 표현 Log 에 찍으려고 둘다 String 으로 변환
            List<Category> categories = detection.getCategories();
            for (Category category : categories) {
                String label = category.getLabel();
                float confidence = category.getScore();
                int _confidence = Math.round(confidence * 100);
                String conf = Integer.toString(_confidence);
                Log.d(label, conf + "%");

                try {
                    jsonObject.put(label, conf);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //감지결과 비트맵으로 바꿔서 확인해보자
            }
            //토스트 메시지로도 띄어보자
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, jsonObject.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
