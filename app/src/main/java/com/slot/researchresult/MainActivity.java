package com.slot.researchresult;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();
    private ImageView iv, ivv;
    private Button btn_ok, btn_take_picture, btn_select_picture, btn_clear;
    private EditText edtInputNum;
    private RadioGroup radioGroup;
    private RadioButton rdb96, rdb384;
    private SurfaceView sv;
    private ConstraintLayout lo;
    private Camera camera;
    private CameraView cameraView;
    private FrameLayout frameLayout, fm_iv;
    private Button btnTag;
    private LinearLayout lo_buttons, lo_dots;
    private ArrayList<Data> first = new ArrayList<>();
    private ArrayList<Data> rgb;
    private int PICK_IMAGE = 100, RESULT_CROP = 200, CAPTURE_IMAGE = 300;

    private Bitmap extractBitmap;

    private int selectOption = -99;

    private boolean isSetting;

    private byte[] captureResizeByte;
    private Bitmap captureResizeBitmap;
    private boolean isRotation;
    private final String IS_ROTATION = "isRotation";
    private final String CAPTURE_RESIZE_BITMAP = "captureResizeBitmap";
    private final String SELECTION_OPTION = "selectionOption";
    private final String RDB96 = "rdb96";
    private final String RDB384 = "rdb384";

    private boolean isIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv = findViewById(R.id.iv);
        btn_ok = findViewById(R.id.btn_ok);
        btn_take_picture = findViewById(R.id.btn_take_picture);

        lo = findViewById(R.id.lo_cons);
        frameLayout = findViewById(R.id.sv);
        btn_select_picture = findViewById(R.id.btn_select_picture);
        lo_buttons = findViewById(R.id.lo_buttons);
        fm_iv = findViewById(R.id.fm_iv);
        lo_dots = findViewById(R.id.lo_dots);
        radioGroup = findViewById(R.id.rgbGroup);
        rdb96 = findViewById(R.id.rgb1);
        rdb384 = findViewById(R.id.rgb2);
        btn_clear = findViewById(R.id.btn_clear);
        edtInputNum = findViewById(R.id.edt_input_num);

        isIntent = false;

        if (requestPermissions()) {
            settingViewClickListener();
        }
    }

    private void settingViewClickListener() {

        btn_select_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectOption != -99) {
                    first.clear();
                    lo_buttons.setVisibility(View.GONE);
//                iv.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    intent.putExtra("crop", true);
                    startActivityForResult(intent, PICK_IMAGE);
                } else {
                    Toast.makeText(getApplicationContext(), "위 옵션을 선택해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_take_picture.setOnClickListener(view -> {
            if (selectOption != -99) {
                isRotation = true;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                frameLayout.setVisibility(View.VISIBLE);
                lo.setVisibility(View.GONE);
                camera = switch_on_camera();
                cameraView = new CameraView(MainActivity.this, camera);
                frameLayout.addView(cameraView, 0);

                //set the properties for button
                btnTag = new Button(MainActivity.this);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.BOTTOM | Gravity.CENTER;
//                params.bottomMargin = 100;
                btnTag.setLayoutParams(params);
                btnTag.setText("촬영");
                btnTag.setTag("Confirm_button");

                btnTag.setOnClickListener(view1 -> {
                    captureImage();
                });
                frameLayout.addView(btnTag);
                frameLayout.addView(new DrawCanvas(getApplicationContext()));

            } else {
                Toast.makeText(getApplicationContext(), "위 옵션을 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
        btn_clear.setOnClickListener(view -> {
            isSetting = false;
            iv.setVisibility(View.GONE);
            iv.setImageResource(0);
            fm_iv.removeAllViews();
            fm_iv.addView(lo_buttons);
            fm_iv.addView(iv);
            iv.setVisibility(View.INVISIBLE);
            lo_buttons.setVisibility(View.VISIBLE);
            selectOption = -99;
            if (rdb96.isChecked()) {
                selectOption = 0;
            } else if (rdb384.isChecked()) {
                selectOption = 1;
            }
            edtInputNum.setText("");
        });

        btn_ok.setOnClickListener(view -> {
                    if (isSetting) {
                        isIntent = true;
                        float inputNum = 0.01f;
                        Log.d(TAG, "first size"+first.size());
                        getRGB(first);

                        if (!edtInputNum.getText().toString().equals("")) {
                            inputNum = Float.parseFloat(edtInputNum.getText().toString());
                        }

                        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                        intent.putExtra("RGBList", rgb);
                        intent.putExtra("inputNum", inputNum);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "값을 넣어주세요", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.rgb1) {
                    selectOption = 0;
                } else if (i == R.id.rgb2) {
                    selectOption = 1;
                }
                btn_clear.performClick();
            }
        });
    }


    private Camera switch_on_camera() {
        Camera cam_obj = null;

        cam_obj = Camera.open();

        Camera.Parameters parameters = cam_obj.getParameters();
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait");
            cam_obj.setDisplayOrientation(90);
            parameters.setRotation(90);
        } else {
            parameters.set("orientation", "landscape");
            cam_obj.setDisplayOrientation(0);
            parameters.setRotation(0);
        }
        return cam_obj;
    }

    ActivityResultLauncher<String> content = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = getBitmapFromUri(result);
                        saveBitmapToCache(bitmap);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    iv.setImageBitmap(bitmap);
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE) {
            CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON).start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            //크롭 성공시
            if (resultCode == RESULT_OK) {
                isSetting = true;
                iv.setVisibility(View.VISIBLE);
                iv.setImageURI(result.getUri());

                Bitmap bitmap = ((BitmapDrawable) iv.getDrawable()).getBitmap();
                extractBitmap = bitmap;
                Log.d("데이터하이트", String.valueOf(bitmap.getHeight()));
                Log.d("데이터하이트", String.valueOf(bitmap.getWidth()));
                Paint paint = new Paint();
                int radius;
                radius = 10;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(getResources().getColor(R.color.red));
                Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
                Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);

                if (selectOption == 0) {
                    int startY = bitmap.getHeight() / 16;
                    int startX = bitmap.getWidth() / 24;
                    for (int ii = startY; ii < bitmap.getHeight(); ii += bitmap.getHeight() / 8) {
                        for (int i = startX; i < bitmap.getWidth(); i += bitmap.getWidth() / 12) {
                            Data dataPosition = new Data();
                            Log.d("포지션", String.valueOf(i));
                            Log.d("포지션2", String.valueOf(ii));
                            dataPosition.setX(i);
                            dataPosition.setY(ii);
                            first.add(dataPosition);

                            canvas.drawCircle(i, ii, radius, paint);
                            iv.setImageBitmap(mutableBitmap);
                        }
                    }
                } else if (selectOption == 1) {
                    int startY = bitmap.getHeight() / 32;
                    int startX = bitmap.getWidth() / 48;
                    for (int ii = startY; ii < bitmap.getHeight(); ii += bitmap.getHeight() / 16) {
                        for (int i = startX; i < bitmap.getWidth(); i += bitmap.getWidth() / 24) {
                            Data dataPosition = new Data();
                            Log.d("포지션", String.valueOf(i));
                            Log.d("포지션2", String.valueOf(ii));
                            dataPosition.setX(i);
                            dataPosition.setY(ii);
                            first.add(dataPosition);

                            canvas.drawCircle(i, ii, radius, paint);
                            iv.setImageBitmap(mutableBitmap);
                        }
                    }
                }
//                addView(true);

                //실패시
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_ROTATION, isRotation);
        outState.putBoolean(RDB96, rdb96.isChecked());
        outState.putBoolean(RDB384, rdb384.isChecked());
        outState.putInt(SELECTION_OPTION, selectOption);
        if (!isRotation && !isIntent) {
            outState.putByteArray(CAPTURE_RESIZE_BITMAP, captureResizeByte);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isRotation = savedInstanceState.getBoolean(IS_ROTATION);
        rdb96.setChecked(savedInstanceState.getBoolean(RDB96));
        rdb384.setChecked(savedInstanceState.getBoolean(RDB384));
        selectOption = savedInstanceState.getInt(SELECTION_OPTION);

        if (isRotation) {
            btn_take_picture.performClick();
        } else {
            this.captureResizeByte = savedInstanceState.getByteArray(CAPTURE_RESIZE_BITMAP);
            if (captureResizeByte != null) {
                this.captureResizeBitmap = byteArrayToBitmap(this.captureResizeByte);
                settingAfterTakePicture(this.captureResizeBitmap);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (frameLayout.getVisibility() == View.VISIBLE) {
            lo.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
        } else {
            frameLayout.removeView(cameraView);
            frameLayout.removeView(btnTag);
            if (lo.getVisibility() == View.VISIBLE && lo_buttons.getVisibility() == View.GONE) {
                iv.setVisibility(View.GONE);
                iv.setImageResource(0);
                lo_buttons.setVisibility(View.VISIBLE);

            } else {
                super.onBackPressed();

            }
        }
    }

    private void addView(Boolean boolean_check) {
        Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[0]));
        Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[1]));
        Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[2]));
        Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[3]));

        int width = (int) (getBitmapPositionInsideImageView(iv)[2]);
        int height = getBitmapPositionInsideImageView(iv)[3];
        Log.d("길이", String.valueOf(width));


        if (boolean_check == true) {

            int finalHeight, finalWidth;

            iv.post(new Runnable() {
                @Override
                public void run() {

                }
            });

        } else {
            Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[0]));
            Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[1]));
            Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[2]));
            Log.d("확인", String.valueOf(getBitmapPositionInsideImageView(iv)[3]));
            Log.d("길이", String.valueOf(getBitmapPositionInsideImageView(iv)[2] - getBitmapPositionInsideImageView(iv)[0]));
            Log.d("높이", String.valueOf(getBitmapPositionInsideImageView(iv)[3] - getBitmapPositionInsideImageView(iv)[1]));

            for (int i = 0; i <= width; i += width / 12) {
                Log.d("길이 나누기", String.valueOf(i));
            }
            for (int i = 0; i <= height; i += height / 12) {
                Log.d("높이 나누기", String.valueOf(i));
            }
        }
    }

    public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
        int[] ret = new int[4];

        if (imageView == null || imageView.getDrawable() == null)
            return ret;

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

// Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

// Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = (int) (d.getIntrinsicWidth());
        final int origH = (int) (d.getIntrinsicHeight());

// Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH) / 2;
        int left = (int) (imgViewW - actW) / 2;

        ret[0] = left;
        ret[1] = top;

        return ret;
    }

    private void performCrop(Uri picUri) {
//        Crop.activity(null).setGuidelines(CropImageView.Guidelines.ON).start(this);
//        try {
//
//
//            Intent cropIntent = new Intent("com.android.camera.action.CROP");
//
//
//            cropIntent.setDataAndType(picUri, "image/*");
//
//            cropIntent.putExtra("crop", "true");
//
//            cropIntent.putExtra("outputX", 200);
//            cropIntent.putExtra("outputY", 300);
//
//            //true 실제비트맵 false 사진바로저장
//            cropIntent.putExtra("return-data", false);
//            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
//            startActivityForResult(cropIntent, RESULT_CROP);
//        }
//
//        catch (ActivityNotFoundException anfe) {
//
//            String errorMessage = "your device doesn't support the crop action!";
//            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
//            toast.show();
//        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public void saveBitmapToCache(Bitmap bitmap) throws IOException {
        String filename = "final_image.jpg";
        File cacheFile = new File(getApplicationContext().getCacheDir(), filename);
        OutputStream out = new FileOutputStream(cacheFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, (int) 100, out);
        out.flush();
        out.close();
    }

    public void getRGB(ArrayList<Data> data) {
        Log.d(TAG, "RGB");
        rgb = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Data rgbdata = new Data();
//            Bitmap bitmap = ((BitmapDrawable) iv.getDrawable()).getBitmap();
            Bitmap bitmap = this.extractBitmap;
//
            int pixel = bitmap.getPixel(data.get(i).getX(), data.get(i).getY());

            //Reading colors
            int redValue = Color.red(pixel);
            int blueValue = Color.blue(pixel);
            int greenValue = Color.green(pixel);
            Log.d(TAG, "reading:" + data.get(i).getX() + " " + data.get(i).getY());
            rgbdata.setR(Color.red(pixel));
            rgbdata.setG(Color.green(pixel));
            rgbdata.setB(Color.blue(pixel));
            rgbdata.setX(data.get(i).getX());
            rgbdata.setY(data.get(i).getY());

            Log.d("알지1", String.valueOf(Color.red(bitmap.getPixel(data.get(i).getX(), data.get(i).getY()))));
            Log.d("알지2", String.valueOf(Color.green(bitmap.getPixel(data.get(i).getX(), data.get(i).getY()))));
            Log.d("알지3", String.valueOf(Color.blue(bitmap.getPixel(data.get(i).getX(), data.get(i).getY()))));

            rgb.add(rgbdata);
        }
    }

    private boolean requestPermissions() {
        return PermissionCheck.checkAndRequestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int result = PermissionCheck.onRequestPermissionsResult(this, requestCode, permissions, grantResults);

        if (result == PermissionCheck.RESULT_GRANTED) {
            settingViewClickListener();
        } else if (result == PermissionCheck.RESULT_NOT_GRANTED) {
            showDialogOK(getString(R.string.permission_not_allow),
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                requestPermissions();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                finish();
                                break;
                        }
                    });
        } else if (result == PermissionCheck.RESULT_DENIED) {

            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }


    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    class DrawCanvas extends View {
        Paint paint;
        int radius = 10;


        public DrawCanvas(Context context) {
            super(context);
            init();
        }

        public DrawCanvas(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public DrawCanvas(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        // 그리기에 필요한 요소를 초기화 한다.
        private void init() {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getResources().getColor(R.color.red));
            paint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (rdb96.isChecked()) {
                Log.d(TAG, "height : ");
                Log.d(TAG, "width : " + getWidth());
                int startY = frameLayout.getHeight() / 16;
                int startX = frameLayout.getWidth() / 24;
                for (int ii = startY; ii < frameLayout.getHeight(); ii += frameLayout.getHeight() / 8) {
                    for (int i = startX; i < frameLayout.getWidth(); i += frameLayout.getWidth() / 12) {
                        Data dataPosition = new Data();
                        Log.d("포지션", String.valueOf(i));
                        Log.d("포지션2", String.valueOf(ii));
                        dataPosition.setX(i);
                        dataPosition.setY(ii);
                        first.add(dataPosition);
                        canvas.drawCircle(i, ii, radius, paint);
                    }
                }
            } else {
                if (rdb384.isChecked()) {
                    Log.d(TAG, "height : ");
                    Log.d(TAG, "width : " + getWidth());
                    int startY = frameLayout.getHeight() / 32;
                    int startX = frameLayout.getWidth() / 48;
                    for (int ii = startY; ii < frameLayout.getHeight(); ii += frameLayout.getHeight() / 16) {
                        for (int i = startX; i < frameLayout.getWidth(); i += frameLayout.getWidth() / 24) {
                            Data dataPosition = new Data();
                            Log.d("포지션", String.valueOf(i));
                            Log.d("포지션2", String.valueOf(ii));
                            dataPosition.setX(i);
                            dataPosition.setY(ii);
                            first.add(dataPosition);
                            canvas.drawCircle(i, ii, radius, paint);
                        }
                    }
                }
            }
        }
    }

    private void captureImage() {
        cameraView.capture(new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 8;
                Bitmap caputerBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//data array안에 있는 데이터 불러와서 비트맵에 저장

                int width = caputerBitmap.getWidth();
                int height = caputerBitmap.getHeight();
//                int newWidth = 200;
//                int newHeight = 200;
//
//                float scaleWidth = ((float) newWidth) / width;
//                float scaleHeight = ((float) newHeight) / height;
//
                Matrix matrix = new Matrix();
//                matrix.postScale(scaleWidth, scaleHeight);
//                matrix.postRotate(90);

                Bitmap resizedBitmap = Bitmap.createBitmap(caputerBitmap, 0, 0, width, height, matrix, true);
                BitmapDrawable bmd = new BitmapDrawable(resizedBitmap);

                captureResizeByte = bitmapToByteArray(resizedBitmap);
                isRotation = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
    }

    private void settingAfterTakePicture(Bitmap captureResizeBitmap) {
        Log.d(TAG, "settingAfterTakePicture");
        first = new ArrayList<>();
        isSetting = true;
        frameLayout.setVisibility(View.INVISIBLE);
        lo.setVisibility(View.VISIBLE);
        iv.setVisibility(View.VISIBLE);
        iv.setImageDrawable(new BitmapDrawable(captureResizeBitmap));//이미지뷰에 사진 보여주기
//        camera.startPreview();

        Bitmap bitmap = ((BitmapDrawable) iv.getDrawable()).getBitmap();
        extractBitmap = bitmap;
        Log.d("데이터하이트", String.valueOf(bitmap.getHeight()));
        Log.d("데이터하이트", String.valueOf(bitmap.getWidth()));
        Paint paint = new Paint();
        int radius;
        radius = 10;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.red));
        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        if (selectOption == 0) {
            int startY = bitmap.getHeight() / 16;
            int startX = bitmap.getWidth() / 24;
            for (int ii = startY; ii < bitmap.getHeight(); ii += bitmap.getHeight() / 8) {
                for (int i = startX; i < bitmap.getWidth(); i += bitmap.getWidth() / 12) {
                    Data dataPosition = new Data();
                    Log.d("포지션", String.valueOf(i));
                    Log.d("포지션2", String.valueOf(ii));
                    dataPosition.setX(i);
                    dataPosition.setY(ii);
                    first.add(dataPosition);

                    canvas.drawCircle(i, ii, radius, paint);
                    iv.setImageBitmap(mutableBitmap);
                }
            }
        } else if (selectOption == 1) {
            int startY = bitmap.getHeight() / 32;
            int startX = bitmap.getWidth() / 48;
            for (int ii = startY; ii < bitmap.getHeight(); ii += bitmap.getHeight() / 16) {
                for (int i = startX; i < bitmap.getWidth(); i += bitmap.getWidth() / 24) {
                    Data dataPosition = new Data();
                    Log.d("포지션", String.valueOf(i));
                    Log.d("포지션2", String.valueOf(ii));
                    dataPosition.setX(i);
                    dataPosition.setY(ii);
                    first.add(dataPosition);

                    canvas.drawCircle(i, ii, radius, paint);
                    iv.setImageBitmap(mutableBitmap);
                }
            }
        }
    }

    // Bitmap을 Byte로 변환
    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    // Byte를 Bitmap으로 변환
    private Bitmap byteArrayToBitmap(byte[] byteArray) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return bitmap;
    }
}