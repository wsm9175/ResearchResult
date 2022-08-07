package com.slot.researchresult;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import com.opencsv.CSVWriter;

public class ResultActivity extends AppCompatActivity implements CallBack {
    private final String TAG = ResultActivity.class.getSimpleName();
    private ArrayList<ResultData> RT_RAMP;
    private ArrayList<ResultData> RT_RPA;
    private ResultAdapter resultAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton btnDownload;
    private TabLayout tabLayout;
    private final String RT_RAMP_RESULT0 = "background";
    private final String RT_RAMP_RESULT1 = "positive"; //yellow
    private final String RT_RAMP_RESULT2 = "negative";
    private final String RT_RAMP_RESULT3 = "N/A";
    private final String RT_RPA_RESULT1 = "positive"; //yellow
    private final String RT_RPA_RESULT2 = "negative";
    private final String RT_RPA_RESULT3 = "Delta"; //red
    private final String RT_RPA_RESULT4 = "Omicron"; // skyblue
    private final String RT_RPA_RESULT5 = "N/A";

    private float inputNum;

    private CSVWriter csvWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initView();
        init();
        ArrayList<Data> resultRGBList = (ArrayList<Data>) getIntent().getSerializableExtra("RGBList");
        this.inputNum = getIntent().getFloatExtra("inputNum", 0.01f);
        classification(resultRGBList);
        resultAdapter.setmList(RT_RAMP);
    }

    public void initView() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerview);
        btnDownload = findViewById(R.id.btn_download);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                switch (pos) {
                    case 0:
                        Log.d(TAG, "R.id.result1");
                        resultAdapter.setmList(RT_RAMP);
                        break;
                    case 1:
                        Log.d(TAG, "R.id.result2");
                        resultAdapter.setmList(RT_RPA);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        btnDownload.setOnClickListener(view -> {
            requestPermission();
        });
    }

    public void init() {
        resultAdapter = new ResultAdapter(getApplicationContext());
        recyclerView.setAdapter(resultAdapter);

    }

    @Override
    public void onClick(ArrayList<Data> data) {
    }

    private void classification(ArrayList<Data> resultRGBList) {
        RT_RAMP = new ArrayList<>();
        RT_RPA = new ArrayList<>();
        int index = 0;
        //RT-LAMP
        float c = 0f;
        for (Data data : resultRGBList) {
            float r = data.getR();
            float g = data.getG();
            float b = data.getB();
            float v = this.inputNum;

            ResultData resultData = new ResultData();
            resultData.setName("W" + (index + 1));
            resultData.setR(String.valueOf(r));
            resultData.setG(String.valueOf(g));
            resultData.setB(String.valueOf(b));
            if (index == 0 || index == 1 || index == 2) {
                c += (r / b);
                Log.d(TAG, "c:"+c);
                resultData.setResult(RT_RAMP_RESULT0);
                if (index == 2) {
                    Log.d(TAG, "cal : " + c);
                    c = c/3.0f;
                }
            } else {
                Log.d(TAG, "RT_RAMP : " + c);
                float cri = r / b;
                String result = "";
                if (cri > this.inputNum * c) {
                    result += RT_RAMP_RESULT1;
                } else if (cri < this.inputNum * c) {
                    result += RT_RAMP_RESULT2;
                } else {
                    result += RT_RAMP_RESULT3;
                }
                resultData.setResult(result);
            }
            resultData.setColor(getColor(resultData.getResult()));
            RT_RAMP.add(resultData);
            index++;
        }
        index = 0;

        // RT-RPA for variants
        for (int i = 0; i < resultRGBList.size(); i += 3) {
            Log.d(TAG, "i : " + i);
            int i1 = i;
            int i2 = i + 1;
            int i3 = i + 2;
            float r1 = resultRGBList.get(i1).getR();
            float g1 = resultRGBList.get(i1).getG();
            float b1 = resultRGBList.get(i1).getB();
            float r2 = resultRGBList.get(i2).getR();
            float g2 = resultRGBList.get(i2).getG();
            float b2 = resultRGBList.get(i2).getB();
            float r3 = resultRGBList.get(i3).getR();
            float g3 = resultRGBList.get(i3).getG();
            float b3 = resultRGBList.get(i3).getB();
            ResultData resultData = new ResultData();


            if (index == 0) {
                String name = "W" + (i1 + 1) + " W" + (i2 + 1) + " W" + (i3 + 1);
                String R = r1 + "," + r2 + "," + r3;
                String G = g1 + "," + g2 + "," + g3;
                String B = b1 + "," + b2 + "," + b3;
                resultData.setName(name);
                resultData.setR(R);
                resultData.setG(G);
                resultData.setB(B);
                Log.d(TAG, "cal : " + ((r1 / b1) + (r2 / b2) + (r3 / b3)));
                Log.d(TAG, "cal2 : " + ((r1 / b1) + (r2 / b2) + (r3 / b3))/3f);

                c = ((r1 / b1) + (r2 / b2) + (r3 / b3)) / 3f;
                resultData.setResult(RT_RAMP_RESULT0);

            } else {


                Log.d(TAG, "RT_RPA : " + c);
                String name = "W" + (i1 + 1) + " W" + (i2 + 1) + " W" + (i3 + 1);
                String R = r1 + "," + r2 + "," + r3;
                String G = g1 + "," + g2 + "," + g3;
                String B = b1 + "," + b2 + "," + b3;
                resultData.setName(name);
                resultData.setR(R);
                resultData.setG(G);
                resultData.setB(B);


                float cri1 = r1 / b1;
                float cri2 = r2 / b2;
                float cri3 = r3 / b3;

                float p1 = 0.8f * c * this.inputNum;
                float p2 = c * this.inputNum;

                if ((p1 < cri1 && cri1 < p2) && (p1 < cri2 && cri2 < p2) && (p1 < cri3 && cri3 < p2)) {
                    resultData.setResult(RT_RPA_RESULT2);
                } else if ((cri1>p2) &&  (p1 < cri2 && cri2 < p2) && (p1 < cri3 && cri3 < p2)) {
                    resultData.setResult(RT_RPA_RESULT1);
                } else if ((cri1>p2) &&  (p1 < cri2 && cri2 < p2) && (cri3>p2) ) {
                    resultData.setResult(RT_RPA_RESULT3);
                } else if ((p1 < cri1 && cri1 < p2) && (cri2>p2) && (p1 < cri3 && cri3 < p2)) {
                    resultData.setResult(RT_RPA_RESULT4);
                } else if (resultData.getResult() == null) {
                    resultData.setResult(RT_RPA_RESULT5);
                }
                resultData.setColor(getColor(resultData.getResult()));
            }
            RT_RPA.add(resultData);
            index += 3;
        }
    }

    public int getColor(String result) {
        int color = getResources().getColor(R.color.white);
        switch (result) {
            case RT_RPA_RESULT1:
                Log.d(TAG, RT_RPA_RESULT1);
                color = getResources().getColor(R.color.positive);
                break;
            case RT_RPA_RESULT2:
                Log.d(TAG, RT_RPA_RESULT2);
                break;
            case RT_RPA_RESULT3:
                Log.d(TAG, RT_RPA_RESULT3);
                color = getResources().getColor(R.color.delta);
                break;
            case RT_RPA_RESULT4:
                Log.d(TAG, RT_RPA_RESULT4);
                color = getResources().getColor(R.color.omicron);
                break;
            case RT_RPA_RESULT5:
                Log.d(TAG, RT_RPA_RESULT5);
                break;
        }
        return color;
    }

    private void downloadExcel() {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TADICA/" + getTime() + "/";
        File dir = new File(root);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        //RT_RAMPdd
        Log.d(TAG, "downloadExcel");

        String filePath1 = root + "RT_RAMP.csv";
        Log.d(TAG, filePath1);

        try {
            File file = new File(filePath1);
            file.createNewFile();
            csvWriter = new CSVWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "저장 오류", Toast.LENGTH_SHORT).show();
        }
        csvWriter.writeNext(new String[]{"sample", "result"});

        for (ResultData resultData : this.RT_RAMP) {
            String name = resultData.getName();
            String result = resultData.getResult();
            csvWriter.writeNext(new String[]{name, result});
        }

        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "파일 닫기 오류", Toast.LENGTH_SHORT).show();
        }

        //RT_RAMP
        String filePath2 = root + "RT_RPA.csv";
        Log.d(TAG, filePath2);
        try {
            File file = new File(filePath2);
            file.createNewFile();
            csvWriter = new CSVWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "저장 오류", Toast.LENGTH_SHORT).show();
        }

        csvWriter.writeNext(new String[]{"sample", "result"});

        for (ResultData resultData : this.RT_RPA) {
            String name = resultData.getName();
            String result = resultData.getResult();
            csvWriter.writeNext(new String[]{name, result});
        }
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "파일 닫기 오류", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(getApplicationContext(), "파일 다운로드 성공", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), root, Toast.LENGTH_SHORT).show();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.d(TAG, "permission disable");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                        999);
                try {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception ex) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            } else {
                Log.d(TAG, "permission accept");
                downloadExcel();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 999) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    downloadExcel();
                    Toast.makeText(getApplicationContext(), "다운로드가 가능합니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "다운로드가 불가능합니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getTime() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        String getTime = sdf.format(date);

        return getTime;
    }
}