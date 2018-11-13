package ung.csci.fierolabeler;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;


public class MainActivityController extends AppCompatActivity {

    private ImageView fieroImageView;
    private Button button1, button2, button3, labelButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fieroImageView = findViewById(R.id.fieroImageView);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        labelButton = findViewById(R.id.labelButton);

        //set default image to prevent crashing
        fieroImageView.setImageResource(R.drawable.vl1987);

        labelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap = ((BitmapDrawable)fieroImageView.getDrawable()).getBitmap();
                try {
                    runLabeler(bitmap);
                    Toast waiting = Toast.makeText(getApplicationContext(), R.string.labelingProcess, Toast.LENGTH_LONG);
                    waiting.setGravity(0,0,0);
                    waiting.show();
                } catch (FirebaseMLException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error with interpreter" + e, Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    // configure the local source and start running the classifier.
    private void runLabeler(Bitmap bitmap) throws FirebaseMLException {

        FirebaseLocalModelSource localSource = new FirebaseLocalModelSource.Builder("fiero")
                .setAssetFilePath("fiero_model.tflite")
                .build();

        FirebaseModelManager manager = FirebaseModelManager.getInstance();
        manager.registerLocalModelSource(localSource);

        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setLocalModelName("fiero")
                .build();

        FirebaseModelInterpreter mInterpreter = FirebaseModelInterpreter.getInstance(options);
            // the 1,224,224,3 comes from the tflite model.
            FirebaseModelInputOutputOptions inputOutputOptions = new FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 9})
                    .build();

            int batchNum = 0;

            float[][][][] input = new float[1][224][224][3];

            for (int i = 0; i < 224; i++) {
                for (int j = 0; j < 224; j++) {
                    // get the pixels from the jpg in the fieroImageView.
                    int pixel = bitmap.getPixel(i, j);
                    // Normalize channel values
                    input[batchNum][i][j][0] = Color.red(pixel) / 255.0f;
                    input[batchNum][i][j][1] = Color.green(pixel) / 255.0f;
                    input[batchNum][i][j][2] = Color.blue(pixel) / 255.0f;
                }
            }
                FirebaseModelInputs inputs = new FirebaseModelInputs.Builder().add(input).build();
        if (mInterpreter != null) {
            mInterpreter.run(inputs, inputOutputOptions)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseModelOutputs>() {
                        @Override
                        public void onSuccess(FirebaseModelOutputs result) {
                            float[][] output = result.getOutput(0);
                            float[] likelihood = output[0];
                            Toast.makeText(getApplicationContext(), R.string.deviceResult, Toast.LENGTH_LONG).show();
                            try {
                                outputLabels(likelihood);
                                Toast.makeText(getApplicationContext(), R.string.deviceResult, Toast.LENGTH_LONG).show();
                           } catch (IOException e) {
                                    Toast.makeText(MainActivityController.this,
                                            String.format(getString(R.string.labelingFailure1), e), Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivityController.this,
                                    String.format(getString(R.string.labelingFailure3), e), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.labelingFailure4), Toast.LENGTH_LONG).show();
        }
    }


    private void outputLabels(float[] likelihood) throws IOException {
        // First step to classify image.
        BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets()
                .open("retrained_labels.txt")));
        for (int i = 0; i < likelihood.length; i++) {
            String label = reader.readLine();
            //Log.i("MLKit", String.format("%s: %1.4f", label, likelihood[k]));
            Toast.makeText(getApplicationContext(), R.string.deviceResult + label + Arrays.toString(likelihood), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity_controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, LiveActivityController.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void changeImage(View view) {
        Bitmap se1984 = BitmapFactory.decodeResource(getResources(), R.drawable.se1984);
        Bitmap vl1987 = BitmapFactory.decodeResource(getResources(), R.drawable.vl1987);
        Bitmap gt1988 = BitmapFactory.decodeResource(getResources(), R.drawable.gt1988);

        if (button1.isPressed()) {
            fieroImageView.setImageBitmap(se1984);
        }
        else if (button2.isPressed()) {
            fieroImageView.setImageBitmap(vl1987);
        }
        else if (button3.isPressed()) {
            fieroImageView.setImageBitmap(gt1988);
        }
    }
}
