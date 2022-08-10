package com.slot.researchresult;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import com.opencsv.CSVWriter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

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

    private ArrayList<Integer> lampPositiveList = new ArrayList<>();
    private ArrayList<Integer> lampNegativeList = new ArrayList<>();
    private ArrayList<Integer> lampNAList = new ArrayList<>();
    private ArrayList<Integer> rpaPositiveList = new ArrayList<>();
    private ArrayList<Integer> rpaNegativeList = new ArrayList<>();
    private ArrayList<Integer> rpaNAList = new ArrayList<>();
    private ArrayList<Integer> rpaDeltaList = new ArrayList<>();
    private ArrayList<Integer> rpaOmicron = new ArrayList<>();

    private ArrayList<Data> resultRGBList;

    private float inputNum;

    private Workbook wb;

    private int tabPos = 0;

    private TextView txtMethod,
                    txtPlate,
                    txtNA,
                    txtNegative,
                    txtPositive,
                    txtDelta,
                    txtOmicron;
    private LinearLayout linearDelta,
                        linearOmicron;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initView();
        init();
        resultRGBList = (ArrayList<Data>) getIntent().getSerializableExtra("RGBList");
        this.inputNum = getIntent().getFloatExtra("inputNum", 0.01f);
        classification(resultRGBList);
        resultAdapter.setmList(RT_RAMP);
        switchSummary(0);
    }

    public void initView() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerview);
        btnDownload = findViewById(R.id.btn_download);
        txtMethod = findViewById(R.id.txt_method);
        txtPlate = findViewById(R.id.txt_plate);
        txtNA = findViewById(R.id.txt_NA);
        txtNegative = findViewById(R.id.txt_negative);
        txtPositive = findViewById(R.id.txt_positive);
        txtDelta = findViewById(R.id.txt_delta);
        txtOmicron = findViewById(R.id.txt_omicron);
        linearDelta = findViewById(R.id.linear_delta);
        linearOmicron = findViewById(R.id.linear_omicron);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                switch (pos) {
                    case 0:
                        Log.d(TAG, "R.id.result1");
                        resultAdapter.setmList(RT_RAMP);
                        tabPos = 0;
                        switchSummary(tabPos);
                        break;
                    case 1:
                        Log.d(TAG, "R.id.result2");
                        resultAdapter.setmList(RT_RPA);
                        tabPos = 1;
                        switchSummary(tabPos);
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
                Log.d(TAG, "c:" + c);
                resultData.setResult(RT_RAMP_RESULT0);
                if (index == 2) {
                    Log.d(TAG, "cal : " + c);
                    c = c / 3.0f;
                }
            } else {
                Log.d(TAG, "RT_RAMP : " + c);
                float cri = r / b;
                String result = "";
                if (cri > this.inputNum * c) {
                    result += RT_RAMP_RESULT1;
                    lampPositiveList.add(index + 1);
                } else if (cri < this.inputNum * c) {
                    result += RT_RAMP_RESULT2;
                    lampNegativeList.add(index + 1);
                } else {
                    result += RT_RAMP_RESULT3;
                    lampNAList.add(index + 1);
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
                Log.d(TAG, "cal2 : " + ((r1 / b1) + (r2 / b2) + (r3 / b3)) / 3f);
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
                    rpaPositiveList.add(index / 3 + 1);
                } else if ((cri1 > p2) && (p1 < cri2 && cri2 < p2) && (p1 < cri3 && cri3 < p2)) {
                    resultData.setResult(RT_RPA_RESULT1);
                    rpaNegativeList.add(index /3 + 1);
                } else if ((cri1 > p2) && (p1 < cri2 && cri2 < p2) && (cri3 > p2)) {
                    resultData.setResult(RT_RPA_RESULT3);
                    rpaDeltaList.add(index / 3 + 1);
                } else if ((p1 < cri1 && cri1 < p2) && (cri2 > p2) && (p1 < cri3 && cri3 < p2)) {
                    resultData.setResult(RT_RPA_RESULT4);
                    rpaOmicron.add(index / 3 + 1);
                } else if (resultData.getResult() == null) {
                    resultData.setResult(RT_RPA_RESULT5);
                    rpaNAList.add(index/3+1);
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

        wb = new HSSFWorkbook();

        if (this.resultRGBList.size() == 96 && tabPos == 0) {
            createLAMP96();
        } else if (this.resultRGBList.size() == 96 && tabPos == 1) {
            createRPA96();
        }else if(this.resultRGBList.size() == 384 && tabPos == 0){
            createLAMP384();
        } else if (this.resultRGBList.size() == 384&& tabPos == 1) {
            createRPA384();
        }
        createFile(dir, "research export.xls");
    }

    private void createLAMP96() {
        Sheet sheetLAMP96 = wb.createSheet("RT-LAMP 96 well result");

        /* row 1 - info*/
        Row infoRow1 = sheetLAMP96.createRow(1);
        //info cell
        Cell infoCell1 = infoRow1.createCell(1);
        infoCell1.setCellValue("Method:");
        Cell infoCell2 = infoRow1.createCell(2);
        infoCell2.setCellValue("RT-LAMP");

        //summary
        StringBuilder positiveList = new StringBuilder();
        String ment1 = "Summary: Sample number";
        String mentPositive = "are positive.";
        for (int num : lampPositiveList) {
            positiveList.append(" " + num + " ");
        }
        Cell infoCell3 = infoRow1.createCell(4);
        infoCell3.setCellValue(ment1 + positiveList.toString() + mentPositive);
        sheetLAMP96.addMergedRegion(new CellRangeAddress(1, 1, 4, 12));

        /* row 2 - info*/
        Row infoRow2 = sheetLAMP96.createRow(2);
        //info cell2
        Cell infoCell4 = infoRow2.createCell(1);
        infoCell4.setCellValue("Plate:");
        Cell infoCell5 = infoRow2.createCell(2);
        infoCell5.setCellValue("96 well");

        /*row 4 - title*/
        Row titleRow = sheetLAMP96.createRow(4);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("Analysis result:");
        sheetLAMP96.addMergedRegion(new CellRangeAddress(4, 4, 1, 5));

        /*row 5 - field*/
        String[] fieldArray = new String[]{"Well", "R", "G", "B", "R/G", "Result"};
        Row fieldRow = sheetLAMP96.createRow(5);
        for (int j = 1; j <= 6; j++) {
            Cell wellCell = fieldRow.createCell(j);
            wellCell.setCellValue(fieldArray[j - 1]);
        }

        int rowIndex = 6;
        int wellIndex = 1;
        for (Data data : this.resultRGBList) {
            Row dataRow = sheetLAMP96.createRow(rowIndex);

            int r = data.getR();
            int g = data.getG();
            int b = data.getB();
            float rg = (float) r / (float) g;
            String result = this.RT_RAMP.get(wellIndex - 1).getResult();

            //Well cell
            Cell cellWell = dataRow.createCell(1);
            cellWell.setCellValue(wellIndex);
            //R cell
            Cell cellR = dataRow.createCell(2);
            cellR.setCellValue(r);
            //G cell
            Cell cellG = dataRow.createCell(3);
            cellG.setCellValue(g);
            //B cell
            Cell cellB = dataRow.createCell(4);
            cellB.setCellValue(b);
            //R/G cell
            Cell cellRG = dataRow.createCell(5);
            cellRG.setCellValue(rg);
            //Result cell
            Cell cellResult = dataRow.createCell(6);
            cellResult.setCellValue(result);

            rowIndex++;
            wellIndex++;
        }
    }

    private void createLAMP384() {
        Sheet sheetLAMP384 = wb.createSheet("RT-LAMP 384 well result");

        /* row 1 - info*/
        Row infoRow1 = sheetLAMP384.createRow(1);
        //info cell
        Cell infoCell1 = infoRow1.createCell(1);
        infoCell1.setCellValue("Method:");
        Cell infoCell2 = infoRow1.createCell(2);
        infoCell2.setCellValue("RT-LAMP");

        //summary
        StringBuilder positiveList = new StringBuilder();
        String ment1 = "Summary: Sample number";
        String mentPositive = "are positive.";
        for (int num : lampPositiveList) {
            positiveList.append(" " + num + " ");
        }
        Cell infoCell3 = infoRow1.createCell(4);
        infoCell3.setCellValue(ment1 + positiveList.toString() + mentPositive);
        sheetLAMP384.addMergedRegion(new CellRangeAddress(1, 1, 4, 12));

        /* row 2 - info*/
        Row infoRow2 = sheetLAMP384.createRow(2);
        //info cell2
        Cell infoCell4 = infoRow2.createCell(1);
        infoCell4.setCellValue("Plate:");
        Cell infoCell5 = infoRow2.createCell(2);
        infoCell5.setCellValue("384 well");

        /*row 4 - title*/
        Row titleRow = sheetLAMP384.createRow(4);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("Analysis result:");
        sheetLAMP384.addMergedRegion(new CellRangeAddress(4, 4, 1, 5));

        /*row 5 - field*/
        String[] fieldArray = new String[]{"Well", "R", "G", "B", "R/G", "Result"};
        Row fieldRow = sheetLAMP384.createRow(5);
        for (int j = 1; j <= 6; j++) {
            Cell wellCell = fieldRow.createCell(j);
            wellCell.setCellValue(fieldArray[j - 1]);
        }

        int rowIndex = 6;
        int wellIndex = 1;
        for (Data data : this.resultRGBList) {
            Row dataRow = sheetLAMP384.createRow(rowIndex);

            int r = data.getR();
            int g = data.getG();
            int b = data.getB();
            float rg = (float) r / (float) g;
            String result = this.RT_RAMP.get(wellIndex - 1).getResult();

            //Well cell
            Cell cellWell = dataRow.createCell(1);
            cellWell.setCellValue(wellIndex);
            //R cell
            Cell cellR = dataRow.createCell(2);
            cellR.setCellValue(r);
            //G cell
            Cell cellG = dataRow.createCell(3);
            cellG.setCellValue(g);
            //B cell
            Cell cellB = dataRow.createCell(4);
            cellB.setCellValue(b);
            //R/G cell
            Cell cellRG = dataRow.createCell(5);
            cellRG.setCellValue(rg);
            //Result cell
            Cell cellResult = dataRow.createCell(6);
            cellResult.setCellValue(result);

            rowIndex++;
            wellIndex++;
        }
    }

    private void createRPA96() {
        Sheet sheetRPA96 = wb.createSheet("RT-RPA 96 well result");

        /* row 1 - info*/
        Row infoRow1 = sheetRPA96.createRow(1);
        //info cell
        Cell infoCell1 = infoRow1.createCell(1);
        infoCell1.setCellValue("Method:");
        Cell infoCell2 = infoRow1.createCell(2);
        infoCell2.setCellValue("RT-RPA");

        //summary1
        StringBuilder positiveList = new StringBuilder();
        String ment1 = "Summary: Sample number";
        String mentPositive = "are positive.";
        for (int num : rpaPositiveList) {
            positiveList.append(" " + num + " ");
        }
        Cell infoCell3 = infoRow1.createCell(4);
        infoCell3.setCellValue(ment1 + positiveList.toString() + mentPositive);
        sheetRPA96.addMergedRegion(new CellRangeAddress(1, 1, 4, 12));

        /* row 2 - info*/
        Row infoRow2 = sheetRPA96.createRow(2);
        //info cell2
        Cell infoCell4 = infoRow2.createCell(1);
        infoCell4.setCellValue("Plate:");
        Cell infoCell5 = infoRow2.createCell(2);
        infoCell5.setCellValue("96 well");

        //summary2
        StringBuilder deltaList = new StringBuilder();
        String ment2 = "Summary: Sample number";
        String mentDelta = "are delta.";
        for (int num : rpaDeltaList) {
            deltaList.append(" " + num + " ");
        }
        Cell infoCell6 = infoRow2.createCell(4);
        infoCell6.setCellValue(ment2 + deltaList.toString() + mentDelta);
        sheetRPA96.addMergedRegion(new CellRangeAddress(2, 2, 4, 12));

        //summary3
        Row infoRow3 = sheetRPA96.createRow(3);

        StringBuilder omicronList = new StringBuilder();
        String ment3 = "Summary: Sample number";
        String mentOmicron = "are Omicron.";
        for (int num : rpaOmicron) {
            omicronList.append(" " + num + " ");
        }
        Cell infoCell7 = infoRow3.createCell(4);
        infoCell7.setCellValue(ment3 + omicronList.toString() + mentOmicron);
        sheetRPA96.addMergedRegion(new CellRangeAddress(3, 3, 4, 12));

        /*row 4 - title*/
        Row titleRow = sheetRPA96.createRow(4);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("Analysis result:");
        sheetRPA96.addMergedRegion(new CellRangeAddress(4, 4, 1, 5));

        /*row 5 - field*/
        String[] fieldArray = new String[]{"Sample", "Well", "R", "G", "B", "R/G", "Result"};
        Row fieldRow = sheetRPA96.createRow(5);
        for (int j = 1; j <= 7; j++) {
            Cell wellCell = fieldRow.createCell(j);
            wellCell.setCellValue(fieldArray[j - 1]);
        }

        int rowIndex = 6;
        int wellIndex = 1;
        int sampleIndex = 1;
        int resultIndex = 0;
        for (int i = 0; i < this.resultRGBList.size(); i += 3) {
            //init data
            Row dataRow1 = sheetRPA96.createRow(rowIndex); // 6
            Row dataRow2 = sheetRPA96.createRow(rowIndex + 1); // 7
            Row dataRow3 = sheetRPA96.createRow(rowIndex + 2); // 8

            int r1 = resultRGBList.get(i).getR();
            int r2 = resultRGBList.get(i + 1).getR();
            int r3 = resultRGBList.get(i + 2).getR();
            int g1 = resultRGBList.get(i).getR();
            int g2 = resultRGBList.get(i + 1).getR();
            int g3 = resultRGBList.get(i + 2).getR();
            int b1 = resultRGBList.get(i).getR();
            int b2 = resultRGBList.get(i + 1).getR();
            int b3 = resultRGBList.get(i + 2).getR();
            float rg1 = (float) r1 / (float) g1;
            float rg2 = (float) r2 / (float) g2;
            float rg3 = (float) r3 / (float) g3;
            String result = RT_RPA.get(resultIndex++).getResult();

            //Sample data
            Cell cellSample1 = dataRow1.createCell(1);
            Cell cellSample2 = dataRow2.createCell(1);
            Cell cellSample3 = dataRow3.createCell(1);
            cellSample1.setCellValue(sampleIndex++);
            sheetRPA96.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 1, 1));

            //Well cell
            Cell cellWell1 = dataRow1.createCell(2);
            cellWell1.setCellValue(wellIndex++);
            Cell cellWell2 = dataRow2.createCell(2);
            cellWell2.setCellValue(wellIndex++);
            Cell cellWell3 = dataRow3.createCell(2);
            cellWell3.setCellValue(wellIndex++);

            //R cell
            Cell cellR1 = dataRow1.createCell(3);
            cellR1.setCellValue(r1);
            Cell cellR2 = dataRow2.createCell(3);
            cellR2.setCellValue(r2);
            Cell cellR3 = dataRow3.createCell(3);
            cellR3.setCellValue(r3);

            //G Cell
            Cell cellG1 = dataRow1.createCell(4);
            cellG1.setCellValue(g1);
            Cell cellG2 = dataRow2.createCell(4);
            cellG2.setCellValue(g2);
            Cell cellG3 = dataRow3.createCell(4);
            cellG3.setCellValue(g3);

            //B Cell
            Cell cellB1 = dataRow1.createCell(5);
            cellB1.setCellValue(b1);
            Cell cellB2 = dataRow2.createCell(5);
            cellB2.setCellValue(b2);
            Cell cellB3 = dataRow3.createCell(5);
            cellB3.setCellValue(b3);

            //R/G Cell
            Cell cellrg1 = dataRow1.createCell(6);
            cellrg1.setCellValue(rg1);
            Cell cellrg2 = dataRow2.createCell(6);
            cellrg2.setCellValue(rg2);
            Cell cellrg3 = dataRow3.createCell(6);
            cellrg3.setCellValue(rg3);

            //Result cell
            Cell cellResult1 = dataRow1.createCell(7);
            Cell cellResult2 = dataRow2.createCell(7);
            Cell cellResult3 = dataRow3.createCell(7);
            cellResult1.setCellValue(result);
            sheetRPA96.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 7, 7));

            rowIndex += 3;
        }
    }

    private void createRPA384() {
        Sheet sheetRPA384 = wb.createSheet("RT-RPA 384 well result");

        /* row 1 - info*/
        Row infoRow1 = sheetRPA384.createRow(1);
        //info cell
        Cell infoCell1 = infoRow1.createCell(1);
        infoCell1.setCellValue("Method:");
        Cell infoCell2 = infoRow1.createCell(2);
        infoCell2.setCellValue("RT-RPA");

        //summary1
        StringBuilder positiveList = new StringBuilder();
        String ment1 = "Summary: Sample number";
        String mentPositive = "are positive.";
        for (int num : rpaPositiveList) {
            positiveList.append(" " + num + " ");
        }
        Cell infoCell3 = infoRow1.createCell(4);
        infoCell3.setCellValue(ment1 + positiveList.toString() + mentPositive);
        sheetRPA384.addMergedRegion(new CellRangeAddress(1, 1, 4, 12));

        /* row 2 - info*/
        Row infoRow2 = sheetRPA384.createRow(2);
        //info cell2
        Cell infoCell4 = infoRow2.createCell(1);
        infoCell4.setCellValue("Plate:");
        Cell infoCell5 = infoRow2.createCell(2);
        infoCell5.setCellValue("384 well");

        //summary2
        StringBuilder deltaList = new StringBuilder();
        String ment2 = "Summary: Sample number";
        String mentDelta = "are delta.";
        for (int num : rpaDeltaList) {
            deltaList.append(" " + num + " ");
        }
        Cell infoCell6 = infoRow2.createCell(4);
        infoCell6.setCellValue(ment2 + deltaList.toString() + mentDelta);
        sheetRPA384.addMergedRegion(new CellRangeAddress(2, 2, 4, 12));

        //summary3
        Row infoRow3 = sheetRPA384.createRow(3);

        StringBuilder omicronList = new StringBuilder();
        String ment3 = "Summary: Sample number";
        String mentOmicron = "are Omicron.";
        for (int num : rpaOmicron) {
            omicronList.append(" " + num + " ");
        }
        Cell infoCell7 = infoRow3.createCell(4);
        infoCell7.setCellValue(ment3 + omicronList.toString() + mentOmicron);
        sheetRPA384.addMergedRegion(new CellRangeAddress(3, 3, 4, 12));

        /*row 4 - title*/
        Row titleRow = sheetRPA384.createRow(4);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("Analysis result:");
        sheetRPA384.addMergedRegion(new CellRangeAddress(4, 4, 1, 5));

        /*row 5 - field*/
        String[] fieldArray = new String[]{"Sample", "Well", "R", "G", "B", "R/G", "Result"};
        Row fieldRow = sheetRPA384.createRow(5);
        for (int j = 1; j <= 7; j++) {
            Cell wellCell = fieldRow.createCell(j);
            wellCell.setCellValue(fieldArray[j - 1]);
        }

        int rowIndex = 6;
        int wellIndex = 1;
        int sampleIndex = 1;
        int resultIndex = 0;
        for (int i = 0; i < this.resultRGBList.size(); i += 3) {
            //init data
            Row dataRow1 = sheetRPA384.createRow(rowIndex); // 6
            Row dataRow2 = sheetRPA384.createRow(rowIndex + 1); // 7
            Row dataRow3 = sheetRPA384.createRow(rowIndex + 2); // 8

            int r1 = resultRGBList.get(i).getR();
            int r2 = resultRGBList.get(i + 1).getR();
            int r3 = resultRGBList.get(i + 2).getR();
            int g1 = resultRGBList.get(i).getR();
            int g2 = resultRGBList.get(i + 1).getR();
            int g3 = resultRGBList.get(i + 2).getR();
            int b1 = resultRGBList.get(i).getR();
            int b2 = resultRGBList.get(i + 1).getR();
            int b3 = resultRGBList.get(i + 2).getR();
            float rg1 = (float) r1 / (float) g1;
            float rg2 = (float) r2 / (float) g2;
            float rg3 = (float) r3 / (float) g3;
            String result = RT_RPA.get(resultIndex++).getResult();

            //Sample data
            Cell cellSample1 = dataRow1.createCell(1);
            Cell cellSample2 = dataRow2.createCell(1);
            Cell cellSample3 = dataRow3.createCell(1);
            cellSample1.setCellValue(sampleIndex++);
            sheetRPA384.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 1, 1));

            //Well cell
            Cell cellWell1 = dataRow1.createCell(2);
            cellWell1.setCellValue(wellIndex++);
            Cell cellWell2 = dataRow2.createCell(2);
            cellWell2.setCellValue(wellIndex++);
            Cell cellWell3 = dataRow3.createCell(2);
            cellWell3.setCellValue(wellIndex++);

            //R cell
            Cell cellR1 = dataRow1.createCell(3);
            cellR1.setCellValue(r1);
            Cell cellR2 = dataRow2.createCell(3);
            cellR2.setCellValue(r2);
            Cell cellR3 = dataRow3.createCell(3);
            cellR3.setCellValue(r3);

            //G Cell
            Cell cellG1 = dataRow1.createCell(4);
            cellG1.setCellValue(g1);
            Cell cellG2 = dataRow2.createCell(4);
            cellG2.setCellValue(g2);
            Cell cellG3 = dataRow3.createCell(4);
            cellG3.setCellValue(g3);

            //B Cell
            Cell cellB1 = dataRow1.createCell(5);
            cellB1.setCellValue(b1);
            Cell cellB2 = dataRow2.createCell(5);
            cellB2.setCellValue(b2);
            Cell cellB3 = dataRow3.createCell(5);
            cellB3.setCellValue(b3);

            //R/G Cell
            Cell cellrg1 = dataRow1.createCell(6);
            cellrg1.setCellValue(rg1);
            Cell cellrg2 = dataRow2.createCell(6);
            cellrg2.setCellValue(rg2);
            Cell cellrg3 = dataRow3.createCell(6);
            cellrg3.setCellValue(rg3);

            //Result cell
            Cell cellResult1 = dataRow1.createCell(7);
            Cell cellResult2 = dataRow2.createCell(7);
            Cell cellResult3 = dataRow3.createCell(7);
            cellResult1.setCellValue(result);
            sheetRPA384.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, 7, 7));

            rowIndex += 3;
        }
    }

    private void createFile(File dir, String name) {

        //엑셀 파일 생성
        File xls = new File(dir, name);
        try {
            FileOutputStream os = new FileOutputStream(xls);
            wb.write(os);
            Toast.makeText(this, "파일 생성 성공" + xls.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        shareFile(xls);
    }

    private void shareFile(File xls) {
        Toast.makeText(this, "파일 공유하기", Toast.LENGTH_SHORT).show();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/excel");    // 엑셀파일 공유 시
        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", xls);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(shareIntent, "파일 공유"));
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

    private void switchSummary(int tabPos){
        switch (tabPos){
            case 0:
                linearDelta.setVisibility(View.INVISIBLE);
                linearOmicron.setVisibility(View.INVISIBLE);
                break;
            case 1:
                linearDelta.setVisibility(View.VISIBLE);
                linearOmicron.setVisibility(View.VISIBLE);
                break;
        }
        settingSummary(tabPos);
    }

    private void settingSummary(int tabPos){
        final String METHOD1 ="RT-LAMP";
        final String METHOD2 ="RT-RPA";
        final String PLATE1 = "96 plate";
        final String PLATE2 = "384 plate";
        final int plate = resultRGBList.size();
        if(tabPos == 0){
            //Method
            txtMethod.setText(METHOD1);
            //plate
            if(plate== 96){
                txtPlate.setText(PLATE1);
            }else if(plate == 384){
                txtPlate.setText(PLATE2);
            }
            //N/A
            StringBuilder NAList = new StringBuilder();
            for(int wellNum:this.lampNAList){
                NAList.append(wellNum +"  ");
            }
            txtNA.setText(NAList.toString());
            //Negative
            StringBuilder NegativeList = new StringBuilder();
            for(int wellNum:this.lampNegativeList){
                NegativeList.append(wellNum +"  ");
            }
            txtNegative.setText(NegativeList.toString());
            //Positive
            StringBuilder PositiveList = new StringBuilder();
            for(int wellNum:this.lampPositiveList){
                PositiveList.append(wellNum +"  ");
            }
            txtPositive.setText(PositiveList.toString());

        }else if(tabPos == 1){
            //Method
            txtMethod.setText(METHOD2);
            //plate
            if(plate== 96){
                txtPlate.setText(PLATE1);
            }else if(plate == 384){
                txtPlate.setText(PLATE2);
            }
            //N/A
            StringBuilder NAList = new StringBuilder();
            for(int wellNum:this.rpaNAList){
                NAList.append(wellNum +"  ");
            }
            txtNA.setText(NAList.toString());
            //Negative
            StringBuilder NegativeList = new StringBuilder();
            for(int wellNum:this.rpaNegativeList){
                NegativeList.append(wellNum +"  ");
            }
            txtNegative.setText(NegativeList.toString());
            //Positive
            StringBuilder PositiveList = new StringBuilder();
            for(int wellNum:this.rpaPositiveList){
                PositiveList.append(wellNum +"  ");
            }
            txtPositive.setText(PositiveList.toString());
            //Delta
            StringBuilder DeltaList = new StringBuilder();
            for(int wellNum:this.rpaDeltaList){
                DeltaList.append(wellNum +"  ");
            }
            txtDelta.setText(DeltaList.toString());
            //Omicron
            StringBuilder OmicronList = new StringBuilder();
            for(int wellNum:this.rpaOmicron){
                OmicronList.append(wellNum +"  ");
            }
            txtOmicron.setText(OmicronList.toString());
        }
    }
}