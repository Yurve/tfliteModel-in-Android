package com.sample.simpletensor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

//권한 설정 클래스
public class PermissionSupport {
    private final Context context;
    public final Activity activity;

    //원하는 임의의 숫자 지정
    public final static int MULTIPLE_PERMISSIONS = 1;  //요청에 대한 결과값 확인을 위해 RequestCode 를 final 로 정의

    //생성자
    public PermissionSupport(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    public void onCheckPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "권한 허용", Toast.LENGTH_SHORT).show();
        }
    }
}
