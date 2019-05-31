package ru.azzzi.less3android3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    public static final int DELAY_MILLIS = 5000;
    String pngFileName = "imageFile.png";
    private int PICK_IMAGE_REQUEST = 1;
    private Handler handler;
    private boolean stopDid = false;
    private TextView textView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.select_image).setOnClickListener(view -> selectImage());
        textView = findViewById(R.id.textView);
        textView.setOnClickListener(v -> setStopConvert());
        progressBar = findViewById(R.id.progressBar);
    }

    private void setStopConvert() {
        stopDid = true;
        Toast.makeText(this, "Конвертация отменена!", Toast.LENGTH_SHORT).show();
        textView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null
        ) {
            textView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            //даем пользователю 5 сек на отмену
            handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!stopDid) {
                        File file = new File(getFilesDir(), pngFileName);

                        Disposable d = getCompletable(data.getData(), file)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(new DisposableCompletableObserver() {
                                    @Override
                                    public void onComplete() {
                                        showSuccess();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        showError(e.getLocalizedMessage());
                                    }
                                });
                    }
                    if (stopDid) {
                        stopDid = false;
                    }
                }

            }, DELAY_MILLIS);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showSuccess() {
        Toast.makeText(this, "converted and save", Toast.LENGTH_SHORT).show();
        textView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void selectImage() {
        Intent myIntent = new Intent(Intent.ACTION_PICK);
        myIntent.setType("image/*");
        startActivityForResult(myIntent, PICK_IMAGE_REQUEST);
    }

    private Completable getCompletable(Uri uri, File file) {
        return Completable.create(emitter -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 75, fos);
                fos.flush();
                fos.close();
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }


}
